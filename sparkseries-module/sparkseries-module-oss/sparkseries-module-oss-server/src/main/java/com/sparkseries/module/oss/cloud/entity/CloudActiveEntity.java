package com.sparkseries.module.oss.cloud.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 云服务配置信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CloudActiveEntity {

    public int type;
    public Long id;
    public int status;
}