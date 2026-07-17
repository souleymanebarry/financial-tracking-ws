/**
 * REST controllers (contrats OpenAPI) et gestion globale des erreurs.
 *
 * <p>{@code @NonNullApi} aligne le contrat de nullité du package sur celui de
 * Spring MVC (paramètres et retours non-null par défaut, exceptions marquées
 * {@code @Nullable}) — requis notamment pour que l'override de
 * {@code ResponseEntityExceptionHandler#handleMethodArgumentNotValid} honore
 * le contrat de la méthode parente (règle Sonar S2638).
 */
@org.springframework.lang.NonNullApi
package com.barry.bank.api.controllers;