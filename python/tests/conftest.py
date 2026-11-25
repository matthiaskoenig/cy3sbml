"""Prevent tests from accessing any live CyREST session."""

import pytest
from requests.sessions import Session


@pytest.fixture(autouse=True)
def block_http(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.delattr(Session, "request")
