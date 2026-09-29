"""Call the automation commands of cy3sbml from Python.

The commands of cy3sbml are Cytoscape commands in the namespace `cy3sbml`, which
CyREST offers as `POST /v1/commands/cy3sbml/<command>` with the arguments as JSON
body. `command` calls them with py4cytoscape and returns the JSON result; the
arguments are sent as JSON, so any value works (e.g. an SBML string).

See https://matthiaskoenig.github.io/cy3sbml/guide/automation/ for the commands.
"""

import time
from pathlib import Path
from typing import Any

import py4cytoscape as p4c
from py4cytoscape.exceptions import CyError

# attempts and seconds between them of export_png
EXPORT_ATTEMPTS: int = 20
EXPORT_WAIT: float = 0.5

# the models of the cy3sbml repository, used by the examples
MODELS: Path = (
    Path(__file__).resolve().parents[2] / "src" / "test" / "resources" / "models"
)


def command(name: str, **arguments: str | int | float | bool) -> dict[str, Any]:
    """Runs a cy3sbml command and returns its result.

    >>> command("import", biomodelsId="BIOMD0000000012")
    {'models': [{'rootNetwork': 52, ...}]}

    Raises a `CyError` with the message of cy3sbml if the command fails.
    """
    result = p4c.cyrest_post(f"commands/cy3sbml/{name}", body=arguments)
    if result["errors"]:
        raise CyError(result["errors"][0]["message"])
    return result["data"]


def network(suid: int) -> str:
    """The network argument of a command for a network SUID."""
    return f"SUID:{suid}"


def network_of_type(model: dict[str, Any], network_type: str) -> int:
    """The SUID of the network of the model with the type base/kinetic/all/layout."""
    for net in model["networks"]:
        if net["type"] == network_type:
            return int(net["suid"])
    message = f"The model {model['name']} has no {network_type} network."
    raise ValueError(message)


def export_png(path: Path, network_suid: int) -> Path:
    """Exports a PNG image of the view of the network.

    Cytoscape renders a change of a view (style, layout, bypasses) asynchronously, so
    an image exported right after the change can show the view before it. The image
    is exported until two exports are identical. The path is a path of the computer
    Cytoscape runs on.
    """
    previous = None
    for _ in range(EXPORT_ATTEMPTS):
        p4c.export_image(
            str(path), type="PNG", network=network_suid, overwrite_file=True
        )
        content = path.read_bytes()
        if content == previous:
            break
        previous = content
        time.sleep(EXPORT_WAIT)
    return path


def check_cytoscape() -> None:
    """Fails if Cytoscape with CyREST and cy3sbml is not running."""
    p4c.cytoscape_ping()
    names = {app["appName"] for app in p4c.get_installed_apps()}
    if "cy3sbml" not in names:
        raise RuntimeError("cy3sbml is not installed in Cytoscape.")
