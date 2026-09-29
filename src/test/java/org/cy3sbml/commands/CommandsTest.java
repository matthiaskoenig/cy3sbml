package org.cy3sbml.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.cytoscape.work.ServiceProperties;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.Tunable;
import org.junit.jupiter.api.Test;

/** The registry of the commands is complete and documented. */
class CommandsTest {
    private static final List<Commands.Command> COMMANDS = Commands.create(new CommandTestSupport().services());

    @Test
    void everyCommandHasADescriptionAndAnExampleJson() throws Exception {
        Set<String> names = new HashSet<>();
        for (Commands.Command command : COMMANDS) {
            assertTrue(names.add(command.name()), "duplicate command " + command.name());
            assertFalse(command.description().isBlank(), command.name());
            assertTrue(
                    command.longDescription().length() > command.description().length(), command.name());
            assertTrue(CommandTestSupport.parse(command.exampleJson()).isObject(), command.name());
            Properties properties = command.properties();
            assertEquals(Commands.NAMESPACE, properties.getProperty(ServiceProperties.COMMAND_NAMESPACE));
            assertEquals(command.name(), properties.getProperty(ServiceProperties.COMMAND));
            assertEquals("true", properties.getProperty(ServiceProperties.COMMAND_SUPPORTS_JSON));
        }
        assertEquals(10, names.size());
    }

    /**
     * Cytoscape sets the arguments by reflection: the tunable fields and methods of the tasks
     * have to be public members of public classes.
     */
    @Test
    void theArgumentsOfEveryCommandAreAccessible() {
        for (Commands.Command command : COMMANDS) {
            TaskIterator iterator = command.factory().createTaskIterator();
            while (iterator.hasNext()) {
                Class<?> type = iterator.next().getClass();
                boolean tunables = false;
                for (Class<?> c = type; c != null; c = c.getSuperclass()) {
                    for (Field field : c.getDeclaredFields()) {
                        if (field.isAnnotationPresent(Tunable.class)) {
                            tunables = true;
                            assertTrue(Modifier.isPublic(field.getModifiers()), field.toString());
                            assertTrue(Modifier.isPublic(c.getModifiers()), c + " of " + command.name());
                        }
                    }
                    for (Method method : c.getDeclaredMethods()) {
                        if (method.isAnnotationPresent(Tunable.class)) {
                            tunables = true;
                            assertTrue(Modifier.isPublic(method.getModifiers()), method.toString());
                            assertTrue(Modifier.isPublic(c.getModifiers()), c + " of " + command.name());
                        }
                    }
                }
                if (tunables) {
                    assertTrue(Modifier.isPublic(type.getModifiers()), type + " of " + command.name());
                }
            }
        }
    }

    private static final Pattern SNIPPET = Pattern.compile("--8<-- \"(.+)\"");

    /**
     * The fence of an embedded example ({@code --8<-- "<file>"}) is longer than every fence in
     * the file, e.g. the {@code ```bash} of a docstring, which would otherwise end the code
     * block of the page.
     */
    @Test
    void theEmbeddedExamplesAreCodeBlocks() throws Exception {
        List<String> lines = Files.readAllLines(Path.of("docs/guide/automation.md"));
        int snippets = 0;
        for (int i = 1; i < lines.size(); i++) {
            Matcher snippet = SNIPPET.matcher(lines.get(i));
            if (!snippet.matches()) {
                continue;
            }
            snippets++;
            int fence = fenceLength(lines.get(i - 1));
            for (String line : Files.readAllLines(Path.of(snippet.group(1)))) {
                assertTrue(
                        fenceLength(line) < fence,
                        "the fence of " + snippet.group(1) + " is not longer than its line: " + line);
            }
        }
        assertTrue(snippets > 0);
    }

    /** The number of backticks at the start of the (stripped) line. */
    private static int fenceLength(String line) {
        String stripped = line.strip();
        int length = 0;
        while (length < stripped.length() && stripped.charAt(length) == '`') {
            length++;
        }
        return length;
    }

    @Test
    void everyCommandIsDocumented() throws Exception {
        String docs = Files.readString(Path.of("docs/guide/automation.md"));
        for (Commands.Command command : COMMANDS) {
            assertTrue(docs.contains("### `cy3sbml " + command.name() + "`"), "not documented: " + command.name());
        }
    }
}
