package org.cy3sbml.gui;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class RenderCoalescerTest {
    @Test
    void acceptsTheFirstTarget() {
        var coalescer = new RenderCoalescer();
        assertTrue(coalescer.accept("a"));
    }

    @Test
    void rejectsARepeatOfTheSameReference() {
        var coalescer = new RenderCoalescer();
        Object target = new Object();
        assertTrue(coalescer.accept(target));
        assertFalse(coalescer.accept(target));
        assertFalse(coalescer.accept(target));
    }

    @Test
    void acceptsADifferentReferenceEvenIfEqual() {
        var coalescer = new RenderCoalescer();
        // two distinct String instances that are .equals() but not the same reference
        String first = new StringBuilder("x").toString();
        String second = new StringBuilder("x").toString();
        assertTrue(coalescer.accept(first));
        assertTrue(coalescer.accept(second));
    }

    @Test
    void acceptsAgainAfterReset() {
        var coalescer = new RenderCoalescer();
        Object target = new Object();
        assertTrue(coalescer.accept(target));
        coalescer.reset();
        assertTrue(coalescer.accept(target));
    }
}
