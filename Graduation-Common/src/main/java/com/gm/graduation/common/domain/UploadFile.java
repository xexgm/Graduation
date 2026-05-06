package com.gm.graduation.common.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Metadata for user uploaded files.
 */
@Data
@TableName("graduation_upload_file")
public class UploadFile {

    @TableId(type = IdType.AUTO)
    private Long fileId;

    private Long uploaderId;

    private String originalName;

    private String storedName;

    private String storagePath;

    private String contentType;

    private Long fileSize;

    private String fileExt;

    private LocalDateTime createTime;
}
