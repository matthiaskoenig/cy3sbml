package org.cy3sbml.reader;

import javax.swing.tree.TreeNode;
import org.cy3sbml.SBML;
import org.cy3sbml.comp.ModelResolution;
import org.cy3sbml.comp.SBaseRefResolution;
import org.cy3sbml.comp.SBaseRefResolution.Resolved;
import org.cy3sbml.comp.SBaseRefResolution.Unresolved;
import org.cy3sbml.comp.SBaseRefResolver;
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
 * Reads the comp package: submodels with their deletions, ports, replaced elements and
 * replaced by elements.
 * <p>
 * Every comp element becomes a node. The target of an SBaseRef (port, deletion, replaced
 * element, replaced by) is resolved with the {@link SBaseRefResolver} and written to the
 * {@code comp_target*} columns; it is usually in the model of a submodel, which is another
 * network. An edge to the target is created if the target is in the model read.
 */
final class CompReader implements PackageReader {
    private static final Logger logger = LoggerFactory.getLogger(CompReader.class);

    @Override
    public void read(ConversionContext context, Model model) {
        CompModelPlugin plugin = compModelPlugin(model);
        if (plugin != null) {
            readSubmodels(context, model, plugin);
            readPorts(context, model, plugin);
        }
        readReplacements(context, model);
    }

    /** Submodels and their deletions, with an edge from the submodel to each deletion. */
    private static void readSubmodels(ConversionContext context, Model model, CompModelPlugin plugin) {
        CyNetwork network = context.network();
        for (Submodel submodel : plugin.getListOfSubmodels()) {
            CyNode node = context.createNode(submodel, SBML.NODETYPE_COMP_SUBMODEL);
            AttributeWriter.setNamedSBaseAttributes(network, node, submodel);
            setString(network, node, SBML.ATTR_COMP_MODELREF, submodel.getModelRef());
            if (submodel.isSetTimeConversionFactor()) {
                setString(network, node, SBML.ATTR_COMP_TIME_CONVERSION_FACTOR, submodel.getTimeConversionFactor());
            }
            if (submodel.isSetExtentConversionFactor()) {
                setString(network, node, SBML.ATTR_COMP_EXTENT_CONVERSION_FACTOR, submodel.getExtentConversionFactor());
            }
            writeModelResolution(context, node, submodel);

            for (Deletion deletion : submodel.getListOfDeletions()) {
                CyNode deletionNode = context.createNode(deletion, SBML.NODETYPE_COMP_DELETION);
                AttributeWriter.setNamedSBaseAttributes(network, deletionNode, deletion);
                context.createEdge(node, deletionNode, SBML.INTERACTION_COMP_SBASE_DELETION);
                writeTarget(context, model, deletionNode, deletion);
                if (!deletion.isSetId() && !deletion.isSetName()) {
                    // label deletions without id and name like replaced elements
                    setString(
                            network,
                            deletionNode,
                            SBML.LABEL,
                            String.format("<%s:%s>", submodel.getId(), reference(deletion)));
                }
            }
        }
    }

    /** Ports, with an edge to the element they expose. */
    private static void readPorts(ConversionContext context, Model model, CompModelPlugin plugin) {
        CyNetwork network = context.network();
        for (Port port : plugin.getListOfPorts()) {
            CyNode node = context.createNode(port, SBML.NODETYPE_COMP_PORT);
            AttributeWriter.setNamedSBaseAttributes(network, node, port);
            writeTarget(context, model, node, port);
        }
    }

    /**
     * Replaced elements and replaced by elements of all elements of the model, with an edge
     * from the element to them and from them to their submodel.
     */
    private static void readReplacements(ConversionContext context, Model model) {
        for (TreeNode treeNode : model.filter(new SBaseFilter())) {
            SBase sbase = (SBase) treeNode;
            if (!(sbase.getExtension(CompConstants.shortLabel) instanceof CompSBasePlugin plugin)) {
                continue;
            }
            CyNode source = sbase.isSetMetaId()
                    ? context.nodeByMetaId(sbase.getMetaId()).orElse(null)
                    : null;
            if (source == null && (plugin.isSetListOfReplacedElements() || plugin.isSetReplacedBy())) {
                logger.warn("The replacements of '{}' have no node to start from.", sbase);
            }
            if (plugin.isSetListOfReplacedElements()) {
                for (ReplacedElement replacedElement : plugin.getListOfReplacedElements()) {
                    readReplacedElement(context, model, source, replacedElement);
                }
            }
            if (plugin.isSetReplacedBy()) {
                readReplacedBy(context, model, source, plugin.getReplacedBy());
            }
        }
    }

    private static void readReplacedElement(
            ConversionContext context, Model model, CyNode source, ReplacedElement replacedElement) {
        CyNetwork network = context.network();
        CyNode node = context.createNode(replacedElement, SBML.NODETYPE_COMP_REPLACED_ELEMENT);
        if (source != null) {
            context.createEdge(source, node, SBML.INTERACTION_COMP_SBASE_REPLACED_ELEMENT);
        }
        writeSubmodelRef(context, node, replacedElement.getSubmodelRef());
        if (replacedElement.isSetConversionFactor()) {
            setString(network, node, SBML.ATTR_COMP_CONVERSION_FACTOR, replacedElement.getConversionFactor());
        }
        if (replacedElement.isSetDeletion()) {
            // replaces the element of a deletion of the submodel, which is removed anyway
            String deletion = replacedElement.getDeletion();
            setString(network, node, SBML.ATTR_COMP_DELETION, deletion);
            setString(network, node, SBML.LABEL, String.format("<%s:%s>", replacedElement.getSubmodelRef(), deletion));
            context.nodeById(deletion)
                    .ifPresent(target -> context.createEdge(node, target, SBML.INTERACTION_COMP_SBASE_DELETION));
            return;
        }
        writeTarget(context, model, node, replacedElement);
    }

    private static void readReplacedBy(ConversionContext context, Model model, CyNode source, ReplacedBy replacedBy) {
        CyNode node = context.createNode(replacedBy, SBML.NODETYPE_COMP_REPLACED_BY);
        if (source != null) {
            context.createEdge(source, node, SBML.INTERACTION_COMP_SBASE_REPLACED_BY);
        }
        writeSubmodelRef(context, node, replacedBy.getSubmodelRef());
        writeTarget(context, model, node, replacedBy);
    }

    /** The submodelRef column and the edge to the submodel. */
    private static void writeSubmodelRef(ConversionContext context, CyNode node, String submodelRef) {
        setString(context.network(), node, SBML.ATTR_COMP_SUBMODELREF, submodelRef);
        if (submodelRef != null) {
            context.nodeById(submodelRef)
                    .ifPresent(submodel -> context.createEdge(node, submodel, SBML.INTERACTION_COMP_SBASEREF_SUBMODEL));
        }
    }

    /** The resolution of the model the submodel instantiates. */
    private static void writeModelResolution(ConversionContext context, CyNode node, Submodel submodel) {
        CyNetwork network = context.network();
        ModelResolution resolution = context.sBaseRefResolver().models().resolve(submodel);
        if (resolution instanceof ModelResolution.Resolved resolved) {
            setString(
                    network, node, SBML.ATTR_COMP_TARGET_MODEL, resolved.model().getId());
            setString(network, node, SBML.ATTR_COMP_RESOLUTION, SBML.COMP_RESOLVED);
        } else if (resolution instanceof ModelResolution.Failed failed) {
            setString(network, node, SBML.ATTR_COMP_RESOLUTION, failed.reason());
            logger.warn("The model of the submodel '{}' could not be resolved: {}", submodel.getId(), failed.reason());
        }
    }

    /**
     * The attributes of the reference and its resolved target, and the edge to the target
     * if it is in the model read.
     */
    private static void writeTarget(ConversionContext context, Model model, CyNode node, SBaseRef ref) {
        CyNetwork network = context.network();
        AttributeWriter.setSBaseRefAttributes(network, node, ref);
        String chain = SBaseRefResolver.describe(ref);
        setString(network, node, SBML.ATTR_COMP_SBASEREF, chain);
        if (!(ref instanceof Port) && !(ref instanceof Deletion)) {
            String submodelRef =
                    ref instanceof ReplacedElement re ? re.getSubmodelRef() : ((ReplacedBy) ref).getSubmodelRef();
            setString(network, node, SBML.LABEL, String.format("<%s:%s>", submodelRef, reference(ref)));
        }

        SBaseRefResolution resolution = context.sBaseRefResolver().resolve(ref);
        if (resolution instanceof Unresolved unresolved) {
            setString(network, node, SBML.ATTR_COMP_RESOLUTION, unresolved.reason());
            logger.warn("The reference '{}' could not be resolved: {}", chain, unresolved.reason());
            return;
        }
        Resolved resolved = (Resolved) resolution;
        SBase target = resolved.target();
        setString(network, node, SBML.ATTR_COMP_RESOLUTION, SBML.COMP_RESOLVED);
        setString(network, node, SBML.ATTR_COMP_TARGET_MODEL, resolved.model().getId());
        setString(network, node, SBML.ATTR_COMP_TARGET_TYPE, target.getElementName());
        setString(network, node, SBML.ATTR_COMP_TARGET_METAID, target.getMetaId());
        if (target.isSetId()) {
            setString(network, node, SBML.ATTR_COMP_TARGET_ID, target.getId());
        }
        if (resolved.model() == model) {
            context.nodeByMetaId(target.getMetaId())
                    .ifPresent(targetNode -> context.createEdge(node, targetNode, interaction(ref)));
        }
    }

    /** The interaction type of the edge to the target, by the kind of the reference. */
    private static String interaction(SBaseRef ref) {
        if (ref.isSetPortRef()) {
            return SBML.INTERACTION_COMP_SBASEREF_PORT;
        } else if (ref.isSetUnitRef()) {
            return SBML.INTERACTION_COMP_SBASEREF_UNIT;
        } else if (ref.isSetMetaIdRef()) {
            return SBML.INTERACTION_COMP_SBASEREF_METAID;
        }
        return SBML.INTERACTION_COMP_SBASEREF_ID;
    }

    /** The referenced id of the first level of the reference. */
    private static String reference(SBaseRef ref) {
        if (ref.isSetPortRef()) {
            return ref.getPortRef();
        } else if (ref.isSetIdRef()) {
            return ref.getIdRef();
        } else if (ref.isSetUnitRef()) {
            return ref.getUnitRef();
        } else if (ref.isSetMetaIdRef()) {
            return ref.getMetaIdRef();
        }
        return "";
    }

    private static void setString(CyNetwork network, CyNode node, String column, String value) {
        if (value != null) {
            AttributeUtil.set(network, node, column, value, String.class);
        }
    }

    private static CompModelPlugin compModelPlugin(Model model) {
        return model.getExtension(CompConstants.shortLabel) instanceof CompModelPlugin plugin ? plugin : null;
    }
}
