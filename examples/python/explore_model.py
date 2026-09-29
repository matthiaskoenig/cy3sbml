"""Read the SBML behind the networks.

- lists the SBML models open in Cytoscape (`cy3sbml networks`),
- gets the SBML document of a network (`cy3sbml document`),
- reads SBML elements by SBML id and of selected nodes (`cy3sbml element`),
- selects the nodes of SBML ids (`cy3sbml nodes` and core `network select`).

```bash
uv run explore_model.py
```
"""

from typing import Any

import py4cytoscape as p4c
from cy3sbml_client import MODELS, check_cytoscape, command, network, network_of_type


def print_element(element: dict[str, Any]) -> None:
    print(
        f"  {element['class']} {element['id']} ({element['name']}),"
        f" SBO {element['sboTerm']}"
    )
    for term in element["cvTerms"]:
        print(f"    {term['qualifier']}: {', '.join(term['resources'])}")


def main() -> None:
    check_cytoscape()
    model = command("import", file=str(MODELS / "unittests" / "core_01.xml"))["models"][
        0
    ]
    base = network_of_type(model, "base")

    for open_model in command("networks")["models"]:
        print(
            f"{open_model['modelId']}:"
            f" SBML L{open_model['level']}V{open_model['version']},"
            f" packages {open_model['packages']},"
            f" {len(open_model['networks'])} networks"
        )

    sbml = command("document", network=network(base))["sbml"]
    print(f"SBML of {model['modelId']}: {len(sbml)} characters")

    # the element of an SBML id
    print("element of the SBML id BLL:")
    for element in command("element", network=network(base), sbmlId="BLL")["elements"]:
        print_element(element)

    # select the nodes of SBML ids, then read the elements of the selected nodes
    nodes = command("nodes", network=network(base), sbmlIds="BLL,IL,AL")["nodes"]
    suids = [suid for node_suids in nodes.values() for suid in node_suids]
    p4c.clear_selection(network=base)
    p4c.select_nodes(suids, by_col="SUID", network=base)
    print("elements of the selected nodes:")
    elements = command("element", network=network(base), nodeList="selected")[
        "elements"
    ]
    for element in elements:
        print_element(element)


if __name__ == "__main__":
    main()
