"""Download the BiGG models into the model corpora of the `models` test suite.

`BiGGTest` reads every SBML file in `src/test/corpora/models/bigg_models`.
See http://bigg.ucsd.edu/data_access for the BiGG web API.

```bash
uv run --project tools python tools/pycysbml/bigg_download.py
```
"""

from pathlib import Path

import requests

BIGG_URL: str = "https://bigg.ucsd.edu"
TARGET_DIR: Path = (
    Path(__file__).parents[2] / "src" / "test" / "corpora" / "models" / "bigg_models"
)
TIMEOUT: float = 60.0


def list_models() -> list[str]:
    """List the ids of all available BiGG models.

    Returns:
        The BiGG ids of the models, e.g. "e_coli_core".
    """
    response = requests.get(f"{BIGG_URL}/api/v2/models", timeout=TIMEOUT)
    response.raise_for_status()
    return [model["bigg_id"] for model in response.json()["results"]]


def download_model(model_id: str, target_dir: Path) -> Path:
    """Download the SBML of a single BiGG model.

    Args:
        model_id: BiGG id of the model.
        target_dir: directory to write the SBML file to.

    Returns:
        The path of the written SBML file.
    """
    url = f"{BIGG_URL}/static/models/{model_id}.xml"
    path = target_dir / f"{model_id}.xml"
    with requests.get(url, stream=True, timeout=TIMEOUT) as response:
        response.raise_for_status()
        with path.open("wb") as f_sbml:
            for chunk in response.iter_content(chunk_size=64 * 1024):
                f_sbml.write(chunk)
    return path


def download_models(model_ids: list[str], target_dir: Path) -> list[Path]:
    """Download the SBML of the given BiGG models.

    Args:
        model_ids: BiGG ids of the models.
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
    download_models(model_ids=list_models(), target_dir=TARGET_DIR)
