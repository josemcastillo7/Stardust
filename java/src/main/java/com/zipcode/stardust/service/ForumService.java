package com.zipcode.stardust.service;

import com.zipcode.stardust.model.Subforum;
import com.zipcode.stardust.repository.SubforumRepository;
import com.zipcode.stardust.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.util.Optional;

@Service
public class ForumService {

    @Autowired
    private SubforumRepository subforumRepository;

    @Autowired
    private UserRepository userRepository;

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
}
