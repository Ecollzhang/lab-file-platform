package com.lab.file.fileservice.controller;

import com.lab.file.common.response.Result;
import com.lab.file.fileservice.entity.FileEntity;
import com.lab.file.fileservice.service.IFileService;
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

/**
 * 文件控制器
 */
@RestController
@RequestMapping("/file")
@Tag(name = "文件管理", description = "文件上传、下载、管理等接口")
public class FileController {

    @Autowired
    private IFileService fileService;

    /**
     * 上传文件
     */
    @Operation(summary = "上传文件", description = "上传文件到指定目录")
    @ApiResponse(responseCode = "200", description = "上传成功",
            content = @Content(schema = @Schema(implementation = FileEntity.class)))
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<FileEntity> uploadFile(
            @Parameter(description = "文件", required = true) @RequestParam("file") MultipartFile file,
            @Parameter(description = "父目录ID，0表示根目录", required = false) @RequestParam(value = "parentId", defaultValue = "0") Long parentId,
            @Parameter(description = "文件描述", required = false) @RequestParam(value = "description", required = false) String description,
            @RequestHeader(value = "userId", required = false) Long userId) {

        FileEntity uploadedFile = fileService.uploadFile(file, userId, parentId, description);
        return Result.success("文件上传成功", uploadedFile);
    }

    /**
     * 创建文件夹
     */
    @Operation(summary = "创建文件夹", description = "创建新的文件夹")
    @ApiResponse(responseCode = "200", description = "创建成功",
                 content = @Content(schema = @Schema(implementation = FileEntity.class)))
    @PostMapping("/folder")
    public Result<FileEntity> createFolder(@Parameter(description = "文件夹名称", required = true) @RequestParam("folderName") String folderName,
                                           @Parameter(description = "父目录ID，0表示根目录", required = false) @RequestParam(value = "parentId", defaultValue = "0") Long parentId,
                                           @RequestHeader(value = "userId", required = false) Long userId) {
        FileEntity folder = fileService.createFolder(folderName, userId, parentId);
        return Result.success("文件夹创建成功", folder);
    }

    /**
     * 获取文件列表
     */
    @Operation(summary = "获取文件列表", description = "获取指定目录下的文件列表")
    @ApiResponse(responseCode = "200", description = "获取成功",
                 content = @Content(schema = @Schema(implementation = List.class)))
    @GetMapping("/list")
    public Result<List<FileEntity>> getFileList(@RequestHeader(value = "userId", required = false) Long userId,
                                                @Parameter(description = "父目录ID，0表示根目录", required = false) @RequestParam(value = "parentId", defaultValue = "0") Long parentId) {
        List<FileEntity> fileList = fileService.getFileListByParentId(userId, parentId);
        return Result.success("获取文件列表成功", fileList);
    }

    /**
     * 删除文件或文件夹
     */
    @Operation(summary = "删除文件或文件夹", description = "删除指定的文件或文件夹")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @DeleteMapping("/{fileId}")
    public Result<Void> deleteFile(@Parameter(description = "文件ID", required = true) @PathVariable Long fileId,
                                   @RequestHeader(value = "userId", required = false) Long userId) {
        boolean success = fileService.deleteFile(fileId, userId);
        if (success) {
            return Result.success("删除成功", null);
        } else {
            return Result.error("删除失败");
        }
    }

    /**
     * 重命名文件或文件夹
     */
    @Operation(summary = "重命名文件或文件夹", description = "重命名指定的文件或文件夹")
    @ApiResponse(responseCode = "200", description = "重命名成功")
    @PutMapping("/{fileId}/rename")
    public Result<Void> renameFile(@Parameter(description = "文件ID", required = true) @PathVariable Long fileId,
                                   @Parameter(description = "新名称", required = true) @RequestParam("newName") String newName,
                                   @RequestHeader(value = "userId", required = false) Long userId) {
        boolean success = fileService.renameFile(fileId, newName, userId);
        if (success) {
            return Result.success("重命名成功", null);
        } else {
            return Result.error("重命名失败");
        }
    }

    /**
     * 移动文件或文件夹
     */
    @Operation(summary = "移动文件或文件夹", description = "移动文件或文件夹到新目录")
    @ApiResponse(responseCode = "200", description = "移动成功")
    @PutMapping("/{fileId}/move")
    public Result<Void> moveFile(@Parameter(description = "文件ID", required = true) @PathVariable Long fileId,
                                 @Parameter(description = "新父目录ID", required = true) @RequestParam("newParentId") Long newParentId,
                                 @RequestHeader(value = "userId", required = false) Long userId) {
        boolean success = fileService.moveFile(fileId, newParentId, userId);
        if (success) {
            return Result.success("移动成功", null);
        } else {
            return Result.error("移动失败");
        }
    }

    /**
     * 下载文件
     */
    @Operation(summary = "下载文件", description = "下载指定文件")
    @ApiResponse(responseCode = "200", description = "下载成功",
                 content = @Content(schema = @Schema(implementation = byte[].class)))
    @GetMapping("/{fileId}/download")
    public Result<byte[]> downloadFile(@Parameter(description = "文件ID", required = true) @PathVariable Long fileId,
                                       @RequestHeader(value = "userId", required = false) Long userId) {
        byte[] fileData = fileService.downloadFile(fileId, userId);
        return Result.success("下载成功", fileData);
    }
}