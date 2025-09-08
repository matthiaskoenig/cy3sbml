import os
from pathlib import Path

from bioservices import BioModels
import json
import zipfile as z

thisDir = os.path.dirname(os.path.abspath(__file__))
s = BioModels()


def get_number_of_curated_models() -> int:
    """Figure out how many curated models there are in biomodels right now"""
    all: dict = s.search("*")
    stats: str = all["facetStats"]  # this is a string.
    json_stats: list = json.loads(stats)
    for item in json_stats:
        for val in item["facetValues"]:
            if val["value"] == "Manually curated":
                return int(val["count"])

    raise ValueError("Somethings not quite right")


def download_biomodels(directory: Path, batch_size: int=100) -> None:
    """Downloads SBML models from the curated section of biomodels.

    Biomodels has a strict limit of 100 models per download so in this
    function we have to dance around this fact and compute appropriate
    chunk sizes.

    :param directory: where to put zip files containing num_per_download sbml models
    :param batch_size: How many SBML models to download at a time.
    """

    if batch_size > 100:
        raise ValueError("Maximum number of models that can be downloaded at a time is 100")

    total_models = get_number_of_curated_models()
    num_downloads = int(total_models / batch_size)
    remainder = total_models % batch_size
    if remainder > 0:
        num_downloads += 1

    file_paths: list[Path] = []
    start = 1

    for download_number in range(1, num_downloads + 1):
        if download_number == num_downloads:
            # handle last, which may have remainder
            end = total_models + 1  # account for 0 indexed python, 1 indexed biomodels

        else:
            end = (download_number * batch_size) + 1  # account for 0 indexed python, 1 indexed biomodels

        p = directory / f"Biomodels{start}-{end - 1}.zip"
        file_paths.append(p)

        if os.path.isfile(p):
            os.remove(p)

        biomodels_ids = [f"BIOMD{i:010}" for i in range(start, end)]
        s.search_download(biomodels_ids, output_filename=str(p))
        print(f"Biomodels models from id {start} to {end - 1} saved to {p}")

        start = end


    # consolidate zips
    with z.ZipFile(file_paths[0], 'a') as z1:
        for fname in file_paths[1:]:
            zf = z.ZipFile(fname, 'r')
            for n in zf.namelist():
                z1.writestr(n, zf.open(n).read())


    # rename first zip
    biomodels_zip = os.path.join(directory, "biomodels.zip")
    if not os.path.isfile(biomodels_zip):
        os.rename(file_paths[0], biomodels_zip)


    # try to get rid of the rest. Windows likes to hang on to them though so might fail
    for i in range(1, len(file_paths)):
        try:
            os.remove(file_paths[i])
        except Exception:
            continue

    return file_paths



if __name__ == "__main__":
    biomodels_dir = Path(__file__).parent / "results"
    download_biomodels(biomodels_dir)