package com.monikoshop.seo;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeoHtmlTest {

    @Test
    void escapesHtmlSpecialCharacters() {
        assertEquals("&lt;script&gt;alert(&quot;x&quot;)&lt;/script&gt; &amp; &#39;",
                SeoHtml.esc("<script>alert(\"x\")</script> & '"));
        assertEquals("", SeoHtml.esc(null));
    }

    @Test
    void jsonInScriptCannotCloseTheTag() {
        String safe = SeoHtml.safeJsonForScript("{\"a\":\"</script><script>alert(1)</script>\"}");
        assertFalse(safe.contains("</script>"));
    }

    @Test
    void pageEscapesUserContentAndHasCanonicalAndOgTags() {
        String html = SeoHtml.page("مونا بیوتی", "کرم <b>تست</b>", "توضیح \"ویژه\"", "https://moniko.ir/products/1",
                "https://api.moniko.ir/api/products/1/images/2", "product", "{\"@type\":\"Product\"}", "کرم", "<p>بدنه</p>");
        assertTrue(html.contains("<link rel=\"canonical\" href=\"https://moniko.ir/products/1\">"));
        assertTrue(html.contains("property=\"og:image\""));
        assertTrue(html.contains("application/ld+json"));
        assertTrue(html.contains("&lt;b&gt;تست&lt;/b&gt;"));
        assertFalse(html.contains("<b>تست</b>"));
    }

    @Test
    void sitemapListsEscapedUrls() {
        String xml = SeoHtml.sitemap(List.of("https://moniko.ir/", "https://moniko.ir/search?a=1&b=2"));
        assertTrue(xml.contains("<loc>https://moniko.ir/</loc>"));
        assertTrue(xml.contains("a=1&amp;b=2"));
    }

    @Test
    void linkListEscapesText() {
        String html = SeoHtml.linkList(List.of(new SeoHtml.Link("https://x/y?a=1&b=2", "<i>نام</i>")));
        assertTrue(html.contains("&lt;i&gt;نام&lt;/i&gt;"));
        assertTrue(html.contains("a=1&amp;b=2"));
    }
}
