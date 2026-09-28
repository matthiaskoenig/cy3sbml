package org.cy3sbml;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Drops the log messages the tests cause on purpose (logback-test.xml), so the test output
 * shows only unexpected warnings and errors. A message is dropped if it matches one of the
 * configured regular expressions in full.
 */
public class ExpectedMessageFilter extends Filter<ILoggingEvent> {
    private final List<Pattern> messages = new ArrayList<>();

    /** Called by logback for every {@code <message>} element. */
    public void addMessage(String regex) {
        messages.add(Pattern.compile(regex.strip()));
    }

    @Override
    public FilterReply decide(ILoggingEvent event) {
        String message = event.getFormattedMessage();
        for (Pattern pattern : messages) {
            if (pattern.matcher(message).matches()) {
                return FilterReply.DENY;
            }
        }
        return FilterReply.NEUTRAL;
    }
}
