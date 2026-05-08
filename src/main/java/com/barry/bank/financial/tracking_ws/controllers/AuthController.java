package com.barry.bank.financial.tracking_ws.controllers;


import com.barry.bank.financial.tracking_ws.config.JwtService;
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
        return ResponseEntity.ok(Map.of("access_Token", accessToken));
    }
}
