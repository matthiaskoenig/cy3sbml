package org.cy3sbml.reader;

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
 * Reads the groups package: every group becomes a CyGroup of its member nodes, in the network
 * with all nodes here and in the subnetworks by {@link SubnetworkBuilder}, see
 * {@link GroupBuilder}.
 */
final class GroupsReader implements PackageReader {
    private static final Logger logger = LoggerFactory.getLogger(GroupsReader.class);

    @Override
    public void read(ConversionContext context, Model model) {
        logger.debug("<groups>");

        GroupsModelPlugin groupsModel = (GroupsModelPlugin) model.getExtension(GroupsConstants.shortLabel);
        if (groupsModel == null) {
            return;
        }

        for (Group group : groupsModel.getListOfGroups()) {
            context.addGroup(group);
            transferListOfMembersInformation(group.getListOfMembers());
        }
        context.createGroups(context.network());
    }

    /**
     * Unlike most lists of objects in SBML, the sboTerm attribute and the Notes and Annotation
     * children are taken from the ListOfMembers to apply directly to every SBML element
     * referenced by each child Member of this ListOfMembers, if that referenced element has no
     * such definition. This changes the SBMLDocument.
     */
    private static void transferListOfMembersInformation(ListOfMembers membersList) {
        for (Member member : membersList) {
            SBase sbase = member.getSBaseInstance();
            if (sbase == null) {
                continue;
            }
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
    }
}
