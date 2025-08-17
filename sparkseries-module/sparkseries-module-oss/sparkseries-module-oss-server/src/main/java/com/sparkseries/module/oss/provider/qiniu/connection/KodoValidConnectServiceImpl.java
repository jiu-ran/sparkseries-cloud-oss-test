package com.sparkseries.module.oss.provider.qiniu.connection;

import com.qiniu.common.QiniuException;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import com.sparkseries.common.util.exception.BusinessException;
import com.sparkseries.module.oss.cloud.dto.CloudConfigDTO;
import com.sparkseries.module.oss.common.api.provider.service.ValidConnectService;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Kodo连接验证服务实现类
 */
@Slf4j
public class KodoValidConnectServiceImpl implements ValidConnectService {
    /**
     * 获取KODO的配置文件
     *
     * @param config 接收到云服务配置文件
     * @return 验证结果
     */
    @Override
    public boolean validConnect(CloudConfigDTO config) {


        String accessKey = config.getKodoAccessKey();
        String secretKey = config.getKodoSecretKey();
        String bucketName = config.getKodoBucketName();

        return connectTest(accessKey, secretKey, bucketName);
    }

    /**
     * KODO测试连接
     *
     * @param accessKey  KODO的AccessKey
     * @param secretKey  KODO的SecretKey
     * @param bucketName KODO的bucketName
     * @return 测试结果
     */
    public boolean connectTest(String accessKey, String secretKey, String bucketName) {

        Region region = Region.autoRegion();
        // 1. 创建 Auth 对象，用于身份验证
        Auth auth = Auth.create(accessKey, secretKey);
        log.info("成功创建 KODO Auth 对象");

        // 2. 创建 Configuration 对象，配置区域
        Configuration cg = new Configuration(region);
        log.info("成功创建 KODO Configuration 对象，区域: {}", region);

        // 3. 创建 BucketManager，用于 bucket 相关操作和 stat/delete 对象
        BucketManager bucketManager = new BucketManager(auth, cg);
        log.info("成功创建 KODO BucketManager");

        // 4. 创建 UploadManager，用于上传对象
        UploadManager uploadManager = new UploadManager(cg);
        log.info("成功创建 KODO UploadManager");

        try {
            bucketManager.buckets();
        } catch (QiniuException e) {
            log.error("KODO 连接失败 accessKey/secretKey 无效", e);
            throw new BusinessException("accessKey/secretKey 无效");
        }

        try {
            bucketManager.getBucketInfo(bucketName);
        } catch (QiniuException e) {
            log.error("KODO bucket不存在", e);
            throw new BusinessException("bucket不存在 请输入正确的bucket");
        }

        String testObjectName = "该文件为权限测试文件可以随意删除-" + System.currentTimeMillis() + ".txt";
        byte[] testContent = "This is a test object for KODO permission validation.".getBytes();

        try (InputStream inputStream = new ByteArrayInputStream(testContent)) {
            // 生成上传凭证
            String upToken = auth.uploadToken(bucketName);
            uploadManager.put(inputStream, testObjectName, upToken, null, null);
            log.info("成功向 bucket '{}' 写入测试对象 '{}'，具备写权限。", bucketName, testObjectName);

        } catch (QiniuException e) {
            log.error("向 bucket '{}' 写入测试对象 '{}' 失败，可能不具备写权限。", bucketName, testObjectName, e);
            throw new BusinessException("该API密钥对该bucket 不具备写权限");
        } catch (IOException e) {
            log.error("向 bucket '{}' 写入测试对象 '{}' 失败。", bucketName, testObjectName, e);
        }

        try {
            bucketManager.stat(bucketName, testObjectName);
            log.info("成功获取测试对象 '{}' 的 stat 信息，具备读权限。", testObjectName);
        } catch (QiniuException e) {
            log.warn("获取测试对象 '{}' 的 stat 信息失败，可能不具备读权限。", testObjectName, e);
            throw new BusinessException("该API密钥对该bucket 不具备读权限");
        }
        // 如果写入成功，尝试删除对象 (删除权限校验)
        try {
            bucketManager.delete(bucketName, testObjectName);
            log.info("成功从 bucket '{}' 删除测试对象 '{}'，具备删除权限。", bucketName, testObjectName);
        } catch (QiniuException e) {
            log.warn("从 bucket '{}' 删除测试对象 '{}' 失败，可能不具备删除权限。", bucketName, testObjectName, e);
            throw new BusinessException("该API密钥对该bucket 不具备删除权限");
        }

        return true;
    }
}
