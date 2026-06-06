package com.barry.bank.batch.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
@EnableConfigurationProperties(MinioProperties.class)
@Log4j2
public class MinioConfig {

    private static final int MAX_ATTEMPTS   = 3;
    private static final int RETRY_DELAY_MS = 2000;

    @Bean("minioUploadExecutor")
    public Executor minioUploadExecutor() {
        return Executors.newCachedThreadPool();
    }

    @Bean
    public MinioClient minioClient(MinioProperties props) {
        return MinioClient.builder()
                .endpoint(props.getEndpoint())
                .credentials(props.getAccessKey(), props.getSecretKey())
                .build();
    }

    /**
     * Crée le bucket au démarrage s'il n'existe pas encore.
     * Tente {@value MAX_ATTEMPTS} fois pour absorber les démarrages lents de MinIO.
     * Si MinIO reste indisponible, un warning est loggué et le démarrage se poursuit.
     */
    @Bean
    public CommandLineRunner ensureBucketExists(MinioClient minioClient, MinioProperties props) {
        return args -> initializeBucketWithRetry(minioClient, props.getBucket());
    }

    private void initializeBucketWithRetry(MinioClient minioClient, String bucket)
            throws InterruptedException {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            if (tryInitializeBucket(minioClient, bucket)) return;
            handleFailedAttempt(attempt);
        }
    }

    private boolean tryInitializeBucket(MinioClient minioClient, String bucket) {
        try {
            createBucketIfAbsent(minioClient, bucket);
            return true;
        } catch (Exception e) {
            log.warn("MinIO indisponible — Cause: {}", e.getMessage());
            return false;
        }
    }

    private void createBucketIfAbsent(MinioClient minioClient, String bucket) throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (exists) {
            log.debug("Bucket MinIO existant : {}", bucket);
            return;
        }
        minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        log.info("Bucket MinIO créé : {}", bucket);
    }

    private void handleFailedAttempt(int attempt) throws InterruptedException {
        if (attempt < MAX_ATTEMPTS) {
            log.warn("Tentative {}/{} — nouvelle tentative dans {}ms", attempt, MAX_ATTEMPTS, RETRY_DELAY_MS);
            Thread.sleep(RETRY_DELAY_MS);
        } else {
            log.warn("MinIO toujours indisponible après {} tentatives — le batch échouera à l'exécution", MAX_ATTEMPTS);
        }
    }
}
