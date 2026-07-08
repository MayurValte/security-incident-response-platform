package com.sirp.user.entity;

import com.sirp.user.enums.Role;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;
    @Column(nullable = false)
    private String username;
    @Column(
            nullable = false,
            unique = true
    )
    private String email;
    @Column(nullable = false)
    private String password;
    @Builder.Default
    @Column(nullable = false)
    private Boolean enabled = true;
    @Enumerated(
            EnumType.STRING
    )
    private Role role;
    @ManyToOne
    @JoinColumn(
            name = "team_id"
    )
    private Team team;
}