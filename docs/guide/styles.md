# Styles

cy3sbml adds two visual styles to Cytoscape when it starts:

- `cy3sbml`: a light style, the default,
- `cy3sbml-dark`: the same mapping on a dark background.

A style is only added if Cytoscape has no style with this name yet, so a style you changed
and saved in a session is kept. cy3sbml also adds the style `robundle` for archive
networks.

Every imported network view gets the style named by the property `cy3sbml.visualStyle`
(default `cy3sbml`). To use the dark style for new imports, set the property to
`cy3sbml-dark` in **Edit → Preferences → Properties...** under `cy3sbml`. To change the
style of an existing view, select the style in the **Style** panel of Cytoscape.

![The cy3sbml style on the base network of the repressilator BIOMD0000000012: species as circles, reactions as small squares, activating modifiers as green dashed edges, inhibiting modifiers as red dashed edges](../images/screenshots/cy3sbml-style.png){ width="600" }

## Mappings

The styles map the columns of the [network model](network.md) to visual properties:

| Visual property | Column |
|---|---|
| node label | `label` |
| node shape, size, label font and label position | `sbml type` |
| node fill color | `sbml type ext` (reversible and irreversible reactions have different colors) |
| node border color | `compartmentCode` (one color per compartment, for up to 8 compartments) |
| edge line type and target arrow shape | `interaction type` |
| edge color, source arrow shape and arrow colors | `shared interaction` (activators and inhibitors have their own colors) |

The node label is a passthrough mapping, all other mappings are discrete mappings. You
can change them in the **Style**
panel like in any other style, and save the result in your session.
