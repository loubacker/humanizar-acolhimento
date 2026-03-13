package com.humanizar.acolhimento.infrastructure.persistence.entity;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.humanizar.acolhimento.domain.model.enums.OutboxStatus;

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
@Table(name = "outbox_event", uniqueConstraints = {
        @UniqueConstraint(name = "uk_outbox_event_id", columnNames = "event_id")
}, indexes = {
        @Index(name = "idx_outbox_dispatch", columnList = "status,next_retry_at,created_at"),
        @Index(name = "idx_outbox_correlation", columnList = "correlation_id"),
        @Index(name = "idx_outbox_aggregate", columnList = "aggregate_type,aggregate_id,created_at")
})
public class OutboxEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "correlation_id", nullable = false)
    private UUID correlationId;

    @Column(name = "producer_service", nullable = false)
    private String producerService;

    @Column(name = "exchange_name", nullable = false)
    private String exchangeName;

    @Column(name = "routing_key", nullable = false)
    private String routingKey;

    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "event_version", nullable = false)
    private Short eventVersion = 1;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "origin_ip")
    private String originIp;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OutboxStatus status;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount = 0;

    @Column(name = "max_attempts", nullable = false)
    private Integer maxAttempts;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "locked_by")
    private UUID lockedBy;

    public OutboxEventEntity() {
    }

    public OutboxEventEntity(Long id, UUID eventId, UUID correlationId, String producerService,
            String exchangeName, String routingKey, String aggregateType,
            UUID aggregateId, Short eventVersion, String payload,
            UUID actorId, String userAgent, String originIp,
            OutboxStatus status, Integer attemptCount, Integer maxAttempts,
            LocalDateTime nextRetryAt, String lastError, LocalDateTime createdAt,
            LocalDateTime publishedAt, UUID lockedBy) {
        this.id = id;
        this.eventId = eventId;
        this.correlationId = correlationId;
        this.producerService = producerService;
        this.exchangeName = exchangeName;
        this.routingKey = routingKey;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventVersion = eventVersion;
        this.payload = payload;
        this.actorId = actorId;
        this.userAgent = userAgent;
        this.originIp = originIp;
        this.status = status;
        this.attemptCount = attemptCount;
        this.maxAttempts = maxAttempts;
        this.nextRetryAt = nextRetryAt;
        this.lastError = lastError;
        this.createdAt = createdAt;
        this.publishedAt = publishedAt;
        this.lockedBy = lockedBy;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getProducerService() {
        return producerService;
    }

    public void setProducerService(String producerService) {
        this.producerService = producerService;
    }

    public String getExchangeName() {
        return exchangeName;
    }

    public void setExchangeName(String exchangeName) {
        this.exchangeName = exchangeName;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public void setRoutingKey(String routingKey) {
        this.routingKey = routingKey;
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

    public Short getEventVersion() {
        return eventVersion;
    }

    public void setEventVersion(Short eventVersion) {
        this.eventVersion = eventVersion;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
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

    public OutboxStatus getStatus() {
        return status;
    }

    public void setStatus(OutboxStatus status) {
        this.status = status;
    }

    public Integer getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(Integer attemptCount) {
        this.attemptCount = attemptCount;
    }

    public Integer getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(Integer maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public LocalDateTime getNextRetryAt() {
        return nextRetryAt;
    }

    public void setNextRetryAt(LocalDateTime nextRetryAt) {
        this.nextRetryAt = nextRetryAt;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public UUID getLockedBy() {
        return lockedBy;
    }

    public void setLockedBy(UUID lockedBy) {
        this.lockedBy = lockedBy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        OutboxEventEntity that = (OutboxEventEntity) o;
        return Objects.equals(id, that.id)
                && Objects.equals(eventId, that.eventId)
                && Objects.equals(correlationId, that.correlationId)
                && Objects.equals(producerService, that.producerService)
                && Objects.equals(exchangeName, that.exchangeName)
                && Objects.equals(routingKey, that.routingKey)
                && Objects.equals(aggregateType, that.aggregateType)
                && Objects.equals(aggregateId, that.aggregateId)
                && Objects.equals(eventVersion, that.eventVersion)
                && Objects.equals(payload, that.payload)
                && Objects.equals(actorId, that.actorId)
                && Objects.equals(userAgent, that.userAgent)
                && Objects.equals(originIp, that.originIp)
                && Objects.equals(status, that.status)
                && Objects.equals(attemptCount, that.attemptCount)
                && Objects.equals(maxAttempts, that.maxAttempts)
                && Objects.equals(nextRetryAt, that.nextRetryAt)
                && Objects.equals(lastError, that.lastError)
                && Objects.equals(createdAt, that.createdAt)
                && Objects.equals(publishedAt, that.publishedAt)
                && Objects.equals(lockedBy, that.lockedBy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, eventId, correlationId, producerService, exchangeName,
                routingKey, aggregateType, aggregateId, eventVersion, payload,
                actorId, userAgent, originIp,
                status, attemptCount, maxAttempts, nextRetryAt, lastError, createdAt,
                publishedAt, lockedBy);
    }

    @Override
    public String toString() {
        return "OutboxEventEntity{" +
                "id=" + id +
                ", eventId=" + eventId +
                ", correlationId=" + correlationId +
                ", producerService='" + producerService + '\'' +
                ", exchangeName='" + exchangeName + '\'' +
                ", routingKey='" + routingKey + '\'' +
                ", aggregateType='" + aggregateType + '\'' +
                ", aggregateId=" + aggregateId +
                ", eventVersion=" + eventVersion +
                ", payload='" + payload + '\'' +
                ", actorId=" + actorId +
                ", userAgent='" + userAgent + '\'' +
                ", originIp='" + originIp + '\'' +
                ", status=" + status +
                ", attemptCount=" + attemptCount +
                ", maxAttempts=" + maxAttempts +
                ", nextRetryAt=" + nextRetryAt +
                ", lastError='" + lastError + '\'' +
                ", createdAt=" + createdAt +
                ", publishedAt=" + publishedAt +
                ", lockedBy=" + lockedBy +
                '}';
    }
}
