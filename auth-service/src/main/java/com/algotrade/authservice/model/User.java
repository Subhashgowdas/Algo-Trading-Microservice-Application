package com.algotrade.authservice.model;

import java.util.Collection;
import java.util.List;

import org.hibernate.annotations.SQLRestriction;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SQLRestriction("status = 'ACTV'")
@Builder
public class User extends BaseEntity implements UserDetails {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 150)
	private String email;

	@Column(nullable = false, length = 100)
	private String password; // BCrypt hashed

	@Column(nullable = false, length = 50)
	private String role;     // e.g., ROLE_USER, ROLE_ADMIN

	// --- UserDetails methods ---
	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority(role));
	}

	@Override
	public String getUsername() {
		return email;
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
		return getStatus() == EntityStatus.ACTV; 
	}
}