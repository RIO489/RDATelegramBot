package com.telegrambot.enums;

public enum UserState {
    ENTER_NAME,
    ENTER_PHONE,
    ENTER_RDA,
    ENTER_PROBLEM_DESCRIPTION,
    ENTER_RESOURCES,
    ENTER_SCALE,
    ENTER_SOLUTION,
    CONFIRM_PROBLEM,
    ENTER_FREQUENCY,
    EDIT_PROBLEM,           // Новий стан для вибору редагування
    EDIT_PROBLEM_DESCRIPTION, // Для редагування опису
    EDIT_RESOURCES,          // Для редагування ресурсів
    EDIT_SCALE,              // Для редагування масштабу
    EDIT_FREQUENCY,          // Для редагування частоти
    EDIT_SOLUTION            // Для редагування рішення
}
