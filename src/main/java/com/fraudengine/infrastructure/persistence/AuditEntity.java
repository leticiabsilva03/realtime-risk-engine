package com.fraudengine.infrastructure.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fraud_audit")
public class AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "transaction_id", nullable = false, length = 100)
    private String transactionId;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(name = "device_id", length = 100)
    private String deviceId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "merchant_id", length = 100)
    private String merchantId;

    @Column(name = "country", length = 2)
    private String country;

    @Column(name = "total_score", nullable = false)
    private int totalScore;

    @Column(name = "decision", nullable = false, length = 10)
    private String decision;

    @Column(name = "rules_version", nullable = false, length = 20)
    private String rulesVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rules_triggered", nullable = false, columnDefinition = "jsonb")
    private String rulesTriggered;  // JSON serializado da lista de RuleResult

    @Column(name = "simulated", nullable = false)
    private boolean simulated;

    @Column(name = "evaluated_at", nullable = false)
    private Instant evaluatedAt;

    // construtor privado para JPA
    protected AuditEntity() {}

    public AuditEntity(String transactionId, String userId, String deviceId,
                       BigDecimal amount, String merchantId, String country,
                       int totalScore, String decision, String rulesVersion,
                       String rulesTriggered, boolean simulated, Instant evaluatedAt) {
        this.transactionId = transactionId;
        this.userId = userId;
        this.deviceId = deviceId;
        this.amount = amount;
        this.merchantId = merchantId;
        this.country = country;
        this.totalScore = totalScore;
        this.decision = decision;
        this.rulesVersion = rulesVersion;
        this.rulesTriggered = rulesTriggered;
        this.simulated = simulated;
        this.evaluatedAt = evaluatedAt;
    }

    // Getters
    public UUID getId()               { return id; }
    public String getTransactionId()  { return transactionId; }
    public String getUserId()         { return userId; }
    public String getDeviceId()       { return deviceId; }
    public BigDecimal getAmount()     { return amount; }
    public String getMerchantId()     { return merchantId; }
    public String getCountry()        { return country; }
    public int getTotalScore()        { return totalScore; }
    public String getDecision()       { return decision; }
    public String getRulesVersion()   { return rulesVersion; }
    public String getRulesTriggered() { return rulesTriggered; }
    public boolean isSimulated()      { return simulated; }
    public Instant getEvaluatedAt()   { return evaluatedAt; }
}