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
import software.amazon.awssdk.services.s3.model.Part;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FileChunkServiceImpl extends ServiceImpl<FileChunkMapper, FileChunk> implements IFileChunkService {

    private static final String UPLOAD_PREFIX = "upload:";
    private static final String CHUNK_PREFIX = "chunk:";
    private static final long UPLOAD_EXPIRE_HOURS = 24;

    @Autowired
    private CustomMinioClient customMinioClient;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private FileMapper fileMapper;

    @Value("${minio.bucket}")
    private String bucketName;

    @Override
    public String initUpload(String fileName, String md5, Integer totalChunks, Long userId) {
        try {
            String objectName = generateObjectName(md5);

            String uploadId = customMinioClient.createMultipartUploadSync(bucketName, objectName);

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

            log.info("Initialized multipart upload, uploadId={}", uploadId);
            return uploadId;
        } catch (Exception e) {
            log.error("Failed to initialize multipart upload", e);
            throw new BusinessException("init multipart upload failed: " + e.getMessage());
        }
    }

    @Override
    public String uploadChunk(String md5, String uploadId, Integer partNumber, Long totalChunks,  MultipartFile file, Long userId) {
        try (InputStream inputStream = file.getInputStream()) {
            String objectName = getUploadObjectName(md5);

            UploadPartResponse response = customMinioClient.uploadPartSync(
                bucketName,
                objectName,
                uploadId,
                partNumber,
                inputStream,
                file.getSize()
            );

            String etag = response.eTag();

            FileChunk chunk = new FileChunk();
            chunk.setUserId(userId);
            chunk.setFileName(file.getName());
            chunk.setTotalChunks(totalChunks);
            chunk.setChunkIdentifier(md5);
            chunk.setUploadId(uploadId);
            chunk.setChunkNumber(partNumber);
            chunk.setEtag(etag);
            chunk.setChunkSize(file.getSize());
            chunk.setCreateTime(LocalDateTime.now());
            chunk.setStatus(1);
            this.save(chunk);

            String chunkKey = CHUNK_PREFIX + md5 + ":" + uploadId;
            redisTemplate.opsForHash().put(chunkKey, String.valueOf(partNumber), etag);
            redisTemplate.expire(chunkKey, UPLOAD_EXPIRE_HOURS, TimeUnit.HOURS);

            return etag;
        } catch (Exception e) {
            log.error("Failed to upload chunk", e);
            throw new BusinessException("upload chunk failed: " + e.getMessage());
        }
    }

    @Override
    public Map<Integer, String> getUploadedChunks(String md5, String uploadId) {
        Map<Integer, String> map = new HashMap<>();
        try {
            String objectName = getUploadObjectName(md5);
            List<Part> parts = customMinioClient.listPartsSync(bucketName, objectName, uploadId);
            for (Part part : parts) {
                map.put(part.partNumber(), part.eTag());
            }
        } catch (Exception e) {
            log.warn("Failed to list uploaded chunks from MinIO, fallback to database", e);
        }

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
            String objectName = getUploadObjectName(md5);
            Integer totalChunks = (Integer) redisTemplate.opsForHash().get(UPLOAD_PREFIX + md5, "totalChunks");
            if (totalChunks == null) {
                throw new BusinessException("upload session expired");
            }

            List<Part> parts = customMinioClient.listPartsSync(bucketName, objectName, uploadId);
            if (parts.size() != totalChunks) {
                throw new BusinessException("chunks incomplete: " + parts.size() + "/" + totalChunks);
            }

            // 完成分片上传
            customMinioClient.completeMultipartUploadSync(bucketName, objectName, uploadId, parts);

            // 计算总大小
            List<FileChunk> chunkList = this.list(new QueryWrapper<FileChunk>()
                    .eq("chunk_identifier", md5)
                    .eq("upload_id", uploadId));
            long totalSize = chunkList.stream().mapToLong(FileChunk::getChunkSize).sum();

            // 生成最终文件路径
            String finalObjectPath = generateFinalObjectName(userId, fileName, md5);

            // 将临时目录下的文件复制到用户正式目录
            customMinioClient.copyObjectSync(bucketName, objectName, finalObjectPath);
            // 删除临时目录下的文件
            customMinioClient.deleteObjectSync(bucketName, objectName);

            FileEntity file = new FileEntity();
            file.setFileName(fileName);
            file.setOriginalName(fileName);
            file.setFileSize(totalSize);
            file.setFileMd5(md5);
            file.setFilePath(finalObjectPath);
            file.setFileType(getFileExtension(fileName));
            file.setFileExtension(getFileExtension(fileName));
            file.setUserId(userId);
            file.setParentId(parentId);
            file.setDescription(description);
            file.setStatus(1);
            file.setCreateTime(LocalDateTime.now());
            file.setUpdateTime(LocalDateTime.now());

            fileMapper.insert(file);

            cleanupUploadCache(md5, uploadId);
            return file;
        } catch (Exception e) {
            log.error("Failed to merge chunks", e);
            try {
                cancelUpload(md5, uploadId);
            } catch (Exception ignored) {
                log.warn("Failed to cancel multipart upload after merge failure", ignored);
            }
            throw new BusinessException("merge chunks failed: " + e.getMessage());
        }
    }

    @Override
    public void cancelUpload(String md5, String uploadId) {
        try {
            String objectName = (String) redisTemplate.opsForHash().get(UPLOAD_PREFIX + md5, "objectName");
            if (objectName != null) {
                customMinioClient.abortMultipartUploadSync(bucketName, objectName, uploadId);
            }
            cleanupUploadCache(md5, uploadId);
        } catch (Exception e) {
            throw new BusinessException("cancel upload failed: " + e.getMessage());
        }
    }

    @Override
    public FileEntity checkFileExists(String md5, Long userId) {
        // 根据文件MD5查找数据库中是否已存在该文件
        FileEntity fileEntity = new FileEntity();
        fileEntity.setFileMd5(md5);
        List<FileEntity> existingFiles = fileMapper.selectList(new QueryWrapper<>(fileEntity));

        if (!existingFiles.isEmpty()) {
            // 如果有相同MD5的文件，返回第一个匹配的结果
            return existingFiles.get(0);
        }

        return null; // 文件不存在，可以进行上传
    }

    private String getUploadObjectName(String md5) {
        String objectName = (String) redisTemplate.opsForHash().get(UPLOAD_PREFIX + md5, "objectName");
        if (objectName == null) {
            throw new BusinessException("upload session expired");
        }
        return objectName;
    }

    private String generateObjectName(String md5) {
        return "temp/" + md5 + "/" + UUID.randomUUID().toString();
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
