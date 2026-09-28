"""Create the SBML test model of the visual styles from an Antimony model.

Writes `src/test/resources/models/styles/graph.xml`, a small model with every
kind of reaction and modifier edge. See the Antimony tutorial for the syntax:

reversibility:
    reversible reaction: '->'
    irreversible reaction: '=>'

modifiers:
    activations '-o'
    inhibitions '-|'
    unknown interactions '-('

```bash
uv run --project tools python tools/pycysbml/graph_to_sbml.py
```
"""

from pathlib import Path

import antimony

TARGET_PATH: Path = (
    Path(__file__).parents[2]
    / "src"
    / "test"
    / "resources"
    / "models"
    / "styles"
    / "graph.xml"
)

ANTIMONY: str = """
model graph()
    // bipartite reaction-species graph
    R1: A + B -> 2 C;
    R2: C => D;
    R3: D -> E;

    // modifiers
    I1: Inh -| R1;
    A1: Act -o R3;
end
"""


def antimony_to_sbml(model: str) -> str:
    """Convert an Antimony model to SBML.

    Args:
        model: Antimony model definition.

    Returns:
        The SBML of the main model.

    Raises:
        ValueError: if the Antimony model cannot be converted.
    """
    antimony.clearPreviousLoads()
    if antimony.loadAntimonyString(model) < 0:
        raise ValueError(antimony.getLastError())
    # the antimony bindings are untyped, narrow their result
    sbml = antimony.getSBMLString(antimony.getMainModuleName())
    if not isinstance(sbml, str) or not sbml:
        raise ValueError(antimony.getLastError())
    return sbml


if __name__ == "__main__":
    TARGET_PATH.write_text(antimony_to_sbml(ANTIMONY))
    print(f"Wrote '{TARGET_PATH}'.")
