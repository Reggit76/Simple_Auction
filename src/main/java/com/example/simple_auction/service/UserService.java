package com.example.simple_auction.service;

import com.example.simple_auction.model.User;
import com.example.simple_auction.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class UserService implements org.springframework.security.core.userdetails.UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("Запрос на загрузку пользователя: {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
        log.debug("Пользователь найден: {}", email);

        return user;
    }

    // === Методы для бизнес-логики ===
    public List<User> getAllUsers() {
        log.info("Запрос на получение всех пользователей");
        return userRepository.findAll();
    }

    public User getUserById(Integer id) {
        log.debug("Запрос на получение пользователя по ID: {}", id);
        return userRepository.findById(id).orElse(null);
    }

    public User saveUser(User user) {
        log.info("Сохранение пользователя: {}", user.getEmail());
        try {
            // Хешируем пароль
            String encodedPassword = passwordEncoder.encode(user.getPassword());
            user.setPassword(encodedPassword);

            // Устанавливаем роль по умолчанию, если не задана
            if (user.getRole() == null || user.getRole().isEmpty()) {
                user.setRole("USER");
                log.debug("Роль пользователя установлена по умолчанию: USER");
            }

            User savedUser = userRepository.save(user);
            log.info("Пользователь {} успешно сохранён", savedUser.getEmail());
            return savedUser;
        } catch (Exception e) {
            log.error("Ошибка сохранения пользователя", e);
            throw e;
        }
    }

    public void deleteUser(Integer id) {
        log.info("Удаление пользователя с ID: {}", id);
        userRepository.deleteById(id);
        log.info("Пользователь с ID: {} удален", id);
    }

    public Optional<User> getUserByEmail(String email) {
        log.debug("Поиск пользователя по email: {}", email);
        return userRepository.findByEmail(email);
    }
}