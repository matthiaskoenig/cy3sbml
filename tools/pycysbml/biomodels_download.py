"""Download the curated BioModels into the test resources of the `models` suite.

`BioModelsTest` reads every SBML file in `src/test/resources/models/biomodels`.
Uses the same BioModels REST API as `org.cy3sbml.biomodel.BiomodelsQuery`, see
https://www.biomodels.org/docs/.

```bash
uv run --project tools python tools/pycysbml/biomodels_download.py
```
"""

from pathlib import Path

import requests

BIOMODELS_URL: str = "https://www.biomodels.org"
CURATED_QUERY: str = 'curationstatus:"Manually curated"'
TARGET_DIR: Path = (
    Path(__file__).parents[2] / "src" / "test" / "resources" / "models" / "biomodels"
)
# BioModels returns at most 100 results per search page
PAGE_SIZE: int = 100
TIMEOUT: float = 60.0


def list_curated_models() -> list[str]:
    """List the ids of all manually curated BioModels.

    Returns:
        The BioModels ids, e.g. "BIOMD0000000001", in ascending order.
    """
    model_ids: list[str] = []
    matches: int | None = None
    while matches is None or len(model_ids) < matches:
        response = requests.get(
            f"{BIOMODELS_URL}/search",
            params={
                "query": CURATED_QUERY,
                "offset": len(model_ids),
                "numResults": PAGE_SIZE,
                "sort": "id-asc",
                "format": "json",
            },
            timeout=TIMEOUT,
        )
        response.raise_for_status()
        result = response.json()
        matches = int(result["matches"])
        page = [model["id"] for model in result["models"]]
        if not page:
            break
        model_ids.extend(page)
    return model_ids


def download_model(model_id: str, target_dir: Path) -> Path:
    """Download the SBML of a single BioModel.

    Args:
        model_id: BioModels id of the model.
        target_dir: directory to write the SBML file to.

    Returns:
        The path of the written SBML file.
    """
    path = target_dir / f"{model_id}.xml"
    with requests.get(
        f"{BIOMODELS_URL}/model/download/{model_id}",
        params={"filename": f"{model_id}_url.xml"},
        stream=True,
        timeout=TIMEOUT,
    ) as response:
        response.raise_for_status()
        with path.open("wb") as f_sbml:
            for chunk in response.iter_content(chunk_size=64 * 1024):
                f_sbml.write(chunk)
    return path


def download_models(model_ids: list[str], target_dir: Path) -> list[Path]:
    """Download the SBML of the given BioModels.

    Args:
        model_ids: BioModels ids of the models.
        target_dir: directory to write the SBML files to.

    Returns:
        The paths of the written SBML files.
    """
    target_dir.mkdir(parents=True, exist_ok=True)
    paths: list[Path] = []
    for k, model_id in enumerate(model_ids, start=1):
        path = download_model(model_id, target_dir=target_dir)
        print(f"[{k}/{len(model_ids)}] {model_id} -> {path}")
        paths.append(path)
    return paths


if __name__ == "__main__":
    download_models(model_ids=list_curated_models(), target_dir=TARGET_DIR)
