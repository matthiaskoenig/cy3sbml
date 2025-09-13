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


/**
 * Testing RegistryUtils.
 */
public class RegistryUtilTest {
    private static final Pattern IDENTIFIERS_ORG_PATTERN =
            Pattern.compile("https?://identifiers\\.org/[^\\s\"'>)]+");

    static Stream<String> resourceFilesProvider() throws Exception {
        // Example resource folders
        HashSet<String> skip = null;
        String filter = null;
        Iterable<Object[]> models1 = TestUtils.findResources(TestUtils.BIOMODELS_RESOURCE_PATH,".xml", filter, skip);
        Iterable<Object[]> models2 = TestUtils.findResources(TestUtils.BIGGMODELS_RESOURCE_PATH, ".xml", filter, skip);
        Iterable<Object[]> models3 = TestUtils.findResources(TestUtils.UNITTESTS_RESOURCE_PATH, ".xml", filter, skip);
        Iterable<Object[]> models4 = TestUtils.findResources(TestUtils.SBMLTESTCASES_RESOURCE_PATH, ".xml", filter, skip);
        return Stream.of(models1)   // Stream<Iterable<Object[]>>
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
                            }            System.out.println(StringUtils.substringAfter(resourcePath, "test"));
                            String content = null;
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
    public void testResourceUriProcessing(String resourceURI) {
        System.out.println("Testing resource URI: " + resourceURI);
        String[] tokens = resourceURI.split("/");
        String identifier = getCompactId(tokens);
        System.out.println("identifier: " + identifier);
        String dataCollection = RegistryUtilities.getDataCollectionPartFromURI(resourceURI);
        System.out.println("dataCollection: " + dataCollection);
        String prefix = StringUtils.substringBefore(identifier, ":").toLowerCase();
        System.out.println("prefix: " + prefix);

        if (result.get(prefix)==null){
            prefix = tokens[tokens.length-2];
        }


        assertNotNull(result.get(prefix));
        Namespace dataType = (result.get(prefix) == null)
                ? result.get(StringUtils.substringAfter(prefix, "."))
                : result.get(prefix);
        assertNotNull(dataType);
    }
    public String getCompactId(String[] tokens){

        String identifier;
        if (tokens[tokens.length - 1].contains(":")){ //format : identifiers.org/[namespace prefix]:[accession]
            identifier = tokens[tokens.length - 1];
        }
        else {
            identifier = tokens[tokens.length - 2]+":"+tokens[tokens.length - 1];
        }
        identifier = identifier.toUpperCase();
        return identifier;
    }

    }
