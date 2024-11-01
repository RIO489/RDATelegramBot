package com.telegrambot.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.List;

public class RdaValidator implements ConstraintValidator<ValidRda, String> {

    private final List<String> validRdaList = Arrays.asList(
            "Львівська РДА",
            "Дрогобицька РДА",
            "Самбірська РДА",
            "Стрийська РДА",
            "Червоноградська РДА",
            "Золочівська РДА",
            "Яворівська РДА",
            "Залізнична Районна Адміністрація Львівської Міської Ради",
            "Галицька Районна Адміністрація Львівської Міської Ради",
            "Сихівська Районна Адміністрація Львівської Міської Ради",
            "Франківська Районна Адміністрація Львівської Міської Ради",
            "Личаківська Районна Адміністрація Львівської Міської Ради",
            "Шевченківська Районна Адміністрація Львівської Міської Ради",
            "Бродівська РДА",
            "Буська РДА",
            "Городоцька РДА",
            "Жидачівська РДА",
            "Кам’янка-Бузька РДА",
            "Мостиська РДА",
            "Жовківська РДА",
            "Миколаївська РДА",
            "Перемишлянська РДА",
            "Пустомитівська РДА",
            "Радехівська РДА",
            "Сколівська РДА",
            "Сокальська РДА",
            "Старосамбірська РДА",
            "Турківська РДА"
    );

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return validRdaList.contains(value);
    }
}
