package com.telegrambot.service.Impl;


import com.telegrambot.dto.UserDTO;
import com.telegrambot.service.UserService;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {
    private static final String FILE_PATH = "users.csv"; // Шлях до CSV-файлу

    @Override
    public void saveUser(UserDTO userDTO) {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(FILE_PATH), StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            String userData = String.format("%s,%s,%s%n", userDTO.getName(), userDTO.getPhone(), userDTO.getRda());
            writer.write(userData);
        } catch (IOException e) {
            e.printStackTrace(); // Логування помилок
        }
    }

    @Override
    public List<UserDTO> loadUsers() {
        List<UserDTO> users = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(Paths.get(FILE_PATH))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] data = line.split(",");
                if (data.length == 3) {
                    String name = data[0];
                    String phone = data[1];
                    String rda = data[2];
                    UserDTO user = new UserDTO(name, phone, rda);
                    users.add(user);
                }
            }
        } catch (IOException e) {
            e.printStackTrace(); // Логування помилок
        }

        return users;
    }
}