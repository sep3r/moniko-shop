package com.monikoshop.security;

import com.monikoshop.entity.User;
import com.monikoshop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

/**
 * ورود: شناسه‌ی واردشده شماره موبایل است (برای حساب‌های قدیمی/مدیر ایمیل هم پذیرفته می‌شود).
 * UserDetails برگشتی username = «id کاربر» دارد؛ همان id در subject توکن JWT و
 * Authentication.getName() استفاده می‌شود، پس تغییر شماره/ایمیل توکن‌ها را خراب نمی‌کند.
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        String id = identifier == null ? "" : identifier.trim();
        String phone = PhoneNumbers.normalize(id);
        User user = (phone != null
                ? userRepository.findByPhone(phone)
                : userRepository.findByEmail(id.toLowerCase()))
                .orElseThrow(() -> new UsernameNotFoundException("user not found"));
        return toUserDetails(user);
    }

    /** برای فیلتر JWT: subject توکن همان id کاربر است. */
    public UserDetails loadUserById(Long id) throws UsernameNotFoundException {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("user not found"));
        return toUserDetails(user);
    }

    private static UserDetails toUserDetails(User user) {
        return org.springframework.security.core.userdetails.User.builder()
                .username(String.valueOf(user.getId()))
                .password(user.getPassword())
                .authorities(user.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority(role.name()))
                        .collect(Collectors.toList()))
                .disabled(!user.isEnabled())
                .build();
    }
}
