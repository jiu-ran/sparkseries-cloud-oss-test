package com.sparkeries.feign;

import com.sparkseries.common.util.entity.Result;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.NotNull;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;


/**
 * 头像服务调用接口
 */
@FeignClient
public interface AvatarClient {

    /**
     * 上传用户头像
     *
     * @param avatar 头像文件
     * @param userId 用户id
     * @return 上传结果
     */
    @PostMapping
    Result<?> uploadAvatar(@RequestParam("file") MultipartFile avatar, @RequestParam("userId") Long userId);

    /**
     * 修改头像
     */
    @PutMapping()
    @Operation(summary = "修改头像")
    Result<?> changeAvatar(@RequestParam("file") @NotNull(message = "请指定上传头像") MultipartFile avatar);



}
