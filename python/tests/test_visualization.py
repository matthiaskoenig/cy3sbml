"""Verify network targeting and conversion behavior without starting Cytoscape."""

import sys
from pathlib import Path
from unittest.mock import Mock

import pandas as pd
import pytest
from pandas.testing import assert_frame_equal

from cy3sbml import (
    AnnotationBoundedText,
    AnnotationShape,
    AnnotationShapeType,
    AnnotationText,
    add_annotations,
    apply_layout,
    export_image,
    export_png,
    read_layout_xml,
    visualization,
    visualize_antimony,
    visualize_sbml,
)

BASE_URL = "http://localhost:4321/v1"


@pytest.fixture
def api(monkeypatch: pytest.MonkeyPatch) -> Mock:
    api = Mock()
    monkeypatch.setattr(visualization, "p4c", api)
    return api


def test_close_session_is_opt_in_and_runs_before_import(
    monkeypatch: pytest.MonkeyPatch, api: Mock, tmp_path: Path
) -> None:
    api.command.return_value = {
        "models": [{"name": "example", "networks": [{"type": "base", "suid": 7}]}]
    }
    monkeypatch.setattr(visualization, "command", api.command)
    monkeypatch.setattr(visualization, "check_cytoscape", api.check_cytoscape)
    path = tmp_path / "model.xml"

    assert visualize_sbml(path, delete_session=True, base_url=BASE_URL) == 7
    assert [call[0] for call in api.mock_calls] == [
        "check_cytoscape",
        "close_session",
        "command",
        "set_current_view",
    ]
    api.close_session.assert_called_once_with(
        save_before_closing=False, base_url=BASE_URL
    )


def test_visualize_antimony_without_extra(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setitem(sys.modules, "antimony", None)
    with pytest.raises(ImportError, match=r"cy3sbml\[antimony\]"):
        visualize_antimony("invalid")


def test_read_layout_xml(tmp_path: Path) -> None:
    path = tmp_path / "layout.xml"
    path.write_text(
        """<layout xmlns="urn:example">
            <boundingBox id="S1" xpos="-21.5" ypos="40"/>
            <boundingBox id="S2" xpos="20" ypos="6.5"/>
        </layout>""",
        encoding="utf-8",
    )
    expected = pd.DataFrame(
        {"x": [-21.5, 20.0], "y": [40.0, 6.5]}, index=pd.Index(["S1", "S2"], name="id")
    )
    assert_frame_equal(read_layout_xml(path), expected)


@pytest.fixture
def layout() -> pd.DataFrame:
    return pd.DataFrame({"x": [2.5, 6], "y": [3.0, 10]}, index=["S2", "S1"])


def test_apply_layout_maps_ids_and_targets_network(
    api: Mock, layout: pd.DataFrame
) -> None:
    api.get_table_columns.return_value = pd.DataFrame(
        {"sbml id": ["S1", "S2", "S2", None]}, index=[21, 22, 23, 24]
    )
    apply_layout(layout, network=9, base_url=BASE_URL)
    api.set_node_position_bypass.assert_called_once_with(
        [22, 23, 21],
        new_x_locations=[2.5, 2.5, 6.0],
        new_y_locations=[3.0, 3.0, 10.0],
        network=9,
        base_url=BASE_URL,
    )
    api.fit_content.assert_called_once_with(network=9, base_url=BASE_URL)


def test_apply_layout_reports_missing_ids_before_mutating(
    api: Mock, layout: pd.DataFrame
) -> None:
    api.get_table_columns.return_value = pd.DataFrame({"sbml id": ["S1"]}, index=[21])
    with pytest.raises(ValueError, match="absent from the network: S2"):
        apply_layout(layout, network=9)
    api.set_node_position_bypass.assert_not_called()
    api.fit_content.assert_not_called()


def test_all_annotations_and_export_target_the_selected_network(api: Mock) -> None:
    shape = AnnotationShape(AnnotationShapeType.RECTANGLE, 10, 20, 50, 100)
    text = AnnotationText("A", 20, 30)
    bounded = AnnotationBoundedText(
        AnnotationShapeType.ROUND_RECTANGLE, "B", 0, 5, 25, 50
    )
    add_annotations([shape, text, bounded], network=9, base_url=BASE_URL)
    for name in ("shape", "text", "bounded_text"):
        method = getattr(api, f"add_annotation_{name}")
        method.assert_called_once()
        assert method.call_args.kwargs["network"] == 9
        assert method.call_args.kwargs["base_url"] == BASE_URL
    assert api.add_annotation_bounded_text.call_args.kwargs["type"] == "ROUND_RECTANGLE"

    export_image(
        "model.svg",
        "SVG",
        fit_content=True,
        hide_labels=True,
        network=9,
        base_url=BASE_URL,
    )
    api.fit_content.assert_called_once_with(network=9, base_url=BASE_URL)
    assert api.export_image.call_args.kwargs == {
        "filename": "model.svg",
        "type": "SVG",
        "network": 9,
        "base_url": BASE_URL,
        "zoom": 400.0,
        "overwrite_file": True,
        "all_graphics_details": True,
        "hide_labels": True,
    }


def test_export_png_waits_until_two_exports_match(
    api: Mock, monkeypatch: pytest.MonkeyPatch, tmp_path: Path
) -> None:
    contents = iter([b"old view", b"new view", b"new view"])

    def write_image(filename: str, **_kwargs: object) -> None:
        Path(filename).write_bytes(next(contents))

    api.export_image.side_effect = write_image
    monkeypatch.setattr(visualization.time, "sleep", Mock())
    path = tmp_path / "model.png"

    assert export_png(path, 9, base_url=BASE_URL) == path
    assert path.read_bytes() == b"new view"
    assert api.export_image.call_count == 3
    api.export_image.assert_called_with(
        str(path), type="PNG", network=9, base_url=BASE_URL, overwrite_file=True
    )
