"""Set the location of py4cytoscape's file logger before it opens its log."""

import os

from platformdirs import user_log_dir

os.environ.setdefault("PY4CYTOSCAPE_DETAIL_LOGGER_DIR", user_log_dir("cy3sbml"))

import py4cytoscape as p4c
from py4cytoscape.exceptions import CyError
from py4cytoscape.py4cytoscape_utils import DEFAULT_BASE_URL

__all__ = ["DEFAULT_BASE_URL", "CyError", "p4c"]
