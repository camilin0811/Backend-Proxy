package com.ucc.aiproxy.service;

import com.ucc.aiproxy.dto.GenerationRequest;
import com.ucc.aiproxy.dto.GenerationResponse;
import com.ucc.aiproxy.util.TokenEstimator;

import java.time.Clock;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * REAL SUBJECT of the Proxy pattern.
 * <p>
 * Simulates an expensive AI text-generation service: it waits {@code latencyMs} (1200 ms by default)
 * and returns a random predefined answer. It knows nothing about plans, limits or quotas —
 * that responsibility belongs to the proxies.
 */
public class MockAIGenerationService implements AIGenerationService {

    public static final String MODEL_NAME = "mock-gpt-1";

    private static final List<String> PREDEFINED_RESPONSES = List.of(
            "The Proxy pattern provides a surrogate or placeholder for another object to control access to it. "
                    + "In this platform, two proxies protect the AI service: one limits requests per minute and "
                    + "the other enforces a monthly token quota.",
            "Great question! A good way to approach this problem is to split it into smaller steps, validate "
                    + "each step independently and then combine the results. This keeps the solution simple, "
                    + "testable and easy to maintain.",
            "Here is a short summary: rate limiting protects the system from bursts of traffic, while quotas "
                    + "control long-term consumption. Together they make the service fair for every user and "
                    + "predictable for the provider.",
            "Software design patterns are reusable solutions to recurring problems. Structural patterns such as "
                    + "Adapter, Bridge, Decorator, Facade and Proxy describe how classes and objects are composed "
                    + "to form larger structures.",
            "I am a simulated AI model. I do not really understand your prompt, but I can pretend very well! "
                    + "This answer was selected at random from a list of predefined responses after a delay of "
                    + "about 1.2 seconds.",
            "Tip: write clear prompts. Give context, describe the expected output format and include an example. "
                    + "Clear prompts usually produce better answers and consume fewer tokens."
    );

    private final long latencyMs;
    private final Clock clock;

    public MockAIGenerationService(long latencyMs, Clock clock) {
        this.latencyMs = latencyMs;
        this.clock = clock;
    }

    @Override
    public GenerationResponse generate(GenerationRequest request) {
        long start = System.currentTimeMillis();
        simulateLatency();

        String text = PREDEFINED_RESPONSES.get(ThreadLocalRandom.current().nextInt(PREDEFINED_RESPONSES.size()));
        int promptTokens = TokenEstimator.estimate(request.prompt());
        int completionTokens = TokenEstimator.estimate(text);

        return new GenerationResponse(
                text,
                promptTokens,
                completionTokens,
                promptTokens + completionTokens,
                MODEL_NAME,
                System.currentTimeMillis() - start,
                clock.instant());
    }

    private void simulateLatency() {
        if (latencyMs <= 0) {
            return;
        }
        try {
            Thread.sleep(latencyMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("AI generation was interrupted", e);
        }
    }
}
