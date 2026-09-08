package com.graminsaathi.data;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SchemesDataLoader {

    private final ObjectMapper objectMapper;

    private List<SchemesReference.Scheme> schemes;

    @PostConstruct
    public void load() {
        try (InputStream is = new ClassPathResource("schemes_reference.json").getInputStream()) {
            schemes = objectMapper.readValue(is, new TypeReference<List<SchemesReference.Scheme>>() {});
        } catch (IOException e) {
            throw new RuntimeException("Failed to load schemes_reference.json", e);
        }
    }

    public List<SchemesReference.Scheme> getAllSchemes() {
        return schemes != null ? schemes : List.of();
    }

    public List<SchemesReference.Scheme> getApplicableSchemes(double projectCost) {
        if (schemes == null) return List.of();
        return schemes.stream()
                .filter(s -> projectCost >= s.getMinCost() && projectCost <= s.getMaxCost())
                .toList();
    }

    public SchemesReference.Scheme getPrimaryScheme(double projectCost) {
        return getApplicableSchemes(projectCost).stream().findFirst().orElse(null);
    }
}