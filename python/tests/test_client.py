"""Exercise py4cytoscape against recorded CyREST response shapes."""

import json
from collections.abc import Callable
from pathlib import Path

import py4cytoscape as p4c
import pytest
from py4cytoscape.exceptions import CyError
from requests import Response
from requests.exceptions import RequestException

from cy3sbml import command, visualize_antimony, visualize_sbml

BASE_URL = "http://localhost:4321/v1"
IMPORT_PATH = "/commands/cy3sbml/import"
VIEW_PATH = "/commands/view/set%20current"
Requests = list[tuple[str, str, dict[str, str] | None]]
CyRestStub = Callable[[dict[tuple[str, str], object]], Requests]


@pytest.fixture
def cyrest(monkeypatch: pytest.MonkeyPatch) -> CyRestStub:
    def install(responses: dict[tuple[str, str], object]) -> Requests:
        calls: Requests = []

        def request(method: str, url: str, **kwargs: object) -> Response:
            assert url.startswith(BASE_URL + "/")
            path = url.removeprefix(BASE_URL)
            body = kwargs.get("json")
            assert body is None or isinstance(body, dict)
            calls.append((method, path, body))
            payload = responses[method, path]
            if isinstance(payload, Exception):
                raise payload
            response = Response()
            response.status_code = 200
            response._content = json.dumps(payload).encode()
            return response

        monkeypatch.setattr(p4c.commands, "_do_request", request)
        return calls

    return install


def import_responses() -> dict[tuple[str, str], object]:
    return {
        ("POST", IMPORT_PATH): {
            "data": {
                "models": [
                    {
                        "rootNetwork": 50,
                        "modelId": "example",
                        "name": "example_1",
                        "modelName": "",
                        "networks": [
                            {"suid": 70, "name": "example_1__all", "type": "all"},
                            {"suid": 71, "name": "example_1", "type": "base"},
                            {
                                "suid": 72,
                                "name": "example_1__kinetic",
                                "type": "kinetic",
                            },
                        ],
                    },
                    {
                        "rootNetwork": 100,
                        "name": "otherModel",
                        "networks": [
                            {"suid": 120, "name": "otherModel", "type": "base"}
                        ],
                    },
                ],
            },
            "errors": [],
        },
        ("GET", "/networks"): [70, 71, 72, 120],
        ("GET", "/networks/71/views"): [171],
        ("POST", VIEW_PATH): {"data": {}, "errors": []},
    }


def test_visualize_sbml_selects_first_models_base_view(
    cyrest: CyRestStub,
    monkeypatch: pytest.MonkeyPatch,
    tmp_path: Path,
) -> None:
    monkeypatch.chdir(tmp_path)
    path = Path("model with spaces.xml")
    calls = cyrest(import_responses())

    assert visualize_sbml(path, base_url=BASE_URL) == 71
    assert calls == [
        ("POST", IMPORT_PATH, {"file": str(tmp_path / path)}),
        ("GET", "/networks", None),
        ("GET", "/networks", None),
        ("GET", "/networks/71/views", None),
        ("POST", VIEW_PATH, {"view": "SUID:171"}),
    ]


@pytest.mark.parametrize("read_file", [False, True])
def test_visualize_antimony_sends_converted_sbml(
    cyrest: CyRestStub, tmp_path: Path, read_file: bool
) -> None:
    pytest.importorskip("antimony")
    source = "J0: S1 -> S2; k1*S1; S1 = 10; S2 = 0; k1 = 0.1"
    path = tmp_path / "model.ant"
    path.write_text(source, encoding="utf-8")
    calls = cyrest(import_responses())

    assert visualize_antimony(path if read_file else source, base_url=BASE_URL) == 71
    method, operation, body = calls[0]
    assert (method, operation) == ("POST", IMPORT_PATH)
    assert body is not None
    assert set(body) == {"sbml"}
    assert '<species id="S1"' in body["sbml"]
    assert '<reaction id="J0"' in body["sbml"]


@pytest.mark.parametrize(
    ("response", "message"),
    [
        (
            {"data": {}, "errors": [{"message": "Invalid SBML level"}]},
            "Invalid SBML level",
        ),
        ({"data": {"models": []}, "errors": []}, "No SBML models were imported"),
        (RequestException("Cytoscape is unavailable"), "Cytoscape is unavailable"),
    ],
)
def test_visualize_logs_cyrest_failures(
    cyrest: CyRestStub,
    caplog: pytest.LogCaptureFixture,
    response: object,
    message: str,
) -> None:
    calls = cyrest({("POST", IMPORT_PATH): response})

    assert visualize_sbml("model.xml", base_url=BASE_URL) is None
    assert message in caplog.text
    assert len(calls) == 1


def test_command_raises_cyrest_error(cyrest: CyRestStub) -> None:
    cyrest(
        {("POST", IMPORT_PATH): {"data": {}, "errors": [{"message": "Invalid SBML"}]}}
    )

    with pytest.raises(CyError, match="Invalid SBML"):
        command("import", sbml="invalid", base_url=BASE_URL)
