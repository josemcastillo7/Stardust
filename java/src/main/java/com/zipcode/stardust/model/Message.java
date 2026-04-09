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

@Entity
@Table(name = "message")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id")
    private User recipient;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(nullable = false, length = 5000)
    private String content;

    @Column(nullable = false)
    private LocalDateTime sentAt;

    @Column(nullable = false)
    private boolean read = false;

    @Column(nullable = false)
    private boolean deletedBySender = false;

    @Column(nullable = false)
    private boolean deletedByRecipient = false;

    public Message() {}

    public Message(User sender, User recipient, String subject, String content) {
        this.sender = sender;
        this.recipient = recipient;
        this.subject = subject;
        this.content = content;
        this.sentAt = LocalDateTime.now();
    }

    public String getTimeString() {
        Duration d = Duration.between(sentAt, LocalDateTime.now());
        long months  = d.toDays() / 30;
        long days    = d.toDays();
        long hours   = d.toHours();
        long minutes = d.toMinutes();
        if (months  > 0) return months  + " month"  + (months  == 1 ? "" : "s") + " ago";
        if (days    > 0) return days    + " day"    + (days    == 1 ? "" : "s") + " ago";
        if (hours   > 0) return hours   + " hour"   + (hours   == 1 ? "" : "s") + " ago";
        if (minutes > 0) return minutes + " minute" + (minutes == 1 ? "" : "s") + " ago";
        return "Just now";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getSender() { return sender; }
    public void setSender(User sender) { this.sender = sender; }
    public User getRecipient() { return recipient; }
    public void setRecipient(User recipient) { this.recipient = recipient; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
    public boolean isDeletedBySender() { return deletedBySender; }
    public void setDeletedBySender(boolean v) { this.deletedBySender = v; }
    public boolean isDeletedByRecipient() { return deletedByRecipient; }
    public void setDeletedByRecipient(boolean v) { this.deletedByRecipient = v; }
}
