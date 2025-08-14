package com.sparkseries.module.oss.provider.service.impl.minio.connection;


import com.sparkseries.common.util.exception.BusinessException;
import com.sparkseries.module.oss.cloud.dto.CloudConfigDTO;
import com.sparkseries.module.oss.provider.service.base.connection.ValidConnectService;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.MinioException;
import io.minio.messages.ErrorResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * Minio连接验证服务实现类
 */
@Slf4j
public class MinioValidConnectServiceImpl implements ValidConnectService {
    /**
     * @param config 接收到云服务配置文件
     * @return 验证结果
     */
    @Override
    public boolean validConnect(CloudConfigDTO config) {

        String endpoint = config.getMinioEndPoint();
        String accessKey = config.getMinioAccessKey();
        String secretKey = config.getMinioSecretKey();
        String bucketName = config.getMinioBucketName();

        return connectTest(endpoint, accessKey, secretKey, bucketName);

    }

    /**
     * 测试Minio连接
     *
     * @param endpoint   Minio的endpoint
     * @param accessKey  Minio的accessKey
     * @param secretKey  Minio的secretKey
     * @param bucketName Minio的bucketName
     * @return 测试结果
     */
    public boolean connectTest(String endpoint, String accessKey, String secretKey, String bucketName) {
        try (MinioClient minioClient = MinioClient.builder().endpoint(endpoint).credentials(accessKey, secretKey).build()) {
            log.info("成功创建 MinioClient 实例，endpoint: {}", endpoint);
            try {
                minioClient.listBuckets();

                log.info("成功列出所有 bucket，accessKey 和 secretKey 有效。");

            } catch (MinioException | InvalidKeyException e) {
                log.error("Minio 连接失败", e);
                if (e instanceof ErrorResponseException) {
                    ErrorResponse errorResponse = ((ErrorResponseException) e).errorResponse();
                    String code = errorResponse.code();
                    if ("SignatureDoesNotMatch".equals(code)) {
                        log.error("签名错误 请输入正确的签名");
                        throw new BusinessException("Minio连接失败 secretKey错误");
                    } else if ("InvalidAccessKeyId".equals(code)) {
                        log.error("你输入的密钥无效 请输入正确的密钥");
                        throw new BusinessException("Minio连接失败 accessKey错误");
                    } else if ("AccessDenied".equals(code)) {
                        log.error("你没有相关的操作权限");
                    }
                }

            } catch (IOException e) {
                log.error("请输入正确的endpoint");
                throw new BusinessException("请输入正确的endpoint");
            } catch (NoSuchAlgorithmException e) {
                log.error("创建 MinioClient 实例时发生异常。", e);
                throw new BusinessException("创建 MinioClient 实例时发生异常");
            } catch (IllegalArgumentException e) {
                log.error("请输入正确的endpoint格式");
            }


            // 2. 校验 bucket 是否存在
            try {
                boolean found = minioClient.bucketExists(io.minio.BucketExistsArgs.builder().bucket(bucketName).build());
                if (!found) {
                    log.warn("Bucket '{}' 不存在", bucketName);
                    throw new BusinessException("你输入的bucket不存在 请输入存在的bucketName");
                }
                log.info("Bucket '{}' 存在。", bucketName);

            } catch (MinioException | InvalidKeyException | NoSuchAlgorithmException | IOException e) {
                log.error("检查 bucket '{}' 是否存在时发生异常。", bucketName, e);
                if (e instanceof ErrorResponseException) {
                    ErrorResponse errorResponse = ((ErrorResponseException) e).errorResponse();
                    String code = errorResponse.code();
                    if ("AccessDenied".equals(code)) {
                        log.error("Minio 该API密钥没有读权限");
                        throw new BusinessException("该API密钥没有读权限");
                    }
                }
                throw new BusinessException(e.getMessage());
            }


            // 3. 校验读写删除权限
            String testObjectName = "该文件为权限测试文件您可随意删除-" + System.currentTimeMillis() + ".txt";
            byte[] testContent = "This is a test object for Minio permission validation.".getBytes();

            // 尝试写入对象 (写权限校验)
            try (InputStream inputStream = new ByteArrayInputStream(testContent)) {
                try {
                    minioClient.putObject(
                            PutObjectArgs.builder()
                                    .bucket(bucketName)
                                    .object(testObjectName)
                                    .stream(inputStream, testContent.length, -1)
                                    .contentType("text/plain")
                                    .build());
                    log.info("成功向 bucket '{}' 写入测试对象 '{}'，具备写权限。", bucketName, testObjectName);
                } catch (MinioException e) {
                    log.error("检查 bucket '{}' 是否存在时发生异常。", bucketName, e);
                    if (e instanceof ErrorResponseException) {
                        ErrorResponse errorResponse = ((ErrorResponseException) e).errorResponse();
                        String code = errorResponse.code();
                        if ("AccessDenied".equals(code)) {
                            log.error("Minio 该API密钥没有写权限");
                            throw new BusinessException("该API密钥没有写权限");
                        }
                    }
                    throw new BusinessException(e.getMessage());
                } catch (InvalidKeyException | NoSuchAlgorithmException | IOException e) {
                    log.error("写入测试对象时发生异常。", e);
                    throw new BusinessException("Minio 写入测试发生异常");
                }
            } catch (IOException e) {
                log.error("关闭输入流失败。", e);
            }

            try (InputStream readStream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(testObjectName)
                            .build())) {
                // 简单读取部分内容，不进行完整内容比对
                byte[] buffer = new byte[1024];
                int bytesRead = readStream.read(buffer);
                if (bytesRead > 0) {
                    log.info("成功从 bucket '{}' 读取测试对象 '{}'，具备读权限。", bucketName, testObjectName);
                } else {
                    log.warn("从 bucket '{}' 读取测试对象 '{}' 失败，可能不具备读权限或对象内容为空。", bucketName, testObjectName);
                    throw new BusinessException("该bucket对外并没有开放读权限");
                }
            } catch (MinioException e) {
                log.warn("从 bucket '{}' 读取测试对象 '{}' 失败，可能不具备读权限。", bucketName, testObjectName, e);
                throw new BusinessException("该bucket对外并没有开放读权限");
            } catch (InvalidKeyException | NoSuchAlgorithmException | IOException e) {
                log.error("读取测试对象时发生异常。", e);
                throw new BusinessException("Minio 读取测试发生异常");
            }
            try {
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(bucketName)
                                .object(testObjectName)
                                .build());
                log.info("成功从 bucket '{}' 删除测试对象 '{}'，具备删除权限。", bucketName, testObjectName);
            } catch (MinioException e) {
                log.warn("从 bucket '{}' 删除测试对象 '{}' 失败，可能不具备删除权限。", bucketName, testObjectName, e);
                throw new BusinessException("该bucket对外并没有开放删除权限");
            } catch (InvalidKeyException | NoSuchAlgorithmException | IOException e) {
                log.error("删除测试对象时发生异常。", e);
                throw new BusinessException("Minio 删除测试发生异常");
            }

        } catch (Exception e) {
            log.warn("minio 测试类关闭失败");
            throw new BusinessException(e.getMessage());
        }
        return true;
    }
}
