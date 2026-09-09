from __future__ import annotations

from pathlib import Path
import tempfile
import unittest

from bkci_agent_sdk import DefaultBuildRunner, DevopsResult


class FakeApi:
    def __init__(self) -> None:
        self.finished = []

    async def worker_build_finish(self, body):
        self.finished.append(body)
        return DevopsResult(data=None, status=0, message="ok")


class BuildRunnerTests(unittest.IsolatedAsyncioTestCase):
    async def test_tracks_task_and_reports_missing_worker(self) -> None:
        api = FakeApi()
        with tempfile.TemporaryDirectory() as directory:
            runner = DefaultBuildRunner(
                api=api,
                work_dir=directory,
                worker_jar_path=Path(directory) / "missing.jar",
                gateway="gateway",
                project_id="project",
                agent_version="agent",
                worker_version="worker",
                finish_delay_seconds=0,
            )
            await runner.on_build(
                {
                    "projectId": "project",
                    "buildId": "build",
                    "vmSeqId": "1",
                    "workspace": "/workspace",
                    "pipelineId": "pipeline",
                }
            )
            self.assertFalse(runner.has_running_job())
            self.assertEqual(runner.get_task_list(), [])
        self.assertEqual(len(api.finished), 1)
        self.assertFalse(api.finished[0]["success"])
        self.assertIn("worker jar missing", api.finished[0]["message"])

    def test_zero_parallel_count_means_unlimited(self) -> None:
        runner = DefaultBuildRunner(
            api=FakeApi(),
            work_dir=".",
            worker_jar_path="worker.jar",
            gateway="gateway",
            project_id="project",
            agent_version="agent",
            worker_version="worker",
            parallel_task_count=0,
            docker_parallel_task_count=0,
        )
        self.assertEqual(runner.check_parallel_task_count(), (True, True))


if __name__ == "__main__":
    unittest.main()

