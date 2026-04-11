package com.zipcode.stardust.model;
 
import jakarta.persistence.*;
import java.time.LocalDateTime;
 
@Entity
@Table(name = "user_profiles")
public class UserProfile {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) 
    private Long id;
 
    private String bio;
 
    private String email;
 
    private LocalDateTime joinDate;
 
    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;
 
    public UserProfile() {}
 
    public UserProfile(User user) {
        this.user = user;
        this.email = user.getEmail();
        this.joinDate = LocalDateTime.now();
    }
 
    public Long getId() {
        return id;
    }
 
    public void setId(Long id) {
        this.id = id;
    }
 
    public String getBio() {
        return bio;
    }
 
    public void setBio(String bio) {
        this.bio = bio;
    }
 
    public String getEmail() {
        return email;
    }
 
    public void setEmail(String email) {
        this.email = email;
    }
 
    public LocalDateTime getJoinDate() {
        return joinDate;
    }
 
    public void setJoinDate(LocalDateTime joinDate) {
        this.joinDate = joinDate;
    }
 
    public User getUser() {
        return user;
    }
 
    public void setUser(User user) {
        this.user = user;
    }
 
}