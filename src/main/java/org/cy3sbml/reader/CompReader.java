package org.cy3sbml.reader;

import javax.swing.tree.TreeNode;
import org.cy3sbml.SBML;
import org.cy3sbml.util.AttributeUtil;
import org.cy3sbml.util.filter.SBaseFilter;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBase;
import org.sbml.jsbml.ext.comp.CompConstants;
import org.sbml.jsbml.ext.comp.CompModelPlugin;
import org.sbml.jsbml.ext.comp.CompSBasePlugin;
import org.sbml.jsbml.ext.comp.Deletion;
import org.sbml.jsbml.ext.comp.Port;
import org.sbml.jsbml.ext.comp.ReplacedBy;
import org.sbml.jsbml.ext.comp.ReplacedElement;
import org.sbml.jsbml.ext.comp.SBaseRef;
import org.sbml.jsbml.ext.comp.Submodel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads the comp package: submodels, deletions, ports, replaced elements and
 * replaced by, with the edges to the elements they reference.
 */
final class CompReader implements PackageReader {
    private static final Logger logger = LoggerFactory.getLogger(CompReader.class);

    /**
     * Create network information from comp model.
     */
    @Override
    public void read(ConversionContext context, Model model) {
        CyNetwork network = context.network();
        logger.debug("<comp>");

        CompModelPlugin compModel = (CompModelPlugin) model.getExtension(CompConstants.namespaceURI);
        if (compModel == null) {
            return;
        }

        // Submodel //
        /*
           Submodels are instantiations of models contained within other models.
           A Submodel object must say which Model object it instantiates, and may additionally define how the Model object is
           to be modified before it is instantiated in the enclosing model.
        */
        logger.debug("<Submodel>");
        for (Submodel submodel : compModel.getListOfSubmodels()) {

            logger.debug(submodel.toString());
            CyNode n = context.createNode(submodel, SBML.NODETYPE_COMP_SUBMODEL);
            AttributeWriter.setNamedSBaseAttributes(network, n, submodel);

            AttributeUtil.set(network, n, SBML.ATTR_COMP_MODELREF, submodel.getModelRef(), String.class);
            if (submodel.isSetTimeConversionFactor()) {
                AttributeUtil.set(
                        network,
                        n,
                        SBML.ATTR_COMP_TIME_CONVERSION_FACTOR,
                        submodel.getTimeConversionFactor(),
                        String.class);
            }
            if (submodel.isSetExtentConversionFactor()) {
                AttributeUtil.set(
                        network,
                        n,
                        SBML.ATTR_COMP_EXTENT_CONVERSION_FACTOR,
                        submodel.getExtentConversionFactor(),
                        String.class);
            }

            // Deletion
            for (Deletion deletion : submodel.getListOfDeletions()) {
                // TODO: add edge
                logger.debug(deletion.toString());
                CyNode nd = context.createNode(deletion, SBML.NODETYPE_COMP_DELETION);
                AttributeWriter.setNamedSBaseAttributes(network, nd, deletion);

                // SbaseRef
                // TODO
                deletion.getIdRef();
            }

            // TODO: generic method for getting node for SbaseRef
            // SBaseRef provides attributes portRef, idRef, unitRef 12
            // and metaIdRef, and a recursive subcomponent, sBaseRef
        }

        // Port //
        logger.debug("<Port>");
        // create port nodes
        for (Port port : compModel.getListOfPorts()) {
            logger.debug(port.toString());
            CyNode n = context.createNode(port, SBML.NODETYPE_COMP_PORT);
            AttributeWriter.setNamedSBaseAttributes(network, n, port);
            AttributeWriter.setSBaseRefAttributes(network, n, port);
        }
        // create port edges
        for (Port port : compModel.getListOfPorts()) {
            CyNode source = AttributeUtil.getNodeByAttribute(network, SBML.ATTR_PORT_SID, port.getId());
            createSBaseRefEdge(context, source, port, model.getId());
        }

        logger.debug("<ReplacedElement & ReplacedBy>");
        // only sbases in current model
        for (TreeNode node : model.filter(new SBaseFilter())) {
            SBase sbase = (SBase) node;
            CompSBasePlugin compSBase = (CompSBasePlugin) sbase.getExtension(CompConstants.namespaceURI);
            if (compSBase != null) {
                logger.debug(compSBase.toString());

                CyNode source = AttributeUtil.getNodeByAttribute(network, SBML.ATTR_CYID, sbase.getMetaId());

                // replacedElements (SBaseRef)
                for (ReplacedElement replacedElement : compSBase.getListOfReplacedElements()) {

                    logger.debug(replacedElement.toString());
                    //
                    // targets can be from other submodels
                    CyNode target = context.createNode(replacedElement, SBML.NODETYPE_COMP_REPLACED_ELEMENT);
                    AttributeWriter.setSBaseRefAttributes(network, target, replacedElement);

                    String ref = getRefFromSBaseRef(replacedElement);
                    String submodel = "";
                    if (replacedElement.isSetSubmodelRef()) {
                        submodel = replacedElement.getSubmodelRef();
                    }

                    AttributeUtil.set(
                            network, target, SBML.LABEL, String.format("<%s:%s>", submodel, ref), String.class);

                    // edge to replacing element
                    context.createEdge(source, target, SBML.INTERACTION_COMP_SBASE_REPLACED_ELEMENT);

                    createSBaseRefEdge(context, target, replacedElement, model.getId());

                    AttributeUtil.set(
                            network,
                            target,
                            SBML.ATTR_COMP_SUBMODELREF,
                            replacedElement.getSubmodelRef(),
                            String.class);
                    if (replacedElement.isSetConversionFactor()) {
                        // FIXME
                        // replacedElement.getConversionFactor();
                    }
                    if (replacedElement.isSetDeletion()) {
                        // FIXME
                        // replacedElement.getDeletion();
                    }

                    /*
                    When deletion is set, it means the ReplacedElement object is actually an annotation to indicate that the replacement object
                    replaces something deleted from a submodel. The use of the deletion attribute overrides the use of the attributes
                    inherited from SBaseRef: instead of using, e.g., portRef or idRef, the ReplacedElement instance sets deletion to
                    the identifier of the Deletion object. In addition, the referenced Deletion must be a child of the Submodel referenced
                    by the submodelRef attribute
                    */

                }

                // replacedBy
                if (compSBase.isSetReplacedBy()) {
                    ReplacedBy replacedBy = compSBase.getReplacedBy();
                    logger.debug(replacedBy.toString());
                    CyNode target = context.createNode(replacedBy, SBML.NODETYPE_COMP_REPLACED_BY);
                    AttributeWriter.setSBaseRefAttributes(network, target, replacedBy);

                    String ref = getRefFromSBaseRef(replacedBy);
                    String submodel = "";
                    if (replacedBy.isSetSubmodelRef()) {
                        submodel = replacedBy.getSubmodelRef();
                    }

                    AttributeUtil.set(
                            network, target, SBML.LABEL, String.format("<%s:%s>", submodel, ref), String.class);

                    // edge to replacing element
                    context.createEdge(source, target, SBML.INTERACTION_COMP_SBASE_REPLACED_BY);
                    createSBaseRefEdge(context, target, replacedBy, model.getId());
                }

                // deletion
            }
        }
    }

    /**
     * Get String of referenced element from port.
     *
     * @return String
     */
    private static String getRefFromSBaseRef(SBaseRef sbaseRef) {

        String ref = "";
        if (sbaseRef.isSetPortRef()) {
            ref = sbaseRef.getPortRef();
        } else if (sbaseRef.isSetIdRef()) {
            ref = sbaseRef.getIdRef();
        } else if (sbaseRef.isSetUnitRef()) {
            ref = sbaseRef.getUnitRef();
        } else if (sbaseRef.isSetMetaIdRef()) {
            ref = sbaseRef.getMetaIdRef();
        }
        return ref;
    }

    /**
     * Creates the edge for the given source node and sBaseRef.
     * <p>
     * Finds the target of the SbaseRef and adds the edge to it.
     */
    private static void createSBaseRefEdge(
            ConversionContext context, CyNode sbaseNode, SBaseRef sBaseRef, String model) {
        CyNetwork network = context.network();

        String submodel = null;
        // necessary to check if sBaseRef in own model
        if (sBaseRef instanceof ReplacedElement replacedElement) {
            submodel = replacedElement.getSubmodelRef();
        } else if (sBaseRef instanceof ReplacedBy replacedBy) {
            submodel = replacedBy.getSubmodelRef();
        }
        // empty submodel points to same model
        if (submodel != null && submodel.length() == 0) {
            submodel = model;
        }

        CyNode target = null;
        String interaction = null;

        // link to other submodel component
        if (submodel != null && !submodel.equals(model)) {
            logger.warn("SBaseRef to other submodel. Link not created.");
        } else {
            if (sBaseRef.isSetPortRef()) {
                String portRef = sBaseRef.getPortRef();
                target = AttributeUtil.getNodeByAttribute(network, SBML.ATTR_PORT_SID, portRef);
                interaction = SBML.INTERACTION_COMP_SBASEREF_PORT;
            } else if (sBaseRef.isSetIdRef()) {
                String idRef = sBaseRef.getIdRef();
                target = AttributeUtil.getNodeByAttribute(network, SBML.ATTR_ID, idRef);
                interaction = SBML.INTERACTION_COMP_SBASEREF_ID;
            } else if (sBaseRef.isSetUnitRef()) {
                String unitRef = sBaseRef.getUnitRef();
                target = AttributeUtil.getNodeByAttribute(network, SBML.ATTR_UNIT_SID, unitRef);
                interaction = SBML.INTERACTION_COMP_SBASEREF_UNIT;
            } else if (sBaseRef.isSetMetaIdRef()) {
                String metaIdRef = sBaseRef.getMetaIdRef();
                target = AttributeUtil.getNodeByAttribute(network, SBML.ATTR_METAID, metaIdRef);
                interaction = SBML.INTERACTION_COMP_SBASEREF_METAID;
            }

            // handle the recursive case
            // FIXME:
            // if (port.isSetSBaseRef()){
            //     port.getSBaseRef();
            // }

            context.createEdge(sbaseNode, target, interaction);
        }
    }
}
