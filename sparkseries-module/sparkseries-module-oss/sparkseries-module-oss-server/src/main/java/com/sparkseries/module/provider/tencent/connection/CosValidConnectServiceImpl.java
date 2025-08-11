package com.sparkseries.module.provider.tencent.connection;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.region.Region;
import com.sparkseries.common.util.exception.BusinessException;
import com.sparkseries.module.cloudconfig.dto.CloudConfigDTO;
import com.sparkseries.module.cloudconfig.service.connect.ValidConnectService;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.UUID;

/**
 * COS验证连接服务实现
 */
@Slf4j
public class CosValidConnectServiceImpl implements ValidConnectService {
    /**
     * 获取COS的配置文件
     *
     * @param config 接收到云服务配置文件
     * @return 验证结果
     */
    @Override
    public boolean validConnect(CloudConfigDTO config) {

        String secretId = config.getCosSecretId();
        String secretKey = config.getCosSecretKey();
        String bucketName = config.getCosBucketName();
        String region = config.getCosRegion();

        return connectTest(secretId, secretKey, bucketName, region);
    }

    /**
     * COS 测试连接
     *
     * @param secretId   COS的SecretId
     * @param secretKey  COS的SecretKey
     * @param bucketName COS的bucketName
     * @param region     COS的region
     * @return 测试结果
     */
    public boolean connectTest(String secretId, String secretKey, String bucketName, String region) {
        log.info("COS 配置文件开始测试");
        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        // 配置客户端，设置 region
        ClientConfig clientConfig = new ClientConfig(new Region(region));
        COSClient cosClient;
        String testObjectKey = "该文件为权限测试文件您可随意删除-" + UUID.randomUUID() + ".txt";
        try {
            cosClient = new COSClient(cred, clientConfig);
            cosClient.listBuckets();
        } catch (CosServiceException ce) {
            log.error("COS的secretId/secretKey 不正确", ce);
            throw new BusinessException("secretId/secretKey 不正确");
        } catch (CosClientException ce) {
            log.error("COS 客户端错误", ce);
            throw new BusinessException("COS 客户端错误");
        }
        try {
            log.info("校验COS中是否存在该bucket:{}", bucketName);
            boolean exist = cosClient.doesBucketExist(bucketName);

            if (!exist) {
                throw new BusinessException("该bucket不存在 请输入正确的bucketName");
            }
        } catch (CosServiceException e) {
            log.error("获取Bucket是否存在时出现了错误", e);
            throw new BusinessException("获取Bucket是否存在时出现了错误");
        } catch (CosClientException e) {
            log.error("COS 获取bucket相关信息时失败", e);
            if (Objects.equals(e.getErrorCode(), "UnknownHost")) {
                log.error("COS 的Region错误", e);
                throw new BusinessException("你输入的region:" + region + "不正确");
            }
            throw new BusinessException(e.getMessage());
        }
        log.info("校验COS中存在该bucket:{}", bucketName);
        byte[] content = "Write, Read, Delete verification.".getBytes();
        InputStream inputStreamForWrite = new ByteArrayInputStream(content);
        ObjectMetadata metadataForWrite = new ObjectMetadata();
        metadataForWrite.setContentLength(content.length);
        metadataForWrite.setContentType("text/plain");

        log.info("开始对该bucket:{} 进行写权限校验", bucketName);
        try {
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, testObjectKey, inputStreamForWrite, metadataForWrite);
            cosClient.putObject(putObjectRequest);
        } catch (CosServiceException e) {
            log.error("COS 写权限测试失败", e);
            throw new BusinessException("对该bucket没有写权限");
        } finally {
            try {
                inputStreamForWrite.close();
            } catch (IOException e) {
                log.error("关闭上传流失败", e);
            }
        }
        log.info("bucket:{} 写权限校验完成", bucketName);
        log.info("开始对该bucket:{} 进行读权限校验", bucketName);
        // 3. 校验读权限 (通过读取刚刚上传的测试对象 - 需要 cos:GetObject 权限)
        try (InputStream downloadedStream = cosClient.getObject(bucketName, testObjectKey).getObjectContent()) {

            int read = downloadedStream.read();
        } catch (CosServiceException | IOException e) {
            log.error("COS 读权限测试失败", e);
            throw new BusinessException("对该bucket没有读权限");
        }
        log.info("bucket:{} 读权限校验完成", bucketName);

        log.info("开始对该bucket:{} 进行删除权限校验", bucketName);
        // 4. 校验删除权限 (通过删除上传的测试对象 - 需要 cos:DeleteObject 权限)
        try {
            cosClient.deleteObject(bucketName, testObjectKey);
        } catch (CosServiceException e) {
            log.error("COS 删除权限测试失败", e);
            throw new BusinessException("对该bucket没有删除权限");
        }
        log.info("bucket:{} 删除权限校验完成", bucketName);
        log.info("COS 读写删除权限校验成功：Bucket '{}' 存在，具备读、写、删除权限。", bucketName);
        return true;
    }
}
