from __future__ import annotations

import json
import unittest

from bkci_agent_sdk import (
    AgentApi,
    AgentConfig,
    ApiPath,
    AuthHeader,
    BuildJobType,
    HttpClient,
    RawResponse,
)


class RecordingTransport:
    def __init__(self) -> None:
        self.calls: list[dict] = []

    def __call__(self, **kwargs):
        self.calls.append(kwargs)
        if kwargs["url"].endswith(ApiPath.ASK):
            return RawResponse(
                200,
                json.dumps(
                    {
                        "status": 0,
                        "message": "ok",
                        "agentStatus": "IMPORT_OK",
                        "data": {"heartbeat": None},
                    }
                ),
            )
        return RawResponse(200, '{"status":0,"message":"ok","data":{}}')


class ApiTests(unittest.IsolatedAsyncioTestCase):
    async def asyncSetUp(self) -> None:
        self.transport = RecordingTransport()
        self.config = AgentConfig(
            gateway="gateway.test",
            project_id="project",
            agent_id="agent",
            secret_key="secret",
        )
        self.api = AgentApi(self.config, HttpClient(transport=self.transport))

    async def test_ask_path_headers_and_result(self) -> None:
        result = await self.api.ask(
            {
                "askEnable": {
                    "build": BuildJobType.ALL,
                    "upgrade": True,
                    "dockerDebug": False,
                    "pipeline": False,
                },
                "heartbeat": {},
                "upgrade": None,
            }
        )
        self.assertEqual(result.agent_status, "IMPORT_OK")
        call = self.transport.calls[-1]
        self.assertEqual(call["method"], "POST")
        self.assertEqual(call["url"], "http://gateway.test" + ApiPath.ASK)
        self.assertEqual(call["headers"][AuthHeader.AGENT_ID], "agent")

    async def test_log_adds_build_headers(self) -> None:
        await self.api.add_log_red_line(
            "build", {"message": "failure", "logType": "ERROR"}, "2"
        )
        headers = self.transport.calls[-1]["headers"]
        self.assertEqual(headers[AuthHeader.BUILD_ID], "build")
        self.assertEqual(headers[AuthHeader.VM_SEQ_ID], "2")
        self.assertTrue(self.transport.calls[-1]["url"].endswith(ApiPath.LOG_RED))

    async def test_finish_upgrade_boolean_query_is_lowercase(self) -> None:
        await self.api.finish_upgrade(True)
        self.assertTrue(self.transport.calls[-1]["url"].endswith("?success=true"))

    async def test_invalid_json_reports_context(self) -> None:
        client = HttpClient(transport=lambda **_: RawResponse(502, "not json"))
        with self.assertRaisesRegex(ValueError, "status=502"):
            await client.into_devops_result(method="GET", url="http://invalid.test")


if __name__ == "__main__":
    unittest.main()

