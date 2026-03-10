/**
 * WebSocket message register / dispatcher for devops-flow.
 *
 * Follows the devops-stream websocket-register pattern:
 *   • Multiple callbacks keyed by a unique `id` string.
 *   • Each callback filters by `wsKey` (= webSocketType + module)
 *     AND checks that the current page path includes the pushed `page`.
 *   • Pages call installWsMessage / unInstallWsMessage in setup / onUnmounted.
 *
 * Usage in a page component:
 *   import { websocketRegister } from '@/utils/websocketRegister'
 *
 *   // in setup()
 *   websocketRegister.installWsMessage(
 *     (data) => { ... },   // callback that receives the parsed message
 *     'IFRAMEprocess',     // wsKey = webSocketType + module
 *     'myPageId',          // unique id for this registration
 *   )
 *
 *   onUnmounted(() => websocketRegister.unInstallWsMessage('myPageId'))
 */

function getPageKey(pathname: string) {
  return pathname.split('/').slice(1).join('/')
}

type MessageCallback = (data: any) => void

interface WsMessageEvent {
  webSocketType?: string
  module?: string
  page?: string
  message?: string
}

const callBacks: Record<string, (event: MessageEvent) => void> = {}

function execCallBacks(event: MessageEvent) {
  Object.values(callBacks).forEach((cb) => cb(event))
}

/**
 * Register a message callback for a specific wsKey and page.
 *
 * @param callback  Receives the parsed `message` payload.
 * @param wsKey     webSocketType + module, e.g. 'IFRAMEprocess'.
 * @param id        Unique identifier so the callback can be removed later.
 */
function installWsMessage(callback: MessageCallback, wsKey: string, id: string) {
  callBacks[id] = (event: MessageEvent) => {
    const data = (event.data ?? {}) as WsMessageEvent
    const { webSocketType, module: mod, page, message } = data
    const eventKey = (webSocketType ?? '') + (mod ?? '')
    const currentPage = getPageKey(location.pathname)

    if (eventKey === wsKey && page && currentPage.includes(page)) {
      try {
        const parsed = JSON.parse(message ?? '{}')
        callback(parsed)
      } catch (e) {
        console.error('[websocketRegister] Failed to parse message:', e)
      }
    }
  }
}

function unInstallWsMessage(id: string) {
  delete callBacks[id]
}

window.addEventListener('message', execCallBacks)

export const websocketRegister = {
  installWsMessage,
  unInstallWsMessage,
}
