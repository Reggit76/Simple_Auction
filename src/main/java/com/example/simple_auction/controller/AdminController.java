package com.example.simple_auction.controller;

import com.example.simple_auction.model.Lot;
import com.example.simple_auction.model.User;
import com.example.simple_auction.model.Comment;
import com.example.simple_auction.service.BidService;
import com.example.simple_auction.service.UserService;
import com.example.simple_auction.service.LotService;
import com.example.simple_auction.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private LotService lotService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private BidService bidService;

    // Метод для получения текущего пользователя
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof User) {
            return (User) auth.getPrincipal();
        }
        return null;
    }

    // Панель администратора
    @GetMapping()
    public String adminPanel(Model model) {
        log.info("Открытие панели администратора");
        User user = getCurrentUser();
        if (user != null && user.isAdmin()) {
            model.addAttribute("user", user);
            log.info("Панель открыта для администратора: {}", user.getEmail());
            return "admin/panel";
        }
        log.warn("Неавторизованный доступ к панели администратора");
        return "redirect:/login?error=unauthorized";
    }

    // Управление пользователями
    @GetMapping("/users")
    public String listUsers(Model model) {
        log.info("Получение списка пользователей");
        List<User> users = userService.getAllUsers();
        if (users == null || users.isEmpty()) {
            log.warn("Список пользователей пуст или null");
            model.addAttribute("users", new ArrayList<>());
        } else {
            log.info("Получено {} пользователей", users.size());
            model.addAttribute("users", users);
        }
        model.addAttribute("user", getCurrentUser());
        return "admin/users";
    }

    @GetMapping("/users/{id}/edit")
    public String editUserForm(@PathVariable Integer id, Model model) {
        log.info("Редактирование пользователя ID: {}", id);
        User userToEdit = userService.getUserById(id);
        if (userToEdit == null) {
            log.warn("Пользователь с ID {} не найден", id);
            return "redirect:/admin/users?error=user_not_found";
        }
        model.addAttribute("userToEdit", userToEdit);
        model.addAttribute("user", getCurrentUser());
        return "admin/edit_user";
    }

    @PostMapping("/users/{id}/edit")
    public String editUser(@PathVariable Integer id, @ModelAttribute User updatedUser) {
        log.info("Сохранение изменений пользователя ID: {}", id);
        User user = userService.getUserById(id);
        user.setName(updatedUser.getName());
        user.setEmail(updatedUser.getEmail());
        user.setAvatarUrl(updatedUser.getAvatarUrl());
        user.setContactInfo(updatedUser.getContactInfo());
        userService.saveUser(user);
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/block")
    public String blockUser(@PathVariable Integer id) {
        log.info("Блокировка пользователя ID: {}", id);
        User user = userService.getUserById(id);
        user.setBlocked(true);
        userService.saveUser(user);
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/unblock")
    public String unblockUser(@PathVariable Integer id) {
        log.info("Разблокировка пользователя ID: {}", id);
        User user = userService.getUserById(id);
        user.setBlocked(false);
        userService.saveUser(user);
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/balance")
    public String updateBalance(@PathVariable Integer id, @RequestParam BigDecimal amount) {
        log.info("Обновление баланса пользователя ID: {}", id);
        User user = userService.getUserById(id);
        user.setBalance(user.getBalance().add(amount));
        userService.saveUser(user);
        return "redirect:/admin/users";
    }

    // Управление лотами
    @GetMapping("/lots")
    public String listLots(Model model) {
        log.info("Получение списка лотов");
        List<Lot> lots = lotService.getAllLots();
        model.addAttribute("lots", lots);
        model.addAttribute("user", getCurrentUser());
        return "admin/lots";
    }

    @GetMapping("/lots/{id}/edit")
    public String editLotForm(@PathVariable Integer id, Model model) {
        log.info("Открытие формы редактирования лота ID: {}", id);
        Lot lot = lotService.getLotById(id);
        model.addAttribute("lot", lot);
        model.addAttribute("user", getCurrentUser());
        return "admin/edit_lot";
    }

    @PostMapping("/lots/{id}/edit")
    public String editLot(
            @PathVariable Integer id,
            @ModelAttribute Lot updatedLot,
            @RequestParam String status) {
        log.info("Сохранение изменений лота ID: {}", id);
        Lot lot = lotService.getLotById(id);

        lot.setTitle(updatedLot.getTitle());
        lot.setDescription(updatedLot.getDescription());
        lot.setStartingPrice(updatedLot.getStartingPrice());
        lot.setMinBidStep(updatedLot.getMinBidStep());
        lot.setEndTime(updatedLot.getEndTime());

        if (status != null && !status.isEmpty()) {
            lot.setStatus(status);
            if ("SOLD".equals(status)) {
                User winner = bidService.findHighestBidder(id);
                if (winner != null) {
                    lot.setWinner(winner);
                    log.info("Лот ID {} присвоен победителю: {}", id, winner.getEmail());
                } else {
                    log.warn("Нет ставок для лота ID {}, статус SOLD не установлен", id);
                    lot.setStatus("ACTIVE");
                }
            } else {
                lot.setWinner(null);
            }
        }

        lotService.saveLot(lot, null);
        return "redirect:/admin/lots";
    }

    @PostMapping("/lots/{id}/deactivate")
    public String deactivateLot(@PathVariable Integer id) {
        log.info("Деактивация лота ID: {}", id);
        Lot lot = lotService.getLotById(id);
        lot.setStatus("CANCELLED");
        lotService.saveLot(lot, null);
        return "redirect:/admin/lots";
    }

    // Управление комментариями
    @GetMapping("/comments")
    public String listComments(Model model) {
        log.info("Получение списка комментариев");
        List<Comment> comments = commentService.getAllComments();
        model.addAttribute("comments", comments);
        model.addAttribute("user", getCurrentUser());
        return "admin/comments";
    }

    @PostMapping("/comments/{id}/delete")
    public String deleteComment(@PathVariable Integer id) {
        log.info("Удаление комментария ID: {}", id);
        commentService.deleteComment(id);
        return "redirect:/admin/comments";
    }
}