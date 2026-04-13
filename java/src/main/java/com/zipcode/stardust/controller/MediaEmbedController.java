package com.zipcode.stardust.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.zipcode.stardust.model.MediaEmbed;
import com.zipcode.stardust.model.MediaEmbed.MediaType;
import com.zipcode.stardust.model.Post;
import com.zipcode.stardust.model.User;
import com.zipcode.stardust.repository.PostRepository;
import com.zipcode.stardust.service.MediaEmbedService;

/**
 * =============================================================
 *  MediaEmbedController — handles HTTP requests related to
 *  adding and deleting media embeds on posts.
 *
 *  KEY CONCEPTS FOR LEARNERS
 *  --------------------------
 *  @Controller    → Spring registers this as a web controller.
 *                   Methods return view names or "redirect:" URLs.
 *
 *  @PostMapping   → only handles HTTP POST requests (form submit).
 *  @RequestParam  → pulls a value out of the request form/URL.
 *
 *  RedirectAttributes → lets us pass a one-time "flash" message
 *                        through a redirect so the user sees
 *                        feedback on the next page.
 *
 *  Why a separate controller?
 *    Keeping media logic out of ForumController means each file
 *    stays focused and easier to read — good team practice.
 * =============================================================
 */
@Controller
public class MediaEmbedController {
 
    @Autowired
    private PostRepository postRepository;
 
    @Autowired
    private MediaEmbedService mediaEmbedService;
 
    // ── Helper ────────────────────────────────────────────────
 
    /**
     * Returns the logged-in User, or null if nobody is logged in.
     * We check for "anonymousUser" because Spring Security sets
     * that string as the principal when no real user is present.
     */
    private User getCurrentUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() ||
                "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return (User) auth.getPrincipal();
    }
 
    // ── Add embed ─────────────────────────────────────────────
 
    /**
     * Handles the "Add Media" form that lives on the viewpost page.
     *
     * Flow:
     *   1. Check the user is logged in.
     *   2. Find the Post by id — redirect home if not found.
     *   3. Validate the URL.
     *   4. Auto-detect IMAGE vs VIDEO (or use what user chose).
     *   5. Validate the caption.
     *   6. Build a MediaEmbed and save it.
     *   7. Redirect back to the post.
     *
     * @param postId            id of the post (from hidden form field)
     * @param url               the media URL the user typed
     * @param mediaTypeOverride optional: "IMAGE" or "VIDEO" from a dropdown
     * @param caption           optional caption text
     * @param auth              Spring Security fills this in automatically
     * @param redirectAttrs     used to send a one-time message through redirect
     */
    @PostMapping("/action_add_embed")
    public String addEmbed(
            @RequestParam("post")               long postId,
            @RequestParam("url")                String url,
            @RequestParam(value = "mediaType",
                          required = false)     String mediaTypeOverride,
            @RequestParam(value = "caption",
                          defaultValue = "")    String caption,
            Authentication auth,
            RedirectAttributes redirectAttrs) {
 
        // 1. Must be logged in
        if (getCurrentUser(auth) == null) {
            return "redirect:/loginform";
        }
 
        // 2. Post must exist
        Optional<Post> optPost = postRepository.findById(postId);
        if (optPost.isEmpty()) {
            return "redirect:/";
        }
        Post post = optPost.get();
 
        // 3. Validate URL
        if (!mediaEmbedService.isValidUrl(url)) {
            redirectAttrs.addFlashAttribute("embedError",
                    "Please enter a valid URL starting with http:// or https://");
            return "redirect:/viewpost?post=" + postId;
        }
 
        // 4. Determine media type
        //    If the user explicitly chose one from the dropdown, use it.
        //    Otherwise, let the service auto-detect from the URL.
        MediaType mediaType;
        if (mediaTypeOverride != null && !mediaTypeOverride.isBlank()) {
            try {
                mediaType = MediaType.valueOf(mediaTypeOverride.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Safety net: valueOf() throws if the string doesn't match an enum name
                mediaType = mediaEmbedService.detectMediaType(url);
            }
        } else {
            mediaType = mediaEmbedService.detectMediaType(url);
        }
 
        // 5. Validate caption
        if (!mediaEmbedService.isValidCaption(caption)) {
            redirectAttrs.addFlashAttribute("embedError",
                    "Caption must be 300 characters or fewer.");
            return "redirect:/viewpost?post=" + postId;
        }
 
        // 6. Build and save
        //    Trim the URL to remove accidental leading/trailing spaces.
        MediaEmbed embed = new MediaEmbed(url.strip(), mediaType, caption.strip(), post);
        mediaEmbedService.save(embed);
 
        // 7. Flash a success message and go back to the post
        redirectAttrs.addFlashAttribute("embedSuccess", "Media added!");
        return "redirect:/viewpost?post=" + postId;
    }
 
    // ── Delete embed ──────────────────────────────────────────
 
    /**
     * Lets the original post author (or an admin) remove an embed.
     *
     * Security check: we compare the logged-in user's username to
     * the post author's username.  Admins (isAdmin()) may also delete.
     *
     * @param embedId    id of the embed to delete
     * @param postId     id of the post (so we can redirect back)
     * @param auth       injected by Spring Security
     * @param redirectAttrs flash message carrier
     */
    @PostMapping("/action_delete_embed")
    public String deleteEmbed(
            @RequestParam("embedId") long embedId,
            @RequestParam("post")    long postId,
            Authentication auth,
            RedirectAttributes redirectAttrs) {
 
        User currentUser = getCurrentUser(auth);
        if (currentUser == null) {
            return "redirect:/loginform";
        }
 
        // Find the post so we can check ownership
        Optional<Post> optPost = postRepository.findById(postId);
        if (optPost.isEmpty()) {
            return "redirect:/";
        }
        Post post = optPost.get();
 
        // Allow deletion only if the user owns the post or is an admin
        boolean isOwner = post.getUser().getUsername()
                              .equals(currentUser.getUsername());
        boolean isAdmin = currentUser.isAdmin();
 
        if (!isOwner && !isAdmin) {
            redirectAttrs.addFlashAttribute("embedError",
                    "You don't have permission to remove that media.");
            return "redirect:/viewpost?post=" + postId;
        }
 
        mediaEmbedService.deleteEmbed(embedId);
        redirectAttrs.addFlashAttribute("embedSuccess", "Media removed.");
        return "redirect:/viewpost?post=" + postId;
    }
}