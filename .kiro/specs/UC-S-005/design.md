# UC-S-005 — Design

## Contesto

`profiles.html` usa `innerHTML` con template literals per rendere i dati dei profili utente. I valori (`username`, `firstName`, `lastName`, `email`, `phoneNumber`) provengono dall'API senza sanitizzazione — un campo con payload XSS viene eseguito nel browser di chi visita la pagina.

## Soluzione: Defense-in-Depth (2 livelli)

### Livello 1: Input Validation (server-side, early rejection)

**Approccio:** custom annotation `@NoXss` + validator Jakarta.

```java
@Documented
@Constraint(validatedBy = NoXssValidator.class)
@Target({FIELD})
@Retention(RUNTIME)
public @interface NoXss {
    String message() default "Il campo contiene caratteri non ammessi";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
```

```java
public class NoXssValidator implements ConstraintValidator<NoXss, String> {
    private static final Pattern XSS_PATTERN = Pattern.compile("[<>\"\\\\]|&[a-zA-Z]+;|&#");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext ctx) {
        if (value == null) return true;
        return !XSS_PATTERN.matcher(value).find();
    }
}
```

**Dove applicare `@NoXss`:**
- `SignUpRequestDto`: su `firstName`, `lastName`
- `UserProfile` entity: su `username`, `firstName`, `lastName`, `phoneNumber`
- `email`: già validato da `@Email`, ma aggiungere `@NoXss` per sicurezza

**Endpoint protetti:**
- `POST /api/auth/signup` — usa `@Valid` su `SignUpRequestDto` (già presente)
- `POST /profiles/add` — aggiungere `@Valid` su `UserProfile`
- `POST /profiles/edit/{id}` — aggiungere `@Valid` su `UserProfile`

### Livello 2: Output Escaping (client-side, safety net)

In `profiles.html`, sostituire l'interpolazione diretta con una funzione di escape:

```javascript
function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/&/g, '&amp;')
              .replace(/</g, '&lt;')
              .replace(/>/g, '&gt;')
              .replace(/"/g, '&quot;');
}
```

Usata nel template:
```javascript
row.innerHTML = `
    <td>${escapeHtml(profile.username) || "Non presente"}</td>
    ...
`;
```

### Impatto

| Componente | Modifica |
|---|---|
| `NoXss.java` | Nuovo — annotation custom |
| `NoXssValidator.java` | Nuovo — validator |
| `SignUpRequestDto.java` | Aggiunta `@NoXss` su `firstName`, `lastName` |
| `UserProfile.java` | Aggiunta `@NoXss` su `username`, `firstName`, `lastName`, `email`, `phoneNumber` |
| `UserProfileWebController.java` | Aggiunta `@Valid` su parametri add/edit |
| `profiles.html` | Aggiunta funzione `escapeHtml()` + uso nella renderizzazione |
| `exploit/UC-S-005/` | Nuovo — exploit demo |

### Nessun impatto su
- Schema DB
- Endpoint esistenti (nessuna modifica di signature pubblica)
- Dati già nel DB (rimangono, ma vengono resi sicuri tramite escaping)
