"""Visualize SBML models, place nodes and annotations, and export images."""

import logging
import time
from collections.abc import Iterable
from dataclasses import asdict, dataclass
from enum import StrEnum
from math import isfinite
from pathlib import Path, PurePath, PurePosixPath, PureWindowsPath
from xml.etree import ElementTree

import pandas as pd
from requests.exceptions import RequestException

from cy3sbml._antimony import antimony_to_sbml
from cy3sbml._py4cytoscape import DEFAULT_BASE_URL, CyError, p4c
from cy3sbml.client import check_cytoscape, command, network_of_type

logger = logging.getLogger(__name__)


def visualize_sbml(
    sbml_path: str | PurePath,
    delete_session: bool = False,
    *,
    base_url: str = DEFAULT_BASE_URL,
) -> int | None:
    """Import a model file visible to Cytoscape and show its base network.

    Return the base network SUID, or log a warning and return None if Cytoscape
    cannot import or display the model. `delete_session=True` closes the current
    session without saving before import.

    Absolute POSIX and Windows paths are passed to Cytoscape unchanged.
    Relative paths are resolved against Python's current directory.
    """
    filename = str(sbml_path)
    if not (
        PurePosixPath(filename).is_absolute() or PureWindowsPath(filename).is_absolute()
    ):
        filename = str(Path(filename).absolute())
    return _visualize(delete_session, base_url, file=filename)


def visualize_antimony(
    source: str | Path,
    delete_session: bool = False,
    *,
    base_url: str = DEFAULT_BASE_URL,
) -> int | None:
    """Convert Antimony text or a local file to SBML and show its base network.

    Use a Path for a filename that must exist. An existing filename passed as a
    string is also accepted. Requires the `antimony` extra. The return value and
    `delete_session` behavior match `visualize_sbml`.
    """
    return _visualize(delete_session, base_url, sbml=antimony_to_sbml(source))


def _visualize(
    delete_session: bool,
    base_url: str,
    **source: str,
) -> int | None:
    try:
        if delete_session:
            check_cytoscape(base_url=base_url)
            p4c.close_session(save_before_closing=False, base_url=base_url)
        models = command("import", base_url=base_url, **source)["models"]
        if not models:
            raise CyError("No SBML models were imported.")
        suid = network_of_type(models[0], "base")
        p4c.set_current_view(network=suid, base_url=base_url)
    except (CyError, RequestException, RuntimeError, ValueError) as exc:
        logger.warning("Cytoscape could not visualize the model: %s", exc)
        return None
    return suid


def read_layout_xml(xml_path: str | Path) -> pd.DataFrame:
    """Read XML boundingBox coordinates as a DataFrame indexed by SBML ID."""
    root = ElementTree.parse(xml_path).getroot()
    boxes = (box for box in root.iter() if box.tag.rsplit("}", 1)[-1] == "boundingBox")
    positions = [
        (box.attrib["id"], float(box.attrib["xpos"]), float(box.attrib["ypos"]))
        for box in boxes
    ]
    return pd.DataFrame(positions, columns=["id", "x", "y"]).set_index("id")


def apply_layout(
    layout: pd.DataFrame,
    network: int | None = None,
    *,
    base_url: str = DEFAULT_BASE_URL,
) -> None:
    """Set x/y positions by SBML ID and fit the selected network to its view.

    The layout must give unique IDs. Nodes representing the same SBML element,
    such as aliases in layout networks, receive the same coordinates.
    """
    if layout.empty:
        return
    if not layout.index.is_unique:
        raise ValueError("Layout IDs must be unique.")

    coordinates = layout[["x", "y"]].astype(float)
    if any(not isfinite(value) for value in coordinates.to_numpy().flat):
        raise ValueError("Layout coordinates must be finite.")

    nodes = p4c.get_table_columns(
        columns=["sbml id"], network=network, base_url=base_url
    )
    if "sbml id" not in nodes:
        raise ValueError("The network has no 'sbml id' column.")
    suids_by_id: dict[str, list[int]] = {}
    for suid, sbml_id in nodes["sbml id"].items():
        if isinstance(sbml_id, str):
            suids_by_id.setdefault(sbml_id, []).append(int(suid))

    missing = [sbml_id for sbml_id in coordinates.index if sbml_id not in suids_by_id]
    if missing:
        raise ValueError(
            f"SBML IDs absent from the network: {', '.join(map(str, missing))}"
        )

    suids = []
    x_values = []
    y_values = []
    for sbml_id, x, y in coordinates.itertuples(name=None):
        for suid in suids_by_id[sbml_id]:
            suids.append(suid)
            x_values.append(x)
            y_values.append(y)
    p4c.set_node_position_bypass(
        suids,
        new_x_locations=x_values,
        new_y_locations=y_values,
        network=network,
        base_url=base_url,
    )
    p4c.fit_content(network=network, base_url=base_url)


class AnnotationShapeType(StrEnum):
    """Supported shapes of Cytoscape annotations."""

    RECTANGLE = "RECTANGLE"
    ROUND_RECTANGLE = "ROUND_RECTANGLE"


@dataclass
class AnnotationShape:
    """Shape annotation on the Cytoscape canvas."""

    type: AnnotationShapeType
    x_pos: int
    y_pos: int
    height: int
    width: int
    fill_color: str = "#000000"
    opacity: int = 100
    border_thickness: int = 1
    border_color: str = "#FFFFFF"
    border_opacity: int = 100
    canvas: str = "background"
    z_order: int = 0


@dataclass
class AnnotationText:
    """Text annotation on the Cytoscape canvas."""

    text: str
    x_pos: int
    y_pos: int
    font_size: int = 12
    font_family: str = "Arial"
    font_style: str = "bold"
    color: str = "#000000"
    angle: float = 0
    canvas: str = "background"


@dataclass
class AnnotationBoundedText:
    """Text annotation inside a shape on the Cytoscape canvas."""

    type: AnnotationShapeType
    text: str
    x_pos: int
    y_pos: int
    height: int
    width: int
    fill_color: str = "#000000"
    opacity: int = 100
    border_thickness: int = 1
    border_color: str = "#FFFFFF"
    border_opacity: int = 100
    font_size: int = 12
    font_family: str = "Arial"
    font_style: str = "bold"
    color: str = "#000000"
    angle: float = 0
    canvas: str = "background"


def add_annotations(
    annotations: Iterable[AnnotationShape | AnnotationText | AnnotationBoundedText],
    network: int | None = None,
    *,
    base_url: str = DEFAULT_BASE_URL,
) -> None:
    """Add shape, text and bounded text annotations to the selected network."""
    for annotation in annotations:
        if isinstance(annotation, AnnotationShape):
            add = p4c.add_annotation_shape
        elif isinstance(annotation, AnnotationText):
            add = p4c.add_annotation_text
        else:
            add = p4c.add_annotation_bounded_text
        add(network=network, base_url=base_url, **asdict(annotation))


def export_image(
    image_path: str | Path,
    format: str = "PNG",  # noqa: A002 - public keyword used by sbmlutils
    fit_content: bool = False,
    hide_labels: bool = False,
    *,
    network: int | None = None,
    base_url: str = DEFAULT_BASE_URL,
) -> None:
    """Export a view to PNG, JPEG, PDF, SVG or PostScript on the Cytoscape host."""
    if fit_content:
        p4c.fit_content(network=network, base_url=base_url)
    p4c.export_image(
        filename=str(image_path),
        type=format,
        network=network,
        base_url=base_url,
        zoom=400.0,
        overwrite_file=True,
        all_graphics_details=True,
        hide_labels=hide_labels,
    )


def export_png(
    path: str | Path,
    network_suid: int,
    *,
    base_url: str = DEFAULT_BASE_URL,
) -> Path:
    """Export up to 20 times and return the path of the PNG Cytoscape wrote.

    Stop when two consecutive PNGs match because Cytoscape draws changes to
    views asynchronously. Python must be able to read the exported file.
    """
    output = Path(path)
    filename = str(output)
    previous = None
    for _ in range(20):
        result = p4c.export_image(
            filename,
            type="PNG",
            network=network_suid,
            base_url=base_url,
            overwrite_file=True,
        )
        output = Path(result["file"])
        content = output.read_bytes()
        if content == previous:
            break
        previous = content
        time.sleep(0.5)
    return output
