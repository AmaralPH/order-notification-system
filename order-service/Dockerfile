# =================================================================
# STAGE 1 — BUILD
# Imagem completa com Maven para compilar o projeto.
# Esta imagem é temporária — será descartada após o build.
# =================================================================
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# Copia apenas o pom.xml primeiro.
# O Docker cacheia esta camada separadamente do código.
# Resultado: se você mudar só o código (não o pom.xml),
# o Maven não precisa baixar as dependências novamente.
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Agora copia o código fonte e compila.
# -DskipTests porque os testes rodam no pipeline de CI, não no build da imagem.
COPY src ./src
RUN mvn package -DskipTests -B

# Extrai o JAR em camadas explícitas do Spring Boot.
# Isso separa: dependências / snapshot-dependencies / spring-boot-loader / application
# Cada uma vira uma camada Docker independente — otimiza o cache no rebuild.
RUN java -Djarmode=layertools -jar target/*.jar extract --destination target/extracted

# =================================================================
# STAGE 2 — RUNTIME
# Imagem mínima com apenas o JRE para executar a aplicação.
# Sem Maven, sem código fonte, sem ferramentas de build.
# =================================================================
FROM eclipse-temurin:17-jre

WORKDIR /app

# Cria um usuário não-root para rodar a aplicação.
# Rodar como root dentro do container é uma má prática de segurança.
RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser

# Copia as camadas extraídas na ordem certa — da que muda menos para a que muda mais.
# O Docker cacheia cada COPY como uma camada separada.
COPY --from=builder /app/target/extracted/dependencies/ ./
COPY --from=builder /app/target/extracted/spring-boot-loader/ ./
COPY --from=builder /app/target/extracted/snapshot-dependencies/ ./
COPY --from=builder /app/target/extracted/application/ ./

# Define o usuário não-root antes de iniciar
USER appuser

# Porta que o order-service expõe (deve bater com server.port no application.yml)
EXPOSE 8080

# Inicia a aplicação usando o launcher de layers do Spring Boot
# JVM flags:
# -XX:+UseContainerSupport        → JVM respeita os limites de CPU/memória do container
# -XX:MaxRAMPercentage=75.0       → usa no máximo 75% da RAM disponível no container
# -Djava.security.egd=...         → acelera a inicialização em ambientes sem entropia suficiente
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "org.springframework.boot.loader.launch.JarLauncher"]
