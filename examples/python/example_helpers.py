"""Model paths for the Python examples."""

from pathlib import Path

# the models of the cy3sbml repository, used by the examples
MODELS: Path = (
    Path(__file__).resolve().parents[2] / "src" / "test" / "resources" / "models"
)
