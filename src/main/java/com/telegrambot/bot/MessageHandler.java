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
                .oneTimeKeyboard(true)
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
                    sendResponse(chatId, "Будь ласка, виберіть вашу територіальна громада з наданого списку.");
                    sendRDASelection(chatId);
                }
                break;

            case ENTER_PROBLEM_DESCRIPTION:
                if (userInput.length() >= 5 && userInput.length() <= 700) {
                    problemRda.setProblemDescription(userInput);
                    problems.add(problemRda);
                    userStates.put(chatId, UserState.ENTER_SCALE);
                    sendResponse(chatId, "Оцініть масштаб проблеми від 1 до 5:");
                } else {
                    sendResponse(chatId, "Опис проблеми має містити від 5 до 700 символів.");
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
                    userStates.put(chatId, UserState.ENTER_RESOURCES);
                    sendResponse(chatId, "Які ресурси були необхідні для її вирішення?");
                } else {
                    sendResponse(chatId, "Опис рішення має містити від 5 до 700 символів.");
                }
                break;

            case ENTER_RESOURCES:
                if (userInput.length() >= 5 && userInput.length() <= 700) {
                    problemRda.setResourcesNeeded(userInput);
                    userStates.put(chatId, UserState.CONFIRM_PROBLEM);
                    sendProblemSummary(chatId, user, problemRda);
                    sendInitialConfirmationOptions(chatId);
                } else {
                    sendResponse(chatId, "Необхідні ресурси мають містити від 5 до 700 символів.");
                }
                break;

            case CONFIRM_PROBLEM:
                if ("Редагувати".equalsIgnoreCase(userInput)) {
                    sendEditOptions(chatId); // Показуємо меню вибору поля для редагування
                    userStates.put(chatId, UserState.EDIT_PROBLEM);
                } else if ("Підтвердити".equalsIgnoreCase(userInput)) {
                    sendFinalOptions(chatId); // Показуємо опції завершення або додавання нової проблеми
                } else if ("Повідомити про ще одну проблему".equalsIgnoreCase(userInput)) {
                    userStates.put(chatId, UserState.ENTER_PROBLEM_DESCRIPTION);
                    sendResponse(chatId, "Опишіть наступну проблему:");
                } else if ("Завершити".equalsIgnoreCase(userInput)) {
                    completeRegistration(chatId, user, problems); // Завершення реєстрації та збереження даних
                    userStates.remove(chatId); // Видаляємо стан користувача, оскільки процес завершено
                } else {
                    sendInitialConfirmationOptions(chatId); // Показуємо опції підтвердження та редагування
                }
                break;

            case EDIT_PROBLEM:
                sendProblemSummary(chatId, user, problemRda);
                handleEditSelection(chatId, userInput, problemRda);
                break;

            case EDIT_PROBLEM_DESCRIPTION:
                problemRda.setProblemDescription(userInput);
                sendResponse(chatId, "Опис проблеми успішно оновлено.");
                userStates.put(chatId, UserState.CONFIRM_PROBLEM);
                sendProblemSummary(chatId, user, problemRda);
                sendInitialConfirmationOptions(chatId);
                break;

            case EDIT_RESOURCES:
                problemRda.setResourcesNeeded(userInput);
                sendResponse(chatId, "Необхідні ресурси успішно оновлено.");
                userStates.put(chatId, UserState.CONFIRM_PROBLEM);
                sendProblemSummary(chatId, user, problemRda);
                sendInitialConfirmationOptions(chatId);
                break;

            case EDIT_SCALE:
                if (isNumeric(userInput) && Integer.parseInt(userInput) >= 1 && Integer.parseInt(userInput) <= 5) {
                    problemRda.setScale(Integer.parseInt(userInput));
                    sendResponse(chatId, "Масштаб проблеми успішно оновлено.");
                } else {
                    sendResponse(chatId, "Масштаб проблеми повинен бути числом від 1 до 5.");
                    return;
                }
                userStates.put(chatId, UserState.CONFIRM_PROBLEM);
                sendProblemSummary(chatId, user, problemRda);
                sendInitialConfirmationOptions(chatId);
                break;

            case EDIT_FREQUENCY:
                if (validateFrequency(userInput)) {
                    problemRda.setFrequency(userInput);
                    sendResponse(chatId, "Частота успішно оновлена.");
                } else {
                    sendFrequencySelection(chatId);
                    return;
                }
                userStates.put(chatId, UserState.CONFIRM_PROBLEM);
                sendProblemSummary(chatId, user, problemRda);
                sendInitialConfirmationOptions(chatId);
                break;

            case EDIT_SOLUTION:
                problemRda.setSolution(userInput);
                sendResponse(chatId, "Опис рішення успішно оновлено.");
                userStates.put(chatId, UserState.CONFIRM_PROBLEM);
                sendProblemSummary(chatId, user, problemRda);
                sendInitialConfirmationOptions(chatId);
                break;
        }
    }
    private void sendEditOptions(Long chatId) {
        ReplyKeyboardMarkup keyboardMarkup = ReplyKeyboardMarkup.builder()
                .keyboard(List.of(
                        new KeyboardRow(List.of(new KeyboardButton("Опис проблеми"), new KeyboardButton("Ресурси"))),
                        new KeyboardRow(List.of(new KeyboardButton("Масштаб"), new KeyboardButton("Частота"))),
                        new KeyboardRow(List.of(new KeyboardButton("Рішення")))
                ))
                .resizeKeyboard(true)
                .oneTimeKeyboard(true)
                .build();

        sendResponse(chatId, "Виберіть поле для редагування:", keyboardMarkup);
    }

    // Метод для обробки вибору поля, яке потрібно редагувати
    private void handleEditSelection(Long chatId, String userInput, ProblemRda problemRda) {
        switch (userInput) {
            case "Опис проблеми":
                userStates.put(chatId, UserState.EDIT_PROBLEM_DESCRIPTION);
                sendResponse(chatId, "Введіть новий опис проблеми:");
                break;

            case "Ресурси":
                userStates.put(chatId, UserState.EDIT_RESOURCES);
                sendResponse(chatId, "Введіть нові ресурси для вирішення проблеми:");
                break;

            case "Масштаб":
                userStates.put(chatId, UserState.EDIT_SCALE);
                sendResponse(chatId, "Введіть новий масштаб проблеми від 1 до 5:");
                break;

            case "Частота":
                userStates.put(chatId, UserState.EDIT_FREQUENCY);
                sendFrequencySelection(chatId);
                break;

            case "Рішення":
                userStates.put(chatId, UserState.EDIT_SOLUTION);
                sendResponse(chatId, "Введіть новий опис рішення:");
                break;

            default:
                sendResponse(chatId, "Невірний вибір. Будь ласка, спробуйте ще раз.");
                sendEditOptions(chatId);
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
                "Львівська територіальна громада", "Дрогобицька територіальна громада", "Самбірська територіальна громада", "Стрийська територіальна громада",
                "Червоноградська територіальна громада", "Золочівська територіальна громада", "Яворівська територіальна громада",
                "Залізнична Районна Адміністрація Львівської Міської Ради",
                "Галицька Районна Адміністрація Львівської Міської Ради",
                "Сихівська Районна Адміністрація Львівської Міської Ради",
                "Франківська Районна Адміністрація Львівської Міської Ради",
                "Личаківська Районна Адміністрація Львівської Міської Ради",
                "Шевченківська Районна Адміністрація Львівської Міської Ради",
                "Бродівська територіальна громада", "Буська територіальна громада", "Городоцька територіальна громада", "Жидачівська територіальна громада",
                "Кам’янка-Бузька територіальна громада", "Мостиська територіальна громада", "Жовківська територіальна громада", "Миколаївська територіальна громада",
                "Перемишлянська територіальна громада", "Пустомитівська територіальна громада", "Радехівська територіальна громада", "Сколівська територіальна громада",
                "Сокальська територіальна громада", "Старосамбірська територіальна громада", "Турківська територіальна громада"
        );
        return validRdas.contains(rda);
    }

    private boolean isNumeric(String str) {
        return str != null && str.matches("\\d+");
    }

    private void sendRDASelection(Long chatId) {
        String text = "Виберіть вашу громаду з доступних варіантів:";
        ReplyKeyboardMarkup keyboardMarkup = buildRDAMenuKeyboard();
        sendResponse(chatId, text, keyboardMarkup);
    }

    private ReplyKeyboardMarkup buildRDAMenuKeyboard() {
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        List<String> rdaOptions = Arrays.asList(
                "Львівська територіальна громада", "Дрогобицька територіальна громада", "Самбірська територіальна громада", "Стрийська територіальна громада",
                "Червоноградська територіальна громада", "Золочівська територіальна громада", "Яворівська територіальна громада",
                "Залізнична Районна Адміністрація Львівської Міської Ради",
                "Галицька Районна Адміністрація Львівської Міської Ради",
                "Сихівська Районна Адміністрація Львівської Міської Ради",
                "Франківська Районна Адміністрація Львівської Міської Ради",
                "Личаківська Районна Адміністрація Львівської Міської Ради",
                "Шевченківська Районна Адміністрація Львівської Міської Ради",
                "Бродівська територіальна громада", "Буська територіальна громада", "Городоцька територіальна громада", "Жидачівська територіальна громада",
                "Кам’янка-Бузька територіальна громада", "Мостиська територіальна громада", "Жовківська територіальна громада", "Миколаївська територіальна громада",
                "Перемишлянська територіальна громада", "Пустомитівська територіальна громада", "Радехівська територіальна громада", "Сколівська територіальна громада",
                "Сокальська територіальна громада", "Старосамбірська територіальна громада", "Турківська територіальна громада"
        );

        for (String rda : rdaOptions) {
            KeyboardRow row = new KeyboardRow();
            row.add(new KeyboardButton(rda));
            keyboardRows.add(row);
        }

        return ReplyKeyboardMarkup.builder()
                .keyboard(keyboardRows)
                .resizeKeyboard(true)
                .oneTimeKeyboard(true)
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
    }

    private void sendInitialConfirmationOptions(Long chatId) {
        ReplyKeyboardMarkup keyboardMarkup = ReplyKeyboardMarkup.builder()
                .keyboard(List.of(
                        new KeyboardRow(List.of(new KeyboardButton("Підтвердити"))),
                        new KeyboardRow(List.of(new KeyboardButton("Редагувати")))
                ))
                .resizeKeyboard(true)
                .oneTimeKeyboard(true)
                .build();

        sendResponse(chatId, "Будь ласка, підтвердіть або виберіть іншу опцію.", keyboardMarkup);
    }

    // Другий метод для відображення кнопок "Повідомити про ще одну проблему" і "Завершити"
    private void sendFinalOptions(Long chatId) {
        ReplyKeyboardMarkup keyboardMarkup = ReplyKeyboardMarkup.builder()
                .keyboard(List.of(
                        new KeyboardRow(List.of(new KeyboardButton("Так"))),
                        new KeyboardRow(List.of(new KeyboardButton("Ні/Завершити")))
                ))
                .resizeKeyboard(true)
                .oneTimeKeyboard(true)
                .build();

        sendResponse(chatId, "Чи є ще якісь проблеми про вирішення яких ви можете нам повідомити?", keyboardMarkup);
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
