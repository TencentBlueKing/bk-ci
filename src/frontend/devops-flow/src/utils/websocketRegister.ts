import SockJS from 'sockjs-client'
import { Stomp } from 'stompjs/lib/stomp.js'
import type { RouteLocationNormalized, Router } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

type MessageCallback = (data: any) => void

interface WebSocketMessage {
  webSocketType?: string
  message?: string
}

const MAX_RECONNECT_TIMES = 8

function createSessionId() {
  return Array.from({ length: 7 }, () =>
    Math.floor((1 + Math.random()) * 0x10000)
      .toString(16)
      .slice(1),
  ).join('')
}

function getWebSocketPage(route: RouteLocationNormalized) {
  return `/console${route.fullPath}`
}

class FlowWebSocket {
  private readonly sessionId = createSessionId()
  private readonly callbacks: Record<string, MessageCallback> = {}
  private readonly reconnectCallbacks: Record<string, () => void> = {}
  private stompClient?: import('stompjs/lib/stomp.js').StompClient
  private currentRoute?: RouteLocationNormalized
  private reconnectTimes = 0
  private retryTimer?: ReturnType<typeof window.setTimeout>
  private isConnecting = false
  private hasConnected = false
  private shouldReconnect = true
  private isRegistered = false

  initialize(router: Router) {
    router.afterEach((to, from) => this.syncRoute(to, from))
    router.isReady().then(() => this.syncRoute(router.currentRoute.value))

    window.addEventListener('beforeunload', () => this.disconnect(true))
    window.addEventListener('offline', () => this.disconnect(false))
    window.addEventListener('online', () => {
      this.shouldReconnect = true
      this.connect()
    })
  }

  installWsMessage(callback: MessageCallback, id: string) {
    this.callbacks[id] = callback
  }

  registerOnReconnect(callback: () => void, id: string) {
    this.reconnectCallbacks[id] = callback
  }

  unInstallWsMessage(id: string) {
    delete this.callbacks[id]
    delete this.reconnectCallbacks[id]
  }

  private syncRoute(to: RouteLocationNormalized, from?: RouteLocationNormalized) {
    if (to.meta.websocket) {
      this.currentRoute = to
      this.shouldReconnect = true
      this.connect()
      this.registerCurrentRoute()
      return
    }

    if (from?.meta.websocket) this.logout(from)
    this.currentRoute = undefined
    this.isRegistered = false
  }

  private connect() {
    if (
      this.isConnecting ||
      this.stompClient?.connected ||
      !this.shouldReconnect ||
      !navigator.onLine
    )
      return

    this.isConnecting = true
    const socket = new SockJS(`/websocket/ws/user?sessionId=${this.sessionId}`)
    const client = Stomp.over(socket)
    client.debug = null
    this.stompClient = client

    socket.onclose = () => {
      this.isConnecting = false
      if (this.stompClient === client) this.stompClient = undefined
      this.scheduleReconnect()
    }

    client.connect(
      {},
      () => {
        const isReconnect = this.hasConnected
        this.hasConnected = true
        this.isConnecting = false
        this.reconnectTimes = 0
        client.subscribe(`/topic/bk/notify/${this.sessionId}`, (frame) =>
          this.handleMessage(frame.body),
        )
        this.registerCurrentRoute()
        if (isReconnect) Object.values(this.reconnectCallbacks).forEach((callback) => callback())
      },
      () => {
        this.isConnecting = false
        this.scheduleReconnect()
      },
    )
  }

  private scheduleReconnect() {
    if (!this.shouldReconnect || this.retryTimer || this.reconnectTimes >= MAX_RECONNECT_TIMES)
      return

    this.reconnectTimes += 1
    this.retryTimer = window.setTimeout(() => {
      this.retryTimer = undefined
      this.connect()
    }, Math.random() * 60000)
  }

  private async registerCurrentRoute() {
    const route = this.currentRoute
    if (!route || !this.stompClient?.connected) return

    const userId = await this.getUserId()
    if (!userId || route !== this.currentRoute || !this.stompClient?.connected) return

    this.stompClient.send(
      '/app/changePage',
      {},
      JSON.stringify({
        sessionId: this.sessionId,
        userId,
        page: getWebSocketPage(route),
        showProjectList: false,
        projectId: route.params.projectId,
      }),
    )
    this.isRegistered = true
  }

  private async logout(route: RouteLocationNormalized) {
    if (!this.isRegistered || !this.stompClient?.connected) return

    const userId = await this.getUserId()
    if (!userId || !this.stompClient?.connected || this.currentRoute?.meta.websocket) return

    this.stompClient.send(
      '/app/loginOut',
      {},
      JSON.stringify({ sessionId: this.sessionId, userId, page: getWebSocketPage(route) }),
    )
    this.isRegistered = false
  }

  private async getUserId() {
    const authStore = useAuthStore()
    return authStore.username || (await authStore.fetchUserInfo())?.username || ''
  }

  private handleMessage(body: string) {
    try {
      const data = JSON.parse(body) as WebSocketMessage
      if (data.webSocketType !== 'IFRAME' || !data.message) return

      const message = JSON.parse(data.message)
      if (message === 'WEBSOCKET_RECONNECT') {
        Object.values(this.reconnectCallbacks).forEach((callback) => callback())
        return
      }

      Object.values(this.callbacks).forEach((callback) => callback(message))
    } catch (error) {
      console.error('[websocketRegister] Failed to parse message:', error)
    }
  }

  private disconnect(isPageClosing: boolean) {
    this.shouldReconnect = false
    if (this.retryTimer) window.clearTimeout(this.retryTimer)
    this.retryTimer = undefined

    if (isPageClosing) {
      const userId = useAuthStore().username
      if (userId) {
        navigator.sendBeacon(
          `/websocket/api/user/websocket/sessions/${this.sessionId}/userIds/${userId}/clear`,
        )
      }
    }

    this.stompClient?.disconnect()
    this.stompClient = undefined
    this.isRegistered = false
  }
}

const flowWebSocket = new FlowWebSocket()

/**
 * Register a callback that receives parsed WebSocket push data.
 *
 * @param callback  Invoked with the parsed `message` payload on each push.
 * @param id        Unique key so the registration can be removed later.
 */
export const websocketRegister = {
  initialize: (router: Router) => flowWebSocket.initialize(router),
  installWsMessage: (callback: MessageCallback, id: string) =>
    flowWebSocket.installWsMessage(callback, id),
  registerOnReconnect: (callback: () => void, id: string) =>
    flowWebSocket.registerOnReconnect(callback, id),
  unInstallWsMessage: (id: string) => flowWebSocket.unInstallWsMessage(id),
}
