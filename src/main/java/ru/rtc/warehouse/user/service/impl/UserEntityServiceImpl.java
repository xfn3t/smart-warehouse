package ru.rtc.warehouse.user.service.impl;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rtc.warehouse.auth.UserDetailsImpl;
import ru.rtc.warehouse.exception.NotFoundException;
import ru.rtc.warehouse.user.model.User;
import ru.rtc.warehouse.user.repository.UserRepository;
import ru.rtc.warehouse.user.service.UserEntityService;

@Service
@RequiredArgsConstructor
public class UserEntityServiceImpl implements UserEntityService {

    private final UserRepository userRepository;

    @Override
    public User save(User user) {
        return userRepository.save(user);
    }

    @Override
    public User update(User user) {
        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository
            .findById(id)
            .orElseThrow(() -> new NotFoundException("User not found"));
    }

    @Override
    public void delete(Long id) {
        userRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository
            .findByEmail(email)
            .orElseThrow(() -> new NotFoundException("User not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existByEmail(String email) {
        return false;
    }

    @Override
    public User saveUser(User user) {
        return userRepository.save(user);
    }

    @Override
    public List<User> findAllByWarehouse(String warehouseCode) {
        return userRepository.findAllByWarehouseCode(warehouseCode);
    }

    @Override
    public User getCurrentUser() {
        Authentication authentication =
            SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new NotFoundException("Current user not authenticated");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetailsImpl userDetails) {
            return userDetails.getUser();
        }
        throw new NotFoundException("Current user not found");
    }
}
