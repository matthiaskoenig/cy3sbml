"""
Use the available cobra test models for testing in cy3sbml.

This script generates a list of filenames for the test cases based on the
cobra models.
Only uses the subset of sbml3 models for testing.
The model collection is available from github via:
git clone https://github.com/opencobra/m_model_collection.git

@author: Matthias Koenig
@date: 2015-07-27
"""

from __future__ import print_function
import os

if __name__ == "__main__":
    folder = os.path.dirname(os.path.abspath(__file__))

    model_folder = os.path.join(folder, '..', '..', 'src', 'test',
                                'resources', 'models', 'm_model_collection', 'sbml3')
    print(model_folder)
    print(os.path.exists(model_folder))
    # list files in dir
    for f in os.listdir(model_folder):
        print(f)

    s_res = ", \n".join(['"{}"'.format(fname) for fname in os.listdir(model_folder)])
    print('*' * 80)
    print(s_res)
    print('*' * 80)
