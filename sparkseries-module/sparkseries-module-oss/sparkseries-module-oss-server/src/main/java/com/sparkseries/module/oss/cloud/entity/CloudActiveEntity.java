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
    @Schema(description = "云服务类型")
    public int type;
    @Schema(description = "云服务 id")
    public Long id;
    @Schema(description = "云服务状态")
    public int status;
}