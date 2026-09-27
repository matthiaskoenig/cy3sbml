package org.cy3sbml.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class PageLoaderTest {

    /** Records every page request with the thread that delivered it. */
    private static final class RecordingTarget implements PageLoader.Target {
        final List<String> pages = Collections.synchronizedList(new ArrayList<>());
        final List<Thread> threads = Collections.synchronizedList(new ArrayList<>());

        @Override
        public void loadText(String text) {
            pages.add("text:" + text);
            threads.add(Thread.currentThread());
        }

        @Override
        public void loadPageFromResource(String resource) {
            pages.add("resource:" + resource);
            threads.add(Thread.currentThread());
        }
    }

    @Test
    void textAndResourcesReachTheTargetInRequestOrder() {
        PageLoader loader = new PageLoader();
        RecordingTarget target = new RecordingTarget();
        loader.attach(target);

        loader.loadText("A");
        loader.loadPageFromResource("help.html");
        loader.loadText("B");
        loader.loadPageFromResource("examples.html");

        assertEquals(List.of("text:A", "resource:help.html", "text:B", "resource:examples.html"), target.pages);
    }

    @Test
    void requestsAreDeliveredOnTheCallingThread() throws InterruptedException {
        // text and resources take the same path to the browser (no hop over another
        // thread for either), so the order of the requests is the order of the pages
        PageLoader loader = new PageLoader();
        RecordingTarget target = new RecordingTarget();
        loader.attach(target);

        Thread renderThread = new Thread(() -> {
            loader.loadText("A");
            loader.loadPageFromResource("help.html");
        });
        renderThread.start();
        renderThread.join();

        assertEquals(List.of("text:A", "resource:help.html"), target.pages);
        assertEquals(List.of(renderThread, renderThread), target.threads);
    }

    @Test
    void requestsBeforeAttachAreHeldAndOnlyTheLatestIsShown() {
        PageLoader loader = new PageLoader();
        RecordingTarget target = new RecordingTarget();

        loader.loadPageFromResource("help.html");
        loader.loadText("A");

        assertTrue(target.pages.isEmpty());
        loader.attach(target);
        assertEquals(List.of("text:A"), target.pages);

        loader.loadPageFromResource("help.html");
        assertEquals(List.of("text:A", "resource:help.html"), target.pages);
    }

    @Test
    void attachWithoutRequestsShowsNothing() {
        PageLoader loader = new PageLoader();
        RecordingTarget target = new RecordingTarget();
        loader.attach(target);
        assertTrue(target.pages.isEmpty());
    }
}
