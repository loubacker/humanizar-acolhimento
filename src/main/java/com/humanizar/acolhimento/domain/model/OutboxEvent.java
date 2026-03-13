package com.humanizar.acolhimento.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import com.humanizar.acolhimento.domain.model.enums.OutboxStatus;

public class OutboxEvent {

    private Long id;
    private UUID eventId;
    private UUID correlationId;
    private String producerService;
    private String exchangeName;
    private String routingKey;
    private String aggregateType;
    private UUID aggregateId;
    private Short eventVersion;
    private String payload;
    private UUID actorId;
    private String userAgent;
    private String originIp;
    private OutboxStatus status;
    private Integer attemptCount;
    private Integer maxAttempts;
    private LocalDateTime nextRetryAt;
    private String lastError;
    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;
    private UUID lockedBy;

    public OutboxEvent() {
    }

    public OutboxEvent(Long id, UUID eventId, UUID correlationId, String producerService,
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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private UUID eventId;
        private UUID correlationId;
        private String producerService;
        private String exchangeName;
        private String routingKey;
        private String aggregateType;
        private UUID aggregateId;
        private Short eventVersion;
        private String payload;
        private UUID actorId;
        private String userAgent;
        private String originIp;
        private OutboxStatus status;
        private Integer attemptCount;
        private Integer maxAttempts;
        private LocalDateTime nextRetryAt;
        private String lastError;
        private LocalDateTime createdAt;
        private LocalDateTime publishedAt;
        private UUID lockedBy;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder eventId(UUID eventId) {
            this.eventId = eventId;
            return this;
        }

        public Builder correlationId(UUID correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public Builder producerService(String producerService) {
            this.producerService = producerService;
            return this;
        }

        public Builder exchangeName(String exchangeName) {
            this.exchangeName = exchangeName;
            return this;
        }

        public Builder routingKey(String routingKey) {
            this.routingKey = routingKey;
            return this;
        }

        public Builder aggregateType(String aggregateType) {
            this.aggregateType = aggregateType;
            return this;
        }

        public Builder aggregateId(UUID aggregateId) {
            this.aggregateId = aggregateId;
            return this;
        }

        public Builder eventVersion(Short eventVersion) {
            this.eventVersion = eventVersion;
            return this;
        }

        public Builder payload(String payload) {
            this.payload = payload;
            return this;
        }

        public Builder actorId(UUID actorId) {
            this.actorId = actorId;
            return this;
        }

        public Builder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public Builder originIp(String originIp) {
            this.originIp = originIp;
            return this;
        }

        public Builder status(OutboxStatus status) {
            this.status = status;
            return this;
        }

        public Builder attemptCount(Integer attemptCount) {
            this.attemptCount = attemptCount;
            return this;
        }

        public Builder maxAttempts(Integer maxAttempts) {
            this.maxAttempts = maxAttempts;
            return this;
        }

        public Builder nextRetryAt(LocalDateTime nextRetryAt) {
            this.nextRetryAt = nextRetryAt;
            return this;
        }

        public Builder lastError(String lastError) {
            this.lastError = lastError;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder publishedAt(LocalDateTime publishedAt) {
            this.publishedAt = publishedAt;
            return this;
        }

        public Builder lockedBy(UUID lockedBy) {
            this.lockedBy = lockedBy;
            return this;
        }

        public OutboxEvent build() {
            return new OutboxEvent(id, eventId, correlationId, producerService,
                    exchangeName, routingKey, aggregateType, aggregateId, eventVersion,
                    payload, actorId, userAgent, originIp,
                    status, attemptCount, maxAttempts, nextRetryAt,
                    lastError, createdAt, publishedAt, lockedBy);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        OutboxEvent that = (OutboxEvent) o;
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
        return "OutboxEvent{" +
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
