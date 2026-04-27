package com.lab.file.fileservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lab.file.common.exception.BusinessException;
import com.lab.file.fileservice.config.CustomMinioClient;
import com.lab.file.fileservice.entity.FileChunk;
import com.lab.file.fileservice.entity.FileEntity;
import com.lab.file.fileservice.mapper.FileChunkMapper;
import com.lab.file.fileservice.mapper.FileMapper;
import com.lab.file.fileservice.service.IFileChunkService;
import io.minio.*;
import io.minio.messages.CompleteMultipartUpload;
import io.minio.messages.Part;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FileChunkServiceImpl extends ServiceImpl<FileChunkMapper, FileChunk> implements IFileChunkService {

    @Autowired
    private CustomMinioClient minioClient;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private FileMapper fileMapper;

    private String bucketName;

    private static final String UPLOAD_PREFIX = "upload:";
    private static final String CHUNK_PREFIX = "chunk:";
    private static final long UPLOAD_EXPIRE_HOURS = 24;

    @Override
    public String initUpload(String fileName, String md5, Integer totalChunks, Long userId) {
        try {
            ensureBucketExists();
            String objectName = generateObjectName(md5, fileName);

            // ✅ 修复：创建分片上传（9.x 正确用法）
            CreateMultipartUploadResponse response = minioClient.createMultipartUpload(
                    CreateMultipartUploadArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
            String uploadId = response.result().uploadId();

            // 保存Redis
            String uploadKey = UPLOAD_PREFIX + md5;
            Map<String, Object> uploadInfo = new HashMap<>();
            uploadInfo.put("uploadId", uploadId);
            uploadInfo.put("fileName", fileName);
            uploadInfo.put("totalChunks", totalChunks);
            uploadInfo.put("userId", userId);
            uploadInfo.put("objectName", objectName);
            uploadInfo.put("createTime", System.currentTimeMillis());

            redisTemplate.opsForHash().putAll(uploadKey, uploadInfo);
            redisTemplate.expire(uploadKey, UPLOAD_EXPIRE_HOURS, TimeUnit.HOURS);

            log.info("初始化分片上传成功: uploadId={}", uploadId);
            return uploadId;
        } catch (Exception e) {
            log.error("初始化上传失败", e);
            throw new BusinessException("初始化上传失败");
        }
    }

    @Override
    public String uploadChunk(String md5, String uploadId, Integer partNumber, MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            String uploadKey = UPLOAD_PREFIX + md5;
            String objectName = (String) redisTemplate.opsForHash().get(uploadKey, "objectName");

            if (objectName == null) {
                throw new BusinessException("上传会话已过期");
            }

            // ✅ 修复：uploadPart（9.x 唯一正确写法）
            UploadPartResponse response = minioClient.uploadPart(
                    UploadPartArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .uploadId(uploadId)
                            .partNumber(partNumber)
                            .stream(inputStream, file.getSize(), -1)
                            .build()
            );
            String etag = response.etag();

            // 保存分片记录
            FileChunk chunk = new FileChunk();
            chunk.setChunkIdentifier(md5);
            chunk.setUploadId(uploadId);
            chunk.setChunkNumber(partNumber);
            chunk.setEtag(etag);
            chunk.setChunkSize(file.getSize());
            chunk.setCreateTime(LocalDateTime.now());
            chunk.setStatus(1);
            this.save(chunk);

            // Redis缓存
            String chunkKey = CHUNK_PREFIX + md5 + ":" + uploadId;
            redisTemplate.opsForHash().put(chunkKey, String.valueOf(partNumber), etag);
            redisTemplate.expire(chunkKey, UPLOAD_EXPIRE_HOURS, TimeUnit.HOURS);

            return etag;
        } catch (Exception e) {
            log.error("上传分片失败", e);
            throw new BusinessException("上传分片失败");
        }
    }

    @Override
    public Map<Integer, String> getUploadedChunks(String md5, String uploadId) {
        Map<Integer, String> map = new HashMap<>();
        try {
            String uploadKey = UPLOAD_PREFIX + md5;
            String objectName = (String) redisTemplate.opsForHash().get(uploadKey, "objectName");

            // ✅ 修复：listParts（9.x 正确用法）
            ListPartsResponse response = minioClient.listParts(
                    ListPartsArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .uploadId(uploadId)
                            .maxParts(1000)
                            .build()
            );

            for (Part part : response.result().partList()) {
                map.put(part.partNumber(), part.etag());
            }
        } catch (Exception e) {
            log.warn("从MinIO获取分片失败，降级查询DB", e);
        }

        // DB兜底
        if (map.isEmpty()) {
            List<FileChunk> list = this.list(new QueryWrapper<FileChunk>()
                    .eq("chunk_identifier", md5)
                    .eq("upload_id", uploadId)
                    .eq("status", 1));
            for (FileChunk chunk : list) {
                map.put(chunk.getChunkNumber(), chunk.getEtag());
            }
        }
        return map;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileEntity mergeChunks(String md5, String uploadId, String fileName,
                                  Long userId, Long parentId, String description) {
        try {
            String uploadKey = UPLOAD_PREFIX + md5;
            String objectName = (String) redisTemplate.opsForHash().get(uploadKey, "objectName");
            Integer totalChunks = (Integer) redisTemplate.opsForHash().get(uploadKey, "totalChunks");

            if (objectName == null || totalChunks == null) {
                throw new BusinessException("上传会话已过期");
            }

            // 获取所有分片
            ListPartsResponse partsResp = minioClient.listParts(
                    ListPartsArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .uploadId(uploadId)
                            .build()
            );
            List<Part> parts = partsResp.result().partList();

            if (parts.size() != totalChunks) {
                throw new BusinessException("分片未完整上传：" + parts.size() + "/" + totalChunks);
            }

            // 排序
            List<Part> sortedParts = parts.stream()
                    .sorted(Comparator.comparingInt(Part::partNumber))
                    .collect(Collectors.toList());

            // ✅ 修复：completeMultipartUpload（9.x 最关键修复点）
            minioClient.completeMultipartUpload(
                    CompleteMultipartUploadArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .uploadId(uploadId)
                            .parts(sortedParts) // 直接传入List<Part>
                            .build()
            );

            // 移动到正式目录
            String finalName = generateFinalObjectName(userId, fileName, md5);
            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .bucket(bucketName)
                            .object(finalName)
                            .source(CopySource.builder().bucket(bucketName).object(objectName).build())
                            .build()
            );
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());

            // 文件信息
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder().bucket(bucketName).object(finalName).build()
            );

            // 入库
            FileEntity file = new FileEntity();
            file.setFileName(fileName);
            file.setFileSize(stat.size());
            file.setFileMd5(md5);
            file.setFilePath(finalName);
            file.setFileType(getFileExtension(fileName));
            file.setUserId(userId);
            file.setParentId(parentId);
            file.setDescription(description);
            file.setStatus(1);
            file.setCreateTime(LocalDateTime.now());
            file.setUpdateTime(LocalDateTime.now());
            fileMapper.insert(file);

            // 清理
            cleanupUploadCache(md5, uploadId);
            return file;

        } catch (Exception e) {
            log.error("合并文件失败", e);
            try { cancelUpload(md5, uploadId); } catch (Exception ex) { /* ignore */ }
            throw new BusinessException("文件合并失败：" + e.getMessage());
        }
    }

    @Override
    public void cancelUpload(String md5, String uploadId) {
        try {
            String uploadKey = UPLOAD_PREFIX + md5;
            String objectName = (String) redisTemplate.opsForHash().get(uploadKey, "objectName");

            if (objectName != null) {
                // ✅ 修复：取消分片上传
                minioClient.abortMultipartUpload(
                        AbortMultipartUploadArgs.builder()
                                .bucket(bucketName)
                                .object(objectName)
                                .uploadId(uploadId)
                                .build()
                );
            }
            cleanupUploadCache(md5, uploadId);
        } catch (Exception e) {
            throw new BusinessException("取消上传失败");
        }
    }

    @Override
    public FileEntity checkFileExists(String md5, Long userId) {
        // 你可以在这里实现秒传功能：根据MD5查询库中是否存在
        return null;
    }

    // ==================== 工具方法 ====================
    private void ensureBucketExists() throws Exception {
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }
    }

    private String generateObjectName(String md5, String fileName) {
        return "temp/" + md5 + "/" + UUID.randomUUID();
    }

    private String generateFinalObjectName(Long userId, String fileName, String md5) {
        String ext = getFileExtension(fileName);
        return String.format("user/%d/%s/%s.%s",
                userId,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd")),
                md5, ext);
    }

    private String getFileExtension(String fileName) {
        int dot = fileName.lastIndexOf(".");
        return dot > 0 ? fileName.substring(dot + 1) : "unknown";
    }

    private void cleanupUploadCache(String md5, String uploadId) {
        redisTemplate.delete(UPLOAD_PREFIX + md5);
        redisTemplate.delete(CHUNK_PREFIX + md5 + ":" + uploadId);
        this.remove(new QueryWrapper<FileChunk>()
                .eq("chunk_identifier", md5)
                .eq("upload_id", uploadId));
    }
}