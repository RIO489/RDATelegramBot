package com.telegrambot.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProblemRdaDTO {
    private String problemDescription; // Опис проблеми
    private String resourcesNeeded;    // Необхідні ресурси
    private Integer scale;             // Масштаб проблеми (від 1 до 5)
    private String frequency;          // Частота ("Щодня", "Щотижня", "Щомісяця", "Рідше")
    private boolean confirmed;         // Підтвердження проблеми
    private boolean addAnotherProblem; // Пропозиція додати ще одну проблему
}
