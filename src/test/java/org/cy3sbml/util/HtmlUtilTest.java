package org.cy3sbml.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class HtmlUtilTest {

    @Test
    void escapesHtmlCharacters() {
        assertEquals("&lt;a href=&quot;x&quot;&gt;A &amp; B&lt;/a&gt;", HtmlUtil.escape("<a href=\"x\">A & B</a>"));
    }

    @Test
    void keepsOtherCharacters() {
        String text = "Glucose 6-phosphate, α-D-glucose, it's 5 µM";
        assertEquals(text, HtmlUtil.escape(text));
    }

    @Test
    void nullStaysNull() {
        assertNull(HtmlUtil.escape(null));
    }
}
