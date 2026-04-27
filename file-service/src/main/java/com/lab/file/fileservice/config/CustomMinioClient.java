package com.lab.file.fileservice.config;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 自定义Minio客户端，使用S3兼容API实现分片上传操作。
 * <p>
 * MinIO Java SDK 8.x 未暴露分片上传方法（9.x 才支持），而 MinIO 完全兼容 S3 协议，
 * 因此本类使用 AWS S3 SDK 的 {@link S3Client} 来实现分片上传。
 */
@Slf4j
public class CustomMinioClient {

    private final S3Client s3Client;

    public CustomMinioClient(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    // ========== 分片上传相关方法 ==========

    /**
     * 创建分片上传
     */
    public String createMultipartUpload(String bucket, String objectName) {
        CreateMultipartUploadRequest request = CreateMultipartUploadRequest.builder()
                .bucket(bucket)
                .key(objectName)
                .build();
        return s3Client.createMultipartUpload(request).uploadId();
    }

    /**
     * 上传分片
     */
    public UploadPartResponse uploadPart(String bucket, String objectName, String uploadId,
                                         int partNumber, InputStream stream, long partSize) {
        UploadPartRequest request = UploadPartRequest.builder()
                .bucket(bucket)
                .key(objectName)
                .uploadId(uploadId)
                .partNumber(partNumber)
                .build();
        return s3Client.uploadPart(request, RequestBody.fromInputStream(stream, partSize));
    }

    /**
     * 查询已上传分片列表
     */
    public List<Part> listParts(String bucket, String objectName, String uploadId) {
        ListPartsRequest request = ListPartsRequest.builder()
                .bucket(bucket)
                .key(objectName)
                .uploadId(uploadId)
                .build();
        return s3Client.listParts(request).parts();
    }

    /**
     * 合并分片
     */
    public void completeMultipartUpload(String bucket, String objectName,
                                        String uploadId, List<Part> parts) {
        List<CompletedPart> completedParts = parts.stream()
                .map(p -> CompletedPart.builder()
                        .partNumber(p.partNumber())
                        .eTag(p.eTag())
                        .build())
                .collect(Collectors.toList());

        CompletedMultipartUpload completedUpload = CompletedMultipartUpload.builder()
                .parts(completedParts)
                .build();

        CompleteMultipartUploadRequest request = CompleteMultipartUploadRequest.builder()
                .bucket(bucket)
                .key(objectName)
                .uploadId(uploadId)
                .multipartUpload(completedUpload)
                .build();

        s3Client.completeMultipartUpload(request);
    }

    /**
     * 取消分片上传
     */
    public void abortMultipartUpload(String bucket, String objectName, String uploadId) {
        AbortMultipartUploadRequest request = AbortMultipartUploadRequest.builder()
                .bucket(bucket)
                .key(objectName)
                .uploadId(uploadId)
                .build();
        s3Client.abortMultipartUpload(request);
    }

    // ========== 业务便捷方法（同步包装，自动处理异常） ==========

    public String createMultipartUploadSync(String bucket, String objectName) {
        try {
            return createMultipartUpload(bucket, objectName);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create multipart upload: " + e.getMessage(), e);
        }
    }

    public UploadPartResponse uploadPartSync(String bucket, String objectName, String uploadId,
                                             int partNumber, InputStream stream, long partSize) {
        try {
            return uploadPart(bucket, objectName, uploadId, partNumber, stream, partSize);
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload part: " + e.getMessage(), e);
        }
    }

    public List<Part> listPartsSync(String bucket, String objectName, String uploadId) {
        try {
            return listParts(bucket, objectName, uploadId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to list parts: " + e.getMessage(), e);
        }
    }

    public void completeMultipartUploadSync(String bucket, String objectName,
                                            String uploadId, List<Part> parts) {
        try {
            completeMultipartUpload(bucket, objectName, uploadId, parts);
        } catch (Exception e) {
            throw new RuntimeException("Failed to complete multipart upload: " + e.getMessage(), e);
        }
    }

    public void abortMultipartUploadSync(String bucket, String objectName, String uploadId) {
        try {
            abortMultipartUpload(bucket, objectName, uploadId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to abort multipart upload: " + e.getMessage(), e);
        }
    }

    // ========== 对象管理方法 ==========

    /**
     * 复制对象（用于合并分片后将临时文件复制到最终目录）
     */
    public void copyObject(String bucket, String sourceKey, String destinationKey) {
        CopyObjectRequest request = CopyObjectRequest.builder()
                .sourceBucket(bucket)
                .sourceKey(sourceKey)
                .destinationBucket(bucket)
                .destinationKey(destinationKey)
                .build();
        s3Client.copyObject(request);
    }

    /**
     * 删除对象（用于删除临时文件）
     */
    public void deleteObject(String bucket, String objectKey) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build();
        s3Client.deleteObject(request);
    }

    public void copyObjectSync(String bucket, String sourceKey, String destinationKey) {
        try {
            copyObject(bucket, sourceKey, destinationKey);
        } catch (Exception e) {
            throw new RuntimeException("Failed to copy object: " + e.getMessage(), e);
        }
    }

    public void deleteObjectSync(String bucket, String objectKey) {
        try {
            deleteObject(bucket, objectKey);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete object: " + e.getMessage(), e);
        }
    }
}
