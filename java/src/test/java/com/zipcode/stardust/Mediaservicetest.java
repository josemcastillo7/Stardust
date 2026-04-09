package com.zipcode.stardust;

import com.zipcode.stardust.model.MediaEmbed.MediaType;
import com.zipcode.stardust.service.MediaEmbedService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * =============================================================
 *  MediaEmbedServiceTest — unit tests for MediaEmbedService.
 *
 *  KEY CONCEPTS FOR LEARNERS
 *  --------------------------
 *  Unit tests check a single class in isolation — no Spring
 *  context, no database, no network.  They run extremely fast.
 *
 *  @Test        → marks a method as a test case.
 *  @BeforeEach  → runs before EACH @Test method to set up state.
 *
 *  assertEquals(expected, actual)
 *    → fails the test if the two values are not equal.
 *  assertTrue(condition)
 *    → fails if condition is false.
 *  assertFalse(condition)
 *    → fails if condition is true.
 *
 *  We create MediaEmbedService directly with "new" here because
 *  we don't need Spring to inject anything — the methods we're
 *  testing only use their own logic.
 * =============================================================
 */
class MediaEmbedServiceTest {
 
    // The object we are testing
    private MediaEmbedService service;
 
    @BeforeEach
    void setUp() {
        // Create a fresh service before every test
        service = new MediaEmbedService();
    }
 
    // ── isValidUrl tests ──────────────────────────────────────
 
    @Test
    void validUrl_httpAccepted() {
        assertTrue(service.isValidUrl("http://example.com/image.jpg"));
    }
 
    @Test
    void validUrl_httpsAccepted() {
        assertTrue(service.isValidUrl("https://i.imgur.com/abc.png"));
    }
 
    @Test
    void validUrl_nullRejected() {
        assertFalse(service.isValidUrl(null));
    }
 
    @Test
    void validUrl_blankRejected() {
        assertFalse(service.isValidUrl("   "));
    }
 
    @Test
    void validUrl_javascriptSchemeRejected() {
        // Security: javascript: URLs must never be accepted
        assertFalse(service.isValidUrl("javascript:alert('xss')"));
    }
 
    @Test
    void validUrl_dataSchemeRejected() {
        assertFalse(service.isValidUrl("data:text/html,<h1>bad</h1>"));
    }
 
    // ── detectMediaType tests ─────────────────────────────────
 
    @Test
    void detectMediaType_jpgIsImage() {
        assertEquals(MediaType.IMAGE,
                service.detectMediaType("https://example.com/photo.jpg"));
    }
 
    @Test
    void detectMediaType_pngIsImage() {
        assertEquals(MediaType.IMAGE,
                service.detectMediaType("https://cdn.example.com/logo.png"));
    }
 
    @Test
    void detectMediaType_gifIsImage() {
        assertEquals(MediaType.IMAGE,
                service.detectMediaType("https://example.com/anim.gif"));
    }
 
    @Test
    void detectMediaType_mp4IsVideo() {
        assertEquals(MediaType.VIDEO,
                service.detectMediaType("https://example.com/clip.mp4"));
    }
 
    @Test
    void detectMediaType_webmIsVideo() {
        assertEquals(MediaType.VIDEO,
                service.detectMediaType("https://example.com/clip.webm"));
    }
 
    @Test
    void detectMediaType_youtubeIsVideo() {
        assertEquals(MediaType.VIDEO,
                service.detectMediaType("https://www.youtube.com/watch?v=dQw4w9WgXcQ"));
    }
 
    @Test
    void detectMediaType_youtubeShortIsVideo() {
        assertEquals(MediaType.VIDEO,
                service.detectMediaType("https://youtu.be/dQw4w9WgXcQ"));
    }
 
    @Test
    void detectMediaType_vimeoIsVideo() {
        assertEquals(MediaType.VIDEO,
                service.detectMediaType("https://vimeo.com/123456789"));
    }
 
    @Test
    void detectMediaType_unknownDefaultsToImage() {
        // An imgur link without extension — defaults to IMAGE
        assertEquals(MediaType.IMAGE,
                service.detectMediaType("https://imgur.com/gallery/abc"));
    }
 
    @Test
    void detectMediaType_queryParamStripped() {
        // .jpg?size=large should still be detected as IMAGE
        assertEquals(MediaType.IMAGE,
                service.detectMediaType("https://example.com/photo.jpg?size=large"));
    }

    // ── isValidCaption tests ──────────────────────────────────

    @Test
    void validCaption_nullIsOk() {
        assertTrue(service.isValidCaption(null));
    }

    @Test
    void validCaption_blankIsOk() {
        assertTrue(service.isValidCaption("   "));
    }

    @Test
    void validCaption_shortTextIsOk() {
        assertTrue(service.isValidCaption("My cute cat"));
    }

    @Test
    void validCaption_exactly300CharsIsOk() {
        String caption = "a".repeat(300);
        assertTrue(service.isValidCaption(caption));
    }

    @Test
    void validCaption_301CharsIsRejected() {
        String caption = "a".repeat(301);
        assertFalse(service.isValidCaption(caption));
    }
}