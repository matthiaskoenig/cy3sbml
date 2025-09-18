package org.cy3sbml.miriam;


import org.apache.commons.lang.StringUtils;
import org.cy3sbml.TestUtils;
import org.cy3sbml.ols.OLSAccess;
import org.cy3sbml.util.IOUtil;
import org.identifiers.registry.RegistryDatabase;
import org.identifiers.registry.RegistryUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import uk.ac.ebi.pride.utilities.ols.web.service.model.Term;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.cy3sbml.gui.SBaseHTMLFactory.result;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;


/**
 * Testing RegistryUtils.
 */
public class RegistryUtilTest {
    private static final Pattern IDENTIFIERS_ORG_PATTERN =
            Pattern.compile("https?://identifiers\\.org/[^\\s\"'>)]+");
    private static final List<String> checkedNamespaces = new ArrayList<>();
    static Stream<String> resourceFilesProvider() throws Exception {
        // Example resource folders
        HashSet<String> skip = null;
        String filter = null;
        Iterable<Object[]> models1 = TestUtils.findResources("test",TestUtils.BIOMODELS_RESOURCE_PATH,".xml", filter, skip);
        Iterable<Object[]> models2 = TestUtils.findResources("test",TestUtils.BIGGMODELS_RESOURCE_PATH, ".xml", filter, skip);
        Iterable<Object[]> models3 = TestUtils.findResources("test",TestUtils.UNITTESTS_RESOURCE_PATH, ".xml", filter, skip);
        Iterable<Object[]> models4 = TestUtils.findResources("test",TestUtils.SBMLTESTCASES_RESOURCE_PATH, ".xml", filter, skip);
        Iterable<Object[]> models5 = TestUtils.findResources("main","/models", ".xml", filter, skip);

        return Stream.of(models3)   // Stream<Iterable<Object[]>>
                .flatMap(iterable -> StreamSupport.stream(iterable.spliterator(), false)
                        .flatMap(resourceInfo -> {
            String resourcePath = (String) resourceInfo[0];
                            InputStream is;
                            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                                try {
                                    is = new FileInputStream(resourcePath);
                                } catch (FileNotFoundException e) {
                                    throw new RuntimeException(e);
                                }
                            } else {
                                is = TestUtils.class.getResourceAsStream(resourcePath);
                            }
                            String content;
                            try {
                                content = IOUtil.inputStream2String(is);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            Set<String> uris = extractIdentifiersOrgLinks(content);
            return uris.stream();
        }));
    }


    public static Set<String> extractIdentifiersOrgLinks(String text) {
        Set<String> uniqueLinks = new LinkedHashSet<>();  // preserves order, no duplicates
        Matcher matcher = IDENTIFIERS_ORG_PATTERN.matcher(text);
        while (matcher.find()) {
            uniqueLinks.add(matcher.group());
        }
        return uniqueLinks;
    }

    @Test
    public void updateMiriamXML() throws Exception {
        File f = File.createTempFile("test", ".xml");
        assertNotNull(f);
        RegistryUtil.updateMiriamJSON(f);
        assertNotNull(RegistryUtil.getMiriamContent());
    }


    @Test
    public void loadRegistry() {
        assertNotNull(RegistryUtil.getMiriamContent());
    }

    @ParameterizedTest
    @MethodSource("resourceFilesProvider")
    public void testResourceUriProcessing(String resourceURI) throws IOException {


            String[] tokens = resourceURI.split("/");
            //created a checkednamespaces list so the code skips the namespaces it already checked before (instead of checking for thousands of identifiers links)
            String checkedNamespace;
            if (tokens[3].contains(":")){
                checkedNamespace = tokens[3].split(":")[0];
            } else {
                checkedNamespace = tokens[3];
            }
            // added brackets around the names in case some namespaces contain others e.g. "go" and may be there is a namespace such as "goxyz"
            if (!checkedNamespaces.contains("[" + checkedNamespace + "]")) {
                checkedNamespaces.add("[" + checkedNamespace + "]");

                //need to eliminate the invalid identifiers links
                URL url = new URL(resourceURI);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                // Use HEAD for lightweight check or GET if HEAD is not supported by server
                connection.setRequestMethod("HEAD");
                connection.setConnectTimeout(6000);  // timeout in ms
                connection.setReadTimeout(6000);

                int responseCode = connection.getResponseCode();
                if  (responseCode >= 200 && responseCode < 400) {
                    String identifier = getCompactId(tokens);
                    String prefix = StringUtils.substringBefore(identifier, ":").toLowerCase();

                    if (result.get(prefix) == null) { //if the prefix is not in the compact ID, it must be in the previous token
                        prefix = tokens[3].toLowerCase();
                    }

                    assertNotNull(result.get(prefix));
                    Namespace dataType = (result.get(prefix) == null)
                            ? result.get(StringUtils.substringAfter(prefix, "."))
                            : result.get(prefix);
                    assertNotNull(dataType);

                }
        }else {
                assumeTrue(checkedNamespaces.contains("[" + checkedNamespace + "]"), "Namespace already checked, skipping");
            }
    }
    public String getCompactId(String[] tokens){

        String identifier;
        if (tokens[tokens.length - 1].contains(":")){ //format : identifiers.org/[namespace prefix]:[accession]
            identifier = tokens[tokens.length - 1];
        } else if (tokens[tokens.length - 1].contains("[!\"#$%&'()*+,\\-./;<=>?@[\\\\\\]^_`{|}~]")){
            //format : sometimes the prefix and the accession are not separated by a column
            identifier = tokens[tokens.length - 1].replace("[!\"#$%&'()*+,\\-./;<=>?@[\\\\\\]^_`{|}~]",":");
        }
        else { // otherwise the compact id is the last two tokens separated by a column
            //
            identifier = tokens[tokens.length - 2]+":"+tokens[tokens.length - 1];
        }

        identifier = identifier.toUpperCase();
        return identifier;
    }

    }
