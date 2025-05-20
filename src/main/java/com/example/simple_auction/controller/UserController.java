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

import java.math.BigDecimal;
import java.util.Optional;

@Controller
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/")
    public String showHomePage(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth.isAuthenticated() && auth.getPrincipal() instanceof User user) {
            model.addAttribute("user", user);
        }
        return "index";
    }

    @GetMapping("/login")
    public String showLoginForm(@RequestParam(name = "error", required = false) String error, Model model) {
        if (error != null) {
            model.addAttribute("loginError", true);
        }
        return "login";
    }

    @GetMapping("/register")
    public String showRegisterForm(@RequestParam(name = "error", required = false) String error, Model model) {
        if (error != null) {
            model.addAttribute("registrationError", true);
        }
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

        if (userService.getUserByEmail(email).isPresent()) {
            return "redirect:/register?error=email_exists";
        }

        User newUser = new User();
        newUser.setEmail(email);
        newUser.setPassword(password); // Пароль будет хеширован в сервисе
        newUser.setName(name);
        newUser.setContactInfo(contactInfo);
        newUser.setAvatarUrl(avatarUrl);
        newUser.setRole("USER");
        newUser.setBalance(BigDecimal.ZERO); // Устанавливаем баланс по умолчанию

        userService.saveUser(newUser);
        return "redirect:/login";
    }

    @GetMapping("/profile")
    public String showProfileForm(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth.isAuthenticated() && auth.getPrincipal() instanceof User user) {
            model.addAttribute("user", user);
            return "profile";
        }
        return "redirect:/login?error=unauthorized";
    }

    @GetMapping("/password-recovery")
    public String showPasswordRecoveryForm(
            @RequestParam(name = "error", required = false) String error,
            @RequestParam(name = "success", required = false) String success,
            Model model) {
        model.addAttribute("error", error != null);
        model.addAttribute("success", success != null);
        return "password-recovery";
    }

    @PostMapping("/password-recovery")
    public String processPasswordRecovery(
            @RequestParam("email") String email,
            RedirectAttributes redirectAttributes) {
        Optional<User> userOpt = userService.getUserByEmail(email);
        if (userOpt.isEmpty()) {
            return "redirect:/password-recovery?error=true";
        }
        // Логика отправки ссылки на восстановление
        return "redirect:/password-recovery?success=true";
    }

    @GetMapping("/admin")
    public String adminPanel(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth.isAuthenticated() && auth.getPrincipal() instanceof User user) {
            if (user.isAdmin()) {
                model.addAttribute("user", user);
                return "admin";
            } else {
                return "redirect:/profile?error=access_denied";
            }
        }
        return "redirect:/login?error=unauthorized";
    }
}