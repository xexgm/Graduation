package com.gm.imbootstrap.controller;

import com.gm.graduation.common.domain.UploadFile;
import com.gm.imbootstrap.dto.ApiResponse;
import com.gm.imbootstrap.dto.file.FileUploadResponse;
import com.gm.imbootstrap.service.FileService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@Slf4j
@RestController
@RequestMapping("/file")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<FileUploadResponse>> upload(
        @RequestParam("file") MultipartFile file,
        HttpServletRequest request
    ) {
        try {
            Long currentUserId = (Long) request.getAttribute("currentUserId");
            UploadFile uploadFile = fileService.upload(file, currentUserId);
            FileUploadResponse response = new FileUploadResponse(
                uploadFile.getFileId(),
                uploadFile.getOriginalName(),
                uploadFile.getFileSize(),
                uploadFile.getContentType()
            );
            return ResponseEntity.ok(ApiResponse.success("上传成功", response));
        } catch (IllegalArgumentException e) {
            log.warn("文件上传参数错误: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("文件上传失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("上传失败"));
        }
    }

    @GetMapping("/download/{fileId}")
    public ResponseEntity<Resource> download(@PathVariable Long fileId) {
        UploadFile uploadFile = fileService.getFile(fileId);
        Resource resource = fileService.loadAsResource(fileId);
        String contentType = uploadFile.getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType))
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                .filename(uploadFile.getOriginalName(), StandardCharsets.UTF_8)
                .build()
                .toString())
            .contentLength(uploadFile.getFileSize())
            .body(resource);
    }
}
