package com.example.demologin.entity;

import jakarta.persistence.*;

@Entity
public class Permission {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code; // e.g. "USER_VIEW"

    private String name; // e.g. "Xem người dùng"
    
    public Permission() {}
    
    public Permission(String code, String name) {
        this.code = code;
        this.name = name;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
} 
