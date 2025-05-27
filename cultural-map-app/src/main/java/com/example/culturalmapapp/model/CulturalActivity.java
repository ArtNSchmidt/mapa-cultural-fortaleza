package com.example.culturalmapapp.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "cultural_activities")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CulturalActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Lob // For longer text
    @Column(columnDefinition = "TEXT")
    private String description;

    private LocalDateTime dateTime;

    private Double latitude;

    private Double longitude;

    private String category;

    @ManyToOne
    @JoinColumn(name = "producer_id", nullable = false)
    private User producer;
}
