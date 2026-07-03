package com.barry.bank.domain.enumerations;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Type de compte bancaire. Source unique de vérité pour le libellé qui sert à la fois
 * de discriminateur JPA ({@code @DiscriminatorValue}) et de valeur exposée par l'API.
 *
 * <p>Les libellés (avec espace) sont figés par les données existantes en base :
 * ne pas les modifier sans migration.
 */
@Getter
@RequiredArgsConstructor
public enum AccountType {

    CURRENT_ACCOUNT(Values.CURRENT_ACCOUNT),
    SAVING_ACCOUNT(Values.SAVING_ACCOUNT);

    private final String label;

    /**
     * Constantes de compilation pour les annotations ({@code @DiscriminatorValue},
     * {@code @Schema}), qui n'acceptent pas d'appel de méthode.
     */
    public static final class Values {

        public static final String CURRENT_ACCOUNT = "CURRENT ACCOUNT";
        public static final String SAVING_ACCOUNT = "SAVING ACCOUNT";

        private Values() {
        }
    }
}
