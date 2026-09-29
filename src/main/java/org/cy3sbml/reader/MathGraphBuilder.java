package org.cy3sbml.reader;

import org.cy3sbml.SBML;
import org.cy3sbml.util.ASTNodeUtil;
import org.cy3sbml.util.AttributeUtil;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.sbml.jsbml.ASTNode;
import org.sbml.jsbml.AbstractMathContainer;
import org.sbml.jsbml.NamedSBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates the edges from the objects referenced in math to the math container node.
 */
final class MathGraphBuilder {
    private static final Logger logger = LoggerFactory.getLogger(MathGraphBuilder.class);

    private MathGraphBuilder() {}

    /**
     * Creates math subgraph for given math container and node.
     */
    static void createMathNetwork(
            ConversionContext context, AbstractMathContainer container, CyNode containerNode, String edgeType) {
        CyNetwork network = context.network();
        if (container.isSetMath()) {
            ASTNode astNode = container.getMath();
            AttributeUtil.set(network, containerNode, SBML.ATTR_MATH, ASTNodeUtil.toFormula(astNode), String.class);

            // Get the refenced objects in math.
            // This can be parameters, localParameters, species, ...
            // create edge if node exists
            for (NamedSBase nsb : ASTNodeUtil.findReferencedNamedSBases(astNode)) {
                CyNode nsbNode = context.nodeByMetaId(nsb.getMetaId()).orElse(null);

                if (nsbNode != null) {
                    context.createEdge(nsbNode, containerNode, edgeType);
                } else {
                    logger.warn(
                            "Node for metaId <{}> not found in math <{}>",
                            nsb.getMetaId(),
                            ASTNodeUtil.toFormula(astNode));
                }
            }
        }
    }
}
