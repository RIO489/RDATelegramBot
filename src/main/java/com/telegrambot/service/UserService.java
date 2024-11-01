package com.telegrambot.service;


import com.telegrambot.dto.UserDTO;
import com.telegrambot.expection.UserException;

import java.util.List;

public interface UserService {
   void  saveUser(UserDTO userDTO);
    List<UserDTO> loadUsers();
}
