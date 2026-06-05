--liquibase formatted sql
--changeset sbarry:006_bank_statement_unique

-- Cet index sert de filet de sécurité au niveau base de données pour garantir qu'un compte ne peut jamais avoir deux relevés pour la même période.
-- Rend l'index de période unique pour empêcher les doublons de relevés
-- (même compte + même période ne peut exister qu'une seule fois)
DROP INDEX IF EXISTS IDX_BANK_STATEMENT_PERIOD;
CREATE UNIQUE INDEX IDX_BANK_STATEMENT_PERIOD ON BANK_STATEMENT(ACCOUNT_ID, PERIOD_START, PERIOD_END);
