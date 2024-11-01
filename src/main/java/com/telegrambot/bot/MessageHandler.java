package com.telegrambot.bot;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import com.telegrambot.entity.User;
import com.telegrambot.entity.ProblemRda;
import com.telegrambot.enums.UserState;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
@Component
public class MessageHandler {
    private TelegramClient telegramClient;

    private Map<Long, User> userData = new HashMap<>();
    private Map<Long, List<ProblemRda>> problemData = new HashMap<>();
    private Map<Long, UserState> userStates = new HashMap<>();


    public void setTelegramClient(TelegramClient telegramClient) {
        this.telegramClient = telegramClient;
    }

    public void handleUpdate(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            switch (messageText) {
                case "/start" -> sendWelcomeMessage(chatId);
                case "/register" -> initiateRegistration(chatId);
                default -> processUserInput(chatId, messageText);
            }
        }
    }

    private void sendWelcomeMessage(Long chatId) {
        String welcomeText = "Ласкаво просимо! Натисніть /register для реєстрації.";
        sendResponse(chatId, welcomeText);
    }

    private void initiateRegistration(Long chatId) {
        sendResponse(chatId, "Будь ласка, введіть ваш ПІБ:");
        userStates.put(chatId, UserState.ENTER_NAME);
        userData.put(chatId, new User());
        problemData.put(chatId, new ArrayList<>()); // Ініціалізація списку проблем для кожного нового користувача
    }

    private boolean validateFrequency(String frequency) {
        List<String> validFrequencies = Arrays.asList("Щодня", "Щотижня", "Щомісяця", "Рідше");
        return validFrequencies.contains(frequency);
    }

    private void sendFrequencySelection(Long chatId) {
        String text = "Виберіть частоту:";
        ReplyKeyboardMarkup keyboardMarkup = buildFrequencyMenuKeyboard();
        sendResponse(chatId, text, keyboardMarkup);
    }

    private ReplyKeyboardMarkup buildFrequencyMenuKeyboard() {
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        List<String> frequencyOptions = Arrays.asList("Щодня", "Щотижня", "Щомісяця", "Рідше");
        for (String frequency : frequencyOptions) {
            KeyboardRow row = new KeyboardRow();
            row.add(new KeyboardButton(frequency));
            keyboardRows.add(row);
        }

        return ReplyKeyboardMarkup.builder()
                .keyboard(keyboardRows)
                .resizeKeyboard(true)
                .build();
    }
    private void processUserInput(Long chatId, String userInput) {
        UserState userState = userStates.get(chatId);

        if (userState == null) {
            sendResponse(chatId, "Натисніть /register для початку реєстрації.");
            return;
        }

        User user = userData.get(chatId);
        List<ProblemRda> problems = problemData.getOrDefault(chatId, new ArrayList<>());
        ProblemRda problemRda = (problems.isEmpty() || userState == UserState.ENTER_PROBLEM_DESCRIPTION)
                ? new ProblemRda()
                : problems.get(problems.size() - 1);

        switch (userState) {
            case ENTER_NAME:
                if (validateName(userInput)) {
                    user.setName(userInput);
                    userStates.put(chatId, UserState.ENTER_PHONE);
                    sendResponse(chatId, "Будь ласка, введіть ваш номер телефону:");
                } else {
                    sendResponse(chatId, "Ім'я повинно містити тільки літери та спеціальні символи, мінімум 2 символи, максимум 50 символів.");
                }
                break;

            case ENTER_PHONE:
                if (validatePhone(userInput)) {
                    user.setPhone(userInput);
                    userStates.put(chatId, UserState.ENTER_RDA);
                    sendRDASelection(chatId);
                } else {
                    sendResponse(chatId, "Номер телефону повинен відповідати українському або міжнародному формату, починатися з + і містити мінімум 10 цифр.");
                }
                break;

            case ENTER_RDA:
                if (validateRDA(userInput)) {
                    user.setRda(userInput);
                    userStates.put(chatId, UserState.ENTER_PROBLEM_DESCRIPTION);
                    sendResponse(chatId, "Яка була проблема?");
                } else {
                    sendResponse(chatId, "Будь ласка, виберіть вашу РДА з наданого списку.");
                    sendRDASelection(chatId);
                }
                break;

            case ENTER_PROBLEM_DESCRIPTION:
                if (userInput.length() >= 5 && userInput.length() <= 700) {
                    problemRda.setProblemDescription(userInput);
                    problems.add(problemRda); // Додаємо проблему в список
                    userStates.put(chatId, UserState.ENTER_RESOURCES);
                    sendResponse(chatId, "Вкажіть необхідні ресурси:");
                } else {
                    sendResponse(chatId, "Опис проблеми має містити від 5 до 700 символів.");
                }
                break;

            case ENTER_RESOURCES:
                if (userInput.length() >= 5 && userInput.length() <= 700) {
                    problemRda.setResourcesNeeded(userInput);
                    userStates.put(chatId, UserState.ENTER_SCALE);
                    sendResponse(chatId, "Оцініть масштаб проблеми від 1 до 5:");
                } else {
                    sendResponse(chatId, "Необхідні ресурси мають містити від 5 до 700 символів.");
                }
                break;

            case ENTER_SCALE:
                if (isNumeric(userInput) && Integer.parseInt(userInput) >= 1 && Integer.parseInt(userInput) <= 5) {
                    problemRda.setScale(Integer.parseInt(userInput));
                    userStates.put(chatId, UserState.ENTER_FREQUENCY);
                    sendFrequencySelection(chatId);
                } else {
                    sendResponse(chatId, "Масштаб проблеми повинен бути числом від 1 до 5.");
                }
                break;

            case ENTER_FREQUENCY:
                if (validateFrequency(userInput)) {
                    problemRda.setFrequency(userInput);
                    userStates.put(chatId, UserState.ENTER_SOLUTION);
                    sendResponse(chatId, "Опишіть, як вирішили проблему:");
                } else {
                    sendFrequencySelection(chatId);
                }
                break;

            case ENTER_SOLUTION:
                if (userInput.length() >= 5 && userInput.length() <= 700) {
                    problemRda.setSolution(userInput);
                    userStates.put(chatId, UserState.CONFIRM_PROBLEM);
                    sendProblemSummary(chatId, user, problemRda);
                } else {
                    sendResponse(chatId, "Опис рішення має містити від 5 до 700 символів.");
                }
                break;

            case CONFIRM_PROBLEM:
                if ("Підтвердити".equalsIgnoreCase(userInput)) {
                    completeRegistration(chatId, user, problems);
                } else if ("Додати ще одну проблему".equalsIgnoreCase(userInput)) {
                    userStates.put(chatId, UserState.ENTER_PROBLEM_DESCRIPTION);
                    sendResponse(chatId, "Опишіть наступну проблему:");
                } else {
                    sendConfirmationOptions(chatId);
                }
                break;
        }
    }

    private boolean validateName(String name) {
        return name.matches("[A-Za-zА-Яа-яІіЇїЄєҐґ '-]{2,50}");
    }

    private boolean validatePhone(String phone) {
        return phone.matches("\\+?\\d{10,}");
    }

    private boolean validateRDA(String rda) {
        List<String> validRdas = Arrays.asList(
                "Львівська РДА", "Дрогобицька РДА", "Самбірська РДА", "Стрийська РДА",
                "Червоноградська РДА", "Золочівська РДА", "Яворівська РДА",
                "Залізнична Районна Адміністрація Львівської Міської Ради",
                "Галицька Районна Адміністрація Львівської Міської Ради",
                "Сихівська Районна Адміністрація Львівської Міської Ради",
                "Франківська Районна Адміністрація Львівської Міської Ради",
                "Личаківська Районна Адміністрація Львівської Міської Ради",
                "Шевченківська Районна Адміністрація Львівської Міської Ради",
                "Бродівська РДА", "Буська РДА", "Городоцька РДА", "Жидачівська РДА",
                "Кам’янка-Бузька РДА", "Мостиська РДА", "Жовківська РДА", "Миколаївська РДА",
                "Перемишлянська РДА", "Пустомитівська РДА", "Радехівська РДА", "Сколівська РДА",
                "Сокальська РДА", "Старосамбірська РДА", "Турківська РДА"
        );
        return validRdas.contains(rda);
    }

    private boolean isNumeric(String str) {
        return str != null && str.matches("\\d+");
    }

    private void sendRDASelection(Long chatId) {
        String text = "Виберіть вашу РДА з доступних варіантів:";
        ReplyKeyboardMarkup keyboardMarkup = buildRDAMenuKeyboard();
        sendResponse(chatId, text, keyboardMarkup);
    }

    private ReplyKeyboardMarkup buildRDAMenuKeyboard() {
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        List<String> rdaOptions = Arrays.asList(
                "Львівська РДА", "Дрогобицька РДА", "Самбірська РДА", "Стрийська РДА",
                "Червоноградська РДА", "Золочівська РДА", "Яворівська РДА",
                "Залізнична Районна Адміністрація Львівської Міської Ради",
                "Галицька Районна Адміністрація Львівської Міської Ради",
                "Сихівська Районна Адміністрація Львівської Міської Ради",
                "Франківська Районна Адміністрація Львівської Міської Ради",
                "Личаківська Районна Адміністрація Львівської Міської Ради",
                "Шевченківська Районна Адміністрація Львівської Міської Ради",
                "Бродівська РДА", "Буська РДА", "Городоцька РДА", "Жидачівська РДА",
                "Кам’янка-Бузька РДА", "Мостиська РДА", "Жовківська РДА", "Миколаївська РДА",
                "Перемишлянська РДА", "Пустомитівська РДА", "Радехівська РДА", "Сколівська РДА",
                "Сокальська РДА", "Старосамбірська РДА", "Турківська РДА"
        );

        for (String rda : rdaOptions) {
            KeyboardRow row = new KeyboardRow();
            row.add(new KeyboardButton(rda));
            keyboardRows.add(row);
        }

        return ReplyKeyboardMarkup.builder()
                .keyboard(keyboardRows)
                .resizeKeyboard(true)
                .build();
    }

    private void sendProblemSummary(Long chatId, User user, ProblemRda problemRda) {
        String summary = String.format(
                "Підтвердіть вашу проблему:\n\nПроблема: %s\nМасштаб: %d\nРесурси: %s\nЧастота: %s\nРішення: %s",
                problemRda.getProblemDescription(),
                problemRda.getScale(),
                problemRda.getResourcesNeeded(),
                problemRda.getFrequency(),
                problemRda.getSolution()
        );
        sendResponse(chatId, summary);
        sendConfirmationOptions(chatId);
    }

    private void sendConfirmationOptions(Long chatId) {
        ReplyKeyboardMarkup keyboardMarkup = ReplyKeyboardMarkup.builder()
                .keyboard(List.of(
                        new KeyboardRow(List.of(new KeyboardButton("Підтвердити"))),
                        new KeyboardRow(List.of(new KeyboardButton("Додати ще одну проблему")))
                ))
                .resizeKeyboard(true)
                .oneTimeKeyboard(true)
                .build();

        sendResponse(chatId, "Будь ласка, підтвердіть проблему або виберіть опцію додати ще одну", keyboardMarkup);
    }

    private void sendResponse(long chatId, String text, ReplyKeyboardMarkup keyboardMarkup) {
        SendMessage message = SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(text)
                .replyMarkup(keyboardMarkup)
                .build();

        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendResponse(long chatId, String text) {
        sendResponse(chatId, text, null);
    }

    private void completeRegistration(Long chatId, User user, List<ProblemRda> problems) {
        try {
            for (ProblemRda problem : problems) {
                GoogleSheetsService.addRowToSheet(user, problem.getProblemDescription(),
                        problem.getResourcesNeeded(), problem.getScale(), problem.getFrequency(), problem.getSolution());
            }
            problemData.put(chatId, new ArrayList<>()); // Очищення списку після відправки
            sendResponse(chatId, "Дякуємо! Інформація збережена.");
        } catch (IOException | GeneralSecurityException e) {
            sendResponse(chatId, "Сталася помилка при збереженні даних. Спробуйте пізніше.");
            e.printStackTrace();
            return;
        }
    }
    // Logging Methods
    private void logAction(String action, long chatId) {
        System.out.println("\n----------------------------");
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        System.out.println(dateFormat.format(date));
        System.out.println("Action: " + action + " | Chat ID: " + chatId);
    }

    private void logUserRegistration(User user, ProblemRda problemRda) {
        System.out.println("New registration:");
        System.out.println("Name: " + user.getName());
        System.out.println("Phone: " + user.getPhone());
        System.out.println("RDA: " + user.getRda());
        System.out.println("Problem Description: " + problemRda.getProblemDescription());
        System.out.println("Resources Needed: " + problemRda.getResourcesNeeded());
        System.out.println("Scale: " + problemRda.getScale());
        System.out.println("Frequency: " + problemRda.getFrequency());
    }
}
