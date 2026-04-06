package com.zipcode.stardust.model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "post")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 5000)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subforum_id")
    private Subforum subforum;

    @Column(nullable = false)
    private LocalDateTime postdate;

    @OneToMany(mappedBy = "post", fetch = FetchType.LAZY)
    private List<Comment> comments = new ArrayList<>();

    private static final int DAYS_PER_MONTH = 30;
    private static final long CACHE_TTL_SECONDS = 30;

    private record TimeCache(String value, LocalDateTime computedAt) {}

    @Transient
    private volatile TimeCache timeCache;

    public Post() {}

    public Post(String title, String content, User user, Subforum subforum) {
        this.title = title;
        this.content = content;
        this.user = user;
        this.subforum = subforum;
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
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Subforum getSubforum() { return subforum; }
    public void setSubforum(Subforum subforum) { this.subforum = subforum; }
    public LocalDateTime getPostdate() { return postdate; }
    public void setPostdate(LocalDateTime postdate) { this.postdate = postdate; }
    public List<Comment> getComments() { return comments; }
    public void setComments(List<Comment> comments) { this.comments = comments; }
}
