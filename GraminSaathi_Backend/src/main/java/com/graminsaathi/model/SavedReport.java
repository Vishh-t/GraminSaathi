package com.graminsaathi.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "saved_reports")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "village_name", nullable = false)
    private String villageName;

    @Column(name = "business_category", nullable = false)
    private String businessCategory;

    @Column(name = "available_margin_capital", nullable = false)
    private Double availableMarginCapital;

    @Column(name = "result_json", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String resultJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}