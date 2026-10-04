package com.example.pdf_agent.Entities;

import java.util.List;

public record AgentResponse(String answer, List<Source> sources, String verificationStatus,
                            List<String> warnings, String agentTraceId) {
    public record Source(int page, String excerpt) {}
}