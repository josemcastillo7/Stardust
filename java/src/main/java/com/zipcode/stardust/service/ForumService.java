package com.zipcode.stardust.service;

import java.util.Optional;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import com.zipcode.stardust.model.Subforum;
import com.zipcode.stardust.model.User;
import com.zipcode.stardust.model.UserProfile;
import com.zipcode.stardust.repository.SubforumRepository;
import com.zipcode.stardust.repository.UserProfileRepository;
import com.zipcode.stardust.repository.UserRepository;

@Service
public class ForumService {

    @Autowired
    private SubforumRepository subforumRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    // Markdown rendering pipeline — thread-safe singletons
    private final Parser mdParser = Parser.builder().build();
    private final HtmlRenderer mdRenderer = HtmlRenderer.builder().build();
    private final PolicyFactory sanitizer = new HtmlPolicyBuilder()
            .allowElements("p", "br", "hr", "b", "strong", "em", "i", "u", "s",
                           "code", "pre", "blockquote", "ul", "ol", "li",
                           "h1", "h2", "h3", "h4")
            .allowUrlProtocols("http", "https")
            .allowElements("a")
            .allowAttributes("href").onElements("a")
            .requireRelNofollowOnLinks()
            .toFactory();

    public String renderMarkdown(String raw) {
        if (raw == null) return "";
        Node document = mdParser.parse(raw);
        String html = mdRenderer.render(document);
        return sanitizer.sanitize(html);
    }

    public String generateLinkPath(Long subforumId) {
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
        profile.setBio(bio);
        return userProfileRepository.save(profile);
    }
}