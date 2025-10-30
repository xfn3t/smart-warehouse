package ru.rtc.warehouse.auth;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import ru.rtc.warehouse.user.model.User;

import java.util.Collection;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class UserDetailsImpl implements UserDetails {

	private final User user;

	// Временное изменение для обхода 403
	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		var role = user.getRole();
		var code = (role != null ? role.getCode() : null);
		if (code == null) return List.of();
		return List.of(new SimpleGrantedAuthority("ROLE_" + code));
	}

	@Override
	public String getPassword() {
		return user.getPassword();
	}

	@Override
	public String getUsername() {
		return user.getEmail();
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}
}