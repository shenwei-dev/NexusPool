package com.family.chat.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import java.net.URI;

@Configuration
public class OssConfig {

    @Value("${oss.b2.endpoint}")
    private String endpoint;

    @Value("${oss.b2.region}")
    private String region;

    @Value("${oss.b2.access-key-id}")
    private String accessKeyId;

    @Value("${oss.b2.secret-access-key}")
    private String secretAccessKey;

    /**
     * 初始化 S3 签名器 (用于生成私有桶的临时访问 URL 与直传凭证)
     */
    @Lazy
    @Bean
    public S3Presigner s3Presigner() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey);

        return S3Presigner.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }
}