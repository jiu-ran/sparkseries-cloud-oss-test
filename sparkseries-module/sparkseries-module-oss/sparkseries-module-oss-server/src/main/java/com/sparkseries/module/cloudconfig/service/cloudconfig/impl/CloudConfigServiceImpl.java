package com.sparkseries.module.cloudconfig.service.cloudconfig.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.sparkeries.enums.StorageTypeEnum;
import com.sparkseries.common.util.entity.Result;
import com.sparkseries.common.util.exception.BusinessException;
import com.sparkseries.module.cloudconfig.dto.CloudConfigDTO;
import com.sparkseries.module.cloudconfig.factory.config.ConfigSaveStrategy;
import com.sparkseries.module.cloudconfig.factory.config.ConfigSaveStrategyManager;
import com.sparkseries.module.cloudconfig.factory.valid.manager.ValidConnectServiceFactoryManager;
import com.sparkseries.module.cloudconfig.service.cloudconfig.CloudConfigService;
import com.sparkseries.module.cloudconfig.service.connect.ValidConnectService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class CloudConfigServiceImpl implements CloudConfigService {

    private final Validator validator;
    private final ValidConnectServiceFactoryManager validConnectServiceFactoryManager;
    private final ConfigSaveStrategyManager configSaveStrategyManager;


    /**
     * 验证云存储配置信息
     *
     * @param config 云存储配置DTO
     * @return 操作结果
     */
    @Override
    public boolean validCloudConfig(CloudConfigDTO config) {

        StorageTypeEnum storageEnum = config.getTypeEnum();

        Class<?> validationGroup = config.getGroup(storageEnum);

        Set<ConstraintViolation<CloudConfigDTO>> violations = validator.validate(config, validationGroup);

        if (!violations.isEmpty()) {
            log.warn("云存储配置（类型: {}）校验失败，发现 {} 个约束违规。", storageEnum, violations.size());

            StringBuilder errorMessage = new StringBuilder("配置信息校验失败：");
            for (ConstraintViolation<CloudConfigDTO> violation : violations) {
                violation.getPropertyPath();
                violation.getMessage();
                errorMessage.append(violation.getPropertyPath()).append(": ").append(violation.getMessage()).append("; ");
                log.warn("校验违规：字段 '{}' - '{}'", violation.getPropertyPath(), violation.getMessage());
            }
            throw new BusinessException(errorMessage.toString());
        }
        log.info("云存储配置（类型: {}）校验通过。", storageEnum);

        ValidConnectService validConnectService = validConnectServiceFactoryManager.getValidConnectService(storageEnum);

        return validConnectService.validConnect(config);

    }

    /**
     * 保存云存储配置信息
     *
     * @param config 云存储配置信息
     * @return 操作结果
     */
    @Override
    public Result<?> saveCloudConfig(CloudConfigDTO config) {

        StorageTypeEnum storageEnum = config.getTypeEnum();

        long id = IdWorker.getId();
        try {
            ConfigSaveStrategy strategy = configSaveStrategyManager.getStrategy(storageEnum);
            Integer row = strategy.saveConfig(config, id);

            String storageTypeName = storageEnum.getKey().toUpperCase();
            if (row <= 0) {
                log.error("{}云储存信息添加失败", storageTypeName);
                throw new BusinessException(storageTypeName + "云储存信息添加失败");
            } else {
                log.info("{}云储存信息添加成功", storageTypeName);
                return Result.ok(storageTypeName + "云储存信息添加成功");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("保存云存储配置时发生异常", e);
            throw new BusinessException("保存云存储配置失败: " + e.getMessage());
        }
    }

    /**
     * 验证云存储配置的连接性并保存配置信息
     *
     * @param config 云存储配置信息
     * @return 操作结果
     */
    @Override
    public Result<?> validAndSave(CloudConfigDTO config) {

        if (!validCloudConfig(config)) {
            return Result.error("云服务配置信息校验失败");
        }

        return saveCloudConfig(config);
    }

    /**
     * 删除云存储配置
     *
     * @param type 云存储类型
     * @param id   配置ID
     * @return 操作结果
     */
    @Override
    public Result<?> deleteCloudConfig(int type, Long id) {

        Integer row = configSaveStrategyManager.getStrategy(type).deleteConfig(id);
        if (row < 1) {
            return Result.error("删除失败");
        }
        return Result.ok("删除成功");


    }

    /**
     * 列出所有云存储配置
     *
     * @param type 云存储类型
     * @return 包含配置列表的操作结果
     */
    @Override
    public Result<List<?>> listCloudConfig(int type) {

        return Result.ok(configSaveStrategyManager.getStrategy(type).listConfig());
    }


}
