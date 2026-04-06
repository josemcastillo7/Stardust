package com.zipcode.stardust.model;

import java.time.Duration;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "comment")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 5000)
    private String content;

    @Column(nullable = false)
    private LocalDateTime postdate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    private static final int DAYS_PER_MONTH = 30;
    private static final long CACHE_TTL_SECONDS = 30;

    private record TimeCache(String value, LocalDateTime computedAt) {}

    @Transient
    private volatile TimeCache timeCache;

    public Comment() {}

    public Comment(String content, User user, Post post) {
        this.content = content;
        this.user = user;
        this.post = post;
        this.postdate = LocalDateTime.now();
    }

    public String getTimeString() {
        LocalDateTime now = LocalDateTime.now();
        TimeCache cache = this.timeCache;
        if (cache != null && Duration.between(cache.computedAt(), now).getSeconds() < CACHE_TTL_SECONDS) {
            return cache.value();
        }
        Duration d = Duration.between(postdate, now);
        String result;
        long months = d.toDays() / DAYS_PER_MONTH;
        long days = d.toDays();
        long hours = d.toHours();
        long minutes = d.toMinutes();
        if (months > 0) {
            result = months + " month" + (months == 1 ? "" : "s") + " ago";
        } else if (days > 0) {
            result = days + " day" + (days == 1 ? "" : "s") + " ago";
        } else if (hours > 0) {
            result = hours + " hour" + (hours == 1 ? "" : "s") + " ago";
        } else if (minutes > 0) {
            result = minutes + " minute" + (minutes == 1 ? "" : "s") + " ago";
        } else {
            result = "Just a moment ago!";
        }
        this.timeCache = new TimeCache(result, now);
        return result;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getPostdate() { return postdate; }
    public void setPostdate(LocalDateTime postdate) { this.postdate = postdate; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Post getPost() { return post; }
    public void setPost(Post post) { this.post = post; }
}
