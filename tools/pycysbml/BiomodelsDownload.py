import os
import subprocess
import sys
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



def download_biomodels(directory: str, num_per_download=100):

    """downloads sbml models from the curated section of biomodels


    Biomodels has a strict limit of 100 models per download so in this

    function we have to dance around this fact and compute appropriate

    chunk sizes.


    :param directory: (str) where to put zip files containing num_per_download sbml models

    :param num_per_download: How many sbml models to download at a time.

    :return:

    """

    if num_per_download > 100:

        raise ValueError("Maximum number of models that can be downloaded at a time is 100")


    # do index math.

    total_models = get_number_of_curated_models()

    num_downloads = int(total_models / num_per_download)

    remainder = total_models % num_per_download

    if remainder > 0:

        num_downloads += 1


    filenames = []


    start = 1

    for download_number in range(1, num_downloads + 1):

        if download_number == num_downloads:

            # handle last, which may have remainder

            end = total_models + 1  # account for 0 indexed python, 1 indexed biomodels

        else:

            end = (download_number * num_per_download) + 1  # account for 0 indexed python, 1 indexed biomodels

        # do something ...


        fname = os.path.join(directory, f"Biomodels{start}-{end - 1}.zip")

        filenames.append(fname)


        if os.path.isfile(fname):

            os.remove(fname)


        biomodels_ids = [f"BIOMD{i:010}" for i in range(start, end)]


        s.search_download(biomodels_ids, output_filename=fname)

        print(f"Biomodels models from id {start} to {end - 1} saved to {fname}")


        start = end


    # consolidate zips

    with z.ZipFile(filenames[0], 'a') as z1:

        for fname in filenames[1:]:

            zf = z.ZipFile(fname, 'r')

            for n in zf.namelist():

                z1.writestr(n, zf.open(n).read())


    # rename first zip

    biomodels_zip = os.path.join(directory, "biomodels.zip")

    if not os.path.isfile(biomodels_zip):

        os.rename(filenames[0], biomodels_zip)


    # try to get rid of the rest. Windows likes to hang on to them though so might fail

    for i in range(1, len(filenames)):

        try:

            os.remove(filenames[i])

        except Exception:

            continue


    return filenames



if __name__ == "__main__":


    # set to true to actually do the download

    ACTIVATE_DOWNLOAD = False


    # we do this so that we do not need to download biomodels

    # every time the documentation is built.

    if ACTIVATE_DOWNLOAD:

        download_biomodels(os.path.join(os.path.dirname(__file__)))