package com.example.mhproject.entity;
import jakarta.persistence.*; import lombok.Data; import java.time.LocalDateTime;
@Data @Entity @Table(name = "user")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private String username; private String password; private String role;
    private LocalDateTime createTime;
}