from __future__ import annotations

import asyncio
import json
import unittest

from bkci_agent_sdk import (
    AgentConfig,
    AgentLoop,
    BuildJobType,
    HeartExtra,
    HttpClient,
    RawResponse,
    StartupInfo,
)


class LoopHandler:
    def __init__(self) -> None:
        self.events: list[tuple] = []
        self.upgrading = False
        self.running_job = False
        self.capacity = (True, True)

    def on_startup(self):
        return StartupInfo("host", "10.0.0.1", "linux", "agent-v", "worker-v")

    def collect_heart_extra(self, _context, upgrade_enable):
        return HeartExtra(
            master_version="agent-v",
            slave_version="worker-v",
            host_name="host",
            agent_ip="10.0.0.1",
            agent_install_path="/agent",
            started_user="user",
            props={
                "arch": "x64",
                "jdkVersion": [],
                "dockerInitFileMd5": {"fileMd5": "", "needUpgrade": False},
                "osVersion": "linux",
            },
            upgrade={"workerVersion": "worker-v"} if upgrade_enable else None,
            override={"taskList": [{"buildId": "running"}]},
        )

    def is_upgrading(self): return self.upgrading
    def check_parallel_task_count(self): return self.capacity
    def has_running_job(self): return self.running_job
    def pipeline_enabled(self): return True
    def docker_debug_enabled(self): return True

    async def on_build(self, build): self.events.append(("build", build["buildId"]))
    async def on_upgrade(self, upgrade, has_build): self.events.append(("upgrade", has_build))
    async def on_pipeline(self, pipeline): self.events.append(("pipeline", pipeline["seqId"]))
    async def on_image_debug(self, debug): self.events.append(("debug", debug["debugId"]))
    async def on_heartbeat_resp(self, response): self.events.append(("heartbeat",))
    async def on_agent_deleted(self): self.events.append(("deleted",))
    async def on_ask_resp(self, response): self.events.append(("ask",))


class LoopTransport:
    def __init__(self, *, deleted: bool = False) -> None:
        self.calls: list[dict] = []
        self.deleted = deleted

    def __call__(self, **kwargs):
        self.calls.append(kwargs)
        if kwargs["url"].endswith("/startup"):
            return RawResponse(200, '{"status":0,"message":"ok","data":null}')
        payload = {
            "status": 0,
            "message": "ok",
            "agentStatus": "DELETE" if self.deleted else "IMPORT_OK",
            "data": {
                "heartbeat": {"parallelTaskCount": 4},
                "build": {"buildId": "b1"},
                "upgrade": {"worker": True},
                "pipeline": {"seqId": "p1"},
                "debug": {"debugId": 9},
            },
        }
        return RawResponse(200, json.dumps(payload))


class LoopTests(unittest.IsolatedAsyncioTestCase):
    def make_loop(self, handler, transport):
        return AgentLoop(
            config=AgentConfig(
                gateway="gateway", project_id="p", agent_id="a", secret_key="s"
            ),
            handler=handler,
            http_client=HttpClient(transport=transport),
            interval_seconds=0.01,
            startup_retry_seconds=0.01,
        )

    async def test_dispatch_and_heartbeat_shape(self) -> None:
        handler = LoopHandler()
        transport = LoopTransport()
        loop = self.make_loop(handler, transport)
        await loop._startup()
        await loop._do_ask()
        await asyncio.sleep(0.02)
        self.assertIn(("ask",), handler.events)
        self.assertIn(("heartbeat",), handler.events)
        self.assertIn(("build", "b1"), handler.events)
        self.assertIn(("upgrade", True), handler.events)
        self.assertIn(("pipeline", "p1"), handler.events)
        self.assertIn(("debug", 9), handler.events)
        ask_body = transport.calls[-1]["body"]
        self.assertEqual(ask_body["heartbeat"]["parallelTaskCount"], 4)
        self.assertEqual(ask_body["heartbeat"]["taskList"][0]["buildId"], "running")
        self.assertEqual(ask_body["askEnable"]["build"], BuildJobType.ALL)

    async def test_deleted_agent_hook(self) -> None:
        handler = LoopHandler()
        loop = self.make_loop(handler, LoopTransport(deleted=True))
        await loop._do_ask()
        self.assertIn(("deleted",), handler.events)

    async def test_build_type_matrix_and_upgrade_gate(self) -> None:
        handler = LoopHandler()
        loop = self.make_loop(handler, LoopTransport())
        handler.capacity = (False, False)
        self.assertEqual(loop._check_build_type(), BuildJobType.NONE)
        handler.capacity = (False, True)
        self.assertEqual(loop._check_build_type(), BuildJobType.BINARY)
        handler.capacity = (True, False)
        self.assertEqual(loop._check_build_type(), BuildJobType.DOCKER)
        handler.running_job = True
        self.assertFalse(loop._check_upgrade())

    async def test_stop_interrupts_wait(self) -> None:
        handler = LoopHandler()
        loop = self.make_loop(handler, LoopTransport())
        loop.interval_seconds = 60
        task = asyncio.create_task(loop.run())
        await asyncio.sleep(0.03)
        loop.stop()
        await asyncio.wait_for(task, timeout=1)
        self.assertFalse(loop.running)


if __name__ == "__main__":
    unittest.main()

