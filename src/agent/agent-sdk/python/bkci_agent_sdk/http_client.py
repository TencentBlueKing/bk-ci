"""Small dependency-free asynchronous HTTP client used by the SDK."""

from __future__ import annotations

import asyncio
import json
from dataclasses import asdict, dataclass, is_dataclass
from enum import Enum
from http.client import HTTPMessage
from typing import Any, BinaryIO, Callable, Generic, Mapping, TypeVar
from urllib.error import HTTPError
from urllib.request import Request, urlopen

from .config import AgentStatus

T = TypeVar("T")


@dataclass(slots=True)
class DevopsResult(Generic[T]):
    data: T | None = None
    status: int = -1
    message: str = ""


@dataclass(slots=True)
class AgentResult(DevopsResult[T]):
    agent_status: str = ""


@dataclass(slots=True)
class RawResponse:
    status: int
    body: str


@dataclass(slots=True)
class StreamResponse:
    status: int
    headers: HTTPMessage
    stream: BinaryIO


RawTransport = Callable[..., RawResponse]


def is_ok(result: DevopsResult[Any]) -> bool:
    return result.status == 0


def is_not_ok(result: DevopsResult[Any]) -> bool:
    return result.status != 0


def is_agent_delete(result: AgentResult[Any]) -> bool:
    return bool(result.agent_status) and result.agent_status == AgentStatus.DELETE


def _json_value(value: Any) -> Any:
    if isinstance(value, Enum):
        return value.value
    if is_dataclass(value) and not isinstance(value, type):
        return _json_value(asdict(value))
    if isinstance(value, Mapping):
        return {str(key): _json_value(item) for key, item in value.items()}
    if isinstance(value, (list, tuple)):
        return [_json_value(item) for item in value]
    return value


def _request_sync(
    method: str,
    url: str,
    headers: Mapping[str, str] | None,
    body: Any,
    timeout_seconds: float,
) -> RawResponse:
    request_headers = dict(headers or {})
    payload: bytes | None = None
    if body is not None:
        payload = json.dumps(
            _json_value(body), ensure_ascii=False, separators=(",", ":")
        ).encode("utf-8")
        request_headers["Content-Type"] = "application/json"
        request_headers["Content-Length"] = str(len(payload))
    request = Request(url, data=payload, headers=request_headers, method=method)
    try:
        with urlopen(request, timeout=timeout_seconds) as response:
            return RawResponse(
                status=int(response.status),
                body=response.read().decode("utf-8", errors="replace"),
            )
    except HTTPError as error:
        return RawResponse(
            status=int(error.code),
            body=error.read().decode("utf-8", errors="replace"),
        )


def _request_stream_sync(
    method: str,
    url: str,
    headers: Mapping[str, str] | None,
    timeout_seconds: float,
) -> StreamResponse:
    request = Request(url, headers=dict(headers or {}), method=method)
    try:
        response = urlopen(request, timeout=timeout_seconds)
        return StreamResponse(int(response.status), response.headers, response)
    except HTTPError as error:
        return StreamResponse(int(error.code), error.headers, error)


async def request(
    *,
    method: str,
    url: str,
    headers: Mapping[str, str] | None = None,
    body: Any = None,
    timeout_seconds: float = 30,
) -> RawResponse:
    return await asyncio.to_thread(
        _request_sync, method, url, headers, body, timeout_seconds
    )


async def request_stream(
    *,
    method: str,
    url: str,
    headers: Mapping[str, str] | None = None,
    timeout_seconds: float = 300,
) -> StreamResponse:
    return await asyncio.to_thread(
        _request_stream_sync, method, url, headers, timeout_seconds
    )


class HttpClient:
    """HTTP client with a default timeout and BK-CI result decoders.

    ``transport`` is an optional synchronous test adapter with the same keyword arguments as
    :func:`_request_sync`.
    """

    def __init__(
        self,
        *,
        timeout_seconds: float = 30,
        transport: RawTransport | None = None,
    ) -> None:
        self.timeout_seconds = timeout_seconds
        self._transport = transport

    async def request_raw(
        self,
        *,
        method: str,
        url: str,
        headers: Mapping[str, str] | None = None,
        body: Any = None,
    ) -> RawResponse:
        if self._transport is not None:
            return await asyncio.to_thread(
                self._transport,
                method=method,
                url=url,
                headers=headers,
                body=body,
                timeout_seconds=self.timeout_seconds,
            )
        return await request(
            method=method,
            url=url,
            headers=headers,
            body=body,
            timeout_seconds=self.timeout_seconds,
        )

    async def into_devops_result(
        self,
        *,
        method: str,
        url: str,
        headers: Mapping[str, str] | None = None,
        body: Any = None,
    ) -> DevopsResult[Any]:
        raw = await self.request_raw(
            method=method, url=url, headers=headers, body=body
        )
        parsed = _parse_result(raw, url)
        return DevopsResult(
            data=parsed.get("data"),
            status=int(parsed.get("status", -1)),
            message=str(parsed.get("message", "")),
        )

    async def into_agent_result(
        self,
        *,
        method: str,
        url: str,
        headers: Mapping[str, str] | None = None,
        body: Any = None,
    ) -> AgentResult[Any]:
        raw = await self.request_raw(
            method=method, url=url, headers=headers, body=body
        )
        parsed = _parse_result(raw, url)
        return AgentResult(
            data=parsed.get("data"),
            status=int(parsed.get("status", -1)),
            message=str(parsed.get("message", "")),
            agent_status=str(parsed.get("agentStatus", "")),
        )


def _parse_result(raw: RawResponse, url: str) -> dict[str, Any]:
    try:
        value = json.loads(raw.body)
    except (TypeError, json.JSONDecodeError) as error:
        raise ValueError(
            f"parse result error, url={url} status={raw.status} body={raw.body[:500]}"
        ) from error
    if not isinstance(value, dict):
        raise ValueError(
            f"parse result error, url={url} status={raw.status} body={raw.body[:500]}"
        )
    return value

