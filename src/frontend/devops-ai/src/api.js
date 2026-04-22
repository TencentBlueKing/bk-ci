import { fetchEventSource } from '@microsoft/fetch-event-source'

const BASE = '/ms/ai/api/user/ai'

function uuid() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0
    return (c === 'x' ? r : (r & 0x3) | 0x8).toString(16)
  })
}

async function request(path, options = {}) {
  const res = await fetch(`${BASE}${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  })
  if (!res.ok) throw new Error(`HTTP ${res.status}`)
  const json = await res.json()
  if (json.status !== 0) throw new Error(json.message || 'Unknown error')
  return json.data
}

export async function getLatestSession(projectId) {
  const query = projectId ? `?projectId=${encodeURIComponent(projectId)}` : ''
  return request(`/sessions/latest${query}`)
}

export async function createSession(projectId, title) {
  return request('/sessions/', {
    method: 'POST',
    body: JSON.stringify({ projectId, title }),
  })
}

export async function getSessionMessages(sessionId) {
  return request(`/sessions/${sessionId}/messages`)
}

export async function getChatStatus(threadId) {
  return request(`/chat/status/${threadId}`)
}

export async function stopChat(threadId) {
  return request(`/chat/stop/${threadId}`, { method: 'POST' })
}

export async function getSessions(projectId) {
  const query = projectId ? `?projectId=${encodeURIComponent(projectId)}` : ''
  return request(`/sessions/${query}`)
}

export async function deleteSession(sessionId) {
  return request(`/sessions/${sessionId}`, {
    method: 'DELETE',
  })
}

export async function updateSessionTitle(sessionId, title) {
  return request(`/sessions/${sessionId}/title?title=${encodeURIComponent(title)}`, {
    method: 'PUT',
  })
}

export async function getPrompts() {
  return request('/prompts/')
}

export async function getSkills() {
  return request('/skills/')
}

export async function getExternalAgents() {
  return request('/external/agents')
}

export async function getMcpServers() {
  return request('/mcp/servers')
}

export async function getWelcomeGuide(projectId) {
  const query = projectId ? `?projectId=${encodeURIComponent(projectId)}` : ''
  return request(`/welcome-guide${query}`)
}

export async function getHotQuestions(page = 1, pageSize = 5) {
  return request(`/welcome-guide/hot-questions?page=${page}&pageSize=${pageSize}`)
}

let _instanceId = 0

export function createChatClient(threadId, { getContext } = {}) {
  const id = ++_instanceId
  let sendCount = 0
  let abortController = null

  function _connectSSE(url, options, callbacks) {
    const {
      onStart, onMessageStart, onDelta, onMessageEnd, onFinish, onError,
      onReasoningStart, onReasoningDelta, onReasoningEnd,
      onToolCallStart, onToolCallArgs, onToolCallEnd, onToolCallResult,
      onMessagesSnapshot,
      onStepStarted, onStepFinished,
      onStateSnapshot, onStateDelta,
      onActivitySnapshot, onActivityDelta,
      onCustom,
    } = callbacks

    abort()
    sendCount++
    abortController = new AbortController()

    fetchEventSource(url, {
      ...options,
      signal: abortController.signal,
      openWhenHidden: true,
      async onopen(response) {
        if (response.ok && (response.headers.get('content-type') || '').includes('text/event-stream')) {
          return
        }
        let errorMsg = `HTTP ${response.status}`
        try {
          const json = await response.json()
          errorMsg = json.message || json.error || JSON.stringify(json)
        } catch {
          // ignore
        }
        onError?.(errorMsg)
        throw new Error(errorMsg)
      },
      onmessage(msg) {
        if (!msg.data) return
        try {
          const event = JSON.parse(msg.data)
          if (process.env.NODE_ENV === 'development') {
            console.debug('%c[SSE]', 'color:#0a0', event.type, event)
          }
          switch (event.type) {
            case 'RUN_STARTED':
              onStart?.(event)
              break
            case 'TEXT_MESSAGE_START':
              onMessageStart?.(event)
              break
            case 'TEXT_MESSAGE_CONTENT':
              onDelta?.(event.delta, event)
              break
            case 'TEXT_MESSAGE_END':
              onMessageEnd?.(event)
              break
            case 'REASONING_MESSAGE_START':
              onReasoningStart?.(event)
              break
            case 'REASONING_MESSAGE_CONTENT':
              onReasoningDelta?.(event.delta, event)
              break
            case 'REASONING_MESSAGE_END':
              onReasoningEnd?.(event)
              break
            case 'TOOL_CALL_START':
              onToolCallStart?.(event)
              break
            case 'TOOL_CALL_ARGS':
              onToolCallArgs?.(event.delta, event)
              break
            case 'TOOL_CALL_END':
              onToolCallEnd?.(event)
              break
            case 'TOOL_CALL_RESULT':
              onToolCallResult?.(event)
              break
            case 'RUN_FINISHED':
              onFinish?.(event)
              break
            case 'RUN_ERROR':
              onError?.(event.message || 'Run error')
              break
            case 'MESSAGES_SNAPSHOT':
              onMessagesSnapshot?.(event.messages, event)
              break
            case 'STEP_STARTED':
              onStepStarted?.(event)
              break
            case 'STEP_FINISHED':
              onStepFinished?.(event)
              break
            case 'STATE_SNAPSHOT':
              onStateSnapshot?.(event.snapshot, event)
              break
            case 'STATE_DELTA':
              onStateDelta?.(event.delta, event)
              break
            case 'ACTIVITY_SNAPSHOT':
              onActivitySnapshot?.(event)
              break
            case 'ACTIVITY_DELTA':
              onActivityDelta?.(event)
              break
            case 'CUSTOM':
              onCustom?.(event)
              break
            case 'RAW':
              if (event.rawEvent?.error) {
                onError?.(event.rawEvent.error)
              }
              break
          }
        } catch {
          // ignore parse errors
        }
      },
      onerror(err) {
        onError?.(err.message || 'Connection error')
        throw err
      },
    })
  }

  function _dispatch(body, callbacks) {
    if (process.env.NODE_ENV === 'development') {
      console.debug(`%c[ChatClient#${id}]`, 'color:#07f', `send #${sendCount + 1}`, body)
    }
    _connectSSE(`${BASE}/chat/run`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    }, callbacks)
  }

  function subscribe(callbacks) {
    if (process.env.NODE_ENV === 'development') {
      console.debug(`%c[ChatClient#${id}]`, 'color:#07f', 'subscribe stream')
    }
    _connectSSE(`${BASE}/chat/stream/${threadId}`, {
      method: 'GET',
    }, callbacks)
  }

  function send(content, callbacks, { tools: sendTools } = {}) {
    const runId = `run-${uuid()}`
    const msgId = `msg-${uuid()}`
    const context = getContext?.() || []
    const body = {
      threadId,
      runId,
      messages: [{ id: msgId, role: 'user', content }],
      ...(context.length > 0 && { context }),
      ...(sendTools?.length > 0 && { tools: sendTools }),
    }
    _dispatch(body, callbacks)
  }

  function continueWithToolResult(runMessages, callbacks, { tools: sendTools } = {}) {
    const runId = `run-${uuid()}`
    const context = getContext?.() || []
    const body = {
      threadId,
      runId,
      messages: runMessages,
      ...(context.length > 0 && { context }),
      ...(sendTools?.length > 0 && { tools: sendTools }),
    }
    _dispatch(body, callbacks)
  }

  function resume(interruptId, payload, callbacks) {
    const runId = `run-${uuid()}`
    const context = getContext?.() || []
    const body = {
      threadId,
      runId,
      resume: {
        ...(interruptId && { interruptId }),
        ...(payload !== undefined && { payload }),
      },
      ...(context.length > 0 && { context }),
    }
    _dispatch(body, callbacks)
  }

  function abort() {
    if (abortController) {
      abortController.abort()
      abortController = null
    }
  }

  function close() {
    abort()
  }

  return { send, continueWithToolResult, resume, subscribe, abort, close }
}
