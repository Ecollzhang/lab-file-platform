package com.lab.file.fileservice.controller;

import com.lab.file.common.response.Result;
import com.lab.file.fileservice.entity.FileEntity;
import com.lab.file.fileservice.service.IFileChunkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 文件分片上传控制器
 */
@RestController
@RequestMapping("/file/chunk")
@Tag(name = "文件分片上传", description = "文件上传、下载等功能接口")
public class FileChunkController {

    @Autowired
    private IFileChunkService fileChunkService;
    /**
     * 初始化分片上传
     */
    @Operation(summary = "初始化分片上传", description = "初始化分片上传并返回uploadId")
    @ApiResponse(responseCode = "200", description = "初始化成功",
            content = @Content(schema = @Schema(implementation = String.class)))
    @PostMapping("/init")
    public Result<String> initUpload(@RequestBody Map<String, Object> params, @RequestHeader(value = "userId", required = false) Long userId) {
        String fileName = (String) params.get("fileName");
        String md5 = (String) params.get("md5");
        Integer totalChunks = (Integer) params.get("totalChunks");

        String uploadId = fileChunkService.initUpload(fileName, md5, totalChunks, userId);
        return Result.success("初始化分片上传成功", uploadId);
    }

    /**
     * 上传单个分片
     */
    @Operation(summary = "上传分片", description = "上传单个分片")
    @ApiResponse(responseCode = "200", description = "上传成功",
            content = @Content(schema = @Schema(implementation = String.class)))
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<String> uploadChunk(
            @Parameter(description = "文件MD5", required = true) @RequestParam("md5") String md5,
            @Parameter(description = "上传ID", required = true) @RequestParam("uploadId") String uploadId,
            @Parameter(description = "分片序号", required = true) @RequestParam("partNumber") Integer partNumber,
            @Parameter(description = "分片序号", required = true) @RequestParam("totalChunks") Long totalChunks,
            @Parameter(description = "分片文件", required = true) @RequestParam("chunk") MultipartFile chunk,
            @RequestHeader(value = "userId", required = false) Long userId) {
        System.out.println(md5);
        System.out.println(uploadId);
        String etag = fileChunkService.uploadChunk(md5, uploadId, partNumber, totalChunks, chunk, userId);
        return Result.success("分片上传成功", etag);
    }

    /**
     * 获取已上传分片列表
     */
    @Operation(summary = "查询已上传分片", description = "查询已上传的分片列表")
    @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(schema = @Schema(implementation = Map.class)))
    @GetMapping("/uploaded")
    public Result<Map<Integer, String>> getUploadedChunks(
            @Parameter(description = "文件MD5", required = true) @RequestParam("md5") String md5,
            @Parameter(description = "上传ID", required = true) @RequestParam("uploadId") String uploadId,
            @RequestHeader(value = "userId", required = false) Long userId) {

        Map<Integer, String> uploadedChunks = fileChunkService.getUploadedChunks(md5, uploadId, userId);
        return Result.success("查询已上传分片成功", uploadedChunks);
    }

    /**
     * 获取未上传的分片序号列表
     */
    @Operation(summary = "查询未上传分片", description = "根据总分片数计算未上传的分片序号列表")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/missing-chunks")
    public Result<List<Integer>> getMissingChunks(
            @Parameter(description = "文件MD5", required = true) @RequestParam("md5") String md5,
            @Parameter(description = "上传ID", required = true) @RequestParam("uploadId") String uploadId,
            @Parameter(description = "总分片数", required = true) @RequestParam("totalChunks") Integer totalChunks,
            @RequestHeader(value = "userId", required = false) Long userId) {

        List<Integer> missing = fileChunkService.getMissingChunks(md5, uploadId, totalChunks, userId);
        return Result.success("查询未上传分片成功", missing);
    }

    /**
     * 合并分片
     */
    @Operation(summary = "合并分片", description = "合并所有分片为完整文件")
    @ApiResponse(responseCode = "200", description = "合并成功",
            content = @Content(schema = @Schema(implementation = FileEntity.class)))
    @PostMapping("/merge")
    public Result<FileEntity> mergeChunks(
            @Parameter(description = "文件MD5", required = true) @RequestParam("md5") String md5,
            @Parameter(description = "上传ID", required = true) @RequestParam("uploadId") String uploadId,
            @Parameter(description = "文件名", required = true) @RequestParam("fileName") String fileName,
            @Parameter(description = "父目录ID", required = false) @RequestParam(value = "parentId", defaultValue = "0") Long parentId,
            @Parameter(description = "文件描述", required = false) @RequestParam(value = "description", required = false) String description,
            @RequestHeader(value = "userId", required = false) Long userId) {

        FileEntity fileEntity = fileChunkService.mergeChunks(md5, uploadId, fileName, userId, parentId, description);
        return Result.success("分片合并成功", fileEntity);
    }

    /**
     * 取消分片上传
     */
    @Operation(summary = "取消分片上传", description = "取消正在进行的分片上传")
    @ApiResponse(responseCode = "200", description = "取消成功")
    @PostMapping("/cancel")
    public Result<Void> cancelUpload(
            @Parameter(description = "文件MD5", required = true) @RequestParam("md5") String md5,
            @Parameter(description = "上传ID", required = true) @RequestParam("uploadId") String uploadId,
            @RequestHeader(value = "userId", required = false) Long userId) {

        fileChunkService.cancelUpload(md5, uploadId, userId);
        return Result.success("取消分片上传成功", null);
    }

    /**
     * 检查文件是否存在（秒传）
     */
    @Operation(summary = "检查文件是否已存在", description = "根据MD5检查文件是否已存在（支持秒传）")
    @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(schema = @Schema(implementation = FileEntity.class)))
    @GetMapping("/exists")
    public Result<FileEntity> checkFileExists(
            @Parameter(description = "文件MD5", required = true) @RequestParam("md5") String md5,
            @RequestHeader(value = "userId", required = false) Long userId) {

        FileEntity fileEntity = fileChunkService.checkFileExists(md5, userId);
        return Result.success("查询完成", fileEntity);
    }
}