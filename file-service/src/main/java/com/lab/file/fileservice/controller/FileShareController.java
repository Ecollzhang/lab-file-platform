package com.lab.file.fileservice.controller;

import com.lab.file.common.response.Result;
import com.lab.file.fileservice.entity.FileEntity;
import com.lab.file.fileservice.entity.FileShare;
import com.lab.file.fileservice.entity.UserInfo;
import com.lab.file.fileservice.service.IFileShareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文件分享控制器
 */
@RestController
@RequestMapping("/share")
@Tag(name = "文件分享", description = "文件分享、分享码下载、共享目录上传等接口")
public class FileShareController {

    @Autowired
    private IFileShareService fileShareService;

    /**
     * 获取所有启用用户（分享下拉选择用）
     */
    @Operation(summary = "获取用户列表", description = "获取所有启用状态的用户列表，用于分享时选择目标用户")
    @GetMapping("/users")
    public Result<List<UserInfo>> getAllUsers() {
        List<UserInfo> users = fileShareService.getAllUsers();
        return Result.success("获取用户列表成功", users);
    }

    /**
     * 创建分享
     */
    @Operation(summary = "创建分享", description = "创建文件或文件夹的分享链接，生成分享码。私密分享需指定targetUserIds")
    @PostMapping("/create")
    public Result<FileShare> createShare(
            @Parameter(description = "文件或文件夹ID", required = true) @RequestParam("fileId") Long fileId,
            @Parameter(description = "分享类型 1-公开分享, 2-私密分享") @RequestParam(value = "shareType", defaultValue = "1") Integer shareType,
            @Parameter(description = "最大下载次数，0表示无限制") @RequestParam(value = "maxDownloadCount", defaultValue = "0") Integer maxDownloadCount,
            @Parameter(description = "过期时间（小时），为空表示永不过期") @RequestParam(value = "expireHours", required = false) Long expireHours,
            @Parameter(description = "私密分享时允许访问的用户ID，逗号分隔") @RequestParam(value = "targetUserIds", required = false) String targetUserIds,
            @RequestHeader(value = "userId", required = false) Long userId) {

        List<Long> userIds = null;
        if (targetUserIds != null && !targetUserIds.trim().isEmpty()) {
            userIds = Arrays.stream(targetUserIds.split(","))
                    .map(String::trim)
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
        }

        FileShare share = fileShareService.createShare(fileId, userId, shareType, maxDownloadCount, expireHours, userIds);
        return Result.success("分享创建成功", share);
    }

    /**
     * 获取用户的所有分享记录
     */
    @Operation(summary = "获取分享列表", description = "获取当前用户的所有分享记录")
    @GetMapping("/list")
    public Result<List<FileShare>> getUserShares(@RequestHeader(value = "userId", required = false) Long userId) {
        List<FileShare> shares = fileShareService.getUserShares(userId);
        return Result.success("获取分享列表成功", shares);
    }

    /**
     * 根据分享码获取分享信息
     */
    @Operation(summary = "获取分享信息", description = "根据分享码获取分享详情")
    @GetMapping("/{shareCode}")
    public Result<FileShare> getShareInfo(@Parameter(description = "分享码", required = true) @PathVariable String shareCode) {
        FileShare share = fileShareService.getShareByCode(shareCode);
        if (share == null) {
            return Result.error(404, "分享码不存在");
        }
        boolean valid = fileShareService.validateShare(shareCode);
        if (!valid) {
            return Result.error(410, "分享链接已失效");
        }
        return Result.success("获取分享信息成功", share);
    }

    /**
     * 获取私密分享的目标用户ID列表
     */
    @Operation(summary = "获取分享目标用户", description = "获取私密分享允许访问的用户ID列表")
    @GetMapping("/{shareCode}/target-users")
    public Result<List<Long>> getShareTargetUsers(@Parameter(description = "分享码", required = true) @PathVariable String shareCode) {
        FileShare share = fileShareService.getShareByCode(shareCode);
        if (share == null) {
            return Result.error(404, "分享码不存在");
        }
        List<Long> userIds = fileShareService.getShareTargetUserIds(share.getId());
        return Result.success("获取成功", userIds);
    }

    /**
     * 通过分享码下载文件
     */
    @Operation(summary = "通过分享码下载文件", description = "使用分享码下载文件，可通过fileId下载共享目录中的指定文件")
    @GetMapping("/{shareCode}/download")
    public void shareDownload(
            @Parameter(description = "分享码", required = true) @PathVariable String shareCode,
            @Parameter(description = "文件ID（共享目录中指定文件时使用）") @RequestParam(value = "fileId", required = false) Long fileId,
            @RequestHeader(value = "userId", required = false) Long userId,
            HttpServletResponse response) {
        if (fileId != null) {
            fileShareService.shareDownloadFile(shareCode, fileId, userId, response);
        } else {
            fileShareService.shareDownload(shareCode, userId, response);
        }
    }

    /**
     * 通过分享码预览文件（内联显示）
     */
    @Operation(summary = "通过分享码预览文件", description = "使用分享码预览文件，支持图片、文本、PDF等格式内联显示")
    @GetMapping("/{shareCode}/preview")
    public void sharePreview(
            @Parameter(description = "分享码", required = true) @PathVariable String shareCode,
            @Parameter(description = "文件ID（共享目录中指定文件时使用）") @RequestParam(value = "fileId", required = false) Long fileId,
            @RequestHeader(value = "userId", required = false) Long userId,
            HttpServletResponse response) {
        if (fileId != null) {
            fileShareService.sharePreviewFile(shareCode, fileId, userId, response);
        } else {
            fileShareService.sharePreview(shareCode, userId, response);
        }
    }

    /**
     * 获取分享目录下的文件列表
     */
    @Operation(summary = "获取分享文件列表", description = "查看分享目录下的文件列表，parentId默认为分享的根目录")
    @GetMapping("/{shareCode}/list")
    public Result<List<FileEntity>> getShareFileList(
            @Parameter(description = "分享码", required = true) @PathVariable String shareCode,
            @Parameter(description = "父目录ID，0或不传表示分享根目录") @RequestParam(value = "parentId", defaultValue = "0") Long parentId,
            @RequestHeader(value = "userId", required = false) Long userId) {
        List<FileEntity> fileList = fileShareService.getShareFileList(shareCode, parentId, userId);
        return Result.success("获取文件列表成功", fileList);
    }

    /**
     * 通过分享码上传文件到共享目录
     */
    @Operation(summary = "通过分享码上传文件", description = "向分享的共享目录上传文件")
    @PostMapping(value = "/{shareCode}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<FileEntity> shareUpload(
            @Parameter(description = "分享码", required = true) @PathVariable String shareCode,
            @Parameter(description = "文件", required = true) @RequestParam("file") MultipartFile file,
            @Parameter(description = "父目录ID，0或不传表示分享根目录") @RequestParam(value = "parentId", defaultValue = "0") Long parentId,
            @Parameter(description = "文件描述") @RequestParam(value = "description", required = false) String description,
            @RequestHeader(value = "userId", required = false) Long userId) {
        FileEntity uploadedFile = fileShareService.shareUpload(shareCode, file, parentId, description, userId);
        return Result.success("文件上传成功", uploadedFile);
    }

    /**
     * 取消分享
     */
    @Operation(summary = "取消分享", description = "取消指定的分享链接")
    @DeleteMapping("/{shareId}")
    public Result<Void> cancelShare(@Parameter(description = "分享记录ID", required = true) @PathVariable Long shareId,
                                    @RequestHeader(value = "userId", required = false) Long userId) {
        boolean success = fileShareService.cancelShare(shareId, userId);
        if (success) {
            return Result.success("取消分享成功", null);
        } else {
            return Result.error("取消分享失败");
        }
    }

    /**
     * 分享测试页面入口
     */
    @Operation(summary = "分享测试页面", description = "文件分享与预览测试页面", hidden = true)
    @GetMapping("/share-test")
    public ResponseEntity<Resource> shareTestPage() throws IOException {
        Resource resource = new ClassPathResource("static/share-test.html");
        if (resource.exists()) {
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(resource);
        }
        return ResponseEntity.notFound().build();
    }
}
