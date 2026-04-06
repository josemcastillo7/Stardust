package com.zipcode.stardust.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "subforum")
public class Subforum {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String title;

    @Column
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Subforum parent;

    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    private List<Subforum> subforums = new ArrayList<>();

    @OneToMany(mappedBy = "subforum", fetch = FetchType.LAZY)
    private List<Post> posts = new ArrayList<>();

    @Column(nullable = false)
    private boolean hidden = false;

    public Subforum() {}

    public Subforum(String title, String description, Subforum parent) {
        this.title = title;
        this.description = description;
        this.parent = parent;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Subforum getParent() { return parent; }
    public void setParent(Subforum parent) { this.parent = parent; }
    public List<Subforum> getSubforums() { return subforums; }
    public void setSubforums(List<Subforum> subforums) { this.subforums = subforums; }
    public List<Post> getPosts() { return posts; }
    public void setPosts(List<Post> posts) { this.posts = posts; }
    public boolean isHidden() { return hidden; }
    public void setHidden(boolean hidden) { this.hidden = hidden; }
}
