from __future__ import annotations

import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest


PROJECT_DIR = Path(__file__).resolve().parents[1]
SETUP_SCRIPT = PROJECT_DIR / "scripts" / "setup_release_env.sh"
BUILD_SCRIPT = PROJECT_DIR / "scripts" / "build_dist.sh"
UPLOAD_SCRIPT = PROJECT_DIR / "scripts" / "upload_dist.sh"


FAKE_PYTHON = """#!/usr/bin/env python3
import json
import os
from pathlib import Path
import sys

with Path(os.environ["RELEASE_CALL_LOG"]).open("a", encoding="utf-8") as stream:
    stream.write(json.dumps(sys.argv[1:]) + "\\n")

args = sys.argv[1:]
if args[:2] == ["-m", "build"]:
    output = Path(args[args.index("--outdir") + 1])
    output.mkdir(parents=True, exist_ok=True)
    (output / "bkci_agent_sdk-1.0.0-py3-none-any.whl").write_bytes(b"wheel")
    (output / "bkci_agent_sdk-1.0.0.tar.gz").write_bytes(b"sdist")
"""


FAKE_BASE_PYTHON = '''#!/usr/bin/env python3
import json
import os
from pathlib import Path
import sys

with Path(os.environ["RELEASE_CALL_LOG"]).open("a", encoding="utf-8") as stream:
    stream.write(json.dumps(sys.argv[1:]) + "\\n")

args = sys.argv[1:]
if args[:2] == ["-m", "venv"]:
    venv_python = Path(args[2]) / "bin" / "python"
    venv_python.parent.mkdir(parents=True, exist_ok=True)
    venv_python.write_text("""#!/usr/bin/env python3
import json
import os
from pathlib import Path
import sys
with Path(os.environ[\"RELEASE_CALL_LOG\"]).open(\"a\", encoding=\"utf-8\") as stream:
    stream.write(json.dumps(sys.argv[1:]) + \"\\\\n\")
""", encoding="utf-8")
    venv_python.chmod(0o755)
'''


class ReleaseScriptTests(unittest.TestCase):
    def make_environment(self, directory: str) -> tuple[dict[str, str], Path, Path]:
        root = Path(directory)
        fake_python = root / "python"
        fake_python.write_text(FAKE_PYTHON, encoding="utf-8")
        fake_python.chmod(0o755)
        log_file = root / "calls.jsonl"
        environment = os.environ.copy()
        environment.update(
            {
                "PYTHON_BIN": str(fake_python),
                "DIST_DIR": str(root / "dist"),
                "RELEASE_CALL_LOG": str(log_file),
            }
        )
        return environment, root / "dist", log_file

    def read_calls(self, log_file: Path) -> list[list[str]]:
        return [json.loads(line) for line in log_file.read_text(encoding="utf-8").splitlines()]

    def test_setup_script_creates_an_isolated_release_environment(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            base_python = root / "base-python"
            base_python.write_text(FAKE_BASE_PYTHON, encoding="utf-8")
            base_python.chmod(0o755)
            log_file = root / "calls.jsonl"
            release_venv = root / "release-venv"
            environment = os.environ.copy()
            environment.update(
                {
                    "BASE_PYTHON": str(base_python),
                    "RELEASE_VENV": str(release_venv),
                    "RELEASE_CALL_LOG": str(log_file),
                }
            )
            result = subprocess.run(
                ["bash", str(SETUP_SCRIPT)],
                cwd=PROJECT_DIR,
                env=environment,
                capture_output=True,
                text=True,
                check=False,
            )
            self.assertEqual(result.returncode, 0, result.stderr)
            self.assertTrue((release_venv / "bin" / "python").exists())
            calls = self.read_calls(log_file)
        self.assertIn(["-m", "venv", str(release_venv)], calls)
        install_call = next(call for call in calls if call[:3] == ["-m", "pip", "install"])
        self.assertEqual(install_call[3], "-r")
        self.assertEqual(Path(install_call[4]), PROJECT_DIR / "requirements-release.txt")

    def test_build_script_creates_both_artifacts_and_cleans_old_ones(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            environment, dist_dir, log_file = self.make_environment(directory)
            dist_dir.mkdir()
            old_artifact = dist_dir / "bkci_agent_sdk-old.whl"
            old_artifact.write_bytes(b"old")
            result = subprocess.run(
                ["bash", str(BUILD_SCRIPT)],
                cwd=PROJECT_DIR,
                env=environment,
                capture_output=True,
                text=True,
                check=False,
            )
            self.assertEqual(result.returncode, 0, result.stderr)
            self.assertFalse(old_artifact.exists())
            self.assertTrue((dist_dir / "bkci_agent_sdk-1.0.0-py3-none-any.whl").exists())
            self.assertTrue((dist_dir / "bkci_agent_sdk-1.0.0.tar.gz").exists())
            calls = self.read_calls(log_file)
        self.assertIn(["-c", "import build"], calls)
        self.assertTrue(any(call[:2] == ["-m", "build"] for call in calls))

    def test_upload_script_checks_then_uploads_with_configured_url(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            environment, dist_dir, log_file = self.make_environment(directory)
            dist_dir.mkdir()
            (dist_dir / "bkci_agent_sdk-1.0.0-py3-none-any.whl").write_bytes(b"wheel")
            (dist_dir / "bkci_agent_sdk-1.0.0.tar.gz").write_bytes(b"sdist")
            environment.update(
                {
                    "TWINE_REPOSITORY_URL": "https://repository.example/upload",
                    "TWINE_USERNAME": "ci-user",
                    "TWINE_PASSWORD": "secret-token",
                }
            )
            result = subprocess.run(
                ["bash", str(UPLOAD_SCRIPT)],
                cwd=PROJECT_DIR,
                env=environment,
                capture_output=True,
                text=True,
                check=False,
            )
            self.assertEqual(result.returncode, 0, result.stderr)
            self.assertNotIn("secret-token", result.stdout + result.stderr)
            calls = self.read_calls(log_file)
        check_call = next(call for call in calls if call[:3] == ["-m", "twine", "check"])
        upload_call = next(call for call in calls if call[:3] == ["-m", "twine", "upload"])
        self.assertIn("--strict", check_call)
        self.assertIn("https://repository.example/upload", upload_call)
        self.assertNotIn("secret-token", upload_call)

    def test_upload_requires_ci_configuration(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            environment, _, _ = self.make_environment(directory)
            for key in ("TWINE_REPOSITORY_URL", "TWINE_USERNAME", "TWINE_PASSWORD"):
                environment.pop(key, None)
            result = subprocess.run(
                ["bash", str(UPLOAD_SCRIPT)],
                cwd=PROJECT_DIR,
                env=environment,
                capture_output=True,
                text=True,
                check=False,
            )
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("TWINE_REPOSITORY_URL is required", result.stderr)


if __name__ == "__main__":
    unittest.main()
