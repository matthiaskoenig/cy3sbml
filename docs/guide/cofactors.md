# Cofactor nodes

Cofactors like ATP, ADP, NAD or water take part in many reactions. In the network, their
nodes connect to many reactions, which pulls the layout together and hides the structure
of the pathway. cy3sbml can split such a node into one node per edge.

## Split a cofactor node

1. Select one or more nodes in a network imported by cy3sbml.
2. Click **Cofactor nodes** in the toolbar.

Every selected node with N edges is replaced by N clone nodes. Every clone has one of the
edges of the original node and a copy of its table values. The column `sbml type` of a
clone gets the suffix `-clone`, for example `species-clone`.

The split only changes the current network. The original node and its edges stay in the
network collection, and the other networks of the model are not changed.

## Merge the clones

1. Select one or more clone nodes.
2. Click **Cofactor nodes** again.

The clones of every selected clone are removed, and the original node with its edges is
added to the network again.

## Notes

- The button is enabled only if the current network was imported by cy3sbml.
- The split state of every network is saved in Cytoscape sessions and restored when the
  session is opened.
- The clones have the SBML id of the original node. [Load Layout](layouts.md#save-and-load-node-positions)
  therefore moves all clones of a node to the same position.
