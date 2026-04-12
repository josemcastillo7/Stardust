package com.zipcode.stardust.service;

import com.zipcode.stardust.model.User;
import com.zipcode.stardust.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

@Component
public class CommonAttributesHelper {

    @Autowired
    private MessageRepository messageRepository;

    @Value("${site.name:Schooner}")
    private String siteName;

    @Value("${site.description:a schooner forum}")
    private String siteDescription;

    public User getCurrentUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() ||
                "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return (User) auth.getPrincipal();
    }

    public void addCommonAttributes(Model model, Authentication auth) {
        model.addAttribute("siteName", siteName);
        model.addAttribute("siteDescription", siteDescription);

        User user = getCurrentUser(auth);
        if (user != null) {
            model.addAttribute("isLoggedIn", true);
            model.addAttribute("currentUser", user.getUsername());
            model.addAttribute("isAdmin", user.isAdmin());
            model.addAttribute("unreadCount",
                    messageRepository.countByRecipientAndReadFalseAndDeletedByRecipientFalse(user));
        } else {
            model.addAttribute("isLoggedIn", false);
            model.addAttribute("isAdmin", false);
            model.addAttribute("unreadCount", 0L);
        }
    }
}
