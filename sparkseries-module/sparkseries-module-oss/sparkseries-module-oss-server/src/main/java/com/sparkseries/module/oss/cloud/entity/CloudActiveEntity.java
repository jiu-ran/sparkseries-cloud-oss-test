package com.sparkseries.module.oss.cloud.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 云服务配置信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "云服务激活信息")
public class CloudActiveEntity {

    public int type;
    public Long id;
    public int status;
}