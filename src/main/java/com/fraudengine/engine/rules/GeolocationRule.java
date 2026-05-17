package com.fraudengine.engine.rules;

import com.fraudengine.config.RulesConfig;
import com.fraudengine.domain.model.RuleResult;
import com.fraudengine.domain.model.Transaction;
import com.fraudengine.engine.Rule;
import com.fraudengine.infrastructure.cache.LastTransactionLocation;
import com.fraudengine.infrastructure.cache.RedisCacheService;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Component
public class GeolocationRule implements Rule {

    private static final String RULE_NAME = "GeolocationRule";

    // Velocidade máxima física: voo comercial rápido ~900 km/h
    private static final double MAX_PLAUSIBLE_SPEED_KMH = 900.0;

    // Coordenadas aproximadas do centro de cada país (centroide)
    private static final Map<String, double[]> COUNTRY_CENTROIDS = Map.ofEntries(
            Map.entry("BR", new double[]{-14.235, -51.925}),
            Map.entry("US", new double[]{37.090, -95.712}),
            Map.entry("NG", new double[]{9.081,   8.675}),
            Map.entry("RO", new double[]{45.943,  24.966}),
            Map.entry("VN", new double[]{14.058, 108.277}),
            Map.entry("PK", new double[]{30.375,  69.344}),
            Map.entry("GB", new double[]{55.378,  -3.435}),
            Map.entry("DE", new double[]{51.165,  10.451}),
            Map.entry("FR", new double[]{46.227,   2.213}),
            Map.entry("AR", new double[]{-38.416, -63.616}),
            Map.entry("MX", new double[]{23.634, -102.552}),
            Map.entry("CN", new double[]{35.861, 104.195}),
            Map.entry("JP", new double[]{36.204, 138.252}),
            Map.entry("IN", new double[]{20.593,  78.962}),
            Map.entry("ZA", new double[]{-30.559,  22.937})
    );

    private final RulesConfig config;
    private final RedisCacheService cacheService;

    public GeolocationRule(RulesConfig config, RedisCacheService cacheService) {
        this.config = config;
        this.cacheService = cacheService;
    }

    @Override
    public RuleResult evaluate(Transaction transaction) {
        RulesConfig.GeolocationConfig cfg = config.getGeolocation();

        if (!cfg.isEnabled()) {
            return RuleResult.notTriggered(RULE_NAME);
        }

        if (transaction.country() == null) {
            return RuleResult.notTriggered(RULE_NAME);
        }

        // Check 1: impossibilidade física (depende do Redis — falha graciosamente)
        Optional<LastTransactionLocation> lastLocation =
                cacheService.getLastTransactionLocation(transaction.userId());

        if (lastLocation.isPresent()) {
            LastTransactionLocation last = lastLocation.get();
            if (isPhysicallyImpossible(last, transaction, cfg.getImpossibilityWindowMinutes())) {
                String reason = "physically impossible travel: %s → %s in %d minutes"
                        .formatted(last.country(), transaction.country(),
                                minutesBetween(last.transactionAt(), transaction.transactionAt()));
                return RuleResult.triggered(RULE_NAME, cfg.getScoreImpossibility(), reason);
            }
        }

        // Check 2: país de alto risco (sem Redis — sempre avaliado)
        if (cfg.getHighRiskCountries().contains(transaction.country())) {
            String reason = "high-risk country: %s".formatted(transaction.country());
            return RuleResult.triggered(RULE_NAME, cfg.getScoreHighRiskCountry(), reason);
        }

        return RuleResult.notTriggered(RULE_NAME);
    }

    private boolean isPhysicallyImpossible(LastTransactionLocation last,
                                           Transaction current,
                                           int windowMinutes) {
        double[] from = COUNTRY_CENTROIDS.get(last.country());
        double[] to = COUNTRY_CENTROIDS.get(current.country());

        // País sem centroide mapeado — não conseguimos calcular, não disparamos
        if (from == null || to == null) {
            return false;
        }

        long minutes = minutesBetween(last.transactionAt(), current.transactionAt());

        // Só verifica impossibilidade dentro da janela configurada
        if (minutes <= 0 || minutes > windowMinutes) {
            return false;
        }

        double distanceKm = haversineKm(from[0], from[1], to[0], to[1]);
        double hours = minutes / 60.0;
        double impliedSpeedKmh = distanceKm / hours;

        return impliedSpeedKmh > MAX_PLAUSIBLE_SPEED_KMH;
    }

    private long minutesBetween(Instant from, Instant to) {
        return Math.abs(to.getEpochSecond() - from.getEpochSecond()) / 60;
    }

    /**
     * Fórmula de Haversine — distância em km entre dois pontos na superfície terrestre.
     * Usa trigonometria esférica; erro < 0.5% para distâncias continentais.
     */
    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0; // raio da Terra em km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}