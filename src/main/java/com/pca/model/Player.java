package com.pca.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "player")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Player {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String phone;
    private Integer age;
    private LocalDate joinDate;

    @ManyToOne
    @JoinColumn(name = "group_id")
    private GroupEntity playerGroup; // NEW NAME
    @Column(length = 1024)
    private String notes;

    private String photoUrl;
}
