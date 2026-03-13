package com.humanizar.acolhimento.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import com.humanizar.acolhimento.domain.model.enums.ProcessedResult;
import com.humanizar.acolhimento.domain.model.enums.ReasonCode;

public class ProcessedEvent {

    private Long id;
    private String consumerName;
    private UUID eventId;
    private UUID correlationId;
    private String sourceExchange;
    private String sourceRoutingKey;
    private String aggregateType;
    private UUID aggregateId;
    private UUID actorId;
    private String userAgent;
    private String originIp;
    private LocalDateTime processedAt;
    private ProcessedResult result;
    private ReasonCode reasonCode;
    private String errorMessage;

    public ProcessedEvent() {
    }

    public ProcessedEvent(Long id, String consumerName, UUID eventId, UUID correlationId,
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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String consumerName;
        private UUID eventId;
        private UUID correlationId;
        private String sourceExchange;
        private String sourceRoutingKey;
        private String aggregateType;
        private UUID aggregateId;
        private UUID actorId;
        private String userAgent;
        private String originIp;
        private LocalDateTime processedAt;
        private ProcessedResult result;
        private ReasonCode reasonCode;
        private String errorMessage;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder consumerName(String consumerName) {
            this.consumerName = consumerName;
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

        public Builder sourceExchange(String sourceExchange) {
            this.sourceExchange = sourceExchange;
            return this;
        }

        public Builder sourceRoutingKey(String sourceRoutingKey) {
            this.sourceRoutingKey = sourceRoutingKey;
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

        public Builder processedAt(LocalDateTime processedAt) {
            this.processedAt = processedAt;
            return this;
        }

        public Builder result(ProcessedResult result) {
            this.result = result;
            return this;
        }

        public Builder reasonCode(ReasonCode reasonCode) {
            this.reasonCode = reasonCode;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public ProcessedEvent build() {
            return new ProcessedEvent(id, consumerName, eventId, correlationId,
                    sourceExchange, sourceRoutingKey, aggregateType, aggregateId,
                    actorId, userAgent, originIp, processedAt, result, reasonCode,
                    errorMessage);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ProcessedEvent that = (ProcessedEvent) o;
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
        return "ProcessedEvent{" +
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
