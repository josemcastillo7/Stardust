package com.zipcode.stardust.controller;

import com.zipcode.stardust.model.Message;
import com.zipcode.stardust.model.User;
import com.zipcode.stardust.repository.MessageRepository;
import com.zipcode.stardust.repository.UserRepository;
import com.zipcode.stardust.service.CommonAttributesHelper;
import com.zipcode.stardust.service.ForumService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
public class MessageController {

    @Autowired private MessageRepository messageRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CommonAttributesHelper helper;
    @Autowired private ForumService forumService;

    // ── Inbox ────────────────────────────────────────────────────────────────

    @GetMapping("/messages/inbox")
    public String inbox(Model model, Authentication auth) {
        helper.addCommonAttributes(model, auth);
        User user = helper.getCurrentUser(auth);
        if (user == null) return "redirect:/loginform";

        List<Message> messages =
                messageRepository.findByRecipientAndDeletedByRecipientFalseOrderBySentAtDesc(user);
        model.addAttribute("messages", messages);
        return "messages/inbox";
    }

    // ── Outbox ───────────────────────────────────────────────────────────────

    @GetMapping("/messages/outbox")
    public String outbox(Model model, Authentication auth) {
        helper.addCommonAttributes(model, auth);
        User user = helper.getCurrentUser(auth);
        if (user == null) return "redirect:/loginform";

        List<Message> messages =
                messageRepository.findBySenderAndDeletedBySenderFalseOrderBySentAtDesc(user);
        model.addAttribute("messages", messages);
        return "messages/outbox";
    }

    // ── Compose — show form ──────────────────────────────────────────────────

    @GetMapping("/messages/compose")
    public String composeForm(
            @RequestParam(required = false, defaultValue = "") String to,
            @RequestParam(required = false, defaultValue = "") String subject,
            Model model, Authentication auth) {

        helper.addCommonAttributes(model, auth);
        if (helper.getCurrentUser(auth) == null) return "redirect:/loginform";

        // Truncate pre-filled subject to 200 chars to stay within the field limit
        if (subject.length() > 200) subject = subject.substring(0, 200);

        model.addAttribute("toUsername", to);
        model.addAttribute("subjectPrefill", subject);
        model.addAttribute("errors", new ArrayList<>());
        return "messages/compose";
    }

    // ── Compose — send message ───────────────────────────────────────────────

    @PostMapping("/messages/compose")
    public String sendMessage(
            @RequestParam String to,
            @RequestParam String subject,
            @RequestParam String content,
            Model model, Authentication auth) {

        helper.addCommonAttributes(model, auth);
        User sender = helper.getCurrentUser(auth);
        if (sender == null) return "redirect:/loginform";

        List<String> errors = new ArrayList<>();

        // Validate subject and content
        if (subject == null || subject.isBlank() || subject.length() > 200)
            errors.add("Subject must be between 1 and 200 characters.");
        if (content == null || content.isBlank() || content.length() > 4999)
            errors.add("Message must be between 1 and 4999 characters.");

        // Look up the recipient by username
        Optional<User> recipientOpt = userRepository.findByUsername(to);
        if (recipientOpt.isEmpty())
            errors.add("No user with the username \"" + to + "\" exists.");

        // If any errors, re-render the compose form with the values intact
        if (!errors.isEmpty()) {
            model.addAttribute("toUsername", to);
            model.addAttribute("subjectPrefill", subject);
            model.addAttribute("errors", errors);
            return "messages/compose";
        }

        Message message = new Message(sender, recipientOpt.get(), subject, content);
        messageRepository.save(message);
        return "redirect:/messages/outbox";
    }

    // ── View a message ───────────────────────────────────────────────────────

    @GetMapping("/messages/view")
    public String viewMessage(
            @RequestParam Long id,
            Model model, Authentication auth) {

        helper.addCommonAttributes(model, auth);
        User user = helper.getCurrentUser(auth);
        if (user == null) return "redirect:/loginform";

        Optional<Message> opt = messageRepository.findById(id);
        if (opt.isEmpty()) return "redirect:/messages/inbox";
        Message message = opt.get();

        // Security check — only the sender or recipient may view this message
        boolean isSender    = message.getSender().getId().equals(user.getId());
        boolean isRecipient = message.getRecipient().getId().equals(user.getId());
        if (!isSender && !isRecipient) return "redirect:/";

        // Mark as read when the recipient opens it for the first time
        if (isRecipient && !message.isRead()) {
            message.setRead(true);
            messageRepository.save(message);
        }

        // Build the reply URL with a properly encoded subject line
        String encodedSubject = URLEncoder.encode("Re: " + message.getSubject(), StandardCharsets.UTF_8);
        String replyUrl = "/messages/compose?to=" + message.getSender().getUsername()
                + "&subject=" + encodedSubject;

        model.addAttribute("message", message);
        model.addAttribute("renderedBody", forumService.renderMarkdown(message.getContent()));
        model.addAttribute("replyUrl", replyUrl);
        model.addAttribute("isSender", isSender);
        return "messages/view";
    }

    // ── Delete (soft) ────────────────────────────────────────────────────────

    @PostMapping("/messages/delete")
    public String deleteMessage(
            @RequestParam Long id,
            @RequestParam String box,
            Authentication auth) {

        User user = helper.getCurrentUser(auth);
        if (user == null) return "redirect:/loginform";

        Optional<Message> opt = messageRepository.findById(id);
        if (opt.isEmpty()) return "redirect:/messages/inbox";
        Message message = opt.get();

        if ("outbox".equals(box) && message.getSender().getId().equals(user.getId())) {
            message.setDeletedBySender(true);
            messageRepository.save(message);
            return "redirect:/messages/outbox";
        }

        if ("inbox".equals(box) && message.getRecipient().getId().equals(user.getId())) {
            message.setDeletedByRecipient(true);
            messageRepository.save(message);
        }

        return "redirect:/messages/inbox";
    }
}
