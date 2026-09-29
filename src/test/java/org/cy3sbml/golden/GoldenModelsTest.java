package org.cy3sbml.golden;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.cy3sbml.TestUtils;
import org.cy3sbml.archive.ArchiveDirectories;
import org.cy3sbml.archive.CombineArchiveReaderTask;
import org.cy3sbml.reader.SBMLReaderTask;
import org.cytoscape.group.GroupTestSupport;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.NetworkTestSupport;
import org.cytoscape.work.TaskMonitor;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Golden snapshot tests of the networks the SBML reader creates for reference models.
 * <p>
 * Every model in {@link #MODELS} is imported with the {@code SBMLReaderTask} and the
 * resulting networks are compared with the snapshot in
 * {@code src/test/resources/golden/<model path with '/' replaced by '__'>.json}.
 * The snapshot format and the excluded columns ({@code SUID}, {@code selected}) are
 * described in {@link NetworkSnapshot}.
 * <p>
 * Models left out on purpose: {@code unittests/groups_01.xml} (snapshot above 6 MB, groups
 * are covered by {@code unittests/groups_02.xml} and {@code unittests/fbc_01.xml}). COMBINE archives are read with the archive
 * reader ({@link #importedArchivesMatchSnapshot}).
 * <p>
 * Regenerate the snapshots after an intended change of the import with
 * <pre>
 * ./mvnw -B -q test -Dtest=GoldenModelsTest -Dgolden.update=true
 * </pre>
 * and review the diff of {@code src/test/resources/golden} before committing it.
 */
public class GoldenModelsTest {

    private static final String MODELS_ROOT = "/models/";
    private static final Path GOLDEN_DIR = Path.of("src", "test", "resources", "golden");
    private static final boolean UPDATE = Boolean.getBoolean("golden.update");

    /** Model resources below {@code /models/} that are pinned by a snapshot. */
    public static final List<String> MODELS = List.of(
            // unittests: all
            "unittests/01134-sbml-l3v1.xml",
            "unittests/comp_01.xml",
            "unittests/core_01.xml",
            "unittests/fbc_01.xml",
            "unittests/galactose.xml",
            "unittests/groups_02.xml",
            "unittests/layout_01.xml",
            "unittests/qual_01.xml",
            "unittests/small_population.xml",
            "unittests/toy_fba.xml",
            "unittests/toy_ode_bounds.xml",
            "unittests/toy_ode_model.xml",
            "unittests/toy_ode_update.xml",
            "unittests/toy_top_level.xml",
            "unittests/yeast_glycolysis.xml",
            // comp: models with submodels, replacements and deletions
            "comp/Watanabe2014/test_replacement_9.xml",
            "comp/Watanabe2014/test_replacement_1.xml",
            "comp/Watanabe2014/test_replacement_4.xml",
            // fbc: v1 and v2
            "fbc/JSBML_testcase_L3V1_fbcV1.xml",
            "fbc/Mini_textbook_L3V1_fbcV2.xml",
            // qual
            "qual/BMID000000017713_L3V1_qualV1_layoutV1.xml",
            "qual/sce04070_L3V1_qualV1_layoutV1.xml",
            // layout
            "layout/hsa00450_L3V1_layoutV1.xml",
            "layout/sce03040_L3V1_qualV1_layoutV1.xml",
            // distrib: uncertainties, distribution functions in math
            "distrib/distrib_uncertainties.xml",
            "sbml-test-suite/stochastic/00091/00091-sbml-l3v2.xml",
            // koenig
            "koenig/Koenig_demo_v02.xml",
            "koenig/van_der_pol.xml",
            // biomodels: rules, function definitions, events
            "biomodels/BIOMD0000000006.xml",
            "biomodels/BIOMD0000000024.xml",
            "biomodels/BIOMD0000000040.xml",
            "biomodels/BIOMD0000000079.xml",
            "biomodels/BIOMD0000000141.xml",
            // bigg
            "bigg_models/e_coli_core.xml",
            // sbml-test-suite: L1, L2 (events, algebraic rule), L3 (initial assignments, delays)
            "sbml-test-suite/semantic/00032/00032-sbml-l1v2.xml",
            "sbml-test-suite/semantic/00026/00026-sbml-l2v4.xml",
            "sbml-test-suite/semantic/00039/00039-sbml-l2v5.xml",
            "sbml-test-suite/semantic/00027/00027-sbml-l3v1.xml",
            "sbml-test-suite/semantic/00072/00072-sbml-l3v2.xml");

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @ParameterizedTest(name = "{0}")
    @FieldSource("MODELS")
    void importedNetworksMatchSnapshot(String model) throws Exception {
        CyNetwork[] networks = TestUtils.readNetwork(MODELS_ROOT + model);
        assertSnapshot(model, networks);
    }

    /** The networks of the SBML models of a COMBINE archive, read by the archive reader. */
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"omex/single.omex", "omex/BIOMD0000000012.omex"})
    void importedArchivesMatchSnapshot(String archive, @TempDir Path directory) throws Exception {
        String fileName = archive.substring(archive.lastIndexOf('/') + 1);
        try (InputStream stream = GoldenModelsTest.class.getResourceAsStream(MODELS_ROOT + archive)) {
            CombineArchiveReaderTask task = new CombineArchiveReaderTask(
                    stream,
                    fileName,
                    new ArchiveDirectories(directory),
                    (in, name, location) -> new SBMLReaderTask(
                            in,
                            name,
                            location,
                            new NetworkTestSupport().getNetworkFactory(),
                            new GroupTestSupport().getGroupFactory()),
                    null);
            task.run(mock(TaskMonitor.class));
            assertSnapshot(archive, task.getNetworks());
        }
    }

    private static void assertSnapshot(String model, CyNetwork[] networks) throws IOException {
        ObjectNode actual = NetworkSnapshot.of(networks);
        Path file = GOLDEN_DIR.resolve(model.replace("/", "__").replaceAll("\\.[^.]*$", "") + ".json");

        if (UPDATE) {
            write(file, actual);
            return;
        }
        assertTrue(Files.exists(file), "Missing snapshot, create it with -Dgolden.update=true: " + file);
        JsonNode expected = read(file);
        assertEquals(expected.toPrettyString(), actual.toPrettyString(), "Snapshot differs: " + file);
    }

    private static void write(Path file, JsonNode snapshot) throws IOException {
        DefaultPrettyPrinter printer = new DefaultPrettyPrinter().withObjectIndenter(new DefaultIndenter("  ", "\n"));
        printer = printer.withArrayIndenter(new DefaultIndenter("  ", "\n"));
        Files.createDirectories(file.getParent());
        String json = MAPPER.writer(printer).writeValueAsString(snapshot) + "\n";
        Files.writeString(file, json, StandardCharsets.UTF_8);
    }

    private static JsonNode read(Path file) throws IOException {
        String json = Files.readString(file, StandardCharsets.UTF_8).replace("\r\n", "\n");
        return MAPPER.readTree(json);
    }
}
