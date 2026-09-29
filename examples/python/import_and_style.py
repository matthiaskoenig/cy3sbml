"""Import SBML models and export images of their networks.

- imports a model from a file and a model from BioModels (`cy3sbml import`),
- applies the dark cy3sbml style to the base network (core `vizmap apply`),
- exports PNG images of the base and the kinetic network (core `view export`).

```bash
uv run import_and_style.py [output directory]
```
"""

import sys
from pathlib import Path

import py4cytoscape as p4c
from cy3sbml_client import MODELS, check_cytoscape, command, export_png, network_of_type


def main(output: Path) -> None:
    check_cytoscape()
    output.mkdir(parents=True, exist_ok=True)

    # import from a file: one model with its base, kinetic and all network
    result = command(
        "import", file=str(MODELS / "fbc" / "Mini_textbook_L3V1_fbcV2.xml")
    )
    textbook = result["models"][0]
    for net in textbook["networks"]:
        print(
            f"{textbook['modelId']}: {net['type']} network"
            f" {net['name']} ({net['suid']})"
        )

    # import from BioModels
    repressilator = command("import", biomodelsId="BIOMD0000000012")["models"][0]
    print(f"{repressilator['modelId']}: {repressilator['modelName']}")

    # styles and images are core Cytoscape commands
    base = network_of_type(textbook, "base")
    p4c.set_visual_style("cy3sbml-dark", network=base)
    export_png(output / "mini_textbook.png", base)
    kinetic = network_of_type(repressilator, "kinetic")
    export_png(output / "repressilator_kinetic.png", kinetic)
    print(f"images written to {output}")


if __name__ == "__main__":
    main(Path(sys.argv[1]) if len(sys.argv) > 1 else Path("results"))
