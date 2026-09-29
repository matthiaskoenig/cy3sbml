# SBML layout package (#71)

## Goal

Read the layouts of the SBML `layout` package (version 1) and show every layout as it is
drawn: one additional network per layout in the collection of its model, whose view has the
nodes at the positions and in the sizes of the glyphs. This ports the layout support of
cy2sbml to Cytoscape 3.

## Decisions

- **One glyph subnetwork per layout** (not a second view of the base network): layouts draw
  one element with several glyphs (aliases, e.g. 14 of the 67 species of `layout_01.xml`),
  which a view of the base network (one node per element) cannot show. The subnetwork has
  one node per glyph and gets its own view.
- **Read only.** Writing Cytoscape positions back as an SBML layout is a separate issue.
  The layout networks and their views are saved in the Cytoscape session like the other
  networks; **Save Layout**/**Load Layout** work on them.
- **Not covered** (follow-up issue): curves (edge bends), the render package, free-standing
  text glyphs (text glyphs without graphical object), the z dimension.

## Current state

`LayoutReader` only logs "Layouts found, but not yet supported.". `SBML` has the unused
constants `NETWORKTYPE_LAYOUT`, `NODETYPE_LAYOUT_SPECIESGLYPH`,
`NODETYPE_LAYOUT_REACTIONGLYPH`. Every view gets the `cy3sbml` style, whose `nodeSizeLocked`
dependency makes the node size one value (`NODE_SIZE`), and the force-directed layout.

## Design

### Networks

For every model source (main model, model definitions, external models, flat model) and
every `Layout` of its `LayoutModelPlugin`, the root network of the model gets one subnetwork
`<name>__layout_<layoutId>` (`<name>__layout_<index>` if the layout has no id, index from 1).
The layout networks come after base, kinetic and all in `getNetworks()`, so the base network
of the main model is still the first network. The network row has `sbmlNetwork` =
`NETWORKTYPE_LAYOUT` (`sbmlLayout`) and `layout_id` = the layout id.

### Nodes (one per glyph)

The glyphs of a layout, in document order: compartment glyphs, species glyphs, reaction
glyphs, the additional graphical objects (general glyphs and plain graphical objects), and
the sub glyphs of general glyphs. Text glyphs are labels (below), not nodes.

- A glyph that references an element (`AbstractReferenceGlyph.getReference()`: species,
  qualitative species, reaction, transition, compartment, or any SId for a general glyph)
  whose node exists in the network of the model (`ConversionContext.nodeById`) *represents*
  that node: the glyph node gets a copy of all shared column values of the model node (so
  `cyId`, `sbml type`, `sbml type ext`, `compartmentCode`, names, annotations ... are the same).
  Selecting a glyph node shows the element in the info panel; the `cyId -> SUIDs` mapping of
  `SBMLManager` contains every alias.
- A glyph without (resolvable) reference is a node of its own: `cyId` = the metaId of the
  glyph (set with `MappingUtil.setSBaseMetaId` like every other node), `sbml type` = the
  glyph type, `sbml id` = the glyph id. Selecting it shows the glyph in the info panel.
- Local columns of the layout network (`CyNetwork.LOCAL_ATTRS`, not in the other networks):
  - `layout_glyph` (String): glyph id; empty for generated nodes
  - `layout_glyphType` (String): `layout:compartmentGlyph`, `layout:speciesGlyph`,
    `layout:reactionGlyph`, `layout:generalGlyph`, `layout:graphicalObject`,
    `layout:generated`
  - `layout_x`, `layout_y` (Double): the centre of the bounding box
  - `layout_width`, `layout_height` (Double): the dimensions of the bounding box
- Missing geometry: a missing position is 0,0; a glyph without (positive) width and height
  is a point, its position is the centre, with the default size 30 (12 for reaction glyphs,
  the KEGG layouts give reaction glyphs only a position); a missing width or height alone
  is the default size.
- Labels (`label` column): the text of a text glyph whose `graphicalObject` is the glyph
  (`text`, else the name or id of `originOfText`); several text glyphs are joined with a
  space. Without text glyph the label of the represented node, else the glyph name or id.
  A text glyph whose graphical object does not exist is ignored with a debug log line.

### Edges

- **Species reference glyphs**: an edge between the reaction glyph node and the species
  glyph node. All edges start at the reaction/transition node, as the model edges of
  cy3sbml do (`reaction-reactant`, `reaction-product`, ...). The edge of the model
  (`__all`) between the represented reaction node and species node is the *model edge*:
  the species reference of the glyph (`getSpeciesReference`) if set and found
  (`ConversionContext.edgeOf`), else the one edge between the two nodes whose interaction
  type fits the role, else the only edge between the two nodes. The layout edge is a copy
  of the shared columns of the model edge. Without model edge the role gives the type:
  `substrate`, `sidesubstrate` -> `reaction-reactant` (`input_transition` for a
  transition); `product`, `sideproduct` -> `reaction-product` (`transition_output`);
  `activator`, `inhibitor` -> `reaction-activator`, `reaction-inhibitor`; `modifier`,
  `undefined` or unset -> `reaction-modifier` (`input_transition`). A species reference glyph without resolvable species glyph is
  skipped with a debug log line.
- **Reaction glyphs without species reference glyphs**: for every model edge of the
  represented reaction/transition to a participant node (the edges with the interaction
  types reactant, product, modifier, activator, inhibitor, qual input and output), an edge
  from the reaction glyph node to the glyph of the participant nearest to the reaction glyph
  (cy2sbml connected all aliases, which gives a tangle of edges in the KEGG layouts), with
  the shared columns of the model edge.
- **Reactions and transitions without glyph** (e.g. the KEGG qual layouts have only species
  glyphs): if at least one participant has a glyph node, a generated node represents the
  reaction/transition (copy of its node like a glyph node, `layout_glyphType` =
  `layout:generated`, no `layout_glyph`, empty label: the layout does not draw it). For
  every participant the glyph nearest to the centroid of all glyphs of the participants is
  chosen; the node is at the centroid of the chosen glyphs, size 12 x 12, with an edge to
  each of them. Participants without glyph are left out.
- **General glyphs**: a reference glyph (`ReferenceGlyph`) with a glyph gives an edge from
  the general glyph node to the node of the referenced glyph, interaction type
  `layout:reference` (role in the column `layout_role`).

### Structure

- `reader.LayoutReader` (PackageReader, keeps its place in the reader list): collects the
  layouts of the model in the `ConversionContext` (`addLayout(Layout)`), like
  `GroupsReader` collects the groups; the networks are built later, when all attributes of
  the model nodes are written (also `DerivedAttributes`).
- `reader.LayoutNetworkBuilder`: `List<CyNetwork> build(CyRootNetwork, String name,
  ConversionContext)` creates the layout networks after `SubnetworkBuilder`; called from
  `SBMLReaderTask.createNetworksFromModel`. The node/edge creation lives in helper methods
  per glyph kind; the geometry defaults in a small record `GlyphBox(x, y, width, height)`
  (centre and size) with `GlyphBox.of(GraphicalObject)`.
- `SBML`: the constants of the new columns, glyph types, `SUFFIX_SUBNETWORK_LAYOUT =
  "__layout_"`, `INTERACTION_LAYOUT_REFERENCE`.

### Views

`SBMLReaderTask.buildCyNetworkView` recognizes a layout network by its `sbmlNetwork` value:

- the style is the layout variant of the configured style (`<style>-layout`, see below);
- every node view gets `NODE_X_LOCATION`/`NODE_Y_LOCATION` from `layout_x`/`layout_y`
  (plain values, so the user can move the nodes);
- no force-directed layout (also when `doLayout` is set);
- the style is applied to the view (`VisualStyle.apply`): Cytoscape applies the style of a
  reader's view only if it is the default style, and for the other views the layout task
  did it;
- Cytoscape fits the content afterwards (`GenerateNetworkViewsTask`).

### Styles

`StyleManager` derives a layout style from every loaded cy3sbml style after loading it
(and after a session load, if missing): `cy3sbml-layout`, `cy3sbml-dark-layout`, created
with the `VisualStyleFactory` as a copy of the base style, and then:

- the `nodeSizeLocked` dependency off, `NODE_WIDTH`/`NODE_HEIGHT` passthrough mappings of
  `layout_width`/`layout_height`;
- `NODE_SHAPE`: the base mapping, with `ROUND_RECTANGLE` for `compartment`;
- `NODE_LABEL_POSITION`: the base mapping, with the label of `compartment` inside at the top;
- `NODE_Z_LOCATION`: discrete mapping of `layout_glyphType`, compartment glyphs behind
  (`-1`), everything else in front (default `0`);
- compartment glyph fill with transparency (value chosen in Cytoscape so species stay
  readable).

The layout styles are code, not XML resources, so they follow every change of the base
styles. The services `VisualStyleFactory` and the passthrough/discrete
`VisualMappingFunctionFactory`s come from `CyActivator`.

### Save/Load Layout

With aliases the `cyId` is not unique in a layout network. `CyBoundingBox` gets the optional
`glyph` (the `layout_glyph` value), written as attribute `glyph` of the node in the layout
XML. Loading matches a node with a glyph id by the glyph id, all others by `cyId` (generated
nodes: `cyId` is unique per layout network), old files by the SBML id as before.

### Error handling

A layout never fails the import: an unexpected exception while building one layout network
is logged as a warning with the layout id and that layout network is left out (the partial
subnetwork is removed from the root network). Unresolvable references are debug log lines
(they are validator findings, not import problems).

## Testing

- `LayoutNetworkBuilderTest` (reader tests with `ReaderTestSupport` / `SBMLReaderTask`):
  - `layout_01.xml` (qual, species glyphs with aliases, no reaction glyphs): one layout
    network, a node per species glyph (87), aliases share the `cyId`, generated transition
    nodes at the centroid, edges to all aliases, geometry = centre of the bounding box.
  - `hsa00450_L3V1_layoutV1.xml` (reaction glyphs without species reference glyphs):
    generated edges from reaction glyph nodes.
  - `small_population.xml` (compartment, general and text glyphs): general glyph nodes,
    text glyph labels.
  - a hand-written model with species reference glyphs (roles, speciesReference, missing
    bounding box parts, a text glyph with `originOfText`, a layout without id, two layouts):
    edge direction/type from the model edge and from the role, defaults, names
    `__layout_<id>`/`__layout_<index>`, network order.
  - a model without layout: no layout network, no layout columns.
- `SBMLReaderTaskTest`: the view of a layout network has the node positions of the columns
  and no force-directed layout; the base network is still first.
- `StyleManager` test: the derived layout style has the passthrough width/height mappings
  and the size lock off.
- Save/Load Layout: round trip of a layout network with aliases (`LayoutReimportTest`
  covers the golden layout models).
- `GoldenModelsTest`: regenerate the snapshots of the layout models and review the diff.
- Manual check in Cytoscape 3.10: `layout_01.xml`, `hsa00450`, `small_population`,
  a session save/load, the info panel on a glyph node, pixel check of the rendering.

## Documentation

`docs/guide/layouts.md` and `docs/guide/packages.md#layout` (what is read, the networks, the
columns, what is not supported), `docs/index.md`, `README.md`, `CLAUDE.md`,
`docs/development/architecture.md`, `release-notes/0.7.0.md` highlight.
