package com.barry.bank.api.controllers.impl;

import com.barry.bank.api.security.config.JwtService;
import com.barry.bank.api.controllers.TokenRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtService jwtService;

    @PostMapping("/token")
    public ResponseEntity<Map<String, String>> generateServiceToken(@RequestBody TokenRequest request) {
        final String accessToken = jwtService.generateServiceToken(request.customerId());
        return ResponseEntity.ok(Map.of("access_token", accessToken));
    }
}
