package com.barry.bank.batch.statement.writer;

import com.barry.bank.batch.statement.pdf.StatementPdfResult;
import com.barry.bank.domain.entities.BankStatement;
import com.barry.bank.domain.entities.enums.StatementStatus;
import com.barry.bank.persistence.repositories.BankStatementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Persiste le PDF sur le système de fichiers et fait passer le relevé de
 * {@link StatementStatus#PENDING} à {@link StatementStatus#GENERATED}.
 *
 * <p>{@code batch.pdf.output-dir} définit le répertoire de sortie.
 * MinIO remplacera ce writer dans un step ultérieur.
 */
@Component
@Log4j2
@RequiredArgsConstructor
public class BankStatementPdfWriter implements ItemWriter<StatementPdfResult> {

    private final BankStatementRepository bankStatementRepository;

    @Value("${batch.pdf.output-dir}")
    private String outputDir;

    @Override
    public void write(Chunk<? extends StatementPdfResult> chunk) throws Exception {
        for (StatementPdfResult result : chunk.getItems()) {
            String fileUrl = savePdf(result);

            BankStatement statement = result.statement();
            statement.setFileUrl(fileUrl);
            statement.setStatus(StatementStatus.GENERATED);
            bankStatementRepository.save(statement);

            log.info("PDF généré — relevé: {}, statut: GENERATED, fichier: {}",
                    statement.getId(), fileUrl);
        }
    }

    private String savePdf(StatementPdfResult result) throws Exception {
        BankStatement stmt = result.statement();
        String filename = "statement_%s_%s_%s.pdf".formatted(
                stmt.getAccount().getAccountId(),
                stmt.getPeriodStart(),
                stmt.getPeriodEnd());

        Path dir  = Path.of(outputDir);
        Files.createDirectories(dir);
        Path file = dir.resolve(filename);
        Files.write(file, result.pdfBytes());
        return file.toAbsolutePath().toString();
    }
}
