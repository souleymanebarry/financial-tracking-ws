package com.barry.bank.batch.config;

import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MinIO connection properties — prefix {@code minio}.
 */
@ConfigurationProperties(prefix = "minio")
@Data
@ToString(exclude = "secretKey")
public class MinioProperties {

    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucket;
}
