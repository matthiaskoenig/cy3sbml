"""Search BioModels and import a model.

- searches BioModels (`cy3sbml biomodels search`),
- imports the first result (`cy3sbml import biomodelsId=...`),
- prints the networks of the imported model.

```bash
uv run biomodels_search.py [query]
```
"""

import sys

from cy3sbml_client import check_cytoscape, command


def main(query: str) -> None:
    check_cytoscape()
    result = command("biomodels search", query=query)
    print(f"{result['matches']} models match '{query}':")
    for model in result["models"][:10]:
        print(f"  {model['id']}: {model['name']}")
    if not result["models"]:
        return

    biomodels_id = result["models"][0]["id"]
    model = command("import", biomodelsId=biomodels_id)["models"][0]
    print(f"imported {model['modelId']} ({model['modelName']}):")
    for net in model["networks"]:
        print(f"  {net['type']}: {net['name']} (SUID {net['suid']})")


if __name__ == "__main__":
    main(" ".join(sys.argv[1:]) or "repressilator")
