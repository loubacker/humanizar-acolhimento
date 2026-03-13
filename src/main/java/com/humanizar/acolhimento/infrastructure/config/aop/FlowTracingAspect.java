package com.humanizar.acolhimento.infrastructure.config.aop;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class FlowTracingAspect {

    private static final Logger log = LoggerFactory.getLogger(FlowTracingAspect.class);
    private static final int MAX_DEPTH = 3;
    private static final boolean FLOW_AOP_ENABLED = false;

    @Pointcut("execution(public * com.humanizar.acolhimento.application.usecase..*(..))")
    public void anyUseCaseMethod() {
    }

    @Pointcut("execution(public * com.humanizar.acolhimento.application.service..*(..))")
    public void anyServiceMethod() {
    }

    @Pointcut("execution(public * com.humanizar.acolhimento.infrastructure.messaging.outbound.outbox.OutboxEventProcessor.*(..))")
    public void anyOutboxProcessorMethod() {
    }

    @Pointcut("execution(public * com.humanizar.acolhimento.infrastructure.messaging.outbound.rabbit.RabbitOutboxPublisher.publish(..))")
    public void rabbitPublishMethod() {
    }

    @Around("anyUseCaseMethod() || anyServiceMethod() || anyOutboxProcessorMethod() || rabbitPublishMethod()")
    public Object traceFlow(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!FLOW_AOP_ENABLED) {
            return joinPoint.proceed();
        }

        long startNanos = System.nanoTime();
        String method = resolveMethod(joinPoint);

        FlowContext inContext = extractContext(joinPoint.getArgs(), null);
        log.info(
                "FLOW|phase=IN|method={}|eventId={}|correlationId={}|exchange={}|routingKey={}|patientId={}",
                method,
                inContext.eventId(),
                inContext.correlationId(),
                inContext.exchange(),
                inContext.routingKey(),
                inContext.patientId());

        try {
            Object result = joinPoint.proceed();
            long durationMs = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
            FlowContext outContext = extractContext(joinPoint.getArgs(), result);
            log.info(
                    "FLOW|phase=OUT|method={}|durationMs={}|eventId={}|correlationId={}|exchange={}|routingKey={}|patientId={}",
                    method,
                    durationMs,
                    outContext.eventId(),
                    outContext.correlationId(),
                    outContext.exchange(),
                    outContext.routingKey(),
                    outContext.patientId());
            return result;
        } catch (Throwable ex) {
            long durationMs = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
            FlowContext errContext = extractContext(joinPoint.getArgs(), null);
            log.error(
                    "FLOW|phase=ERR|method={}|durationMs={}|errorType={}|errorMessage={}|eventId={}|correlationId={}|exchange={}|routingKey={}|patientId={}",
                    method,
                    durationMs,
                    ex.getClass().getSimpleName(),
                    sanitize(ex.getMessage()),
                    errContext.eventId(),
                    errContext.correlationId(),
                    errContext.exchange(),
                    errContext.routingKey(),
                    errContext.patientId(),
                    ex);
            throw ex;
        }
    }

    private String resolveMethod(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getDeclaringTypeName() + "." + signature.getName();
    }

    private FlowContext extractContext(Object[] args, Object result) {
        FlowContextBuilder builder = new FlowContextBuilder();
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());

        inspect(args, builder, visited, 0);
        inspect(result, builder, visited, 0);

        return builder.build();
    }

    private void inspect(Object value, FlowContextBuilder builder, Set<Object> visited, int depth) {
        if (value == null || depth > MAX_DEPTH) {
            return;
        }

        if (value.getClass().isArray()) {
            Object[] values = toObjectArray(value);
            for (Object item : values) {
                inspect(item, builder, visited, depth + 1);
            }
            return;
        }

        if (isLeaf(value.getClass())) {
            return;
        }

        if (!visited.add(value)) {
            return;
        }

        builder.trySetEventId(asText(readProperty(value, "eventId")));
        builder.trySetCorrelationId(asText(readProperty(value, "correlationId")));
        builder.trySetExchange(asText(firstNonNull(
                readProperty(value, "exchangeName"),
                readProperty(value, "exchange"))));
        builder.trySetRoutingKey(asText(readProperty(value, "routingKey")));
        builder.trySetPatientId(asText(readProperty(value, "patientId")));

        List<Object> nestedCandidates = new ArrayList<>();
        nestedCandidates.add(readProperty(value, "payload"));
        nestedCandidates.add(readProperty(value, "envelope"));
        nestedCandidates.add(readProperty(value, "request"));
        nestedCandidates.add(readProperty(value, "command"));
        nestedCandidates.add(readProperty(value, "event"));
        nestedCandidates.add(readProperty(value, "dto"));

        for (Object nested : nestedCandidates) {
            if (nested != null && !isLeaf(nested.getClass())) {
                inspect(nested, builder, visited, depth + 1);
            }
        }
    }

    private Object readProperty(Object target, String propertyName) {
        if (target == null || propertyName == null || propertyName.isBlank()) {
            return null;
        }

        if (target instanceof Map<?, ?> map) {
            return map.get(propertyName);
        }

        Class<?> type = target.getClass();
        String capitalized = Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
        List<String> methodNames = List.of(propertyName, "get" + capitalized, "is" + capitalized);

        for (String methodName : methodNames) {
            Method method = findMethod(type, methodName);
            if (method == null || method.getParameterCount() != 0) {
                continue;
            }
            try {
                method.setAccessible(true);
                return method.invoke(target);
            } catch (ReflectiveOperationException ignored) {
            }
        }

        Field field = findField(type, propertyName);
        if (field != null) {
            try {
                field.setAccessible(true);
                return field.get(target);
            } catch (IllegalAccessException ignored) {
                return null;
            }
        }

        return null;
    }

    private Method findMethod(Class<?> type, String name) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredMethod(name);
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            }
        }

        try {
            return type.getMethod(name);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private Field findField(Class<?> type, String name) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private Object firstNonNull(Object first, Object second) {
        return first != null ? first : second;
    }

    private Object[] toObjectArray(Object array) {
        if (array instanceof Object[] objectArray) {
            return objectArray;
        }
        return new Object[0];
    }

    private boolean isLeaf(Class<?> type) {
        return type.isPrimitive()
                || CharSequence.class.isAssignableFrom(type)
                || Number.class.isAssignableFrom(type)
                || Boolean.class.isAssignableFrom(type)
                || UUID.class.isAssignableFrom(type)
                || Temporal.class.isAssignableFrom(type)
                || Enum.class.isAssignableFrom(type)
                || type.getName().startsWith("java.")
                || type.getName().startsWith("javax.")
                || type.getName().startsWith("jakarta.");
    }

    private String asText(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof UUID uuid) {
            return uuid.toString();
        }
        return sanitize(String.valueOf(value));
    }

    private String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "N/A";
        }
        String sanitized = value
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replace('|', '/')
                .trim();
        if (sanitized.length() > 220) {
            return sanitized.substring(0, 220) + "...";
        }
        return sanitized;
    }

    private record FlowContext(
            String eventId,
            String correlationId,
            String exchange,
            String routingKey,
            String patientId) {
    }

    private static final class FlowContextBuilder {
        private String eventId;
        private String correlationId;
        private String exchange;
        private String routingKey;
        private String patientId;

        void trySetEventId(String value) {
            if (isUnset(eventId) && isSet(value)) {
                eventId = value;
            }
        }

        void trySetCorrelationId(String value) {
            if (isUnset(correlationId) && isSet(value)) {
                correlationId = value;
            }
        }

        void trySetExchange(String value) {
            if (isUnset(exchange) && isSet(value)) {
                exchange = value;
            }
        }

        void trySetRoutingKey(String value) {
            if (isUnset(routingKey) && isSet(value)) {
                routingKey = value;
            }
        }

        void trySetPatientId(String value) {
            if (isUnset(patientId) && isSet(value)) {
                patientId = value;
            }
        }

        FlowContext build() {
            return new FlowContext(
                    defaultValue(eventId),
                    defaultValue(correlationId),
                    defaultValue(exchange),
                    defaultValue(routingKey),
                    defaultValue(patientId));
        }

        private boolean isUnset(String value) {
            return value == null || value.isBlank() || "N/A".equals(value);
        }

        private boolean isSet(String value) {
            return value != null && !value.isBlank() && !"N/A".equals(value);
        }

        private String defaultValue(String value) {
            return isUnset(value) ? "N/A" : value;
        }
    }
}
