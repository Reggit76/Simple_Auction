package com.example.simple_auction.controller;

import com.example.simple_auction.model.User;
import com.example.simple_auction.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Optional;

@Controller
public class UserController {
    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @GetMapping("/")
    public String showHomePage(Model model) {
        log.info("Открытие главной страницы");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth.isAuthenticated() && auth.getPrincipal() instanceof User user) {
            model.addAttribute("user", user);
            log.info("Пользователь авторизован: {}", user.getEmail());
        }
        return "index";
    }

    @GetMapping("/login")
    public String showLoginForm(@RequestParam(name = "error", required = false) String error, Model model) {
        log.info("Открытие формы входа");
        if (error != null) {
            model.addAttribute("loginError", true);
            log.warn("Ошибка входа");
        }
        model.addAttribute("user", null);
        return "login";
    }

    @GetMapping("/register")
    public String showRegisterForm(@RequestParam(name = "error", required = false) String error, Model model) {
        log.info("Открытие формы регистрации");
        if (error != null) {
            model.addAttribute("registrationError", true);
            log.warn("Ошибка регистрации");
        }
        model.addAttribute("user", null);
        return "register";
    }

    @PostMapping("/register")
    public String processRegistration(
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam("name") String name,
            @RequestParam(name = "contactInfo", required = false) String contactInfo,
            @RequestParam(name = "avatarUrl", required = false) String avatarUrl,
            RedirectAttributes redirectAttributes) {
        log.info("Обработка регистрации пользователя: {}", email);
        if (userService.getUserByEmail(email).isPresent()) {
            log.warn("Попытка регистрации с существующим email: {}", email);
            return "redirect:/register?error=email_exists";
        }
        User newUser = new User();
        newUser.setEmail(email);
        newUser.setPassword(password);
        newUser.setName(name);
        newUser.setContactInfo(contactInfo);
        newUser.setAvatarUrl(avatarUrl);
        newUser.setRole("USER");
        newUser.setBalance(BigDecimal.ZERO);
        userService.saveUser(newUser);
        log.info("Пользователь зарегистрирован: {}", email);
        return "redirect:/login";
    }

    @GetMapping("/profile")
    public String showProfileForm(Model model) {
        log.info("Открытие профиля пользователя");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth.isAuthenticated() && auth.getPrincipal() instanceof User user) {
            model.addAttribute("user", user);
            log.info("Профиль открыт для: {}", user.getEmail());
            return "profile/profile";
        }
        log.warn("Неавторизованный доступ к профилю");
        return "redirect:/login?error=unauthorized";
    }

    @GetMapping("/profile/edit")
    public String showEditProfileForm(Model model) {
        log.info("Открытие формы редактирования профиля");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth.isAuthenticated() && auth.getPrincipal() instanceof User user) {
            model.addAttribute("user", user);
            log.info("Форма редактирования открыта для: {}", user.getEmail());
            return "profile/edit";
        }
        log.warn("Неавторизованный доступ к редактированию профиля");
        return "redirect:/login?error=unauthorized";
    }

    @PostMapping("/profile/edit")
    public String editProfile(
            @RequestParam String name,
            @RequestParam String contactInfo,
            @RequestParam String avatarUrl) {
        log.info("Сохранение изменений профиля");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth.isAuthenticated() && auth.getPrincipal() instanceof User user) {
            user.setName(name);
            user.setContactInfo(contactInfo);
            user.setAvatarUrl(avatarUrl);
            userService.saveUser(user);
            log.info("Профиль обновлен для: {}", user.getEmail());
            return "redirect:/profile";
        }
        log.warn("Неавторизованная попытка редактирования профиля");
        return "redirect:/login?error=unauthorized";
    }

    @GetMapping("/password-recovery")
    public String showPasswordRecoveryForm(
            @RequestParam(name = "error", required = false) String error,
            @RequestParam(name = "success", required = false) String success,
            Model model) {
        log.info("Открытие формы восстановления пароля");
        model.addAttribute("error", error != null);
        model.addAttribute("success", success != null);
        return "password-recovery";
    }

    @PostMapping("/password-recovery")
    public String processPasswordRecovery(
            @RequestParam("email") String email,
            RedirectAttributes redirectAttributes) {
        log.info("Обработка восстановления пароля для: {}", email);
        Optional<User> userOpt = userService.getUserByEmail(email);
        if (userOpt.isEmpty()) {
            log.warn("Пользователь не найден при восстановлении пароля: {}", email);
            return "redirect:/password-recovery?error=true";
        }
        return "redirect:/password-recovery?success=true";
    }
}