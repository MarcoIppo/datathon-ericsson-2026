# UC-S-002 — Tasks: Password esposta in log/API

## Pipeline

```
sec-analyzer  →  sec-fixer
```

---

## Agente 1 — sec-analyzer

- [x] Analisi `LoginDto.java` — `toString()` include password cleartext
- [x] Analisi `SignUpRequestDto.java` — `toString()` include password cleartext
- [x] Analisi `UserProfile.java` — campo `password` senza protezione JSON
- [x] Analisi `EggUpInfo.java` — campo `password` senza protezione JSON
- [x] Analisi controller/service — logging esplicito di oggetti con password (AuthServiceImpl righe 69, 103)
- [x] Scrittura `exploit/UC-S-002/vulnerability_report.md`

## Agente 2 — sec-fixer

- [x] Lettura `exploit/UC-S-002/vulnerability_report.md`
- [x] Fix V1: `LoginDto.toString()` → `[PROTECTED]`
- [x] Fix V2: `SignUpRequestDto.toString()` → `[PROTECTED]`
- [x] Fix V3: `UserProfile.password` → `@JsonProperty(WRITE_ONLY)`
- [x] Fix V4: `EggUpInfo.password` → `@JsonProperty(WRITE_ONLY)`
- [x] Scrittura `PasswordExposureTest.java` (5 test con Javadoc)
- [x] Build: `./mvnw clean package` — BUILD SUCCESS 36/36 test (JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64)
- [x] Docker: `docker compose up --build -d`
- [x] Verifica su target: tutti i vettori PROTECTED (campo `password` assente da GET /api/profiles)

## Completamento

- [x] Commit su branch `feature/UC-S-002` (2748a05)
- [x] Aggiornamento `GiornaleDiBordo.md`
