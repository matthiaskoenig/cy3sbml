"""Map data with SBML ids onto the nodes.

Data from other tools (simulations, flux balance analysis, measurements) have
SBML ids. `cy3sbml nodes` returns the nodes of every SBML id of a network; the
data table joined on the column `sbml id` (core table import) and a style
mapping show the data on the nodes; the node SUIDs of the SBML ids address the
nodes directly. Here: a flux distribution of the glycolysis of the fbc model
mini_textbook, the absolute flux as size of the reactions (table import and
style mapping), the direction as color (bypasses of the node SUIDs).

```bash
uv run map_data.py [output directory]
```
"""

import sys
from pathlib import Path

import pandas as pd
import py4cytoscape as p4c
from cy3sbml_client import (
    MODELS,
    check_cytoscape,
    command,
    export_png,
    network,
    network_of_type,
)

# fluxes of the reactions (mmol/gDW/h), e.g. the result of a flux balance analysis
FLUXES: dict[str, float] = {
    "R_EX_glc__D_e": -10.0,
    "R_GLCpts": 10.0,
    "R_PGI": 10.0,
    "R_PFK": 10.0,
    "R_FBA": 10.0,
    "R_TPI": 10.0,
    "R_GAPD": 20.0,
    "R_PGK": -20.0,
    "R_PGM": -20.0,
    "R_ENO": 20.0,
    "R_PYK": 10.0,
    "R_LDH_D": -20.0,
    "R_D_LACt2": -20.0,
    "R_EX_lac__D_e": 20.0,
    "R_ATPM": 20.0,
}
STYLE: str = "cy3sbml-fluxes"
FORWARD: str = "#B2182B"
BACKWARD: str = "#2166AC"


def main(output: Path) -> None:
    check_cytoscape()
    output.mkdir(parents=True, exist_ok=True)
    model = command(
        "import", file=str(MODELS / "fbc" / "Mini_textbook_L3V1_fbcV2.xml")
    )["models"][0]
    base = network_of_type(model, "base")

    # the SBML ids of the data which are nodes of the network
    nodes = command("nodes", network=network(base), sbmlIds=",".join(FLUXES))["nodes"]
    missing = [sbml_id for sbml_id, suids in nodes.items() if not suids]
    print(
        f"{len(FLUXES) - len(missing)} of {len(FLUXES)} reactions are nodes,"
        f" missing: {missing}"
    )

    # 1. the data as node table column, joined on the SBML id, and a style mapping:
    #    a copy of the cy3sbml style with the absolute flux as size of the reactions
    data = pd.DataFrame({"sbml id": list(FLUXES), "flux": list(FLUXES.values())})
    data["abs flux"] = data["flux"].abs()
    p4c.load_table_data(
        data, data_key_column="sbml id", table_key_column="sbml id", network=base
    )
    if STYLE in p4c.get_visual_style_names():
        # a copy of an earlier run
        p4c.delete_visual_style(STYLE)
    p4c.copy_visual_style("cy3sbml", STYLE)
    p4c.set_node_size_mapping("abs flux", [0.0, 20.0], [15, 45], style_name=STYLE)
    p4c.set_visual_style(STYLE, network=base)

    # 2. the node SUIDs of the SBML ids: the direction of the flux as color of the
    #    reaction nodes (bypasses; the style keeps the colors of the other nodes)
    suids = [suid for sbml_id in FLUXES for suid in nodes[sbml_id]]
    colors = [
        FORWARD if FLUXES[sbml_id] >= 0 else BACKWARD
        for sbml_id in FLUXES
        for _ in nodes[sbml_id]
    ]
    p4c.set_node_color_bypass(suids, colors, network=base)
    export_png(output / "mini_textbook_fluxes.png", base)
    print(f"image written to {output}")


if __name__ == "__main__":
    main(Path(sys.argv[1]) if len(sys.argv) > 1 else Path("results"))
