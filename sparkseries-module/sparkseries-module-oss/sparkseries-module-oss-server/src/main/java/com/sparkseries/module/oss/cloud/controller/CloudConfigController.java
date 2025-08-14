package com.sparkseries.module.oss.cloud.controller;

import com.sparkeries.enums.StorageTypeEnum;
import com.sparkseries.common.util.entity.Result;
import com.sparkseries.module.oss.cloud.dto.CloudConfigDTO;
import com.sparkseries.module.oss.cloud.service.CloudConfigService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 云存储配置控制器
 */
@Validated
@RestController
@RequestMapping("/storage/cloudconfig")
@RequiredArgsConstructor
public class CloudConfigController {

    private final CloudConfigService cloudConfigService;

    /**
     * 保存云存储配置信息
     *
     * @param config 云存储配置信息
     * @return 保存结果
     */
    @PostMapping()
    @Operation(summary = "保存云存储配置信息")
    public Result<?> saveConfig(@RequestBody CloudConfigDTO config) {

        if (config.getTypeEnum() == null) {
            return Result.error("请选择服务类型");
        }
        return cloudConfigService.validAndSave(config);
    }

    /**
     * 删除指定类型的云服务配置信息
     *
     * @param type 云服务类型
     * @param id   云服务ID
     * @return 删除结果
     */
    @DeleteMapping()
    @Operation(summary = "删除指定类型的云服务配置信息")
    public Result<?> deleteConfig(@RequestParam @NotNull(message = "请选择服务类型") StorageTypeEnum type,
                                  @RequestParam @NotNull(message = "请指定云服务id") Long id) {

        return cloudConfigService.deleteCloudConfig(type.getValue(), id);
    }

    /**
     * 获取指定类型的已保存的云服务配置信息
     *
     * @param type 云服务类型
     * @return 云服务配置信息列表
     */
    @GetMapping()
    @Operation(summary = "获取指定类型的已保存的云服务配置信息")
    public Result<List<?>> listConfig(@RequestParam(required = false) StorageTypeEnum type) {
        if (type == null) {
            return Result.error("请选择服务类型");
        }
        return cloudConfigService.listCloudConfig(type.getValue());
    }
}
