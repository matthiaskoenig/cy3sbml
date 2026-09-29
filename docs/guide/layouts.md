# Layouts

## Layout on import

cy3sbml applies the Cytoscape `force-directed` layout to every network view it creates.
If this layout is not available, the default layout of Cytoscape is used. You can apply
any other Cytoscape layout afterwards with the **Layout** menu.

The SBML `layout` package is not imported yet, so the positions stored in an SBML file are
not used. See [Supported SBML packages](packages.md#layout).

## Save and load node positions

The toolbar buttons **Save Layout** and **Load Layout** store the node positions of a
network view in an XML file, and apply them to a network view again. Use them to keep a
manually arranged layout of a model, and to apply it after a new import of the model,
for example after the model was changed.

**Save Layout** writes the position and size of every node of the current network view:

```xml
<layout>
  <listOfBoundingBoxes>
    <boundingBox cyId="PX" id="PX" xpos="120.5" ypos="-43.0" height="35.0" width="35.0"/>
    <boundingBox cyId="law__v1" xpos="80.0" ypos="12.5" height="35.0" width="35.0"/>
    ...
  </listOfBoundingBoxes>
</layout>
```

The `cyId` identifies the node (column `cyId`): it is the unique id cy3sbml gives every SBML
element of the model, the metaid of the element if it has one, else an id derived from the
SBML id or the parent element, for example `law__v1` for the kinetic law of the
reaction `v1`. The `id` is the SBML id of the node (column `sbml id`), only written for
nodes of elements with an SBML id.

**Load Layout** reads such a file and moves every node of the current network view whose
`cyId` is in the file to the stored position. Nodes without a stored position keep
their position. The stored sizes are not applied. Layout files of cy3sbml versions before
0.7.0 have only the `id`; their positions are applied to the nodes with this SBML id.

Because every node is matched by its `cyId`, a layout file positions all nodes, also the
nodes of elements without SBML id such as kinetic laws, rules, units and the fbc `AND` and
`OR` nodes. It can be applied to every network of the same model, and to other versions of
the model with the same ids and metaids. SBML Level 1 has no metaids, so in Level 1 models
only the nodes with an SBML id are positioned.
