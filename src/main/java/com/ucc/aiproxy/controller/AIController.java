package com.ucc.aiproxy.controller;

import com.ucc.aiproxy.dto.GenerateRequestBody;
import com.ucc.aiproxy.dto.GenerationRequest;
import com.ucc.aiproxy.dto.GenerationResponse;
import com.ucc.aiproxy.service.AIGenerationService;
import com.ucc.aiproxy.util.TokenEstimator;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * CLIENT of the Proxy pattern. It depends only on the {@link AIGenerationService} interface;
 * Spring injects the head of the proxy chain (RateLimit -> Quota -> Mock).
 */
@RestController
@RequestMapping("/api/ai")
public class AIController {

    public static final String USER_HEADER = "X-User-Id";

    private final AIGenerationService aiGenerationService;

    public AIController(AIGenerationService aiGenerationService) {
        this.aiGenerationService = aiGenerationService;
    }

    @PostMapping("/generate")
    public GenerationResponse generate(@RequestHeader(USER_HEADER) String userId,
                                       @Valid @RequestBody GenerateRequestBody body) {
        return aiGenerationService.generate(new GenerationRequest(UserIds.normalize(userId), body.prompt()));
    }

    /** Helper endpoint: approximate token count for a prompt (same formula as the frontend estimator). */
    @PostMapping("/estimate")
    public Map<String, Integer> estimate(@RequestBody Map<String, String> body) {
        return Map.of("estimatedTokens", TokenEstimator.estimate(body.getOrDefault("prompt", "")));
    }
}
