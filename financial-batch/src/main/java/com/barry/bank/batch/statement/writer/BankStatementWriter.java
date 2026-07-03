package com.barry.bank.batch.statement.writer;

import com.barry.bank.domain.model.BankStatement;
import com.barry.bank.persistence.repositories.BankStatementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
@Log4j2
@RequiredArgsConstructor
public class BankStatementWriter implements ItemWriter<BankStatement> {

    private final BankStatementRepository bankStatementRepository;

    @Override
    public void write(Chunk<? extends BankStatement> chunk) {
        bankStatementRepository.saveAll(chunk.getItems());
        log.info("BankStatementWriter — {} relevés persistés", chunk.size());
    }
}
