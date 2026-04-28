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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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

    // ========== 辅助方法 ==========

    private String buildUploadKey(Long userId, String md5) {
        return UPLOAD_PREFIX + userId + ":" + md5;
    }

    /**
     * 从 Redis 获取 objectName，若 Redis 中不存在则尝试从 DB 恢复。
     * DB 恢复需要 chunkPath 字段已存储 objectName（uploadChunk 中负责写入）。
     */
    private String getUploadObjectName(String md5, Long userId) {
        String uploadKey = buildUploadKey(userId, md5);
        String objectName = (String) redisTemplate.opsForHash().get(uploadKey, "objectName");
        if (objectName != null) {
            return objectName;
        }
        // Redis 中不存在，尝试从 DB 恢复
        FileChunk chunk = this.lambdaQuery()
                .eq(FileChunk::getChunkIdentifier, md5)
                .eq(FileChunk::getUserId, userId)
                .eq(FileChunk::getStatus, 1)
                .last("LIMIT 1")
                .one();
        if (chunk != null && chunk.getChunkPath() != null) {
            objectName = chunk.getChunkPath();
            log.info("Recovered objectName from DB: {}", objectName);
            // 恢复 Redis 缓存
            redisTemplate.opsForHash().put(uploadKey, "objectName", objectName);
            redisTemplate.expire(uploadKey, UPLOAD_EXPIRE_HOURS, TimeUnit.HOURS);
            return objectName;
        }
        throw new BusinessException("upload session expired");
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

    private void cleanupUploadCache(String md5, String uploadId, Long userId) {
        String uploadKey = buildUploadKey(userId, md5);
        redisTemplate.delete(uploadKey);
        redisTemplate.delete(CHUNK_PREFIX + md5 + ":" + uploadId);
        // 保留数据库分片记录，不物理删除，方便追溯和断点续传
    }

    /**
     * 验证 MinIO 中的分片上传是否仍然有效
     */
    private boolean isUploadValid(String objectName, String uploadId) {
        try {
            customMinioClient.listPartsSync(bucketName, objectName, uploadId);
            return true;
        } catch (Exception e) {
            log.warn("Multipart upload no longer valid in MinIO: object={}, uploadId={}", objectName, uploadId);
            return false;
        }
    }

    // ========== 核心业务方法 ==========

    @Override
    public String initUpload(String fileName, String md5, Integer totalChunks, Long userId) {
        String uploadKey = buildUploadKey(userId, md5);

        // 1. 检查 Redis 中是否存在活跃 session
        String existingUploadId = (String) redisTemplate.opsForHash().get(uploadKey, "uploadId");
        if (existingUploadId != null) {
            log.info("Reusing existing upload session from Redis, uploadId={}", existingUploadId);
            redisTemplate.opsForHash().put(uploadKey, "fileName", fileName);
            redisTemplate.opsForHash().put(uploadKey, "totalChunks", totalChunks);
            redisTemplate.expire(uploadKey, UPLOAD_EXPIRE_HOURS, TimeUnit.HOURS);
            return existingUploadId;
        }

        // 2. Redis 中没有，尝试从 DB 恢复（页面刷新后 Redis 丢失的场景）
        FileChunk existingChunk = this.lambdaQuery()
                .eq(FileChunk::getChunkIdentifier, md5)
                .eq(FileChunk::getUserId, userId)
                .eq(FileChunk::getStatus, 1)
                .last("LIMIT 1")
                .one();
        if (existingChunk != null && existingChunk.getUploadId() != null && existingChunk.getChunkPath() != null) {
            String recoverUploadId = existingChunk.getUploadId();
            String recoverObjectName = existingChunk.getChunkPath();

            // 验证 MinIO 中的上传是否仍然有效
            if (isUploadValid(recoverObjectName, recoverUploadId)) {
                log.info("Recovered upload session from DB, uploadId={}", recoverUploadId);
                Map<String, Object> uploadInfo = new HashMap<>();
                uploadInfo.put("uploadId", recoverUploadId);
                uploadInfo.put("fileName", fileName);
                uploadInfo.put("totalChunks", totalChunks);
                uploadInfo.put("userId", userId);
                uploadInfo.put("objectName", recoverObjectName);
                uploadInfo.put("createTime", System.currentTimeMillis());
                redisTemplate.opsForHash().putAll(uploadKey, uploadInfo);
                redisTemplate.expire(uploadKey, UPLOAD_EXPIRE_HOURS, TimeUnit.HOURS);
                return recoverUploadId;
            } else {
                // MinIO 中的上传已失效，清理旧的 DB 记录，重新创建上传
                log.warn("Previous multipart upload expired, removing old chunk records and creating new one");
                this.remove(new QueryWrapper<FileChunk>()
                        .eq("chunk_identifier", md5)
                        .eq("user_id", userId));
            }
        }

        // 3. 没有可恢复的 session，创建全新的分片上传
        try {
            String objectName = generateObjectName(md5);
            String uploadId = customMinioClient.createMultipartUploadSync(bucketName, objectName);

            Map<String, Object> uploadInfo = new HashMap<>();
            uploadInfo.put("uploadId", uploadId);
            uploadInfo.put("fileName", fileName);
            uploadInfo.put("totalChunks", totalChunks);
            uploadInfo.put("userId", userId);
            uploadInfo.put("objectName", objectName);
            uploadInfo.put("createTime", System.currentTimeMillis());

            redisTemplate.opsForHash().putAll(uploadKey, uploadInfo);
            redisTemplate.expire(uploadKey, UPLOAD_EXPIRE_HOURS, TimeUnit.HOURS);

            log.info("Initialized new multipart upload, uploadId={}, objectName={}", uploadId, objectName);
            return uploadId;
        } catch (Exception e) {
            log.error("Failed to initialize multipart upload", e);
            throw new BusinessException("init multipart upload failed: " + e.getMessage());
        }
    }

    @Override
    public String uploadChunk(String md5, String uploadId, Integer partNumber, Long totalChunks, MultipartFile file, Long userId) {
        try (InputStream inputStream = file.getInputStream()) {
            String objectName = getUploadObjectName(md5, userId);

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
            chunk.setChunkPath(objectName); // 记录 objectName 以便后续恢复 session
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
    public Map<Integer, String> getUploadedChunks(String md5, String uploadId, Long userId) {
        Map<Integer, String> map = new HashMap<>();
        try {
            String objectName = getUploadObjectName(md5, userId);
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
    public List<Integer> getMissingChunks(String md5, String uploadId, Integer totalChunks, Long userId) {
        Map<Integer, String> uploaded = getUploadedChunks(md5, uploadId, userId);
        List<Integer> missing = new ArrayList<>();
        for (int i = 1; i <= totalChunks; i++) {
            if (!uploaded.containsKey(i)) {
                missing.add(i);
            }
        }
        return missing;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileEntity mergeChunks(String md5, String uploadId, String fileName,
                                  Long userId, Long parentId, String description) {
        try {
            String objectName = getUploadObjectName(md5, userId);
            String uploadKey = buildUploadKey(userId, md5);
            Integer totalChunks = (Integer) redisTemplate.opsForHash().get(uploadKey, "totalChunks");
            if (totalChunks == null) {
                // 尝试从 DB 恢复 totalChunks
                FileChunk chunk = this.lambdaQuery()
                        .eq(FileChunk::getChunkIdentifier, md5)
                        .eq(FileChunk::getUploadId, uploadId)
                        .last("LIMIT 1")
                        .one();
                if (chunk != null) {
                    totalChunks = chunk.getTotalChunks().intValue();
                }
            }
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

            // 生成最终文件路径并复制
            String finalObjectPath = generateFinalObjectName(userId, fileName, md5);
            customMinioClient.copyObjectSync(bucketName, objectName, finalObjectPath);
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

            cleanupUploadCache(md5, uploadId, userId);
            return file;
        } catch (Exception e) {
            log.error("Failed to merge chunks", e);
            try {
                cancelUpload(md5, uploadId, userId);
            } catch (Exception ignored) {
                log.warn("Failed to cancel multipart upload after merge failure", ignored);
            }
            throw new BusinessException("merge chunks failed: " + e.getMessage());
        }
    }

    @Override
    public void cancelUpload(String md5, String uploadId, Long userId) {
        try {
            String objectName = null;
            try {
                objectName = getUploadObjectName(md5, userId);
            } catch (Exception e) {
                log.warn("Could not get objectName for cancel, skipping MinIO abort", e);
            }
            if (objectName != null) {
                customMinioClient.abortMultipartUploadSync(bucketName, objectName, uploadId);
            }
            cleanupUploadCache(md5, uploadId, userId);
        } catch (Exception e) {
            throw new BusinessException("cancel upload failed: " + e.getMessage());
        }
    }

    @Override
    public FileEntity checkFileExists(String md5, Long userId) {
        FileEntity fileEntity = new FileEntity();
        fileEntity.setUserId(userId);
        fileEntity.setFileMd5(md5);
        List<FileEntity> existingFiles = fileMapper.selectList(new QueryWrapper<>(fileEntity));

        if (!existingFiles.isEmpty()) {
            return existingFiles.get(0);
        }

        return null;
    }
}
