package com.zipcode.stardust.model;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
@Entity
public class Reaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String type;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user; //user relationship

    @ManyToOne
    @JoinColumn(name = "post_id")
    private Post post; //post relationship

    @ManyToOne
    @JoinColumn(name = "comment_id" , nullable = true)
    private Comment comment; //comment relationship

    //getters and setters
public Long getId() { return id; }

public String getType() { return type; }
public void setType(String type) { this.type = type; }

public User getUser() { return user; }
public void setUser(User user) { this.user = user; }

public Post getPost() { return post; }
public void setPost(Post post) { this.post = post; }

public Comment getComment() { return comment; }
public void setComment(Comment comment) { this.comment = comment; }
}
