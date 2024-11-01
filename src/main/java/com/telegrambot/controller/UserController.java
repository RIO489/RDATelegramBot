package com.telegrambot.controller;

import com.telegrambot.dto.UserDTO;
import com.telegrambot.expection.UserException;
import com.telegrambot.service.UserService;
import com.telegrambot.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

}