"""Call the automation commands of the cy3sbml Cytoscape app."""

from typing import Any

from cy3sbml._py4cytoscape import DEFAULT_BASE_URL, CyError, p4c


def command(
    name: str,
    *,
    base_url: str = DEFAULT_BASE_URL,
    **arguments: str | int | float | bool,
) -> dict[str, Any]:
    """Send a cy3sbml command with JSON arguments and return its result.

    Raises a py4cytoscape CyError if the command fails.
    """
    result = p4c.cyrest_post(
        f"commands/cy3sbml/{name}", body=arguments, base_url=base_url
    )
    if result["errors"]:
        raise CyError(result["errors"][0]["message"])
    return result["data"]


def network(suid: int) -> str:
    """Format a network SUID for the network argument of a cy3sbml command."""
    return f"SUID:{suid}"


def network_of_type(model: dict[str, Any], network_type: str) -> int:
    """Return the SUID of the first network of the given type in the model."""
    for net in model["networks"]:
        if net["type"] == network_type:
            return int(net["suid"])
    raise ValueError(f"The model {model['name']} has no {network_type} network.")


def check_cytoscape(*, base_url: str = DEFAULT_BASE_URL) -> None:
    """Check whether Cytoscape is reachable and its cy3sbml app is installed."""
    p4c.cytoscape_ping(base_url=base_url)
    names = {app["appName"] for app in p4c.get_installed_apps(base_url=base_url)}
    if "cy3sbml" not in names:
        raise RuntimeError("cy3sbml is not installed in Cytoscape.")
