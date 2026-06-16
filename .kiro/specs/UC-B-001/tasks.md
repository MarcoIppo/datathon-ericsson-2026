# UC-B-001 — Tasks

## Task 1: Creare `JpaAuditingConfig.java`
- Creare `src/main/java/org/elis/ericsson/datathon/user_management/configuration/JpaAuditingConfig.java`
- Annotare con `@Configuration` e `@EnableJpaAuditing`
- Nessuna logica aggiuntiva

## Task 2: Test di evidenza (pre-fix, opzionale)
- Creare `src/test/java/org/elis/ericsson/datathon/user_management/audit/JpaAuditingTest.java`
- Verificare che senza il fix i campi audit sono `null` (documentazione del bug)
- Questo test verrà invertito nel Task 3

## Task 3: Test di regressione
- Nel file `JpaAuditingTest.java`, scrivere un test `@DataJpaTest` che:
  1. Persiste un `Role` → asserisce `createdAt` non è `null`
  2. Modifica il `Role`, flush → asserisce `updatedAt` non è `null` e `>= createdAt`
- Annotare con Javadoc descrittivo

## Task 4: Verifica build
- Eseguire `./mvnw clean compile` per verificare che il progetto compili
- Eseguire i test con `./mvnw test` per verificare che il test di regressione passi

## Task 5: Aggiornamento GiornaleDiBordo
- Aggiungere entry con evidenza del bug, fix applicata, e risultato test
