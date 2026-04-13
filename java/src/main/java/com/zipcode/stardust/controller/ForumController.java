package com.zipcode.stardust.controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.zipcode.stardust.model.Comment;
import com.zipcode.stardust.model.Post;
import com.zipcode.stardust.model.Reaction;
import com.zipcode.stardust.model.Subforum;
import com.zipcode.stardust.model.User;
import com.zipcode.stardust.repository.CommentRepository;
import com.zipcode.stardust.repository.MediaEmbedRepository;
import com.zipcode.stardust.repository.PostRepository;
import com.zipcode.stardust.repository.SubforumRepository;
import com.zipcode.stardust.repository.UserRepository;
import com.zipcode.stardust.service.CommonAttributesHelper;
import com.zipcode.stardust.service.ForumService;

@Controller
public class ForumController {

    @Autowired private SubforumRepository subforumRepository;
    @Autowired private PostRepository postRepository;
    @Autowired private CommentRepository commentRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private ForumService forumService;
    @Autowired private MediaEmbedRepository mediaEmbedRepository;
    @Autowired private CommonAttributesHelper helper;

    private User getCurrentUser(Authentication auth) {
        return helper.getCurrentUser(auth);
    }

    private void addCommonAttributes(Model model, Authentication auth) {
        helper.addCommonAttributes(model, auth);
    }

    @GetMapping("/")
    public String index(Model model, Authentication auth) {
        addCommonAttributes(model, auth);
        List<Subforum> topLevel = subforumRepository.findByParentIsNull();
        model.addAttribute("subforums", topLevel);
        return "subforums";
    }

    @GetMapping("/subforum")
    public String subforum(@RequestParam long sub, Model model, Authentication auth) {
        addCommonAttributes(model, auth);
        Optional<Subforum> opt = subforumRepository.findById(sub);
        if (opt.isEmpty()) return "redirect:/";
        Subforum sf = opt.get();
        List<Post> posts = postRepository.findBySubforumOrderByPostdateDesc(sf);
        List<Subforum> children = subforumRepository.findByParent(sf);
        String breadcrumb = forumService.generateLinkPath(sub);
        model.addAttribute("subforum", sf);
        model.addAttribute("posts", posts);
        model.addAttribute("children", children);
        model.addAttribute("breadcrumb", breadcrumb);
        return "subforum";
    }

    @GetMapping("/loginform")
    public String loginForm(Model model, Authentication auth,
                             @RequestParam(required = false) String error) {
        addCommonAttributes(model, auth);
        model.addAttribute("errors", new ArrayList<>());
        if (error != null) {
            List<String> errors = new ArrayList<>();
            errors.add("Invalid username or password.");
            model.addAttribute("errors", errors);
        }
        return "login";
    }

    @PostMapping("/action_createaccount")
    public String createAccount(@RequestParam String username,
                                 @RequestParam String password,
                                 @RequestParam String email,
                                 Model model, Authentication auth) {
        addCommonAttributes(model, auth);
        List<String> errors = new ArrayList<>();

        if (!forumService.validUsername(username)) {
            errors.add("Username must be 4-40 alphanumeric characters (also allowed: !@#%&).");
        }
        if (!forumService.validPassword(password)) {
            errors.add("Password must be 6-40 alphanumeric characters (also allowed: !@#%&).");
        }
        if (forumService.usernameTaken(username)) {
            errors.add("Username is already taken.");
        }
        if (forumService.emailTaken(email)) {
            errors.add("Email is already registered.");
        }

        if (!errors.isEmpty()) {
            model.addAttribute("errors", errors);
            return "login";
        }

        User user = new User(email, username, password, passwordEncoder);
        userRepository.save(user);
        return "redirect:/loginform";
    }

    @GetMapping("/addpost")
    public String addPostForm(@RequestParam long sub, Model model, Authentication auth) {
        addCommonAttributes(model, auth);
        Optional<Subforum> opt = subforumRepository.findById(sub);
        if (opt.isEmpty()) return "redirect:/";
        model.addAttribute("subforum", opt.get());
        model.addAttribute("errors", new ArrayList<>());
        return "createpost";
    }

    @PostMapping("/action_post")
    public String createPost(@RequestParam long sub,
                              @RequestParam String title,
                              @RequestParam String content,
                              Model model, Authentication auth) {
        addCommonAttributes(model, auth);

        if (auth == null || !auth.isAuthenticated()) {
            return "redirect:/loginform";
        }

        List<String> errors = new ArrayList<>();
        if (!forumService.validTitle(title)) {
            errors.add("Title must be between 5 and 139 characters.");
        }
        if (!forumService.validContent(content)) {
            errors.add("Content must be between 11 and 4999 characters.");
        }

        Optional<Subforum> opt = subforumRepository.findById(sub);
        if (opt.isEmpty()) return "redirect:/";

        if (!errors.isEmpty()) {
            model.addAttribute("subforum", opt.get());
            model.addAttribute("errors", errors);
            return "createpost";
        }

        User user = getCurrentUser(auth);
        Post post = new Post(title, content, user, opt.get());
        postRepository.save(post);
        return "redirect:/subforum?sub=" + sub;
    }

    @GetMapping("/viewpost")
    public String viewPost(@RequestParam long post, Model model, Authentication auth) {
        addCommonAttributes(model, auth);
        Optional<Post> opt = postRepository.findById(post);
        if (opt.isEmpty()) return "redirect:/";
        Post p = opt.get();
        List<Comment> comments = commentRepository.findByPostOrderByPostdateAsc(p);
        String breadcrumb = forumService.generateLinkPath(p.getSubforum().getId());
        Map<Long, String> commentContents = new LinkedHashMap<>();
        for (Comment c : comments) {
            commentContents.put(c.getId(), forumService.renderMarkdown(c.getContent()));
        }
        model.addAttribute("post", p);
        model.addAttribute("postContent", forumService.renderMarkdown(p.getContent()));
        model.addAttribute("comments", comments);
        model.addAttribute("commentContents", commentContents);
        model.addAttribute("embeds", mediaEmbedRepository.findByPostOrderByIdAsc(p));
        model.addAttribute("breadcrumb", breadcrumb);
        model.addAttribute("errors", new ArrayList<>());

        // all reaction counts
        model.addAttribute("likeCount",      forumService.getLikeCount(p));
        model.addAttribute("dislikeCount",   forumService.getDislikeCount(p));
        model.addAttribute("fireCount",      forumService.getFireCount(p));
        model.addAttribute("funnyCount",     forumService.getFunnyCount(p));
        model.addAttribute("sadCount",       forumService.getSadCount(p));
        model.addAttribute("celebrateCount", forumService.getCelebrateCount(p));

        // current user's reaction
        User currentUser = getCurrentUser(auth);
        if (currentUser != null) {
            Reaction userReaction = forumService.getUserReaction(currentUser, p);
            model.addAttribute("userReaction", userReaction != null ? userReaction.getType() : "");
        } else {
            model.addAttribute("userReaction", "");
        }

        return "viewpost";
    }

    @PostMapping("/action_comment")
    public String addComment(@RequestParam long post,
                              @RequestParam String content,
                              Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return "redirect:/loginform";
        }
        Optional<Post> opt = postRepository.findById(post);
        if (opt.isEmpty()) return "redirect:/";
        User user = getCurrentUser(auth);
        Comment comment = new Comment(content, user, opt.get());
        commentRepository.save(comment);
        return "redirect:/viewpost?post=" + post;
    }

    @GetMapping("/action_comment")
    public String addCommentGet(@RequestParam long post) {
        return "redirect:/viewpost?post=" + post;
    }

    @PostMapping("/action_preview")
    @ResponseBody
    public String preview(@RequestBody String raw) {
        return forumService.renderMarkdown(raw);
    }

    @GetMapping("/settings")
    public String settingsPage(Model model, Authentication auth) {
        User user = getCurrentUser(auth);
        if (user == null) return "redirect:/loginform";
        addCommonAttributes(model, auth);
        model.addAttribute("profile", forumService.getUserProfile(user));
        model.addAttribute("errors", new ArrayList<>());
        model.addAttribute("success", "");
        return "settings";
    }

    @PostMapping("/action_update_bio")
    public String updateBio(@RequestParam String bio, Authentication auth) {
        User user = getCurrentUser(auth);
        if (user == null) return "redirect:/loginform";
        forumService.updateBio(user, bio);
        return "redirect:/settings?saved=bio";
    }

    @PostMapping("/action_update_email")
    public String updateEmail(@RequestParam String email, Authentication auth,
                               org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        User user = getCurrentUser(auth);
        if (user == null) return "redirect:/loginform";
        if (!forumService.updateEmail(user, email)) {
            ra.addFlashAttribute("emailError", "Email is already in use or invalid.");
        } else {
            ra.addFlashAttribute("success", "Email updated.");
        }
        return "redirect:/settings";
    }

    @PostMapping("/action_update_password")
    public String updatePassword(@RequestParam String currentPassword,
                                  @RequestParam String newPassword,
                                  Authentication auth,
                                  org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        User user = getCurrentUser(auth);
        if (user == null) return "redirect:/loginform";
        if (!forumService.updatePassword(user, currentPassword, newPassword, passwordEncoder)) {
            ra.addFlashAttribute("passwordError", "Current password is wrong or new password is invalid (6-40 chars, alphanumeric + !@#%&).");
        } else {
            ra.addFlashAttribute("success", "Password updated.");
        }
        return "redirect:/settings";
    }

    @PostMapping("/action_react")
    public String reactToPost(@RequestParam long postId,
                               @RequestParam String type,
                               Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return "redirect:/loginform";
        }
        Optional<Post> opt = postRepository.findById(postId);
        if (opt.isEmpty()) return "redirect:/";

        User user = getCurrentUser(auth);
        forumService.reactToPost(user, opt.get(), type);

        return "redirect:/viewpost?post=" + postId + "&reacted=" + type;
    }

    // ── Delete post ───────────────────────────────────────────────
    @PostMapping("/action_delete_post")
    public String deletePost(@RequestParam long postId, Authentication auth) {
        User user = getCurrentUser(auth);
        if (user == null) return "redirect:/loginform";
        Optional<Post> opt = postRepository.findById(postId);
        if (opt.isEmpty()) return "redirect:/";
        Post post = opt.get();
        boolean isOwner = post.getUser().getUsername().equals(user.getUsername());
        if (!isOwner && !user.isAdmin()) return "redirect:/viewpost?post=" + postId;
        long subId = post.getSubforum().getId();
        forumService.moderatePost(postId);
        return "redirect:/subforum?sub=" + subId;
    }

    // ── Delete comment ────────────────────────────────────────────
    @PostMapping("/action_delete_comment")
    public String deleteComment(@RequestParam long commentId,
                                 @RequestParam long postId,
                                 Authentication auth) {
        User user = getCurrentUser(auth);
        if (user == null) return "redirect:/loginform";
        Optional<Comment> opt = commentRepository.findById(commentId);
        if (opt.isEmpty()) return "redirect:/viewpost?post=" + postId;
        Comment comment = opt.get();
        boolean isOwner = comment.getUser().getUsername().equals(user.getUsername());
        if (!isOwner && !user.isAdmin()) return "redirect:/viewpost?post=" + postId;
        forumService.moderateComment(commentId);
        return "redirect:/viewpost?post=" + postId;
    }

    // ── Edit post form ────────────────────────────────────────────
    @GetMapping("/editpost")
    public String editPostForm(@RequestParam long postId, Model model, Authentication auth) {
        User user = getCurrentUser(auth);
        if (user == null) return "redirect:/loginform";
        Optional<Post> opt = postRepository.findById(postId);
        if (opt.isEmpty()) return "redirect:/";
        Post post = opt.get();
        if (!post.getUser().getUsername().equals(user.getUsername())) return "redirect:/viewpost?post=" + postId;
        addCommonAttributes(model, auth);
        model.addAttribute("post", post);
        return "editpost";
    }

    // ── Save edited post ──────────────────────────────────────────
    @PostMapping("/action_edit_post")
    public String saveEditPost(@RequestParam long postId,
                                @RequestParam String title,
                                @RequestParam String content,
                                Authentication auth) {
        User user = getCurrentUser(auth);
        if (user == null) return "redirect:/loginform";
        forumService.editPost(postId, title, content, user);
        return "redirect:/viewpost?post=" + postId;
    }

    // ── Save edited comment ───────────────────────────────────────
    @PostMapping("/action_edit_comment")
    public String saveEditComment(@RequestParam long commentId,
                                   @RequestParam long postId,
                                   @RequestParam String content,
                                   Authentication auth) {
        User user = getCurrentUser(auth);
        if (user == null) return "redirect:/loginform";
        boolean saved = forumService.editComment(commentId, content, user);
        if (!saved) return "redirect:/viewpost?post=" + postId + "&editError=1";
        return "redirect:/viewpost?post=" + postId;
    }
}