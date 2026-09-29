package org.cy3sbml.reader;

import java.util.List;
import java.util.Optional;
import javax.swing.tree.TreeNode;
import org.cy3sbml.SBML;
import org.cy3sbml.util.AttributeUtil;
import org.cy3sbml.util.DistribUtil;
import org.cytoscape.model.CyIdentifiable;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBase;
import org.sbml.jsbml.ext.distrib.Uncertainty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads the distrib package: the uncertainties of an element are written to the
 * columns {@link SBML#ATTR_DISTRIB_UNCERTAINTY} (a summary) and
 * {@link SBML#ATTR_DISTRIB_UNCERTAINTY_COUNT} of its node, or of its edge for a
 * species reference. The distribution functions in math need no reader, they are
 * part of the math of the core elements.
 */
final class DistribReader implements PackageReader {
    private static final Logger logger = LoggerFactory.getLogger(DistribReader.class);

    @Override
    public void read(ConversionContext context, Model model) {
        for (TreeNode treeNode : model.filter(o ->
                o instanceof SBase sbase && !DistribUtil.uncertainties(sbase).isEmpty())) {
            SBase sbase = (SBase) treeNode;
            List<Uncertainty> uncertainties = DistribUtil.uncertainties(sbase);
            Optional<CyIdentifiable> element = element(context, sbase);
            if (element.isEmpty()) {
                logger.debug("No node or edge for the uncertainties of <{}>", sbase);
                continue;
            }
            AttributeUtil.set(
                    context.network(),
                    element.get(),
                    SBML.ATTR_DISTRIB_UNCERTAINTY,
                    DistribUtil.summary(uncertainties),
                    String.class);
            AttributeUtil.set(
                    context.network(),
                    element.get(),
                    SBML.ATTR_DISTRIB_UNCERTAINTY_COUNT,
                    uncertainties.size(),
                    Integer.class);
        }
    }

    /** The node of the SBase, else its edge (species references). */
    private static Optional<CyIdentifiable> element(ConversionContext context, SBase sbase) {
        if (sbase.isSetMetaId()) {
            Optional<CyIdentifiable> node =
                    context.nodeByMetaId(sbase.getMetaId()).map(CyIdentifiable.class::cast);
            if (node.isPresent()) {
                return node;
            }
        }
        return context.edgeOf(sbase).map(CyIdentifiable.class::cast);
    }
}
