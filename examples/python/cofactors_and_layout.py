"""Split cofactor nodes and save and restore node positions.

- splits the nodes of the cofactors ATP, ADP and H+ into one node per edge
  (`cy3sbml cofactors split`),
- saves the positions of the nodes in a layout file (`cy3sbml layout save`),
- applies another layout (core `layout`), then restores the saved positions
  (`cy3sbml layout load`),
- merges the split nodes back (`cy3sbml cofactors merge`).

```bash
uv run cofactors_and_layout.py [output directory]
```
"""

import sys
from pathlib import Path

import py4cytoscape as p4c
from cy3sbml_client import (
    MODELS,
    check_cytoscape,
    command,
    export_png,
    network,
    network_of_type,
)

COFACTORS: str = "M_atp_c,M_adp_c,M_h_c"


def main(output: Path) -> None:
    check_cytoscape()
    output.mkdir(parents=True, exist_ok=True)
    model = command(
        "import", file=str(MODELS / "fbc" / "Mini_textbook_L3V1_fbcV2.xml")
    )["models"][0]
    base = network_of_type(model, "base")

    nodes = command("nodes", network=network(base), sbmlIds=COFACTORS)["nodes"]
    node_list = ",".join(f"SUID:{suid}" for suids in nodes.values() for suid in suids)
    clones = command("cofactors split", network=network(base), nodeList=node_list)[
        "clones"
    ]
    print(f"split {len(nodes)} cofactors into {len(clones)} nodes")
    export_png(output / "cofactors_split.png", base)

    layout_file = output / "mini_textbook_layout.xml"
    saved = command(
        "layout save", network=network(base), file=str(layout_file.resolve())
    )
    print(f"saved {saved['nodes']} positions in {saved['file']}")

    p4c.layout_network("circular", network=base)
    loaded = command(
        "layout load", network=network(base), file=str(layout_file.resolve())
    )
    print(f"restored {loaded['nodes']} positions")

    merged = command("cofactors merge", network=network(base))["merged"]
    print(f"merged {len(merged)} cofactors")


if __name__ == "__main__":
    main(Path(sys.argv[1]) if len(sys.argv) > 1 else Path("results"))
