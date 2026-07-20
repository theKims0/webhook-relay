# ==============================================================
# Dockerfile — Relay (Spring Boot)
# ==============================================================
# Multi-stage build:
#   Stage 1 (builder): Compile & package JAR dengan Maven
#   Stage 2 (runtime): Jalankan JAR di image ringan (JRE saja)
#
# Kenapa multi-stage?
#   - Image akhir tidak membawa Maven, source code, dll
#   - Ukuran image jauh lebih kecil (~150MB vs ~600MB)
# ==============================================================

# ---------------------------------------------------------------
# STAGE 1: BUILD
# Base image: Maven + JDK (untuk compile)
# eclipse-temurin adalah distribusi OpenJDK yang resmi & ringan
# ---------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

# TRICK: Copy pom.xml DULU sebelum source code.
#    Docker cache layer per instruksi — jika pom.xml tidak berubah,
#    layer "mvn dependency:go-offline" di-cache dan tidak dijalankan ulang.
#    Ini membuat build jauh lebih cepat saat hanya ada perubahan kode.
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Setelah deps ter-cache, baru copy source code
COPY src ./src

# Build JAR, skip tests (tests dijalankan di CI terpisah)
# Flag -B = batch mode (no interactive output)
RUN mvn clean package -DskipTests -B


# ---------------------------------------------------------------
# STAGE 2: RUNTIME
# Base image: JRE Alpine (hanya runtime Java, tanpa Maven/JDK)
# Alpine = distro Linux super kecil (~5MB), aman, dan cepat
# ---------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine

# Label metadata image — berguna untuk identifikasi di GHCR
LABEL maintainer="Abdul Khakim"
LABEL app="relay"
LABEL description="Relay Webhook Platform — Spring Boot"

WORKDIR /app

# Copy HANYA file JAR dari stage builder (bukan seluruh /app)
# target/*.jar mengambil file JAR apapun yang ada di folder target
COPY --from=builder /app/target/*.jar app.jar

# Port yang di-expose container (sesuai SERVER_PORT env var)
EXPOSE 8080

# Healthcheck: pastikan app sudah ready sebelum dianggap "healthy"
# Spring Boot Actuator endpoint /actuator/health (jika diaktifkan)
# Fallback: cek apakah port 8080 merespons
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health > /dev/null 2>&1 || exit 1

# Jalankan Spring Boot app
# -Dspring.profiles.active=prod → aktifkan application-prod.properties
# -XX:+UseContainerSupport → JVM otomatis baca memory limit Docker
# -XX:MaxRAMPercentage=75.0 → pakai max 75% dari RAM container
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Dspring.profiles.active=prod", \
    "-jar", "app.jar"]
