from __future__ import annotations

import hashlib
from pathlib import Path
import tempfile
import unittest
from unittest.mock import AsyncMock, patch

from bkci_agent_sdk import DefaultWorkerJarManager, DownloadResult


class WorkerUpgradeTests(unittest.IsolatedAsyncioTestCase):
    async def test_initialize_uses_valid_active_worker(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            worker = Path(directory) / "worker-agent.jar"
            worker.write_bytes(b"active")
            manager = DefaultWorkerJarManager(
                gateway="gateway",
                auth_headers={},
                work_dir=directory,
                jdk17_path="java",
            )
            with patch(
                "bkci_agent_sdk.worker_upgrade.detect_worker_version",
                AsyncMock(return_value="v1.2.3"),
            ):
                state = await manager.initialize()
        self.assertEqual(state.version, "v1.2.3")
        self.assertEqual(state.md5, hashlib.md5(b"active").hexdigest())
        self.assertEqual(manager.get_version(), "v1.2.3")

    async def test_upgrade_validates_then_replaces_active_worker(self) -> None:
        content = b"candidate"
        digest = hashlib.md5(content).hexdigest()

        async def fake_download(_gateway, _headers, directory, _timeout):
            (Path(directory) / "worker-agent.jar").write_bytes(content)
            return DownloadResult(digest, False)

        with tempfile.TemporaryDirectory() as directory:
            active = Path(directory) / "worker-agent.jar"
            active.write_bytes(b"old")
            manager = DefaultWorkerJarManager(
                gateway="gateway",
                auth_headers={},
                work_dir=directory,
                jdk17_path="java",
            )
            with (
                patch(
                    "bkci_agent_sdk.worker_upgrade.download_worker_jar",
                    side_effect=fake_download,
                ),
                patch(
                    "bkci_agent_sdk.worker_upgrade.detect_worker_version",
                    AsyncMock(return_value="v2.0.0"),
                ),
            ):
                result = await manager.upgrade()
            self.assertEqual(active.read_bytes(), content)
        self.assertTrue(result.changed)
        self.assertEqual(result.md5, digest)
        self.assertEqual(result.version, "v2.0.0")


if __name__ == "__main__":
    unittest.main()
