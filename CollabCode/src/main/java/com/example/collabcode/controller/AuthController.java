package com.example.collabcode.controller;

import com.example.collabcode.model.User;
import com.example.collabcode.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
@CrossOrigin(origins = "https://3.86.42.230:3000")
@RestController
@RequestMapping("/api/auth")

public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OAuth2AuthorizedClientService authorizedClientService;

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @GetMapping("/login/success")
    public ResponseEntity<User> handleGitHubLogin(Authentication auth) {
        log.info("Handling GitHub login...");

        if (auth == null || !auth.isAuthenticated()) {
            log.warn("Authentication is null! Check your GitHub OAuth configuration.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        OAuth2User oauthUser = (OAuth2User) auth.getPrincipal();
        log.info("Authenticated user attributes: {}", oauthUser.getAttributes());
        Object idAttribute = oauthUser.getAttribute("id");
        String userId = idAttribute instanceof Integer ? String.valueOf(idAttribute) : (String) idAttribute;
        String username = oauthUser.getAttribute("login");
        String email = oauthUser.getAttribute("email");
        log.info("User ID: {}", userId);
        log.info("email: {}", email);
        if (email == null) {
            email = fetchGitHubEmail(auth);
            log.info("Fetched Email: {}", email);
        }

        User user = userRepository.findByUserId(userId);
        if (user == null) {
            user = new User();
            user.setUserId(userId);
            user.setUsername(username);
            user.setRoomIds(new ArrayList<>());
            user.setFileIds(new ArrayList<>());
        }
            user.setEmail(email);

        userRepository.save(user);
        log.info("User saved with email: {}", user.getEmail());
        log.info("User saved : {}", user.getUserId(), user.getUsername());

        String redirectUrl = "http://localhost:3000/rooms?userId=" + userId + "&username=" + username;
        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", redirectUrl)
                .build();
    }

    private String fetchGitHubEmail(Authentication auth) {
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                ((OAuth2AuthenticationToken) auth).getAuthorizedClientRegistrationId(),
                auth.getName()
        );

        String accessToken = client.getAccessToken().getTokenValue();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                "http://api.github.com/user/emails", HttpMethod.GET, entity, new ParameterizedTypeReference<>() {}
        );

        log.info("GitHub Email API Response: {}", response.getBody());

        if (response.getStatusCode() == HttpStatus.OK) {
            for (Map<String, Object> emailData : response.getBody()) {
                log.info("Email Data: {}", emailData);
                if (Boolean.TRUE.equals(emailData.get("primary"))) {
                    return (String) emailData.get("email");
                }
            }
        }
        log.warn("Could not fetch email from GitHub");
        return null;
    }
    @GetMapping("/loginFailure")
    public ResponseEntity<String> handleLoginFailure(HttpServletRequest request) {
        String error = request.getParameter("error");
        log.error("Login failed with error: {}", error);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Login failed: " + error);
    }
}
