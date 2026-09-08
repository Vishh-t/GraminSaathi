package com.graminsaathi.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class DiscoveryResponse {
    private List<DiscoveredBusinessResponse> businesses;
}