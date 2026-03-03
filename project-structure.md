# order-notification-system — Estrutura de Pacotes

## Visão Geral

```
order-notification-system/
├── order-service/
├── notification-service/
└── docker-compose.yml
```

---

## order-service

```
order-service/
├── src/
│   ├── main/
│   │   ├── java/com/example/orderservice/
│   │   │   ├── OrderServiceApplication.java
│   │   │   │
│   │   │   ├── api/                          # Camada HTTP — só recebe e delega
│   │   │   │   ├── controller/
│   │   │   │   │   └── OrderController.java
│   │   │   │   ├── request/
│   │   │   │   │   └── CreateOrderRequest.java
│   │   │   │   └── response/
│   │   │   │       └── OrderResponse.java
│   │   │   │
│   │   │   ├── domain/                       # Regra de negócio pura — sem Spring aqui
│   │   │   │   ├── model/
│   │   │   │   │   ├── Order.java            # @Entity
│   │   │   │   │   └── OrderStatus.java      # Enum: PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED
│   │   │   │   ├── repository/
│   │   │   │   │   └── OrderRepository.java  # Interface JPA
│   │   │   │   ├── service/
│   │   │   │   │   └── OrderService.java     # Orquestra regras + chama publisher
│   │   │   │   └── exception/
│   │   │   │       ├── OrderNotFoundException.java
│   │   │   │       └── InvalidStatusTransitionException.java
│   │   │   │
│   │   │   ├── messaging/                    # Tudo relacionado a Kafka
│   │   │   │   ├── publisher/
│   │   │   │   │   └── OrderEventPublisher.java
│   │   │   │   └── event/
│   │   │   │       └── OrderStatusChangedEvent.java  # DTO do evento Kafka
│   │   │   │
│   │   │   └── config/                       # Configurações técnicas
│   │   │       ├── KafkaConfig.java
│   │   │       └── GlobalExceptionHandler.java  # @ControllerAdvice
│   │   │
│   │   └── resources/
│   │       ├── application.yml
│   │       └── application-test.yml
│   │
│   └── test/
│       └── java/com/example/orderservice/
│           │
│           ├── unit/                         # Testes unitários — rápidos, sem Spring context
│           │   ├── service/
│           │   │   └── OrderServiceTest.java
│           │   └── messaging/
│           │       └── OrderEventPublisherTest.java
│           │
│           ├── integration/                  # Testes com Testcontainers — sobem infra real
│           │   └── OrderFlowIntegrationTest.java
│           │
│           └── config/
│               └── TestcontainersConfig.java  # Config compartilhada dos containers
│
├── pom.xml
└── Dockerfile
```

### Responsabilidades por camada

| Pacote | Responsabilidade | Pode depender de |
|---|---|---|
| `api` | Receber HTTP, validar input, serializar response | `domain.service` |
| `domain.model` | Entidades e enums de negócio | Ninguém |
| `domain.service` | Regras de negócio, orquestração | `domain.model`, `domain.repository`, `messaging.publisher` |
| `domain.repository` | Contrato de persistência (interface) | `domain.model` |
| `messaging` | Publicar/consumir eventos Kafka | `domain.model` |
| `config` | Beans técnicos e handlers globais | Qualquer um |

---

## notification-service

```
notification-service/
├── src/
│   ├── main/
│   │   ├── java/com/example/notificationservice/
│   │   │   ├── NotificationServiceApplication.java
│   │   │   │
│   │   │   ├── api/                          # REST para consulta de notificações
│   │   │   │   ├── controller/
│   │   │   │   │   └── NotificationController.java
│   │   │   │   └── response/
│   │   │   │       └── NotificationResponse.java
│   │   │   │
│   │   │   ├── domain/
│   │   │   │   ├── model/
│   │   │   │   │   └── Notification.java     # @Entity
│   │   │   │   ├── repository/
│   │   │   │   │   └── NotificationRepository.java
│   │   │   │   ├── service/
│   │   │   │   │   ├── NotificationService.java       # Cria e persiste notificações
│   │   │   │   │   └── NotificationCacheService.java  # Lógica de cache Redis
│   │   │   │   └── exception/
│   │   │   │       └── NotificationNotFoundException.java
│   │   │   │
│   │   │   ├── messaging/
│   │   │   │   ├── consumer/
│   │   │   │   │   └── OrderEventConsumer.java   # @KafkaListener
│   │   │   │   └── event/
│   │   │   │       └── OrderStatusChangedEvent.java  # Mesmo DTO do order-service
│   │   │   │
│   │   │   └── config/
│   │   │       ├── KafkaConfig.java
│   │   │       ├── RedisConfig.java
│   │   │       └── GlobalExceptionHandler.java
│   │   │
│   │   └── resources/
│   │       ├── application.yml
│   │       └── application-test.yml
│   │
│   └── test/
│       └── java/com/example/notificationservice/
│           │
│           ├── unit/
│           │   ├── service/
│           │   │   ├── NotificationServiceTest.java
│           │   │   └── NotificationCacheServiceTest.java
│           │   └── messaging/
│           │       └── OrderEventConsumerTest.java
│           │
│           ├── integration/
│           │   └── NotificationFlowIntegrationTest.java
│           │
│           └── config/
│               └── TestcontainersConfig.java
│
├── pom.xml
└── Dockerfile
```

---

## docker-compose.yml (raiz do projeto)

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: orderdb
      POSTGRES_USER: user
      POSTGRES_PASSWORD: pass
    ports:
      - "5432:5432"

  redis:
    image: redis:7
    ports:
      - "6379:6379"

  zookeeper:
    image: confluentinc/cp-zookeeper:7.4.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181

  kafka:
    image: confluentinc/cp-kafka:7.4.0
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1

  order-service:
    build: ./order-service
    depends_on:
      - postgres
      - kafka
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/orderdb
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092

  notification-service:
    build: ./notification-service
    depends_on:
      - postgres
      - kafka
      - redis
    ports:
      - "8081:8081"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/orderdb
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092
      SPRING_REDIS_HOST: redis
```

---

## Convenção de nomenclatura dos testes (TDD)

Padrão recomendado: **`dado_quando_entao`** (Given/When/Then no nome do método)

```java
// Exemplos em OrderServiceTest.java
@Test
void dado_pedido_valido_quando_criar_deve_retornar_com_id_gerado() {}

@Test
void dado_status_invalido_quando_atualizar_deve_lancar_InvalidStatusTransitionException() {}

@Test
void dado_pedido_inexistente_quando_buscar_deve_lancar_OrderNotFoundException() {}

// Exemplos em OrderEventConsumerTest.java
@Test
void dado_evento_order_confirmed_quando_consumir_deve_criar_notificacao_com_mensagem_correta() {}

@Test
void dado_evento_ja_processado_quando_consumir_deve_ignorar_sem_duplicar_notificacao() {}
```

> Nomes longos são bem-vindos em testes — eles documentam o comportamento do sistema.

---

## Ordem sugerida de implementação (TDD)

```
1. Order.java + OrderStatus.java          (modelo — sem teste necessário)
2. OrderServiceTest.java (RED)            ← escreve os testes primeiro
3. OrderService.java (GREEN)              ← implementa para passar
4. OrderService.java (REFACTOR)           ← revisa SOLID/Clean Code
5. OrderEventPublisherTest.java (RED)
6. OrderEventPublisher.java (GREEN/REFACTOR)
7. OrderController.java                   ← controller é fino, testa no integration test
8. OrderFlowIntegrationTest.java          ← valida o fluxo completo
9. Repete o ciclo para o notification-service
```
