package org.cy3sbml.reader;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.cy3sbml.SBML;
import org.cy3sbml.util.AttributeUtil;
import org.cytoscape.group.CyGroup;
import org.cytoscape.group.CyGroupFactory;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.sbml.jsbml.SBase;
import org.sbml.jsbml.ext.groups.Group;
import org.sbml.jsbml.ext.groups.Member;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates the Cytoscape groups of the SBML groups of a model in one network.
 * <p>
 * Every network gets its own {@link CyGroup} with its own group node for an SBML group, with
 * the members that are in this network. Cytoscape does not support one group in several
 * networks: a session restores such a group once per network with the members of all
 * networks, so expanding it adds nodes of other networks, e.g. parameters to the base
 * network (#171). A group without member in the network is not created in it, and the
 * edges of a group are the edges of its network.
 * <p>
 * A member that is a group is the group node of that group in the same network, so the
 * nested groups are created first, independent of their order in the model.
 */
final class GroupBuilder {
    private static final Logger logger = LoggerFactory.getLogger(GroupBuilder.class);

    private final CyGroupFactory groupFactory;
    private final Function<String, Optional<CyNode>> nodeByMetaId;
    private final CySubNetwork network;
    private final boolean logMissingMembers;

    private final Map<Group, CyGroup> cyGroups = new LinkedHashMap<>();
    private final Set<Group> visiting = new HashSet<>();
    private final Set<Group> visited = new HashSet<>();

    /**
     * @param nodeByMetaId      node of an SBase by metaId in the network with all nodes of the model
     * @param network           network to create the groups in
     * @param logMissingMembers log the members without node, true for the network with all nodes,
     *                          in which every member with a node has its node
     */
    GroupBuilder(
            CyGroupFactory groupFactory,
            Function<String, Optional<CyNode>> nodeByMetaId,
            CyNetwork network,
            boolean logMissingMembers) {
        this.groupFactory = groupFactory;
        this.nodeByMetaId = nodeByMetaId;
        this.network = (CySubNetwork) network;
        this.logMissingMembers = logMissingMembers;
    }

    /**
     * Creates the groups in the network.
     *
     * @param groups SBML groups with metaIds
     * @return the created groups by SBML group
     */
    Map<Group, CyGroup> build(List<Group> groups) {
        for (Group group : groups) {
            create(group);
        }
        return cyGroups;
    }

    /** Creates the group after its nested groups, returns null if it has no member in the network. */
    private CyGroup create(Group group) {
        if (visited.contains(group)) {
            return cyGroups.get(group);
        }
        if (!visiting.add(group)) {
            logger.warn("Group <{}> is a member of itself, the member is skipped.", group.getId());
            return null;
        }
        List<CyNode> members = new ArrayList<>();
        for (Member member : group.getListOfMembers()) {
            memberNode(group, member).ifPresent(node -> {
                if (!members.contains(node)) {
                    members.add(node);
                }
            });
        }
        visiting.remove(group);
        visited.add(group);
        if (members.isEmpty()) {
            return null;
        }

        // created with its members, so the internal and external edges are the edges of the
        // network; CyGroup.addNodes would take the edges of all networks of the root network
        CyGroup cyGroup = groupFactory.createGroup(network, members, null, true);
        setAttributes(cyGroup.getGroupNode(), group);
        cyGroups.put(group, cyGroup);
        return cyGroup;
    }

    /** Node of the member in the network: the group node for a group, else the node of the SBase. */
    private Optional<CyNode> memberNode(Group group, Member member) {
        SBase sbase = member.getSBaseInstance();
        if (sbase == null) {
            if (logMissingMembers) {
                logger.warn("Member <{}> of group <{}> does not reference an element.", member, group.getId());
            }
            return Optional.empty();
        }
        if (sbase instanceof Group memberGroup) {
            return Optional.ofNullable(create(memberGroup)).map(CyGroup::getGroupNode);
        }
        Optional<CyNode> node = nodeByMetaId.apply(sbase.getMetaId());
        if (node.isEmpty() && logMissingMembers) {
            logger.warn("Member <{}> of group <{}> has no node.", member, group.getId());
        }
        return node.filter(network::containsNode);
    }

    /**
     * Sets the attributes of the group node. Group nodes are registered in the root network,
     * so the attributes are set in the root network.
     */
    private void setAttributes(CyNode node, Group group) {
        CyRootNetwork rootNetwork = network.getRootNetwork();
        String metaId = group.getMetaId();
        AttributeUtil.set(rootNetwork, node, SBML.ATTR_CYID, metaId, String.class);
        AttributeUtil.set(rootNetwork, node, SBML.NODETYPE_ATTR, SBML.NODETYPE_GROUP, String.class);
        AttributeUtil.set(rootNetwork, node, SBML.LABEL, metaId, String.class);
        AttributeWriter.setNamedSBaseAttributes(rootNetwork, node, group);
    }
}
