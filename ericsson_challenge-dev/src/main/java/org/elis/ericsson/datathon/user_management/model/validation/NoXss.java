package org.elis.ericsson.datathon.user_management.model.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = NoXssValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface NoXss {
    String message() default "Il campo contiene caratteri non ammessi";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
