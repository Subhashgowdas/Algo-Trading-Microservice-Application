package com.algotrade.authservice.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.algotrade.authservice.model.EntityStatus;
import com.algotrade.authservice.model.User;

public interface UserRepository extends JpaRepository<User, Long>{
	
	Optional<User> findByEmail(String email);
	
	boolean existsByEmail(String email);
	
	@Query("select u from User u where u.email = :email")
	Optional<User> findByEmailIncludingDeleted(@Param("email") String email);
	
	boolean existsByEmailAndStatus(String email, EntityStatus status);

}
