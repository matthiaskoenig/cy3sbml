"""
Filenames of cobra models.

"""

from __future__ import print_function
import os

if __name__ == "__main__":
    dir = os.path.dirname(os.path.abspath(__file__))

    model_folder = os.path.join(dir, '..', '..', 'src', 'test',
                                'resources', 'models', 'm_model_collection', 'sbml3')
    print(model_folder)
    print(os.path.exists(model_folder))
    # list files in dir
    for f in os.listdir(model_folder):
        print(f)


    s_res = ", ".join(['"{}"'.format(fname) for fname in os.listdir(model_folder)])
    print(s_res)