package com.ucc.aiproxy.controller;

import com.ucc.aiproxy.dto.QuotaStatusResponse;
import com.ucc.aiproxy.dto.UpgradeRequest;
import com.ucc.aiproxy.model.DailyUsage;
import com.ucc.aiproxy.service.QuotaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.ucc.aiproxy.controller.AIController.USER_HEADER;

@RestController
@RequestMapping("/api/quota")
public class QuotaController {

    private final QuotaService quotaService;

    public QuotaController(QuotaService quotaService) {
        this.quotaService = quotaService;
    }

    @GetMapping("/status")
    public QuotaStatusResponse status(@RequestHeader(USER_HEADER) String userId) {
        return quotaService.getStatus(UserIds.normalize(userId));
    }

    @GetMapping("/history")
    public List<DailyUsage> history(@RequestHeader(USER_HEADER) String userId) {
        return quotaService.getHistory(UserIds.normalize(userId));
    }

    @PostMapping("/upgrade")
    public QuotaStatusResponse upgrade(@RequestHeader(USER_HEADER) String userId,
                                       @RequestBody(required = false) UpgradeRequest body) {
        return quotaService.upgrade(UserIds.normalize(userId), body == null ? null : body.plan());
    }
}
