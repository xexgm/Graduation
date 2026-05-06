package com.gm.imbootstrap.service;

import com.gm.graduation.common.domain.UploadFile;
import com.gm.imbootstrap.mapper.UploadFileMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Service
public class FileService {

    private static final DateTimeFormatter YEAR_FORMATTER = DateTimeFormatter.ofPattern("yyyy");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("MM");
    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("dd");

    private final UploadFileMapper uploadFileMapper;
    private final Path baseDir;
    private final long maxSizeBytes;

    public FileService(
        UploadFileMapper uploadFileMapper,
        @Value("${file.upload.base-dir:UserUploadFiles}") String baseDir,
        @Value("${file.upload.max-size-mb:10}") long maxSizeMb
    ) {
        this.uploadFileMapper = uploadFileMapper;
        this.baseDir = Paths.get(baseDir).toAbsolutePath().normalize();
        this.maxSizeBytes = maxSizeMb * 1024 * 1024;
    }

    public UploadFile upload(MultipartFile file, Long uploaderId) {
        validateFile(file, uploaderId);

        String originalName = sanitizeOriginalName(file.getOriginalFilename());
        String fileExt = extractExtension(originalName);
        String storedName = UUID.randomUUID().toString().replace("-", "") + fileExt;
        Path relativeDir = buildDateRelativeDir();
        Path targetDir = baseDir.resolve(relativeDir).normalize();
        Path targetPath = targetDir.resolve(storedName).normalize();

        if (!targetPath.startsWith(baseDir)) {
            throw new IllegalArgumentException("文件存储路径非法");
        }

        try {
            Files.createDirectories(targetDir);
            file.transferTo(targetPath);
        } catch (IOException e) {
            throw new IllegalStateException("文件保存失败", e);
        }

        UploadFile uploadFile = new UploadFile();
        uploadFile.setUploaderId(uploaderId);
        uploadFile.setOriginalName(originalName);
        uploadFile.setStoredName(storedName);
        uploadFile.setStoragePath(relativeDir.resolve(storedName).toString());
        uploadFile.setContentType(file.getContentType());
        uploadFile.setFileSize(file.getSize());
        uploadFile.setFileExt(fileExt.isBlank() ? null : fileExt.substring(1));
        uploadFile.setCreateTime(LocalDateTime.now());

        uploadFileMapper.insert(uploadFile);
        return uploadFile;
    }

    public UploadFile getFile(Long fileId) {
        if (fileId == null) {
            throw new IllegalArgumentException("文件ID不能为空");
        }
        UploadFile uploadFile = uploadFileMapper.selectById(fileId);
        if (uploadFile == null) {
            throw new IllegalArgumentException("文件不存在");
        }
        return uploadFile;
    }

    public Resource loadAsResource(Long fileId) {
        UploadFile uploadFile = getFile(fileId);
        Path filePath = baseDir.resolve(uploadFile.getStoragePath()).normalize();
        if (!filePath.startsWith(baseDir) || !Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            throw new IllegalArgumentException("文件不存在或已被删除");
        }
        return new FileSystemResource(filePath);
    }

    private void validateFile(MultipartFile file, Long uploaderId) {
        if (uploaderId == null) {
            throw new IllegalArgumentException("未登录或凭证无效");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        if (file.getSize() > maxSizeBytes) {
            throw new IllegalArgumentException("文件大小不能超过 " + (maxSizeBytes / 1024 / 1024) + "MB");
        }
    }

    private String sanitizeOriginalName(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "unknown";
        }
        return Paths.get(originalFilename).getFileName().toString();
    }

    private String extractExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex).toLowerCase(Locale.ROOT);
    }

    private Path buildDateRelativeDir() {
        LocalDate today = LocalDate.now();
        return Paths.get(
            today.format(YEAR_FORMATTER),
            today.format(MONTH_FORMATTER),
            today.format(DAY_FORMATTER)
        );
    }
}
