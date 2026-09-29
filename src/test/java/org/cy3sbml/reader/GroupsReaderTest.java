package org.cy3sbml.reader;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import java.util.stream.Collectors;
import org.cy3sbml.SBML;
import org.cytoscape.group.CyGroup;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.junit.jupiter.api.Test;

class GroupsReaderTest {

    @Test
    void createsGroupsOfMemberNodes() throws Exception {
        ConversionContext context = ReaderTestSupport.read("groups_01.xml", new CoreReader(), new GroupsReader());
        CyNetwork network = context.network();
        assertEquals(39, context.groups().size());

        // <groups:group groups:name="Methionine Salvage" groups:id="g1" groups:kind="partonomy">
        //   members R_UNK3, R_ARD, R_ENOPH, R_MDRPD, R_MTRI
        CyNode groupNode = ReaderTestSupport.nodeById(context, "g1");
        CyRootNetwork rootNetwork = ((CySubNetwork) network).getRootNetwork();
        assertEquals(SBML.NODETYPE_GROUP, rootNetwork.getRow(groupNode).get(SBML.NODETYPE_ATTR, String.class));
        assertEquals("Methionine Salvage", rootNetwork.getRow(groupNode).get(SBML.ATTR_NAME, String.class));

        CyGroup group = context.groups().values().stream()
                .filter(g -> g.getGroupNode().equals(groupNode))
                .findFirst()
                .orElseThrow();
        Set<String> memberIds = group.getNodeList().stream()
                .map(n -> network.getRow(n).get(SBML.ATTR_ID, String.class))
                .collect(Collectors.toSet());
        assertEquals(Set.of("R_UNK3", "R_ARD", "R_ENOPH", "R_MDRPD", "R_MTRI"), memberIds);
    }
}
