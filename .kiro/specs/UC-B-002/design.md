# UC-B-002 — Design

## Contesto

`@EnableAutoConfiguration` è presente su `RefreshToken` e `Role`. È un'annotazione di bootstrap Spring Boot, non ha alcun significato su un'entity JPA.

## Soluzione

Rimozione pura dell'annotazione e del relativo import da entrambi i file. Nessuna sostituzione necessaria.

### Impatto

| Componente | Modifica |
|---|---|
| `RefreshToken.java` | Rimozione `@EnableAutoConfiguration` + import |
| `Role.java` | Rimozione `@EnableAutoConfiguration` + import |

### Test di regressione

Test via reflection che scansiona il package `model.entity` e verifica che nessuna classe sia annotata con `@EnableAutoConfiguration`.
