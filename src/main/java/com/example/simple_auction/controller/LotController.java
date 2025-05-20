package com.example.simple_auction.controller;

import com.example.simple_auction.model.Lot;
import com.example.simple_auction.model.User;
import com.example.simple_auction.service.BidService;
import com.example.simple_auction.service.LotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
@RequestMapping("/lots")
public class LotController {

    private static final Logger log = LoggerFactory.getLogger(LotController.class);

    @Autowired
    private LotService lotService;

    @Autowired
    private BidService bidService;

    @GetMapping
    public String listLots(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String error,
            Model model,
            Authentication authentication
    ) {
        log.info("Открытие списка лотов");
        log.debug("Получен параметр поиска: {}", search);
        log.debug("Получен параметр error: {}", error);

        try {
            List<Lot> lots = (search != null && !search.isEmpty())
                    ? lotService.searchLots(search)
                    : lotService.getActiveLots();

            log.debug("Найдено {} лотов", lots.size());
            model.addAttribute("lots", lots);
            model.addAttribute("error", error);

            // Передаем объект User в модель
            if (authentication != null && authentication.isAuthenticated()) {
                User user = (User) authentication.getPrincipal();
                model.addAttribute("user", user);
            }

            return "lots/list";

        } catch (Exception e) {
            log.error("Ошибка при получении списка лотов", e);
            return "redirect:/lots?error=internal";
        }
    }

    @GetMapping("/{id}")
    public String viewLot(
            @PathVariable Integer id,
            Model model,
            Authentication authentication
    ) {
        log.info("Открытие лота с ID: {}", id);

        try {
            Lot lot = lotService.getLotById(id);
            if (lot == null) {
                log.warn("Лот с ID {} не найден", id);
                return "redirect:/lots?error=notfound";
            }

            model.addAttribute("lot", lot);
            model.addAttribute("bids", bidService.getBidsByLot(id));

            // Передаем объект User в модель
            if (authentication != null && authentication.isAuthenticated()) {
                User user = (User) authentication.getPrincipal();
                model.addAttribute("user", user);
            }

            log.debug("Лот '{}' загружен", lot.getTitle());
            return "lots/view";

        } catch (Exception e) {
            log.error("Ошибка при загрузке лота с ID {}", id, e);
            return "redirect:/lots?error=internal";
        }
    }

    @GetMapping("/create")
    public String showCreateForm(
            Model model,
            Authentication authentication
    ) {
        log.info("Открытие формы создания лота");
        model.addAttribute("lot", new Lot());

        // Передаем объект User в модель
        if (authentication != null && authentication.isAuthenticated()) {
            User user = (User) authentication.getPrincipal();
            model.addAttribute("user", user);
        }

        return "lots/create";
    }

    @PostMapping("/create")
    public String createLot(
            @ModelAttribute Lot lot,
            Authentication authentication
    ) {
        log.info("Создание нового лота");

        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                log.warn("Попытка создания лота без аутентификации");
                return "redirect:/login";
            }

            User creator = (User) authentication.getPrincipal();
            log.debug("Создатель лота: {}", creator.getEmail());

            lotService.saveLot(lot, creator);
            log.info("Лот '{}' успешно создан", lot.getTitle());

            return "redirect:/lots";

        } catch (Exception e) {
            log.error("Ошибка при создании лота", e);
            return "redirect:/lots?error=creation";
        }
    }

    @PostMapping("/{id}/bid")
    public String placeBid(
            @PathVariable Integer id,
            @RequestParam BigDecimal bidAmount,
            Authentication authentication
    ) {
        log.info("Пользователь делает ставку на лот ID: {}", id);

        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                log.warn("Неавторизованный пользователь пытается сделать ставку");
                return "redirect:/login";
            }

            User bidder = (User) authentication.getPrincipal();
            log.debug("Пользователь '{}', ставка: {}", bidder.getEmail(), bidAmount);

            bidService.placeBid(id, bidAmount, bidder);
            log.info("Ставка {} на лот {} успешно сделана", bidAmount, id);

            return "redirect:/lots/" + id;

        } catch (IllegalStateException e) {
            log.warn("Ошибка при ставке на лот {}", id, e);
            return "redirect:/lots/" + id + "?error=invalid_bid";
        } catch (Exception e) {
            log.error("Ошибка при обработке ставки", e);
            return "redirect:/lots/" + id + "?error=internal";
        }
    }

    @PostMapping("/{id}/comment")
    public String addComment(
            @PathVariable Integer id,
            @RequestParam String commentText,
            Authentication authentication
    ) {
        log.info("Добавление комментария к лоту ID: {}", id);

        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                log.warn("Неавторизованный пользователь пытается оставить комментарий");
                return "redirect:/login";
            }

            User user = (User) authentication.getPrincipal();
            log.debug("Комментарий от пользователя '{}': {}", user.getEmail(), commentText);

            lotService.addComment(id, commentText, user);
            log.info("Комментарий к лоту {} успешно добавлен", id);
            return "redirect:/lots/" + id;

        } catch (Exception e) {
            log.error("Ошибка при добавлении комментария", e);
            return "redirect:/lots/" + id + "?error=internal";
        }
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteLot(
            @PathVariable Integer id,
            Authentication authentication
    ) {
        log.info("Удаление лота с ID: {}", id);

        try {
            User admin = (User) authentication.getPrincipal();
            log.debug("Администратор '{}' удаляет лот ID: {}", admin.getEmail(), id);

            lotService.deleteLot(id, admin);
            log.info("Лот {} удален", id);
            return "redirect:/lots";

        } catch (Exception e) {
            log.error("Ошибка при удалении лота {}", id, e);
            return "redirect:/lots?error=delete";
        }
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public String cancelAuction(
            @PathVariable Integer id,
            Authentication authentication
    ) {
        log.info("Отмена аукциона с ID: {}", id);

        try {
            User admin = (User) authentication.getPrincipal();
            log.debug("Администратор '{}' отменяет аукцион ID: {}", admin.getEmail(), id);

            lotService.cancelAuction(id, admin);
            log.info("Аукцион {} отменен", id);
            return "redirect:/lots";

        } catch (Exception e) {
            log.error("Ошибка при отмене аукциона {}", id, e);
            return "redirect:/lots?error=cancel";
        }
    }
}