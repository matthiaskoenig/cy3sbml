package org.cy3sbml.layout;

/**
 * Position and size of a node in a layout file.
 * <p>
 * The node is identified by its {@code cyId}, the unique id of the SBML element of the node
 * (column {@code cyId}), which every node of an imported SBML network has. The SBML id
 * (column {@code sbml id}) is only set for nodes of elements with an id; layout files written
 * before the {@code cyId} was stored identify the nodes by it alone.
 *
 * @param cyId   cyId of the node, null in old layout files
 * @param sbmlId SBML id of the node, null for nodes of elements without id
 */
public record CyBoundingBox(String cyId, String sbmlId, double x, double y, double height, double width) {

    public CyBoundingBox {
        if (cyId == null && sbmlId == null) {
            throw new IllegalArgumentException("bounding box without cyId and sbml id");
        }
    }
}
