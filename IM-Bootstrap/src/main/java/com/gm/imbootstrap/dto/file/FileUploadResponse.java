package com.gm.imbootstrap.dto.file;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FileUploadResponse {

    private Long fileId;

    private String fileName;

    private Long fileSize;

    private String contentType;
}
