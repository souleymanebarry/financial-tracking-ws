package com.barry.bank.batch.statement.writer;

import com.barry.bank.batch.config.MinioProperties;
import com.barry.bank.batch.statement.pdf.StatementPdfResult;
import com.barry.bank.domain.entities.BankStatement;
import com.barry.bank.domain.entities.enums.StatementStatus;
import com.barry.bank.persistence.repositories.BankStatementRepository;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * Upload le PDF dans MinIO et fait passer le relevé de
 * {@link StatementStatus#PENDING} à {@link StatementStatus#GENERATED}.
 *
 * <p>La {@code fileUrl} stockée en base est la clé objet MinIO :
 * {@code statements/{year}/{month}/{accountId}_{periodStart}_{periodEnd}.pdf}
 */
@Component
@Log4j2
@RequiredArgsConstructor
public class BankStatementPdfWriter implements ItemWriter<StatementPdfResult> {

    private final BankStatementRepository bankStatementRepository;
    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    @Override
    public void write(Chunk<? extends StatementPdfResult> chunk) throws Exception {
        for (StatementPdfResult result : chunk.getItems()) {
            String objectKey = buildObjectKey(result.statement());
            upload(objectKey, result.pdfBytes());

            BankStatement statement = result.statement();
            statement.setFileUrl(objectKey);
            statement.setStatus(StatementStatus.GENERATED);
            bankStatementRepository.save(statement);

            log.info("Downloaded PDF — relevé: {}, bucket: {}, clé: {}",
                    statement.getId(), minioProperties.getBucket(), objectKey);
        }
    }

    private void upload(String objectKey, byte[] pdfBytes)
            throws MinioException, IOException, InvalidKeyException, NoSuchAlgorithmException {
        // La clé est déterministe (accountId + période) : si l'upload réussit mais que
        // le save() JPA échoue ensuite, le relevé reste PENDING et sera retraité.
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
