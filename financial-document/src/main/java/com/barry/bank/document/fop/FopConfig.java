package com.barry.bank.document.fop;

import lombok.extern.log4j.Log4j2;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.FopFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

@Configuration
@Log4j2
public class FopConfig {

    @Bean
    public FopFactory fopFactory() throws URISyntaxException {
        URI baseUri = Objects.requireNonNull(
                FopConfig.class.getResource("/fop/"),
                "Ressource /fop/ introuvable sur le classpath"
        ).toURI();
        FopFactory factory = new FopFactoryBuilder(baseUri).build();
        log.info("FopFactory initialisé — baseUri: {}", baseUri);
        return factory;
    }
}