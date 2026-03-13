# -- Stage de Build (GraalVM Java 25) --
FROM ghcr.io/graalvm/native-image-community:25 AS builder

WORKDIR /app

# Copia Arquivos Necessários para o Build
COPY mvnw .
COPY .mvn/ .mvn/
COPY pom.xml .

# Permissão de execução no Wrapper mvnw
RUN chmod +x mvnw

# Baixa Dependências no Modo Nativo
RUN ./mvnw -Pnative -DskipTests dependency:go-offline

# Copia o Código-Fonte para o Container
COPY src ./src

# Compila a Aplicação para um Binário Nativo
RUN ./mvnw -Pnative -DskipTests native:compile

# -- Stage Runtime (Debian Slim) --
FROM debian:bookworm-slim

WORKDIR /app

# Cria um user non-root
RUN groupadd -r appgroup && useradd -r -g appgroup appuser

# Copia o Binário Nativo do Stage Build para o Stage Runtime
COPY --from=builder --chown=appuser:appgroup /app/target/humanizar-acolhimento /app/app-binario

EXPOSE 9099
USER appuser

# Ponto de Entrada no Docker, Executa o Binário Nativo com Configurações de TTL para DNS
ENTRYPOINT ["/app/app-binario", "-Dsun.net.inetaddr.ttl=30", "-Dsun.net.inetaddr.negative.ttl=2"]