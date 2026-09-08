package com.graminsaathi.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class EvidenceResponse {
    private List<EvidenceItem> evidence;

    @Data
    public static class EvidenceItem {
        private String claim;
        private String source;
        private String details;

        public EvidenceItem(String claim, String source, String details) {
            this.claim = claim;
            this.source = source;
            this.details = details;
        }
    }
}