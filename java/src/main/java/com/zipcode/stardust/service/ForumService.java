package com.zipcode.stardust.service;

import java.util.Optional;

import org.commonmark.ext.autolink.AutolinkExtension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.ext.task.list.items.TaskListItemsExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import com.zipcode.stardust.model.Comment;
import com.zipcode.stardust.model.Post;
import com.zipcode.stardust.model.Reaction;
import com.zipcode.stardust.model.Subforum;
import com.zipcode.stardust.model.User;
import com.zipcode.stardust.model.UserProfile;
import com.zipcode.stardust.repository.CommentRepository;
import com.zipcode.stardust.repository.PostRepository;
import com.zipcode.stardust.repository.ReactionRepository;
import com.zipcode.stardust.repository.SubforumRepository;
import com.zipcode.stardust.repository.UserProfileRepository;
import com.zipcode.stardust.repository.UserRepository;

@Service
public class ForumService {

    @Autowired
    private SubforumRepository subforumRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private ReactionRepository reactionRepository;

    // Markdown rendering pipeline — thread-safe singletons
    private static final java.util.List<org.commonmark.Extension> MD_EXTENSIONS = java.util.List.of(
            TablesExtension.create(),
            StrikethroughExtension.create(),
            TaskListItemsExtension.create(),
            AutolinkExtension.create()
    );
    private final Parser mdParser = Parser.builder().extensions(MD_EXTENSIONS).build();
    private final HtmlRenderer mdRenderer = HtmlRenderer.builder().extensions(MD_EXTENSIONS).build();
    private final PolicyFactory sanitizer = new HtmlPolicyBuilder()
            .allowElements("p", "br", "hr", "b", "strong", "em", "i", "u", "s",
                           "code", "pre", "blockquote", "ul", "ol", "li",
                           "h1", "h2", "h3", "h4")
            .allowUrlProtocols("http", "https")
            .allowElements("a")
            .allowAttributes("href").onElements("a")
            .requireRelNofollowOnLinks()
            .allowElements("img")
            .allowAttributes("src", "alt").onElements("img")
            // Tables (GFM)
            .allowElements("table", "thead", "tbody", "tr", "th", "td")
            .allowAttributes("align").onElements("th", "td")
            // Task list checkboxes
            .allowElements("input")
            .allowAttributes("type", "disabled", "checked").onElements("input")
            // Inline spans for font size and font family
            .allowElements("span")
            .allowStyling()
            // Uploaded video embeds
            .allowElements("video")
            .allowAttributes("src", "controls", "width", "height").onElements("video")
            .toFactory();

    public String renderMarkdown(String raw) {
        if (raw == null) return "";
        Node document = mdParser.parse(raw);
        String html = mdRenderer.render(document);
        return sanitizer.sanitize(html);
    }

    public String generateLinkPath(long subforumId) {
        StringBuilder sb = new StringBuilder();
        sb.append(" / <a href='/'>Forum Index</a>");
        Optional<Subforum> opt = subforumRepository.findById(subforumId);
        if (opt.isEmpty()) return sb.toString();
        Subforum current = opt.get();
        java.util.LinkedList<Subforum> chain = new java.util.LinkedList<>();
        while (current != null) {
            chain.addFirst(current);
            current = current.getParent();
        }
        for (Subforum sf : chain) {
            sb.append(" / <a href='/subforum?sub=").append(sf.getId()).append("'>")
              .append(escapeHtml(sf.getTitle())).append("</a>");
        }
        return sb.toString();
    }

    private String escapeHtml(String text) {
        return HtmlUtils.htmlEscape(text != null ? text : "");
    }

    public boolean validTitle(String title) {
        return title != null && title.length() > 4 && title.length() < 140;
    }

    public boolean validContent(String content) {
        return content != null && content.length() > 10 && content.length() < 5000;
    }

    public boolean validUsername(String username) {
        return username != null && username.matches("^[a-zA-Z0-9!@#%&]{4,40}$");
    }

    public boolean validPassword(String password) {
        return password != null && password.matches("^[a-zA-Z0-9!@#%&]{6,40}$");
    }

    public boolean usernameTaken(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean emailTaken(String email) {
        return userRepository.existsByEmail(email);
    }


    // ADD 2 - UserProfile methods
    public UserProfile getUserProfile(User user) {
        return userProfileRepository.findByUser(user);
    }

    public UserProfile createUserProfile(User user) {
        UserProfile profile = new UserProfile(user);
        return userProfileRepository.save(profile);
    }

    public UserProfile updateBio(User user, String bio) {
        UserProfile profile = userProfileRepository.findByUser(user);
        if (profile == null) profile = new UserProfile(user);
        profile.setBio(bio);
        return userProfileRepository.save(profile);
    }

    public boolean updateEmail(User user, String newEmail) {
        if (newEmail == null || newEmail.isBlank()) return false;
        if (emailTaken(newEmail) && !newEmail.equalsIgnoreCase(user.getEmail())) return false;
        user.setEmail(newEmail);
        userRepository.save(user);
        return true;
    }

    public boolean updatePassword(User user, String currentRaw, String newRaw,
                                   org.springframework.security.crypto.password.PasswordEncoder encoder) {
        if (!user.checkPassword(currentRaw, encoder)) return false;
        if (!validPassword(newRaw)) return false;
        user.setPasswordHash(encoder.encode(newRaw));
        userRepository.save(user);
        return true;
    }



    // ── REACTIONS ────────────────────────────────────────────────

    public void reactToPost(User user, Post post, String type) {
        Reaction existing = reactionRepository.findByUserAndPost(user, post);

        if (existing == null) {
            Reaction reaction = new Reaction();
            reaction.setUser(user);
            reaction.setPost(post);
            reaction.setType(type);
            reactionRepository.save(reaction);

        } else if (existing.getType().equals(type)) {
            reactionRepository.delete(existing);

        } else {
            existing.setType(type);
            reactionRepository.save(existing);
        }
    }

    public Long getLikeCount(Post post) {
        return reactionRepository.countByPostAndType(post, "LIKE");
    }

    public Long getDislikeCount(Post post) {
        return reactionRepository.countByPostAndType(post, "DISLIKE");
    }

    public Long getFireCount(Post post) {
        return reactionRepository.countByPostAndType(post, "FIRE");
    }

    public Long getFunnyCount(Post post) {
        return reactionRepository.countByPostAndType(post, "FUNNY");
    }

    public Long getSadCount(Post post) {
        return reactionRepository.countByPostAndType(post, "SAD");
    }

    public Long getCelebrateCount(Post post) {
        return reactionRepository.countByPostAndType(post, "CELEBRATE");
    }

    public Reaction getUserReaction(User user, Post post) {
        return reactionRepository.findByUserAndPost(user, post);
    }

    public void moderatePost(long postId) {
        postRepository.deleteById(postId);
    }

    public void moderateComment(long commentId) {
        commentRepository.deleteById(commentId);
    }

    public boolean editPost(long postId, String title, String content, User requestingUser) {
        Optional<Post> opt = postRepository.findById(postId);
        if (opt.isEmpty()) return false;
        Post post = opt.get();
        if (!post.getUser().getId().equals(requestingUser.getId())) return false;
        post.setTitle(title);
        post.setContent(content);
        postRepository.save(post);
        return true;
    }

    public boolean editComment(long commentId, String content, User requestingUser) {
        Optional<Comment> opt = commentRepository.findById(commentId);
        if (opt.isEmpty()) return false;
        Comment comment = opt.get();
        if (!comment.getUser().getId().equals(requestingUser.getId())) return false;
        comment.setContent(content);
        commentRepository.save(comment);
        return true;
    }

    public void banUser(String username) {
        Optional<User> opt = userRepository.findByUsername(username);
        if (opt.isPresent()) {
            User user = opt.get();
            user.setBanned(true);
            userRepository.save(user);
        }
    }
}
