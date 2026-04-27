package com.lab.file.fileservice.config;

import io.minio.*;
import io.minio.messages.Part;
import okhttp3.OkHttpClient;

import javax.net.ssl.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CustomMinioClient {

    private final MinioAsyncClient asyncClient;

    public CustomMinioClient(MinioAsyncClient asyncClient) {
        this.asyncClient = asyncClient;
    }



    // 创建分片上传
    public CompletableFuture<String> createMultipartUpload(CreateMultipartUploadArgs args) {
        return asyncClient.createMultipartUpload(args);
    }

    // 上传分片
    public CompletableFuture<UploadPartResponse> uploadPart(UploadPartArgs args) {
        return asyncClient.uploadPart(args);
    }

    // 查询已上传分片
    public CompletableFuture<ListPartsResponse> listParts(ListPartsArgs args) {
        return asyncClient.listParts(args);
    }

    // 合并分片
    public CompletableFuture<CompleteMultipartUploadResponse> completeMultipartUpload(CompleteMultipartUploadArgs args) {
        return asyncClient.completeMultipartUpload(args);
    }

    // 取消上传
    public CompletableFuture<Void> abortMultipartUpload(AbortMultipartUploadArgs args) {
        return asyncClient.abortMultipartUpload(args);
    }

    // 同步方法（业务直接用）
    public String createMultipartUploadSync(CreateMultipartUploadArgs args) {
        return createMultipartUpload(args).join();
    }

    public UploadPartResponse uploadPartSync(UploadPartArgs args) {
        return uploadPart(args).join();
    }

    public ListPartsResponse listPartsSync(ListPartsArgs args) {
        return listParts(args).join();
    }

    public void completeMultipartUploadSync(CompleteMultipartUploadArgs args) {
        completeMultipartUpload(args).join();
    }

    public void abortMultipartUploadSync(AbortMultipartUploadArgs args) {
        abortMultipartUpload(args).join();
    }

    // 构建器
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String endpoint;
        private String accessKey;
        private String secretKey;

        public Builder endpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        public Builder credentials(String accessKey, String secretKey) {
            this.accessKey = accessKey;
            this.secretKey = secretKey;
            return this;
        }

        public CustomMinioClient build() {
            MinioAsyncClient asyncClient = MinioAsyncClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .build();
            return new CustomMinioClient(asyncClient);
        }
    }
}