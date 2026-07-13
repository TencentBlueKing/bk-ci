/**
 * WebSocket message register / dispatcher for devops-flow.
 *
 * devops-flow runs inside the devops-nav iframe. The parent (devops-nav)
 * manages the SockJS/STOMP connection, performs page matching, and forwards
 * IFRAME-type messages to the iframe via window.postMessage.
 *
 * Therefore this module only needs to:
 *   1. Listen for `message` events on the iframe window.
 *   2. Filter for `webSocketType === 'IFRAME'`.
 *   3. Parse the `message` JSON payload and dispatch to registered callbacks.
 *   4. Handle `WEBSOCKET_RECONNECT` events from the parent.
 *
 * Reference: devops-pipeline/src/utils/webSocketMessage.js
 */

type MessageCallback = (data: any) => void

const callBacks: Record<string, MessageCallback> = {}
const reconnectCallBacks: Record<string, () => void> = {}

function onMessage(event: MessageEvent) {
  const type = event.data?.webSocketType
  if (type !== 'IFRAME' || !event.data?.message) return

  console.log('[websocketRegister] Received IFRAME message', event.data)

  try {
    const message = JSON.parse(event.data.message)
    if (message === 'WEBSOCKET_RECONNECT') {
      Object.values(reconnectCallBacks).forEach((cb) => cb())
    } else {
      console.log('[websocketRegister] Dispatching to', Object.keys(callBacks).length, 'callbacks, data:', message)
      Object.values(callBacks).forEach((cb) => cb(message))
    }
  } catch (e) {
    console.error('[websocketRegister] Failed to parse message:', e)
  }
}

window.addEventListener('message', onMessage)

/**
 * Register a callback that receives parsed WebSocket push data.
 *
 * @param callback  Invoked with the parsed `message` payload on each push.
 * @param id        Unique key so the registration can be removed later.
 */
function installWsMessage(callback: MessageCallback, id: string) {
  callBacks[id] = callback
}

/**
 * Register a callback for WebSocket reconnect events.
 * Typically used to trigger a full data refresh after reconnect.
 */
function registerOnReconnect(callback: () => void, id: string) {
  reconnectCallBacks[id] = callback
}

function unInstallWsMessage(id: string) {
  delete callBacks[id]
  delete reconnectCallBacks[id]
}

export const websocketRegister = {
  installWsMessage,
  registerOnReconnect,
  unInstallWsMessage,
}
