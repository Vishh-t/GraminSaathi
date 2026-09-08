package com.graminsaathi.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DemoDataLoader {

    private final ObjectMapper objectMapper;

    private DemoData demoData;
    private Map<String, DemoData.VillageData> villageMap = new ConcurrentHashMap<>();
    private Map<String, DemoData.BusinessCategoryData> categoryMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void load() {
        try (InputStream is = new ClassPathResource("demo_data.json").getInputStream()) {
            demoData = objectMapper.readValue(is, DemoData.class);
            if (demoData.getVillages() != null) {
                villageMap = demoData.getVillages().stream()
                        .collect(Collectors.toMap(DemoData.VillageData::getVillageName, v -> v));
            }
            if (demoData.getBusinessCategories() != null) {
                categoryMap = demoData.getBusinessCategories().stream()
                        .collect(Collectors.toMap(DemoData.BusinessCategoryData::getCategoryName, c -> c));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load demo_data.json", e);
        }
    }

    public List<DemoData.VillageData> getAllVillages() {
        return demoData != null ? demoData.getVillages() : List.of();
    }

    public List<DemoData.BusinessCategoryData> getAllBusinessCategories() {
        return demoData != null ? demoData.getBusinessCategories() : List.of();
    }

    public DemoData.VillageData getVillage(String villageName) {
        return villageMap.get(villageName);
    }

    public DemoData.BusinessCategoryData getBusinessCategory(String categoryName) {
        return categoryMap.get(categoryName);
    }

    public DemoData.BusinessData getBusinessData(String villageName, String categoryName) {
        DemoData.VillageData village = villageMap.get(villageName);
        if (village == null || village.getBusinessData() == null) {
            return null;
        }
        return village.getBusinessData().get(categoryName);
    }
}