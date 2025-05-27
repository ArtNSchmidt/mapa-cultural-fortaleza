package com.example.culturalmapapp.service;

import com.example.culturalmapapp.model.User;
import com.example.culturalmapapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.Arrays;


@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        // Spring Security expects roles to be in the format "ROLE_USER", "ROLE_ADMIN" etc.
        // If your roles are stored differently (e.g., just "USER", "ADMIN"), adjust here.
        // For simplicity, assuming roles in DB are like "ROLE_CONSUMER", "ROLE_PRODUCER"
        // If they are "CONSUMER", "PRODUCER", they need "ROLE_" prefix
        
        // If roles are stored as "CONSUMER", "PRODUCER", "ADMIN" in DB:
        // Collection<? extends GrantedAuthority> authorities = 
        //     Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole()));

        // If roles are stored as "ROLE_CONSUMER", "ROLE_PRODUCER", "ROLE_ADMIN" in DB:
        Collection<? extends GrantedAuthority> authorities =
            Arrays.stream(user.getRole().split(",")) // Assuming roles are comma-separated if multiple
                  .map(SimpleGrantedAuthority::new)
                  .collect(Collectors.toList());


        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                authorities);
    }
}
