package com.humanizar.acolhimento.infrastructure.persistence.entity;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.humanizar.acolhimento.domain.model.enums.ProcessedResult;
import com.humanizar.acolhimento.domain.model.enums.ReasonCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "processed_event", uniqueConstraints = {
        @UniqueConstraint(name = "uk_processed_consumer_event", columnNames = { "consumer_name", "event_id" })
}, indexes = {
        @Index(name = "idx_processed_correlation", columnList = "correlation_id"),
        @Index(name = "idx_processed_event", columnList = "event_id"),
        @Index(name = "idx_processed_time", columnList = "processed_at"),
        @Index(name = "idx_processed_reason_code", columnList = "reason_code"),
        @Index(name = "idx_processed_source_routing_key", columnList = "source_routing_key")
})
public class ProcessedEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "consumer_name", nullable = false)
    private String consumerName;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "correlation_id", nullable = false)
    private UUID correlationId;

    @Column(name = "source_exchange", nullable = false)
    private String sourceExchange;

    @Column(name = "source_routing_key", nullable = false)
    private String sourceRoutingKey;

    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "origin_ip")
    private String originIp;

    @CreationTimestamp
    @Column(name = "processed_at", nullable = false, updatable = false)
    private LocalDateTime processedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false)
    private ProcessedResult result;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code")
    private ReasonCode reasonCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    public ProcessedEventEntity() {
    }

    public ProcessedEventEntity(Long id, String consumerName, UUID eventId, UUID correlationId,
            String sourceExchange, String sourceRoutingKey, String aggregateType, UUID aggregateId,
            UUID actorId, String userAgent, String originIp,
            LocalDateTime processedAt, ProcessedResult result,
            ReasonCode reasonCode, String errorMessage) {
        this.id = id;
        this.consumerName = consumerName;
        this.eventId = eventId;
        this.correlationId = correlationId;
        this.sourceExchange = sourceExchange;
        this.sourceRoutingKey = sourceRoutingKey;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.actorId = actorId;
        this.userAgent = userAgent;
        this.originIp = originIp;
        this.processedAt = processedAt;
        this.result = result;
        this.reasonCode = reasonCode;
        this.errorMessage = errorMessage;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getConsumerName() {
        return consumerName;
    }

    public void setConsumerName(String consumerName) {
        this.consumerName = consumerName;
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public UUID getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(UUID correlationId) {
        this.correlationId = correlationId;
    }

    public String getSourceExchange() {
        return sourceExchange;
    }

    public void setSourceExchange(String sourceExchange) {
        this.sourceExchange = sourceExchange;
    }

    public String getSourceRoutingKey() {
        return sourceRoutingKey;
    }

    public void setSourceRoutingKey(String sourceRoutingKey) {
        this.sourceRoutingKey = sourceRoutingKey;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public void setAggregateType(String aggregateType) {
        this.aggregateType = aggregateType;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(UUID aggregateId) {
        this.aggregateId = aggregateId;
    }

    public UUID getActorId() {
        return actorId;
    }

    public void setActorId(UUID actorId) {
        this.actorId = actorId;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getOriginIp() {
        return originIp;
    }

    public void setOriginIp(String originIp) {
        this.originIp = originIp;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public ProcessedResult getResult() {
        return result;
    }

    public void setResult(ProcessedResult result) {
        this.result = result;
    }

    public ReasonCode getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(ReasonCode reasonCode) {
        this.reasonCode = reasonCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ProcessedEventEntity that = (ProcessedEventEntity) o;
        return Objects.equals(id, that.id)
                && Objects.equals(consumerName, that.consumerName)
                && Objects.equals(eventId, that.eventId)
                && Objects.equals(correlationId, that.correlationId)
                && Objects.equals(sourceExchange, that.sourceExchange)
                && Objects.equals(sourceRoutingKey, that.sourceRoutingKey)
                && Objects.equals(aggregateType, that.aggregateType)
                && Objects.equals(aggregateId, that.aggregateId)
                && Objects.equals(actorId, that.actorId)
                && Objects.equals(userAgent, that.userAgent)
                && Objects.equals(originIp, that.originIp)
                && Objects.equals(processedAt, that.processedAt)
                && Objects.equals(result, that.result)
                && Objects.equals(reasonCode, that.reasonCode)
                && Objects.equals(errorMessage, that.errorMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, consumerName, eventId, correlationId, sourceExchange,
                sourceRoutingKey, aggregateType, aggregateId, actorId, userAgent,
                originIp, processedAt, result, reasonCode, errorMessage);
    }

    @Override
    public String toString() {
        return "ProcessedEventEntity{" +
                "id=" + id +
                ", consumerName='" + consumerName + '\'' +
                ", eventId=" + eventId +
                ", correlationId=" + correlationId +
                ", sourceExchange='" + sourceExchange + '\'' +
                ", sourceRoutingKey='" + sourceRoutingKey + '\'' +
                ", aggregateType='" + aggregateType + '\'' +
                ", aggregateId=" + aggregateId +
                ", actorId=" + actorId +
                ", userAgent='" + userAgent + '\'' +
                ", originIp='" + originIp + '\'' +
                ", processedAt=" + processedAt +
                ", result=" + result +
                ", reasonCode=" + reasonCode +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}
