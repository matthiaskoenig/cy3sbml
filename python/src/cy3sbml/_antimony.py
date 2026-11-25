"""Convert Antimony using its process-wide model registry."""

from pathlib import Path
from threading import Lock

_lock = Lock()


def antimony_to_sbml(source: str | Path) -> str:
    """Convert Antimony text or a file to an SBML string."""
    try:
        import antimony
    except ModuleNotFoundError as exc:
        if exc.name != "antimony":
            raise
        raise ImportError(
            "Antimony support requires `pip install 'cy3sbml[antimony]'`."
        ) from exc

    path = Path(source)
    try:
        is_file = path.is_file()
    except OSError:
        if isinstance(source, Path):
            raise
        is_file = False

    if isinstance(source, Path) and not is_file:
        raise FileNotFoundError(f"Antimony file does not exist: {source}")

    # Loading changes the active model. Keep loading, extraction and cleanup together.
    with _lock:
        antimony.clearPreviousLoads()
        try:
            if is_file:
                status = antimony.loadAntimonyFile(str(path))
            else:
                status = antimony.loadAntimonyString(str(source))
            if status == -1:
                raise ValueError(f"Antimony error: {antimony.getLastError()}")
            return str(antimony.getSBMLString())
        finally:
            antimony.clearPreviousLoads()
