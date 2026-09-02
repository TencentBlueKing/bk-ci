from __future__ import annotations

import hashlib
from email.message import Message
import io
from pathlib import Path
import tempfile
import unittest
from unittest.mock import AsyncMock, patch
from urllib.parse import parse_qs, urlparse

from bkci_agent_sdk import StreamResponse, download_file


def response(status: int, content: bytes = b"", checksum: str = "") -> StreamResponse:
    headers = Message()
    if checksum:
        headers["X-Checksum-Md5"] = checksum
    return StreamResponse(status, headers, io.BytesIO(content))


class DownloadTests(unittest.IsolatedAsyncioTestCase):
    async def test_download_then_etag_not_modified(self) -> None:
        content = b"worker-content"
        digest = hashlib.md5(content).hexdigest()
        request_mock = AsyncMock(
            side_effect=[response(200, content, digest), response(304)]
        )
        with patch("bkci_agent_sdk.download.request_stream", request_mock):
            with tempfile.TemporaryDirectory() as directory:
                save = Path(directory) / "worker-agent.jar"
                first = await download_file(
                    gateway="http://gateway.test",
                    auth_headers={"X-Test": "yes"},
                    server_file="jar/worker-agent.jar",
                    save_path=save,
                )
                second = await download_file(
                    gateway="http://gateway.test",
                    auth_headers={},
                    server_file="jar/worker-agent.jar",
                    save_path=save,
                )
                self.assertEqual(save.read_bytes(), content)
        self.assertFalse(first.not_modified)
        self.assertTrue(second.not_modified)
        self.assertEqual(first.md5, second.md5)
        first_query = parse_qs(urlparse(request_mock.await_args_list[0].kwargs["url"]).query)
        second_query = parse_qs(urlparse(request_mock.await_args_list[1].kwargs["url"]).query)
        self.assertEqual(first_query["file"], ["jar/worker-agent.jar"])
        self.assertEqual(second_query["eTag"], [digest])

    async def test_checksum_failure_does_not_replace_existing_file(self) -> None:
        with patch(
            "bkci_agent_sdk.download.request_stream",
            AsyncMock(return_value=response(200, b"new", "bad")),
        ):
            with tempfile.TemporaryDirectory() as directory:
                save = Path(directory) / "file"
                save.write_bytes(b"old")
                with self.assertRaisesRegex(RuntimeError, "md5 not match"):
                    await download_file(
                        gateway="gateway.test",
                        auth_headers={},
                        server_file="file",
                        save_path=save,
                    )
                self.assertEqual(save.read_bytes(), b"old")


if __name__ == "__main__":
    unittest.main()

