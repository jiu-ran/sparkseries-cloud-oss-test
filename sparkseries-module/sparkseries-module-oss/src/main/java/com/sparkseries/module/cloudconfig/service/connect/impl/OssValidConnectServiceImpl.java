package com.sparkseries.module.cloudconfig.service.connect.impl;

import com.aliyun.oss.*;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.comm.SignVersion;
import com.aliyun.oss.model.Bucket;
import com.aliyun.oss.model.ListObjectsRequest;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.sparkseries.common.util.exception.BusinessException;
import com.sparkseries.module.cloudconfig.dto.CloudConfigDTO;
import com.sparkseries.module.cloudconfig.service.connect.ValidConnectService;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * OSS连接有效性服务实现类
 */
@Slf4j
public class OssValidConnectServiceImpl implements ValidConnectService {
    /**
     * 获取OSS的配置文件
     *
     * @param config 接收到云服务配置文件
     * @return 验证结果
     */
    @Override
    public boolean validConnect(CloudConfigDTO config) {

        String endpoint = config.getOssEndPoint();
        String accessKeyId = config.getOssAccessKeyId();
        String accessKeySecret = config.getOssAccessKeySecret();
        String bucketName = config.getOssBucketName();
        String region = config.getOssRegion();

        return connectTest(endpoint, accessKeyId, accessKeySecret, bucketName, region);
    }

    /**
     * 测试OSS连接
     *
     * @param endpoint        OSS的域名
     * @param accessKeyId     OSS的AccessKeyId
     * @param accessKeySecret OSS的AccessKeySecret
     * @param bucketName      OSS的BucketName
     * @param region          OSS的区域
     * @return 测试结果
     */
    public boolean connectTest(String endpoint, String accessKeyId, String accessKeySecret, String bucketName, String region) {
        log.info("OSS 配置文件开始测试");
        OSS oss;

        InputStream inputStream = null;

        String testObjectKey = "该文件为权限测试文件您可随意删除-" + UUID.randomUUID() + ".txt";

        DefaultCredentialProvider credentialsProvider = new DefaultCredentialProvider
                (accessKeyId, accessKeySecret);

        ClientBuilderConfiguration ossConfig = new ClientBuilderConfiguration();

        ossConfig.setSignatureVersion(SignVersion.V4);

        oss = OSSClientBuilder.create()
                .endpoint(endpoint)
                .credentialsProvider(credentialsProvider)
                .clientConfiguration(ossConfig)
                .region(region)
                .build();

        log.info("成功创建 OSSClient 实例，endpoint: {}, region: {}", endpoint, region);

        try {
            log.info("校验 bucket '{}' 是否存在", bucketName);
            boolean exist = false;
            List<Bucket> buckets = oss.listBuckets();
            for (Bucket bucket : buckets) {
                if (Objects.equals(bucket.getName(), bucketName)) {
                    exist = true;
                    break;
                }
            }

            if (!exist) {
                log.warn("Bucket '{}' 不存在", bucketName);
                throw new BusinessException(bucketName + "不存在 请输入存在的BucketName");
            }
            log.info("Bucket '{}' 存在", bucketName);

        } catch (OSSException oe) {
            log.error("阿里云OSS服务异常: 错误码: {}, 错误信息: {}, 请求ID: {}",
                    oe.getErrorCode(), oe.getErrorMessage(), oe.getRequestId(), oe);
            if ("InvalidAccessKeyId".equalsIgnoreCase(oe.getErrorCode())) {
                log.error("-> 错误原因：Access Key ID 不正确");
                throw new BusinessException("accessKeyId 不正确 请输入正确的accessKeyId");
            } else if ("SignatureDoesNotMatch".equalsIgnoreCase(oe.getErrorCode())) {
                log.error("-> 错误原因：Secret Access Key 不正确，签名不匹配");
                throw new BusinessException("accessKeySecret不正确 签名不匹配");
            } else if ("InvalidArgument".equals(oe.getErrorCode())) {
                log.error("Region 错误 请输入正确的Region");
                throw new BusinessException("Region 错误 请输入正确的Region");
            } else if ("AccessDenied".equalsIgnoreCase(oe.getErrorCode())) {
                log.error("该API密钥权限不足");
                throw new BusinessException("该API密钥对该: " + bucketName + "列表没有读权限");
            } else {
                throw new BusinessException(oe.getErrorMessage());
            }
        } catch (ClientException ce) {
            log.error("可能是网络连接问题或 Endpoint 地址不正确", ce);
            throw new BusinessException("Endpoint 地址不正确");
        }

        try {
            log.info("测试对 bucket '{}' 的读权限...", bucketName);
            ListObjectsRequest listObjectsRequest = new ListObjectsRequest(bucketName).withMaxKeys(1);
            oss.listObjects(listObjectsRequest); // 尝试列出最多1个对象，校验读权限
            log.info("对 bucket '{}' 具备读权限", bucketName);

        } catch (OSSException oe) {
            log.error("测试读权限失败阿里云OSS服务异常: 错误码: {}, 错误信息: {}, 请求ID: {}",
                    oe.getErrorCode(), oe.getErrorMessage(), oe.getRequestId(), oe);
            if ("AccessDenied".equalsIgnoreCase(oe.getErrorCode())) {
                log.error("对bucket没有读权限");
                throw new BusinessException("该API密钥对该: " + bucketName + "没有读权限");
            }
            throw new BusinessException(oe.getMessage());
        }

        try {
            log.info("测试对 bucket '{}' 的写权限...", bucketName);
            byte[] content = "Write permission test.".getBytes(StandardCharsets.UTF_8);
            inputStream = new ByteArrayInputStream(content);
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(content.length);
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, testObjectKey, inputStream, metadata);
            oss.putObject(putObjectRequest);
            log.info("成功向 bucket '{}' 写入测试对象 '{}'，具备写权限", bucketName, testObjectKey);

        } catch (OSSException oe) {
            log.error("测试写权限失败阿里云OSS服务异常: 错误码: {}, 错误信息: {}, 请求ID: {}",
                    oe.getErrorCode(), oe.getErrorMessage(), oe.getRequestId(), oe);
            if ("AccessDenied".equalsIgnoreCase(oe.getErrorCode())) {
                log.error("对bucket没有写权限");
                throw new BusinessException("该API密钥对该: " + bucketName + "没有写权限");
            }
            throw new BusinessException(oe.getMessage());
        } finally {
            // Ensure the input stream for putObject is closed
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                log.error("关闭写入测试对象输入流时发生异常", e);
            }
        }

        try {
            log.info("测试对 bucket '{}' 的删除权限...", bucketName);
            oss.deleteObject(bucketName, testObjectKey);
            log.info("成功从 bucket '{}' 删除测试对象 '{}'，具备删除权限", bucketName, testObjectKey);

        } catch (OSSException oe) {
            log.error("测试删除权限失败阿里云OSS服务异常: 错误码: {}, 错误信息: {}, 请求ID: {}",
                    oe.getErrorCode(), oe.getErrorMessage(), oe.getRequestId(), oe);
            if ("AccessDenied".equalsIgnoreCase(oe.getErrorCode())) {
                log.error("对bucket没有删除权限");
                throw new BusinessException("该API密钥对该: " + bucketName + "没有删除权限");
            }
            throw new BusinessException(oe.getMessage());
        }

        log.info("OSS 配置文件成功完成测试");

        return true;
    }
}

