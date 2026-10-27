FROM rust:1.99.0-bookworm AS builder

WORKDIR /app

COPY Cargo.toml Cargo.lock rust-toolchain.toml ./
COPY src ./src

RUN cargo build --release --locked

FROM debian:bookworm-slim AS runtime

WORKDIR /app

RUN apt-get update \
    && apt-get install --yes --no-install-recommends ca-certificates curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 10001 appgroup \
    && useradd --system --uid 10001 --gid appgroup --no-create-home --shell /usr/sbin/nologin appuser

COPY --from=builder --chown=appuser:appgroup /app/target/release/humanizar-acolhimento /app/humanizar-acolhimento

ENV APP_ENV=production \
    SERVER_HOST=0.0.0.0 \
    SERVER_PORT=9099

EXPOSE 9099

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD ["curl", "--fail", "--silent", "--show-error", "http://127.0.0.1:9099/health"]

USER appuser

ENTRYPOINT ["/app/humanizar-acolhimento"]
