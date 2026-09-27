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
    <boundingBox id="PX" xpos="120.5" ypos="-43.0" height="35.0" width="35.0"/>
    ...
  </listOfBoundingBoxes>
</layout>
```

The `id` is the SBML id of the node (column `sbml id`).

**Load Layout** reads such a file and moves every node of the current network view whose
`sbml id` is in the file to the stored position. Nodes without a stored position keep
their position. The stored sizes are not applied.

Because the nodes are matched by their SBML id, a layout file can be applied to every
network of the same model, and to other versions of the model with the same ids. Nodes
without an SBML id, for example units, which have a `unitSid`, and the fbc `AND` and `OR`
nodes, are not positioned.
