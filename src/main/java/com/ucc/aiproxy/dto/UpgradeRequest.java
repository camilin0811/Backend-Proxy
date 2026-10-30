package com.ucc.aiproxy.dto;

import com.ucc.aiproxy.model.Plan;

/**
 * Body for POST /api/quota/upgrade. If plan is null the default upgrade is FREE -> PRO.
 * paymentReference comes from the frontend's simulated payment (no real charge).
 */
public record UpgradeRequest(Plan plan, String paymentReference) {
}
