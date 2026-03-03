# order-notification-system

Microsserviços event-driven com Spring Boot e Kafka para notificações de status de pedidos — desenvolvido para praticar Kafka, testes robustos com Testcontainers, cache com Redis e uso de IA como ferramenta no fluxo de desenvolvimento.

---

## Visão Geral

Dois microsserviços comunicando de forma assíncrona via Kafka:

- **order-service** — expõe uma API REST para criar e gerenciar pedidos; publica eventos de mudança de status no Kafka
- **notification-service** — consome eventos de pedidos, gera notificações e armazena em cache no Redis
```
[Cliente REST]
     │
     ▼
[order-service] ──── Kafka (order.events) ────▶ [notification-service]
     │                                                    │
     ▼                                                    ▼
[PostgreSQL]                                    [Redis] + [PostgreSQL]
```

---

## Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 17 |
| Framework | Spring Boot 3.x |
| Mensageria | Apache Kafka (Spring Kafka) |
| Persistência | PostgreSQL + Spring Data JPA |
| Cache | Redis |
| Testes | JUnit 5 + Mockito + Testcontainers |
| Infraestrutura | Docker + Docker Compose |
| Documentação | Swagger / OpenAPI |

---

## Como Executar

### Pré-requisitos

- Docker e Docker Compose
- Java 17+
- Maven 3.8+

### Rodando localmente
```bash
# Subir toda a infraestrutura
docker-compose up -d

# Rodar o order-service
cd order-service
mvn spring-boot:run

# Rodar o notification-service (terminal separado)
cd notification-service
mvn spring-boot:run
```

### Rodando os testes
```bash
# Apenas testes unitários (rápidos, sem infraestrutura)
mvn test -Dgroups="unit"

# Testes de integração (requer Docker — Testcontainers sobe os containers automaticamente)
mvn test -Dgroups="integration"

# Todos os testes
mvn test
```

### Documentação da API

- order-service: http://localhost:8080/swagger-ui.html
- notification-service: http://localhost:8081/swagger-ui.html

---

## Estrutura do Projeto
```
order-notification-system/
├── order-service/
│   └── src/
│       ├── main/java/com/example/orderservice/
│       │   ├── api/          # Controllers, requests, responses
│       │   ├── domain/       # Models, services, repositories, exceptions
│       │   ├── messaging/    # Kafka publisher e event DTOs
│       │   └── config/       # Kafka config, exception handler
│       └── test/java/com/example/orderservice/
│           ├── unit/         # Testes unitários com Mockito
│           └── integration/  # Testes end-to-end com Testcontainers
│
├── notification-service/
│   └── src/
│       ├── main/java/com/example/notificationservice/
│       │   ├── api/          # Controllers, responses
│       │   ├── domain/       # Models, services, repositories, exceptions
│       │   ├── messaging/    # Kafka consumer e event DTOs
│       │   └── config/       # Kafka, Redis config, exception handler
│       └── test/java/com/example/notificationservice/
│           ├── unit/
│           └── integration/
│
└── docker-compose.yml
```

---

## Abordagem de Desenvolvimento

### TDD — Test-Driven Development

O projeto segue TDD estrito. Toda funcionalidade começa com um teste falhando.

Convenção de nomenclatura: `dado_[contexto]_quando_[acao]_deve_[resultado]`
```java
@Test
void dado_pedido_valido_quando_criar_deve_retornar_com_id_gerado() { ... }

@Test
void dado_evento_ja_processado_quando_consumir_deve_ignorar_sem_duplicar_notificacao() { ... }
```

Ciclo RED → GREEN → REFACTOR aplicado de forma consistente em todas as funcionalidades.

### Desenvolvimento Assistido por IA

A IA foi utilizada ativamente como ferramenta de produtividade — não como substituto das decisões de engenharia. Veja o [Log de Uso de IA](#log-de-uso-de-ia) para os detalhes.

Fluxo adotado:
- Desenvolvedor escreve os testes (a especificação)
- IA implementa ou sugere código para satisfazer os testes
- Desenvolvedor revisa SOLID, Clean Code e corretude
- IA sugere refatorações; desenvolvedor aprova e aplica

---

## Decisões de Arquitetura

### ADR-001 — Separação dos pacotes `api` e `domain`
**Decisão:** Manter HTTP (controllers, DTOs) completamente separado da lógica de negócio (services, models).

**Justificativa:** Permite testes unitários do `OrderService` sem carregar contexto Spring. Controllers ficam finos — recebem, validam, delegam e respondem.

### ADR-002 — Redis como cache de leitura para notificações
**Decisão:** Consultas buscam primeiro no Redis (com TTL), caindo para PostgreSQL em cache miss.

**Justificativa:** Notificações são intensivas em leitura e raramente mudam. TTL garante consistência eventual sem invalidação manual.

### ADR-003 — Dead Letter Topic para mensagens com falha
**Decisão:** Mensagens que falham após retries vão para `order.events.DLT` em vez de serem descartadas.

**Justificativa:** Previne perda silenciosa de dados. Mensagens podem ser inspecionadas e reprocessadas manualmente.

### ADR-004 — Controle de idempotência no consumer
**Decisão:** O `notification-service` rastreia IDs de eventos já processados para evitar notificações duplicadas.

**Justificativa:** Kafka garante at-least-once por padrão. Sem idempotência, restart ou rebalanceamento gera duplicatas.

---

## Log de Uso de IA

| Tarefa | Papel da IA | Papel do dev | Aprendizado |
|---|---|---|---|
| docker-compose.yml | Gerou configuração base | Revisou configurações de rede e volumes | Configuração de `advertised listeners` para acesso local vs container |
| Estrutura do OrderService | Sugeriu implementação após testes escritos | Escreveu todos os testes, aprovou estrutura | Primeira versão violou SRP — lógica de publicação separada para `OrderEventPublisher` |
| Configuração do consumer Kafka | Explicou consumer groups, gerou config | Validou com teste de integração | Diferença entre offset reset `earliest` e `latest` |
| Padrão de cache Redis | Explicou cache-aside, gerou `NotificationCacheService` | Escreveu testes para hit/miss/TTL | Importância do TTL para evitar cache stale |
| Setup do Testcontainers | Gerou boilerplate do `TestcontainersConfig` | Depurou timing de startup do Kafka | `@DynamicPropertySource` para injetar portas no contexto Spring |

---

## Conceitos Praticados

**Kafka:** producer/consumer, consumer groups, at-least-once, idempotência, Dead Letter Topic, Testcontainers

**Redis:** padrão cache-aside, TTL, Spring Data Redis com `RedisTemplate`

**Testes:** unitários com Mockito, integração com Testcontainers, ciclo TDD consistente

**Clean Code & SOLID:** Single Responsibility, Dependency Inversion, nomes significativos, métodos pequenos

---

## Licença

MIT