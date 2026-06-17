package org.elis.ericsson.datathon.user_management.model.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class NoXssValidator implements ConstraintValidator<NoXss, String> {

    private static final Pattern XSS_PATTERN = Pattern.compile("[<>\"\\\\]|&[a-zA-Z]+;|&#");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return true;
        return !XSS_PATTERN.matcher(value).find();
    }
}
