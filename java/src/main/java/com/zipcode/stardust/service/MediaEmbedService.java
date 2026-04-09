package com.zipcode.stardust.service;

import com.zipcode.stardust.model.MediaEmbed;
import com.zipcode.stardust.model.MediaEmbed.MediaType;
import com.zipcode.stardust.model.Post;
import com.zipcode.stardust.repository.MediaEmbedRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * =============================================================
 *  MediaEmbedService — business logic for media embedding.
 *
 *  KEY CONCEPTS FOR LEARNERS
 *  --------------------------
 *  @Service   → marks this as a Spring-managed "service" bean.
 *               Spring creates one instance and shares it across
 *               the whole app (singleton by default).
 *
 *  @Autowired → Spring automatically injects (provides) the
 *               MediaEmbedRepository; you don't call "new".
 *
 *  Why a Service layer?
 *    Controllers should only handle HTTP (requests/responses).
 *    Repositories should only talk to the database.
 *    Services sit in the middle and hold the business rules —
 *    validation, detection logic, saving — keeping each layer
 *    focused on one job (the "Single Responsibility Principle").
 * =============================================================
 */
@Service
public class MediaEmbedService {
 
    @Autowired
    private MediaEmbedRepository mediaEmbedRepository;
 
    // ── URL validation ────────────────────────────────────────
 
    /**
     * Checks that a URL is non-blank and starts with http:// or
     * https://. We reject everything else to block javascript:
     * and other dangerous schemes.
     *
     * @param url the URL string to check
     * @return true if the URL looks safe and well-formed
     */
    public boolean isValidUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        String lower = url.strip().toLowerCase();
        // Only allow http and https — block javascript:, data:, etc.
        return lower.startsWith("http://") || lower.startsWith("https://");
    }
 
    // ── Media-type detection ──────────────────────────────────
 
    /**
     * Detects whether a URL points to an IMAGE or a VIDEO.
     *
     * How it works:
     *   1. Strip query parameters (?foo=bar) so ".jpg?size=large"
     *      is still recognised as an image.
     *   2. Check the file extension against known image extensions.
     *   3. Check for known video-hosting domains.
     *   4. Default to IMAGE if we can't tell (handles imgur, etc.).
     *
     * @param url the URL to inspect
     * @return IMAGE or VIDEO
     */
    public MediaType detectMediaType(String url) {
        if (url == null) return MediaType.IMAGE;
 
        // Remove query string for extension matching
        String path = url.split("\\?")[0].toLowerCase();
 
        // Common image file extensions
        if (path.endsWith(".jpg")  || path.endsWith(".jpeg") ||
            path.endsWith(".png")  || path.endsWith(".gif")  ||
            path.endsWith(".webp") || path.endsWith(".svg")  ||
            path.endsWith(".bmp")) {
            return MediaType.IMAGE;
        }
 
        // Common video file extensions
        if (path.endsWith(".mp4") || path.endsWith(".webm") ||
            path.endsWith(".ogg") || path.endsWith(".mov")) {
            return MediaType.VIDEO;
        }
 
        // Known video hosting domains
        String lower = url.toLowerCase();
        if (lower.contains("youtube.com") || lower.contains("youtu.be") ||
            lower.contains("vimeo.com")   || lower.contains("twitch.tv")) {
            return MediaType.VIDEO;
        }
 
        // When in doubt, treat it as an image
        return MediaType.IMAGE;
    }
 
    // ── Caption validation ────────────────────────────────────
 
    /**
     * Captions are optional but must not exceed 300 characters
     * if provided. Returns true when the caption is acceptable.
     *
     * @param caption the caption string (may be null or blank)
     * @return true if valid
     */
    public boolean isValidCaption(String caption) {
        if (caption == null || caption.isBlank()) {
            return true;   // captions are optional
        }
        return caption.length() <= 300;
    }
 
    // ── CRUD operations ───────────────────────────────────────
 
    /**
     * Saves one MediaEmbed to the database.
     * The repository's save() method handles both INSERT (new
     * record) and UPDATE (existing record with same id).
     *
     * @param embed the embed to persist
     * @return the saved embed (id is now populated)
     */
    public MediaEmbed save(MediaEmbed embed) {
        return mediaEmbedRepository.save(embed);
    }
 
    /**
     * Returns all embeds attached to a post, in insertion order.
     *
     * @param post the parent post
     * @return list of MediaEmbed objects (may be empty)
     */
    public List<MediaEmbed> getEmbedsForPost(Post post) {
        return mediaEmbedRepository.findByPostOrderByIdAsc(post);
    }
 
    /**
     * Deletes a single embed by its id.
     * deleteById() does nothing if the id doesn't exist,
     * so this is safe to call even with stale ids.
     *
     * @param embedId the id of the embed to remove
     */
    public void deleteEmbed(Long embedId) {
        mediaEmbedRepository.deleteById(embedId);
    }
}

