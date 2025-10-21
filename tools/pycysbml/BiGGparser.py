"""
Download the available BiGG models via the web interface.
http://bigg.ucsd.edu/web_api
http://bigg.ucsd.edu/data_access
"""
from pathlib import Path
import requests
import zipfile


def list_models() -> list[str]:
    """List all available BiGG models.

    returns list of model ids
    """
    r = requests.get('http://bigg.ucsd.edu/api/v2/models')
    json = r.json()
    model_ids = [model['bigg_id'] for model in json['results']]

    print('-' * 80)
    print("Bigg models")
    print('-' * 80)
    for k, model_id in enumerate(model_ids):
        print(k, model_id)
    print('-' * 80)

    return model_ids


def download_model(model_id: str, target_dir: Path) -> None:
    """Download single BiGG model."""
    url: str = f'http://bigg.ucsd.edu/static/models/{model_id}.xml'
    path: Path = target_dir / f'{model_id}.xml'
    print(url, '->', path)
    r = requests.get(url, stream=True)
    if r.status_code == 200:
        with open(path, 'wb') as f:
            for block in r.iter_content(1024):
                f.write(block)


def download_models(model_ids: list[str], target_dir: Path) -> None:
    """Download all BiGG Models."""
    model_fnames = []
    for k, model_id in enumerate(model_ids):
        print(model_id)
        try:
            download_model(model_id, target_dir=target_dir)
            model_fnames.append(f"{model_id}.xml")
        except zipfile.BadZipfile:
            print("Zip file missing: ", model_id)

    # ---------------------------
    # create the file listing for java
    print(len(model_fnames))
    print('*' * 80)
    print(", \n".join(model_fnames))
    print('*' * 80)


if __name__ == "__main__":
    # target_dir = '/home/mkoenig/cy3sbml/src/test/resources/models/BiGG'
    target_dir = Path('/home/mkoenig/tmp')
    download_models(
        model_ids=list_models(),
        target_dir=target_dir,
    )
