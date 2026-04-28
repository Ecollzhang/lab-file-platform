package com.lab.file.fileservice.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * 分片上传测试页面入口。
 * <p>
 * 访问 {@code /chunk-test} 即可打开测试工具页面。
 */
@RestController
@RequestMapping("/file/chunk")
@Tag(name = "文件分片上传", description = "文件分片上传、合并等功能接口")
public class ChunkTestController {

    @GetMapping("/chunk-test")
    public ResponseEntity<Resource> chunkTest() throws IOException {
        Resource resource = new ClassPathResource("static/chunk-test.html");
        if (resource.exists()) {
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(resource);
        }
        return ResponseEntity.notFound().build();
    }
}
