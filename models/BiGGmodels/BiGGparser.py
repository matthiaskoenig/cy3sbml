"""
Download the available BiGG models via the web interface.
http://bigg.ucsd.edu/web_api

Implementation with request (Requests: HTTP for Humans)
http://docs.python-requests.org/en/latest/

The web service returns JSON content.

@author: Matthias Koenig
@date: 2015-07-05
"""
from __future__ import print_function
import requests
import zipfile
import StringIO
import os

# --------------------------------
# Example usage of web API
# --------------------------------

# Get a list of BiGG
r = requests.get('http://bigg.ucsd.edu/api/v2/models')
print('Status code:', r.status_code)
print('Headers:', r.headers['content-type'])
print('Encoding:', r.encoding)
print('Text:', r.text)
print('Json:', r.json())

json = r.json()
for k, model in enumerate(json['results']):
    print(k, model['bigg_id'])
    print(k, model['organism'])

# get models in xml
# static models without fbc and Miriam
r1 = requests.get('http://bigg.ucsd.edu/static/dumped_models/e_coli_core.xml')
print(r1.text)

print('#' * 80)

# --------------------------------
# Download all models
# --------------------------------
def download_model(model_id, target_dir, polished=True):
    """
    Download single BiGG model.

    :param model_id: BiGG model id
    :param target_dir: directory for storage
    :param polished: use the polished (FBC and Miriam, zipped) or basic dumped models
    :return:
    """
    if polished:
        # polished models
        # bigg.ucsd.edu/static/polished_models/e_coli_core.xml.zip
        url = 'http://bigg.ucsd.edu/static/polished_models/{}.xml.zip'.format(model_id)
        path = os.path.join(target_dir, '{}.zip'.format(model_id))
    else:
        # dumped models
        url = 'http://bigg.ucsd.edu/static/dumped_models/{}.xml'.format(model_id)
        path = os.path.join(target_dir, '{}.xml'.format(model_id))

    print(url, '->', path)

    if polished:
        print(url)
        r = requests.get(url)
        z = zipfile.ZipFile(StringIO.StringIO(r.content))
        z.extractall(path=target_dir)
    else:
        print(url)
        r = requests.get(url, stream=True)
        if r.status_code == 200:
            with open(path, 'wb') as f:
                for block in r.iter_content(1024):
                    f.write(block)


# -------------------------------------------------------------------
# Get all BiGG models
# -------------------------------------------------------------------
# Usage:
#  - select polished True/False to get polished/dumped models
#  - select target_dir where the xml is stored
# -------------------------------------------------------------------

polished = True
# target_dir = '/home/mkoenig/cy3sbml/src/test/resources/models/BiGG'
target_dir = '/home/mkoenig/tmp'
model_fnames = []

r = requests.get('http://bigg.ucsd.edu/api/v2/models')
for k, model in enumerate(json['results']):
    model_id = model['bigg_id']
    print(model_id)
    try:
        download_model(model_id, target_dir=target_dir, polished=polished)
        model_fnames.append('"{}.xml"'.format(model_id))
    except zipfile.BadZipfile:
        print("Zip file missing: ", model_id)

# ---------------------------
# create the file listing for java
print(len(model_fnames))
java_string = ", ".join(model_fnames)
print(java_string)

