package com.example.collabcode.service;

import com.example.collabcode.model.User;
import com.example.collabcode.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;
import org.springframework.security.core.userdetails.UserDetails;

@Service
public class UserService  {

    @Autowired
    private UserRepository userRepository;

    public String registerUser(User user) {
        Optional<User> existingUser = userRepository.findByUsername(user.getUsername());
        if (userRepository.existsByUserId(user.getUserId())) {
            return "User ID already exists";
        }
        if (existingUser.isPresent()) {
            return "Username is already taken";
        }

        userRepository.save(user);
        return "User registered successfully";
    }

    public User loginUser(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);

        System.out.println("Searching for user: " + username+" password "+password);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getPassword().equals(password)) {
                return user;
            } else {
                System.out.println("Password mismatch for user: " + username);
            }
        } else {
            System.out.println("user: "+userOpt);
            System.out.println("User not found for username: " + username);
        }
        return null;
    }

}
