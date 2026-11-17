import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'

const sourcePath = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../bsWebSocket.js')

function loadWebSocketClass ({ useRouteChange = false } = {}) {
    const source = fs.readFileSync(sourcePath, 'utf8')
        .replace(/^import .*$/gm, '')
        .replace(/^const Stomp = .*$/gm, 'const Stomp = stomp')
        .replace('export default new BlueShieldWebSocket()', 'return BlueShieldWebSocket')

    const sockets = []
    const clients = []
    const timers = []
    const listeners = {}
    const refreshes = []
    const sends = []
    const cookie = {
        get: () => 'cookie-project'
    }
    const route = {
        meta: { webSocket: ['executeDetail'] },
        path: '/console/pipeline/project/executeDetail/A',
        params: { projectId: 'route-project' }
    }
    const window = {
        location: {
            origin: 'https://ci.example.com',
            href: 'https://ci.example.com/console/pipeline/project/executeDetail/A'
        },
        PUBLIC_URL_PREFIX: '',
        userInfo: { username: 'tester' },
        addEventListener: (type, listener) => {
            listeners[type] = listener
        },
        devops: { $router: { currentRoute: route } }
    }
    const document = {
        getElementById: () => ({
            contentWindow: {
                postMessage: message => refreshes.push(message)
            }
        })
    }
    const navigator = { onLine: true }

    function FakeSockJS () {
        sockets.push(this)
        this.close = () => {
            this.closed = true
            if (this.onclose) this.onclose({ code: 1000 })
        }
    }

    const stomp = {
        over (socket) {
            const client = {
                connected: false,
                ws: socket,
                connect (_, onConnected, onError) {
                    client.onError = onError
                    socket.onopen = () => {
                        client.connected = true
                        onConnected()
                    }
                    socket.onclose = () => {
                        client.connected = false
                        client.cleanedUp = true
                        onError('socket closed')
                    }
                },
                subscribe () {},
                send (destination, headers, body) {
                    sends.push({ destination, headers, body })
                },
                disconnect () {
                    client.disconnectCalls = (client.disconnectCalls || 0) + 1
                    if (!client.connected) throw new Error('cannot disconnect before handshake')
                    client.connected = false
                }
            }
            clients.push(client)
            return client
        }
    }

    const context = {
        clearTimeout: timer => {
            if (timer) timer.active = false
        },
        console: {
            log: () => {},
            error: () => {}
        },
        JSON,
        location: window.location,
        Math,
        RegExp,
        setTimeout: (callback, delay) => {
            const timer = { callback, delay, active: true }
            timers.push(timer)
            return timer
        },
        document,
        navigator,
        window
    }
    const BlueShieldWebSocket = new Function(
        'SockJS',
        'stomp',
        'window',
        'document',
        'navigator',
        'location',
        'setTimeout',
        'clearTimeout',
        'cookie',
        'X_DEVOPS_PROJECT_ID',
        source
    )(
        FakeSockJS,
        stomp,
        window,
        document,
        navigator,
        window.location,
        context.setTimeout,
        context.clearTimeout,
        cookie,
        'project-id'
    )

    const webSocket = new BlueShieldWebSocket()
    webSocket.detectHasWebsocket = () => true
    if (!useRouteChange) webSocket.changeRoute = () => {}
    webSocket.handleMessage = res => {
        const data = JSON.parse(res.body)
        refreshes.push(data.page)
    }

    return {
        clients,
        listeners,
        refreshes,
        route,
        sends,
        sockets,
        timers,
        webSocket,
        window
    }
}

function runNextTimer (timers) {
    const timer = timers.find(item => item.active)
    assert.ok(timer, 'a reconnect backoff timer is scheduled')
    timer.active = false
    timer.callback()
}

test('unexpected close preserves STOMP cleanup and reconnects with the current route', () => {
    const { clients, refreshes, route, sockets, timers, webSocket, window } = loadWebSocketClass()
    webSocket.connect()
    assert.equal(sockets.length, 1, 'initial connection creates one socket')

    sockets[0].onopen()
    sockets[0].onclose({ code: 1006 })

    assert.equal(clients[0].cleanedUp, true, 'STOMP close cleanup runs before reconnect')
    assert.equal(clients[0].connected, false, 'the closed STOMP client is no longer connected')
    assert.equal(sockets.length, 2, 'an established socket reconnects immediately')
    assert.equal(timers.filter(timer => timer.active).length, 0, 'established sockets do not add a backoff timer')

    route.path = '/console/pipeline/project/executeDetail/B'
    window.location.href = `https://ci.example.com${route.path}`
    sockets[1].onopen()

    assert.deepEqual(refreshes, [route.path], 'recovery refreshes the route active at recovery time')
})

test('reconnect compensation is available to detail, history, and list pages', () => {
    const paths = [
        '/console/pipeline/project/executeDetail/A',
        '/console/pipeline/project/buildHistory',
        '/console/pipeline/list'
    ]

    paths.forEach(path => {
        const { refreshes, route, sockets, webSocket, window } = loadWebSocketClass()
        route.path = path
        window.location.href = `https://ci.example.com${path}`
        webSocket.connect()
        sockets[0].onopen()
        sockets[0].onclose({ code: 1006 })

        route.path = `${path}/current`
        window.location.href = `https://ci.example.com${route.path}`
        sockets[1].onopen()

        assert.deepEqual(refreshes, [route.path], `recovery refreshes ${path}`)
    })
})

test('reconnect compensation follows a realtime route entered before recovery', () => {
    const { refreshes, route, sockets, webSocket, window } = loadWebSocketClass()
    webSocket.detectHasWebsocket = currentRoute => currentRoute.meta.webSocket.length > 0
    route.meta.webSocket = []
    route.path = '/console/home'
    window.location.href = `https://ci.example.com${route.path}`
    webSocket.connect()
    sockets[0].onopen()
    sockets[0].onclose({ code: 1006 })

    route.meta.webSocket = ['buildHistory']
    route.path = '/console/pipeline/project/buildHistory/current'
    window.location.href = `https://ci.example.com${route.path}`
    sockets[1].onopen()

    assert.deepEqual(refreshes, [route.path])
})

test('handshake failures back off without duplicate timers and refresh once after recovery', () => {
    const { clients, refreshes, sockets, timers, webSocket } = loadWebSocketClass()
    webSocket.connect()
    sockets[0].onclose({ code: 1006 })

    assert.equal(sockets.length, 1, 'a failed handshake waits for backoff')
    assert.equal(timers.filter(timer => timer.active).length, 1, 'one backoff timer is active')

    runNextTimer(timers)
    sockets[1].onclose({ code: 1006 })
    assert.equal(sockets.length, 2, 'the next handshake is created by the backoff')
    assert.equal(timers.filter(timer => timer.active).length, 1, 'repeated failures keep one timer')

    runNextTimer(timers)
    sockets[2].onopen()

    assert.equal(clients[2].connected, true)
    assert.equal(refreshes.length, 1, 'recovery sends one compensation refresh')
})

test('repeated STOMP errors keep one reconnect timer', () => {
    const { clients, timers } = loadWebSocketClass()

    clients[0].onError('first error')
    clients[0].onError('second error')

    assert.equal(timers.filter(timer => timer.active).length, 1)
})

test('STOMP errors invalidate a connected client before retrying', () => {
    const { clients, sockets, timers } = loadWebSocketClass()

    sockets[0].onopen()
    clients[0].onError('connection error')

    assert.equal(clients[0].connected, false)
    runNextTimer(timers)
    assert.equal(sockets.length, 2)
})

test('STOMP error cleanup falls back to closing the socket', () => {
    const { clients, sockets, timers } = loadWebSocketClass()

    sockets[0].onopen()
    clients[0].disconnect = () => {
        throw new Error('disconnect failed')
    }
    const originalConsoleError = console.error
    console.error = () => {}
    try {
        clients[0].onError('connection error')
    } finally {
        console.error = originalConsoleError
    }

    assert.equal(sockets[0].closed, true)
    runNextTimer(timers)
    assert.equal(sockets.length, 2)
})

test('online event can retry after a failed handshake', () => {
    const { listeners, sockets, timers } = loadWebSocketClass()

    sockets[0].onclose({ code: 1006 })
    listeners.online()

    assert.equal(sockets.length, 2)
    assert.equal(timers.filter(timer => timer.active).length, 0)
})

test('normal close after a connected session reconnects immediately', () => {
    const { clients, sockets, timers } = loadWebSocketClass()

    sockets[0].onopen()
    sockets[0].onclose({ code: 1000 })

    assert.equal(clients[0].cleanedUp, true)
    assert.equal(sockets.length, 2)
    assert.equal(timers.filter(timer => timer.active).length, 0)
})

test('normal close during handshake retries after backoff and online recovery', () => {
    const { listeners, sockets, timers } = loadWebSocketClass()

    sockets[0].onclose({ code: 1000 })

    assert.equal(timers.filter(timer => timer.active).length, 1)
    listeners.online()

    assert.equal(sockets.length, 2)
    assert.equal(timers.filter(timer => timer.active).length, 0)
})

test('intentional offline close does not schedule a reconnect', () => {
    const { listeners, sockets, timers } = loadWebSocketClass()

    sockets[0].onopen()
    listeners.offline()
    sockets[0].onclose({ code: 1000 })

    assert.equal(sockets.length, 1)
    assert.equal(timers.filter(timer => timer.active).length, 0)

    listeners.online()
    assert.equal(sockets.length, 2)
})

test('offline recovery refreshes the current route once after reconnecting', () => {
    const { listeners, refreshes, route, sockets, webSocket, window } = loadWebSocketClass()

    sockets[0].onopen()
    listeners.offline()
    listeners.offline()
    route.path = '/console/pipeline/project/executeDetail/B'
    window.location.href = `https://ci.example.com${route.path}`
    listeners.online()
    sockets[1].onopen()

    assert.deepEqual(refreshes, [route.path])
    assert.equal(webSocket.reconnectRefreshPending, false)
})

test('route project takes priority over the shared project cookie', () => {
    const { route, sends, sockets, timers } = loadWebSocketClass({ useRouteChange: true })

    sockets[0].onopen()
    timers.find(timer => timer.active).callback()

    const changePageMessage = sends.find(message => message.destination === '/app/changePage')
    assert.ok(changePageMessage)
    assert.equal(JSON.parse(changePageMessage.body).projectId, route.params.projectId)
})
