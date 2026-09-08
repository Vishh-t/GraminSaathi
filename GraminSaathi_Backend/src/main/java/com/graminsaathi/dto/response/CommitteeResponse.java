package com.graminsaathi.dto.response;

import lombok.Data;

@Data
public class CommitteeResponse {
    private AgentOpinion marketAgent;
    private AgentOpinion financeAgent;
    private AgentOpinion riskAgent;
    private String finalVerdict;
    private String verdictReason;

    @Data
    public static class AgentOpinion {
        private String agent;
        private String opinion;
        private String basis;
    }
}