package com.telegrambot.entity;


import jakarta.persistence.Entity;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProblemRda {
    @NotBlank(message = "Опис проблеми обов'язковий")
    @Size(min = 5, max = 700, message = "Опис проблеми має містити від 5 до 700 символів")
    private String problemDescription; // Опис проблеми

    @NotBlank(message = "Необхідні ресурси обов'язкові")
    @Size(min = 5, max = 700, message = "Необхідні ресурси мають містити від 5 до 700 символів")
    private String resourcesNeeded;    // Необхідні ресурси

    @NotNull(message = "Масштаб проблеми обов'язковий")
    @Min(value = 1, message = "Масштаб повинен бути не менше 1")
    @Max(value = 5, message = "Масштаб повинен бути не більше 5")
    private Integer scale;             // Масштаб проблеми (від 1 до 5)

    @NotBlank(message = "Частота є обов'язковою")
    @Pattern(regexp = "Щодня|Щотижня|Щомісяця|Рідше", message = "Частота має бути одним із значень: Щодня, Щотижня, Щомісяця, Рідше")
    private String frequency;          // Частота ("Щодня", "Щотижня", "Щомісяця", "Рідше")

    @NotBlank(message = "Опис рішення обов'язковий")
    @Size(min = 5, max = 700, message = "Опис рішення має містити від 5 до 700 символів")
    private String solution; // Опис проблеми
}
