package com.lab.file.fileservice.service;

import com.lab.file.fileservice.entity.FileEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 文件分片上传 Service 接口
 */
public interface IFileChunkService {

    /**
     * 初始化分片上传，获取 uploadId
     * @param fileName 文件名
     * @param md5 文件md5
     * @param totalChunks 总分片数
     * @param userId 用户id
     * @return uploadId
     */
    String initUpload(String fileName, String md5, Integer totalChunks, Long userId);

    /**
     * 上传单个分片
     * @param md5 文件md5
     * @param uploadId 分片上传id
     * @param partNumber 分片序号
     * @param file 分片文件
     * @return etag
     */
    String uploadChunk(String md5, String uploadId, Integer partNumber,  Long totalChunks, MultipartFile file, Long userId);

    /**
     * 获取已上传分片集合
     * @param md5 文件md5
     * @param uploadId 分片上传id
     * @return key:分片序号, value:etag
     */
    Map<Integer, String> getUploadedChunks(String md5, String uploadId);

    /**
     * 合并分片
     * @param md5 文件md5
     * @param uploadId 分片上传id
     * @param fileName 文件名
     * @param userId 用户id
     * @param parentId 父目录id
     * @param description 文件描述
     * @return 最终文件实体
     */
    FileEntity mergeChunks(String md5, String uploadId, String fileName,
                           Long userId, Long parentId, String description);

    /**
     * 取消/终止分片上传
     * @param md5 文件md5
     * @param uploadId 分片上传id
     */
    void cancelUpload(String md5, String uploadId);

    /**
     * 秒传校验：根据md5判断文件是否已存在
     * @param md5 文件md5
     * @param userId 用户id
     * @return 存在则返回文件信息，不存在返回null
     */
    FileEntity checkFileExists(String md5, Long userId);
}