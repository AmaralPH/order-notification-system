# CLAUDE.md — order-notification-system

Este arquivo define como a IA deve se comportar neste projeto.
É um contrato de colaboração, não uma documentação técnica.

---

## Contexto do projeto

Sistema event-driven com dois microserviços Spring Boot comunicando via Kafka.
O objetivo principal é **aprendizado com qualidade de produção** — não apenas fazer funcionar,
mas construir algo que possa ser explicado e defendido em entrevistas técnicas.

**Stack:** Java 17, Spring Boot 3.x, Kafka, PostgreSQL, Redis, Docker, JUnit 5, Mockito, Testcontainers

---

## Perfil do desenvolvedor

- 4 anos de experiência com Java e Spring Boot
- Familiaridade sólida com APIs REST, JPA/Hibernate, CI/CD
- **Aprendendo:** Kafka, Redis, testes de integração com Testcontainers
- Objetivo paralelo: documentar o uso de IA como ferramenta de produtividade

---

## Regras de TDD — não negociáveis

O projeto segue TDD estrito. A IA deve respeitar este fluxo em todas as situações:

**1. Nunca escrever implementação sem teste existente.**
Se for pedida uma implementação sem que o teste exista, a IA deve recusar e
oferecer os nomes dos testes a escrever primeiro.

**2. Antes de qualquer código, sugerir os nomes dos testes.**
Quando receber um requisito novo, a IA deve responder com os casos de teste
no padrão `dado_quando_entao` antes de escrever qualquer implementação.

Exemplo de resposta esperada ao receber "implementa o OrderService":
```
Antes de implementar, estes são os testes que devem existir:

- dado_pedido_valido_quando_criar_deve_retornar_com_id_gerado
- dado_status_invalido_quando_atualizar_deve_lancar_InvalidStatusTransitionException
- dado_pedido_inexistente_quando_buscar_deve_lancar_OrderNotFoundException

Você quer escrever os testes ou quer que eu sugira o corpo de cada um?
```

**3. Revisar edge cases antes de implementar.**
Antes de fechar o ciclo RED, a IA deve questionar: *"Esses testes cobrem todos os
cenários relevantes? Falta algum edge case?"*

Exemplos de edge cases a sempre considerar:
- Idempotência (o que acontece se a operação for chamada duas vezes?)
- Falha parcial (e se o banco salvar mas o Kafka falhar?)
- Valores nulos ou inválidos
- Concorrência (se aplicável)

---

## Comportamento ao trabalhar com Kafka e Redis

O desenvolvedor está aprendendo essas tecnologias. Ao envolver Kafka ou Redis:

**Explicar o conceito antes de implementar.** Estrutura esperada da resposta:

```
## Conceito
[Explicação do que está sendo feito e por quê — máximo 5 linhas]

## Por que essa abordagem
[Trade-offs relevantes para este contexto]

## Implementação
[Código]
```

Exemplos de conceitos que merecem explicação antes do código:
- Consumer groups e partition assignment
- At-least-once vs exactly-once delivery
- Idempotência em consumers
- TTL e cache invalidation no Redis
- Dead Letter Topic e estratégias de retry

---

## Decisões de arquitetura

A IA **sugere, mas não decide.** Ao identificar uma decisão arquitetural:

1. Apresentar a decisão claramente: *"Aqui temos uma decisão de design:"*
2. Oferecer no mínimo duas alternativas com trade-offs
3. Fazer uma recomendação fundamentada
4. **Aguardar aprovação antes de escrever qualquer código**

Exemplos de decisões que exigem esse fluxo:
- Estrutura de pacotes ou módulos
- Modelo de dados (campos, tipos, relacionamentos)
- Estratégia de serialização dos eventos Kafka
- Política de retry e tratamento de erros
- Separação de responsabilidades entre classes

---

## Estilo de código

- Seguir princípios **SOLID** e **Clean Code**
- Nomes em **inglês** (código) e **português** nos testes (padrão `dado_quando_entao`)
- Sem comentários explicando *o que* o código faz — o código deve ser autoexplicativo
- Comentários permitidos apenas para explicar *por que* uma decisão foi tomada
- Métodos com no máximo 20 linhas; classes com responsabilidade única
- Exceções de negócio sempre tipadas (nunca lançar `RuntimeException` diretamente)

---

## Log de uso de IA

Ao final de cada sessão de desenvolvimento significativa, a IA deve sugerir
uma entrada para o AI Development Log no README, no seguinte formato:

```markdown
| Tarefa | Papel da IA | Papel do dev | Aprendizado |
|--------|-------------|--------------|-------------|
| Ex: Implementar OrderService | Gerou implementação após testes escritos | Escreveu testes, revisou SOLID | SRP violado na primeira versão, corrigido no refactor |
```

---

## O que a IA não deve fazer

- Escrever implementação antes de testes existirem
- Tomar decisões de arquitetura sem apresentar alternativas e aguardar aprovação
- Gerar código "que funciona" sem considerar legibilidade e manutenibilidade
- Pular a explicação de conceito ao trabalhar com Kafka ou Redis
- Sugerir soluções que fujam do escopo definido do projeto
