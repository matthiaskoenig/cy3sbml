package org.cy3sbml.reader;

import java.util.ArrayList;
import java.util.List;
import org.cytoscape.group.CyGroup;
import org.cytoscape.model.CyNode;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBase;
import org.sbml.jsbml.ext.groups.Group;
import org.sbml.jsbml.ext.groups.GroupsConstants;
import org.sbml.jsbml.ext.groups.GroupsModelPlugin;
import org.sbml.jsbml.ext.groups.ListOfMembers;
import org.sbml.jsbml.ext.groups.Member;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads the groups package: every group becomes a CyGroup of its member nodes.
 */
final class GroupsReader implements PackageReader {
    private static final Logger logger = LoggerFactory.getLogger(GroupsReader.class);

    /**
     * Create groups.
     * Groups are implemented as group nodes.
     * <p>
     * A CyGroup is created either as an empty group
     * CyGroup emptyGroup = groupFactory.createGroup(network, true);
     * or by turning an existing node into an empty group:
     * CyGroup emptyGroup = groupFactory.createGroup(network, node, true);
     */
    @Override
    public void read(ConversionContext context, Model model) {
        logger.debug("<groups>");

        GroupsModelPlugin groupsModel = (GroupsModelPlugin) model.getExtension(GroupsConstants.shortLabel);
        if (groupsModel == null) {
            return;
        }

        for (Group group : groupsModel.getListOfGroups()) {
            logger.debug(String.format("Reading group: <%s>", group));

            // empty group node & sets attributes
            CyGroup cyGroup = context.createGroup(group);

            // collect nodes from members
            List<CyNode> nodes = new ArrayList<>();
            ListOfMembers membersList = group.getListOfMembers();
            for (Member member : membersList) {

                // resolve object & node
                SBase sbase = member.getSBaseInstance();
                CyNode memberNode = context.nodeByMetaId(sbase.getMetaId()).orElse(null);

                if (memberNode != null) {
                    nodes.add(memberNode);
                } else {
                    logger.error(String.format("Member <%s> of group <%s> not found via metaId.", group, member));
                }

                // Information transfer to members

                // Unlike most lists of objects in SBML, the sboTerm attribute and the Notes
                // and Annotation children are taken from the ListOfMembers to apply directly to every
                // SBML element referenced by each child Member of this ListOfMembers,
                // if that referenced element has no such definition.
                // Thus, if a referenced element has no defined sboTerm, child Notes, or child Annotation,
                // that element should be considered to now have the sboTerm, child Notes, or child Annotation of the
                // ListOfMembers.

                // ! this changes the SBMLDocument
                if (membersList.isSetSBOTerm() && !sbase.isSetSBOTerm()) {
                    sbase.setSBOTerm(membersList.getSBOTerm());
                }
                if (membersList.isSetNotes() && !sbase.isSetNotes()) {
                    sbase.setNotes(membersList.getNotes());
                }
                if (membersList.isSetAnnotation() && !sbase.isSetAnnotation()) {
                    sbase.setAnnotation(membersList.getAnnotation());
                }
            }
            logger.debug(String.format("Adding %s nodes to cyGroup", nodes.size()));
            cyGroup.addNodes(nodes);
        }
    }
}
