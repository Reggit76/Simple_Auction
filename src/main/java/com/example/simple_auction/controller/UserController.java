package com.example.simple_auction.controller;

import com.example.simple_auction.model.User;
import com.example.simple_auction.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    // === Главная ===
    @GetMapping("/")
    public String showHomePage() {
        return "index";
    }

    // === Вход ===
    @GetMapping("/login")
    public String showLoginForm() {
        log.info("Открытие формы входа");
        return "login";
    }

    @PostMapping("/login")
    public String processLogin() {
        log.info("Обработка входа");
        return "redirect:/";
    }

    // === Регистрация ===
    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("error", false);
        log.info("Открытие формы регистрации");
        return "register";
    }

    @PostMapping("/register")
    public String processRegistration(
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam("name") String name,
            @RequestParam(name = "contactInfo", required = false) String contactInfo,
            @RequestParam(name = "avatarUrl", required = false) String avatarUrl,
            Model model) {

        log.info("Начало обработки регистрации");
        log.debug("Полученные данные: email={}, passwordLength={}, name={}, contactInfo={}, avatarUrl={}",
                email, password != null ? password.length() : 0, name, contactInfo, avatarUrl);

        try {
            // Проверка существования email
            log.debug("Проверка существования email: {}", email);
            if (userService.getUserByEmail(email).isPresent()) {
                log.warn("Email {} уже существует", email);
                model.addAttribute("error", true);
                return "register";
            }

            // Создание нового пользователя
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setPassword(password);
            newUser.setName(name);
            newUser.setContactInfo(contactInfo);
            newUser.setAvatarUrl(avatarUrl);
            newUser.setRole("USER");

            log.info("Создан пользователь: {}", newUser);

            // Сохранение пользователя
            userService.saveUser(newUser);
            log.info("Пользователь {} успешно зарегистрирован", email);

            return "redirect:/login";

        } catch (Exception e) {
            log.error("Ошибка при регистрации пользователя", e);
            model.addAttribute("error", true);
            return "register";
        }
    }

    // === Профиль ===
    @GetMapping("/profile")
    public String showProfileForm(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth.isAuthenticated() && auth.getPrincipal() instanceof User user) {
            model.addAttribute("user", user);
            return "profile";
        }

        return "redirect:/login";
    }

    // === Восстановление пароля ===
    @GetMapping("/password-recovery")
    public String showPasswordRecoveryForm(Model model) {
        model.addAttribute("error", false);
        model.addAttribute("success", false);
        log.info("Открытие страницы восстановления пароля");
        return "password-recovery";
    }

    @PostMapping("/password-recovery")
    public String processPasswordRecovery(
            @RequestParam("email") String email,
            Model model) {

        log.info("Попытка восстановления пароля для: {}", email);

        Optional<User> userOpt = userService.getUserByEmail(email);
        if (userOpt.isEmpty()) {
            model.addAttribute("error", true);
            return "password-recovery";
        }

        // Здесь можно реализовать отправку ссылки на восстановление пароля
        model.addAttribute("success", true);
        return "password-recovery";
    }
}