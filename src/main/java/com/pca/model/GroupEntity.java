package com.pca.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "player_group")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name; // Junior, Sub-Junior, Super-Junior, Senior, Advanced
}
