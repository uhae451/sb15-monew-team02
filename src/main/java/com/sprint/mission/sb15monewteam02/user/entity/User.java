package com.sprint.mission.sb15monewteam02.user.entity;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false, unique = true)
    private String email;
    @Column(nullable = false)
    private String password;
    @Column(nullable = false)
    private String nickname;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    @Column()
    private Instant deletedAt;
    public User(String email, String nickname, String password){
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.createdAt = Instant.now();
    }
    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }
    public void delete(){
        this.deletedAt = Instant.now();
    }
    public boolean isDeleted() { return deletedAt != null; }

}
