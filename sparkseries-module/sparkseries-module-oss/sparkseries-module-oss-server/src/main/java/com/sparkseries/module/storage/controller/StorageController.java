package com.sparkseries.module.storage.controller;

import com.sparkeries.enums.StorageTypeEnum;
import com.sparkseries.common.util.entity.Result;
import com.sparkseries.module.storage.service.storage.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 储存服务
 */
@Slf4j
@RestController
@RequestMapping("/storage")
@RequiredArgsConstructor
@Tag(name = "储存服务", description = "储存服务")
public class StorageController {

    private final StorageService storageService;

    /**
     * 切换储存服务
     *
     * @param type 云服务类型
     * @param id   云服务ID
     * @return 切换结果
     */
    @PutMapping("/strategy")
    @Operation(summary = "切换储存服务")
    public Result<?> changeStorage(@RequestParam @NotNull(message = "请选择服务类型") StorageTypeEnum type,
                                   @RequestParam Long id) {

        return storageService.changeService(type.getValue(), id);
    }

    /**
     * 获取当前使用的储存服务
     *
     * @return 当前使用的储存服务
     */
    @GetMapping("/active")
    @Operation(summary = "获取当前使用的储存服务")
    public Result<?> activeStorage() {

        return storageService.getActiveStorageInfo();
    }
}
