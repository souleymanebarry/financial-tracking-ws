package com.barry.bank.document.fop;

import com.barry.bank.document.statement.DocumentRenderingException;
import com.barry.bank.document.statement.StatementData;
import com.barry.bank.document.statement.StatementLineData;
import com.barry.bank.document.statement.StatementRenderer;
import com.barry.bank.domain.entities.enums.OperationType;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.Reader;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Log4j2
public class FopStatementRenderer implements StatementRenderer {

    private static final String[] LABEL_KEYS = {
            "statement.title", "statement.period", "statement.holder",
            "statement.email", "statement.rib", "statement.generated.at",
            "statement.opening.balance", "statement.closing.balance",
            "statement.operations.title", "statement.col.date",
            "statement.col.number", "statement.col.label",
            "statement.col.amount", "statement.col.balance",
            "statement.no.operations", "statement.legal.footer"
    };

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DT_FMT   = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final FopRenderer fopRenderer;
    private final Configuration freemarkerDocumentConfiguration;
    private final MessageSource messageSource;
    private final Map<Locale, Map<String, String>> labelsCache = new ConcurrentHashMap<>();

    public FopStatementRenderer(
            FopRenderer fopRenderer,
            Configuration freemarkerDocumentConfiguration,
            @Qualifier("documentMessageSource") MessageSource messageSource) {
        this.fopRenderer = fopRenderer;
        this.freemarkerDocumentConfiguration = freemarkerDocumentConfiguration;
        this.messageSource = messageSource;
    }

    @Override
    public byte[] render(StatementData data, Locale locale) {
        log.debug("Rendu PDF FOP — relevé: {}, locale: {}", data.statementId(), locale);
        try {
            Reader xslFoReader = generateXslFo(data, locale);
            return fopRenderer.render(xslFoReader);
        } catch (Exception e) {
            throw new DocumentRenderingException(
                    "Échec rendu PDF — relevé: " + data.statementId(), e);
        }
    }

    private Reader generateXslFo(StatementData data, Locale locale) throws IOException, TemplateException {
        Template template = freemarkerDocumentConfiguration
                .getTemplate("statement/bank-statement.fo.ftl");

        AmountFormatter amtFmt = new AmountFormatter(locale);

        Map<String, Object> model = new HashMap<>();
        model.put("labels",         resolveLabels(locale));
        model.put("periodStart",    data.periodStart().format(DATE_FMT));
        model.put("periodEnd",      data.periodEnd().format(DATE_FMT));
        model.put("holderName",     data.customerFullName());
        model.put("email",          data.customerEmail() != null ? data.customerEmail() : "");
        model.put("rib",            data.accountRib() != null ? data.accountRib() : "");
        model.put("generatedAt",    data.generatedAt().format(DT_FMT));
        model.put("openingBalance", amtFmt.format(data.openingBalance()));
        model.put("closingBalance", amtFmt.format(data.closingBalance()));
        model.put("lines",          toLineModels(data.lines(), amtFmt));

        CharArrayWriter writer = new CharArrayWriter(128 * 1024);
        template.process(model, writer);
        return new CharArrayReader(writer.toCharArray());
    }

    private List<Map<String, Object>> toLineModels(List<StatementLineData> lines, AmountFormatter amtFmt) {
        return lines.stream().map(line -> {
            Map<String, Object> m = new HashMap<>();
            m.put("date",           line.operationDate().format(DATE_FMT));
            m.put("number",         line.operationNumber() != null ? line.operationNumber() : "");
            m.put("label",          line.label() != null ? line.label() : "");
            m.put("isDebit",        line.operationType() == OperationType.DEBIT);
            m.put("amount",         amtFmt.format(line.amount()));
            m.put("runningBalance", amtFmt.format(line.runningBalance()));
            return m;
        }).toList();
    }

    private Map<String, String> resolveLabels(Locale locale) {
        return labelsCache.computeIfAbsent(locale, l -> {
            Map<String, String> labels = new HashMap<>();
            for (String key : LABEL_KEYS) {
                labels.put(key, messageSource.getMessage(key, null, l));
            }
            return labels;
        });
    }
}
