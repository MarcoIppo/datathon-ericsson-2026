# UC-B-001 — Design

## Contesto

La classe `DateAudit` è una `@MappedSuperclass` che fornisce campi `createdAt`/`updatedAt` con `@CreatedDate`/`@LastModifiedDate` e registra `AuditingEntityListener`. Tuttavia, Spring Data JPA Auditing non è abilitato: manca `@EnableJpaAuditing` su qualsiasi `@Configuration`.

Di conseguenza, `AuditingEntityListener` non viene attivato e i campi restano `null` al persist/update.

## Soluzione

### Approccio: classe `@Configuration` dedicata

Creare `JpaAuditingConfig.java` nel package `configuration` con la sola annotazione `@EnableJpaAuditing`.

```java
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
```

### Perché una classe dedicata
- Separazione di responsabilità (non si sovraccarica `SecurityConfig` o `CorsConfig`)
- Facilmente individuabile e testabile in isolamento
- Pattern standard nei progetti Spring Data JPA

### Flusso dopo il fix

```
Entity persist/update
  → AuditingEntityListener intercetta l'evento
  → Spring Auditing infrastructure (abilitata da @EnableJpaAuditing) popola:
      - createdAt → LocalDateTime.now() al primo persist
      - updatedAt → LocalDateTime.now() ad ogni save
```

### Impatto

| Componente | Modifica |
|---|---|
| `JpaAuditingConfig.java` | Nuovo file in `configuration/` |
| `DateAudit.java` | Nessuna modifica |
| Entità (7) | Nessuna modifica |
| Schema DB | Nessuna modifica |

### Test di regressione

Integration test con `@DataJpaTest` che:
1. Persiste un `Role` e asserisce `createdAt != null`
2. Modifica il `Role`, fa flush, e asserisce `updatedAt != null` e `updatedAt >= createdAt`

`@DataJpaTest` carica automaticamente la configurazione JPA (inclusa `JpaAuditingConfig`) quindi il test è sufficiente a validare il fix.
