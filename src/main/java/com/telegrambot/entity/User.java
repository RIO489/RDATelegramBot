package com.telegrambot.entity;


import com.telegrambot.validator.ValidRda;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
//@Table(name = "users") // Зміна імені таблиці на "users"
public class User {
    @NotBlank(message = "Ім'я не може бути порожнім.")
    @Size(min = 2, max = 50, message = "Ім'я повинно містити від 2 до 50 символів.")
    @Pattern(regexp = "^[А-Яа-яA-Za-z '-]+$", message = "Тільки літери та спеціальні символи (кирилиця або латиниця).")
    private String name;

    @NotBlank(message = "Номер телефону не може бути порожнім.")
    @Pattern(regexp = "\\d+", message = "Телефон повинен містити тільки цифри")
    private String phone;

    @NotBlank(message = "РДА не може бути порожнім.")
    @ValidRda
    private String rda;


}
