package com.barry.bank.batch.statement.writer;

import com.barry.bank.batch.config.MinioProperties;
import com.barry.bank.batch.statement.pdf.StatementPdfResult;
import com.barry.bank.domain.entities.BankStatement;
import com.barry.bank.domain.entities.enums.StatementStatus;
import com.barry.bank.persistence.repositories.BankStatementRepository;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.MinioException;
import lombok.extern.log4j.Log4j2;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

/**
 * Upload le PDF dans MinIO et fait passer le relevé de
 * {@link StatementStatus#PENDING} à {@link StatementStatus#GENERATED}.
 *
 * <p>La {@code fileUrl} stockée en base est la clé objet MinIO :
 * {@code statements/{year}/{month}/{accountId}_{periodStart}_{periodEnd}.pdf}
 *
 * <p>Les uploads MinIO d'un même chunk sont exécutés en parallèle via
 * {@code minioUploadExecutor} ; le {@code saveAll} DB n'est lancé
 * qu'une fois tous les uploads terminés.
 */
@Component
@Log4j2
public class BankStatementPdfWriter implements ItemWriter<StatementPdfResult> {

    private final BankStatementRepository bankStatementRepository;
    private final MinioClient minioClient;
    private final MinioProperties minioProperties;
    private final Executor uploadExecutor;

    public BankStatementPdfWriter(BankStatementRepository bankStatementRepository,
                                  MinioClient minioClient,
                                  MinioProperties minioProperties,
                                  @Qualifier("minioUploadExecutor") Executor uploadExecutor) {
        this.bankStatementRepository = bankStatementRepository;
        this.minioClient             = minioClient;
        this.minioProperties         = minioProperties;
        this.uploadExecutor          = uploadExecutor;
    }

    @Override
    public void write(Chunk<? extends StatementPdfResult> chunk) throws Exception {
        List<CompletableFuture<BankStatement>> futures = chunk.getItems().stream()
                .map(result -> CompletableFuture.supplyAsync(() -> uploadAndPrepare(result), uploadExecutor))
                .toList();

        List<BankStatement> toSave;
        try {
            toSave = futures.stream().map(CompletableFuture::join).toList();
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception ex) throw ex;
            throw e;
        }

        bankStatementRepository.saveAll(toSave);
    }

    private BankStatement uploadAndPrepare(StatementPdfResult result) {
        String objectKey = buildObjectKey(result.statement());
        try {
            upload(objectKey, result.pdfBytes());
        } catch (Exception e) {
            throw new CompletionException(e);
        }
        BankStatement statement = result.statement();
        statement.setFileUrl(objectKey);
        statement.setStatus(StatementStatus.GENERATED);
        log.info("PDF uploadé — relevé: {}, bucket: {}, clé: {}",
                statement.getId(), minioProperties.getBucket(), objectKey);
        return statement;
    }

    private void upload(String objectKey, byte[] pdfBytes)
            throws MinioException, IOException, InvalidKeyException, NoSuchAlgorithmException {
        // La clé est déterministe (accountId + période) : si l'upload réussit mais que
        // le saveAll() JPA échoue ensuite, le relevé reste PENDING et sera retraité.
        // MinIO écrasera silencieusement le fichier existant — opération idempotente.
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(minioProperties.getBucket())
                .object(objectKey)
                .stream(new ByteArrayInputStream(pdfBytes), pdfBytes.length, -1)
                .contentType("application/pdf")
                .build());
    }

    private String buildObjectKey(BankStatement stmt) {
        return "statements/%d/%02d/%s_%s_%s.pdf".formatted(
                stmt.getPeriodStart().getYear(),
                stmt.getPeriodStart().getMonthValue(),
                stmt.getAccount().getAccountId(),
                stmt.getPeriodStart(),
                stmt.getPeriodEnd());
    }
}