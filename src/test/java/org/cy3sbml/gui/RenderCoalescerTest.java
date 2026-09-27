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
        // two distinct instances that are .equals() (a record's generated equals compares
        // components) but not the same reference
        assertTrue(coalescer.accept(new Value("x")));
        assertTrue(coalescer.accept(new Value("x")));
    }

    private record Value(String v) {}

    @Test
    void acceptsAgainAfterReset() {
        var coalescer = new RenderCoalescer();
        Object target = new Object();
        assertTrue(coalescer.accept(target));
        coalescer.reset();
        assertTrue(coalescer.accept(target));
    }
}
