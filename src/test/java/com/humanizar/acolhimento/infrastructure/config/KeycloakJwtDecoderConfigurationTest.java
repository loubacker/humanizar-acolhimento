package com.humanizar.acolhimento.infrastructure.config;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

class KeycloakJwtDecoderConfigurationTest {

    private static final String ISSUER = "https://auth.example/realms/humanizar";
    private static final String AUDIENCE = "humanizar-client";
    private static final String KEY_ID = "trusted-key";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Base64.Encoder BASE64_URL = Base64.getUrlEncoder().withoutPadding();

    private static HttpServer jwksServer;
    private static KeyPair trustedKeyPair;
    private static KeyPair untrustedKeyPair;
    private static String jwkSetUrl;

    @BeforeAll
    static void startJwksServer() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        trustedKeyPair = keyPairGenerator.generateKeyPair();
        untrustedKeyPair = keyPairGenerator.generateKeyPair();

        byte[] jwkSet = createJwkSet((RSAPublicKey) trustedKeyPair.getPublic()).getBytes(UTF_8);
        jwksServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        jwksServer.createContext("/jwks", exchange -> writeJwkSet(exchange, jwkSet));
        jwksServer.start();
        jwkSetUrl = "http://127.0.0.1:" + jwksServer.getAddress().getPort() + "/jwks";
    }

    @AfterAll
    static void stopJwksServer() {
        if (jwksServer != null) {
            jwksServer.stop(0);
        }
    }

    @Test
    void resourceServerShouldEnforceSignatureIssuerAudienceExpirationAndRs256() throws Exception {
        ApplicationContextRunner contextRunner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(OAuth2ResourceServerAutoConfiguration.class))
                .withPropertyValues(
                        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=" + jwkSetUrl,
                        "spring.security.oauth2.resourceserver.jwt.issuer-uri=" + ISSUER,
                        "spring.security.oauth2.resourceserver.jwt.audiences=" + AUDIENCE,
                        "spring.security.oauth2.resourceserver.jwt.jws-algorithms=RS256");

        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(JwtDecoder.class);
            JwtDecoder decoder = context.getBean(JwtDecoder.class);

            assertThat(decoder.decode(createToken(trustedKeyPair, ISSUER, List.of(AUDIENCE), 300, "RS256")))
                    .extracting(jwt -> jwt.getSubject(), jwt -> jwt.getIssuer().toString())
                    .containsExactly("canonical-keycloak-user-id", ISSUER);
            assertRejected(decoder, createToken(untrustedKeyPair, ISSUER, List.of(AUDIENCE), 300, "RS256"));
            assertRejected(decoder, createToken(trustedKeyPair, ISSUER + "/other", List.of(AUDIENCE), 300, "RS256"));
            assertRejected(decoder, createToken(trustedKeyPair, ISSUER, List.of("other-audience"), 300, "RS256"));
            assertRejected(decoder, createToken(trustedKeyPair, ISSUER, List.of(AUDIENCE), -30, "RS256"));
            assertRejected(decoder, createToken(trustedKeyPair, ISSUER, List.of(AUDIENCE), 300, "RS512"));
        });
    }

    private static void assertRejected(JwtDecoder decoder, String token) {
        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }

    private static String createToken(
            KeyPair signingKeyPair,
            String issuer,
            List<String> audience,
            long expiresInSeconds,
            String algorithm) throws Exception {
        Instant now = Instant.now();
        String header = encodeJson(Map.of("alg", algorithm, "kid", KEY_ID, "typ", "JWT"));
        String payload = encodeJson(Map.of(
                "iss", issuer,
                "sub", "canonical-keycloak-user-id",
                "aud", audience,
                "iat", now.minusSeconds(5).getEpochSecond(),
                "exp", now.plusSeconds(expiresInSeconds).getEpochSecond()));
        String signingInput = header + "." + payload;

        Signature signature = Signature.getInstance("RS512".equals(algorithm) ? "SHA512withRSA" : "SHA256withRSA");
        signature.initSign(signingKeyPair.getPrivate());
        signature.update(signingInput.getBytes(UTF_8));
        return signingInput + "." + BASE64_URL.encodeToString(signature.sign());
    }

    private static String encodeJson(Map<String, Object> value) throws IOException {
        return BASE64_URL.encodeToString(OBJECT_MAPPER.writeValueAsBytes(value));
    }

    private static String createJwkSet(RSAPublicKey publicKey) throws IOException {
        return OBJECT_MAPPER.writeValueAsString(Map.of(
                "keys",
                List.of(Map.of(
                        "kty", "RSA",
                        "kid", KEY_ID,
                        "use", "sig",
                        "alg", "RS256",
                        "n", encodeUnsigned(publicKey.getModulus()),
                        "e", encodeUnsigned(publicKey.getPublicExponent())))));
    }

    private static String encodeUnsigned(BigInteger value) {
        byte[] signedBytes = value.toByteArray();
        byte[] unsignedBytes = signedBytes.length > 1 && signedBytes[0] == 0
                ? Arrays.copyOfRange(signedBytes, 1, signedBytes.length)
                : signedBytes;
        return BASE64_URL.encodeToString(unsignedBytes);
    }

    private static void writeJwkSet(HttpExchange exchange, byte[] jwkSet) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, jwkSet.length);
        try (var outputStream = exchange.getResponseBody()) {
            outputStream.write(jwkSet);
        } finally {
            exchange.close();
        }
    }
}
