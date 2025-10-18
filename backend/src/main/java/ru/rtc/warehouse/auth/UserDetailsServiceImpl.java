package ru.rtc.warehouse.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import ru.rtc.warehouse.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

	private final UserRepository users;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		var user = users.findByEmail(username)
				.orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
		return new UserDetailsImpl(user);
	}
}
