package com.syntra.controller.api;

import com.syntra.dto.WebhookPayloadDTO;
import com.syntra.model.Lead;
import com.syntra.service.LeadService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/webhook")
public class WebhookController {

    private final LeadService leadService;

    @Value("${syntra.webhook.secret}")
    private String webhookSecret;

    public WebhookController(LeadService leadService) {
        this.leadService = leadService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> receberLead(
            @Valid @RequestBody WebhookPayloadDTO payload,
            HttpServletRequest request) {

        String token = request.getHeader("X-Webhook-Token");
        if (token == null) token = request.getParameter("token");

        if (!webhookSecret.equals(token)) {
            return ResponseEntity.status(401).body(Map.of("erro", "Token inválido"));
        }

        Lead lead = leadService.criarViaWebhook(payload);

        return ResponseEntity.status(201).body(Map.of(
                "id", lead.getId(),
                "mensagem", "Lead recebido com sucesso"
        ));
    }
}
