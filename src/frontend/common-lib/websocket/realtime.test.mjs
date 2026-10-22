import test from 'node:test'
import assert from 'node:assert/strict'
import { RealtimeConnection, createRefreshTask } from './realtime.js'

function browser (t) {
    const oldWindow = globalThis.window
    const oldDocument = globalThis.document
    const oldNavigator = Object.getOwnPropertyDescriptor(globalThis, 'navigator')
    const oldWebSocket = globalThis.WebSocket
    const window = new EventTarget()
    window.location = { href: 'https://ci.example.test/console', origin: 'https://ci.example.test' }
    window.parent = window
    const document = new EventTarget()
    document.hidden = false
    document.referrer = ''
    class Socket {
        static OPEN = 1
        static instances = []
        constructor (url) { this.url = url; this.readyState = 1; this.sent = []; Socket.instances.push(this) }
        send (value) { this.sent.push(JSON.parse(value)) }
        close () { this.readyState = 3 }
        receive (frame) { this.onmessage?.({ data: JSON.stringify({ v: 2, ...frame }) }) }
    }
    Object.assign(globalThis, { window, document, WebSocket: Socket })
    Object.defineProperty(globalThis, 'navigator', { configurable: true, value: { onLine: true } })
    const cleanups = []
    t.cleanup = callback => cleanups.push(callback)
    t.after(() => {
        cleanups.forEach(cleanup => cleanup())
        globalThis.window = oldWindow
        globalThis.document = oldDocument
        globalThis.WebSocket = oldWebSocket
        if (oldNavigator) Object.defineProperty(globalThis, 'navigator', oldNavigator)
        else delete globalThis.navigator
    })
    return { Socket, document, window }
}

const flush = () => new Promise(resolve => setImmediate(resolve))

test('shared topics bind once and only unsubscribe after last callback', t => {
    const { Socket } = browser(t)
    const connection = new RealtimeConnection()
    t.cleanup(() => connection.dispose())
    const events = []
    const first = connection.subscribe(['ps:p:a'], type => events.push(type))
    const second = connection.subscribe(['ps:p:a'], () => {})
    assert.equal(Socket.instances.length, 1)
    const socket = Socket.instances[0]
    socket.receive({ op: 'hello', heartbeatInterval: 30000 })
    assert.equal(socket.sent.length, 1)
    const request = socket.sent[0]
    socket.receive({ op: 'sub_ack', requestId: request.requestId, generation: request.generation, accepted: ['ps:p:a'] })
    assert.deepEqual(events, ['sync'])
    socket.receive({ op: 'invalidate', topic: 'ps:p:a', generation: 'stale' })
    assert.equal(events.length, 1)
    socket.receive({ op: 'invalidate', topic: 'ps:p:a', generation: request.generation })
    assert.deepEqual(events, ['sync', 'invalidate'])
    first()
    assert.equal(socket.sent.length, 1)
    second()
    assert.equal(socket.sent[1].op, 'unsub')
})

test('128 list topics are sent in four bounded batches', t => {
    const { Socket } = browser(t)
    const connection = new RealtimeConnection()
    t.cleanup(() => connection.dispose())
    connection.subscribe(Array.from({ length: 128 }, (_, index) => `ps:p:p${index}`), () => {})
    Socket.instances[0].receive({ op: 'hello' })
    assert.equal(Socket.instances[0].sent.length, 4)
    assert.ok(Socket.instances[0].sent.every(frame => frame.topics.length === 32))
})

test('dirty during a snapshot triggers one trailing query, never concurrent queries', async t => {
    browser(t)
    t.mock.timers.enable({ apis: ['setTimeout', 'setInterval', 'Date'], now: 100000 })
    let calls = 0
    let complete
    const applied = []
    const task = createRefreshTask({
        load: () => { calls++; return new Promise(resolve => { complete = resolve }) },
        apply: value => applied.push(value),
        interval: 1000
    })
    t.cleanup(() => task.dispose())
    t.mock.timers.tick(0)
    assert.equal(calls, 1)
    task.refresh()
    task.refresh()
    t.mock.timers.tick(5000)
    assert.equal(calls, 1)
    complete('first')
    await flush()
    t.mock.timers.tick(0)
    assert.equal(calls, 2)
    task.dispose()
    complete('stale')
    await flush()
    assert.deepEqual(applied, ['first'])
})

test('live connections still reconcile when the last event is lost', async t => {
    browser(t)
    t.mock.timers.enable({ apis: ['setTimeout', 'setInterval', 'Date'], now: 100000 })
    let calls = 0
    const task = createRefreshTask({ load: async () => ++calls, liveInterval: 10000, pollingInterval: 5000 })
    t.cleanup(() => task.dispose())
    task.refresh('sync')
    t.mock.timers.tick(0)
    await flush()
    assert.equal(calls, 1)
    t.mock.timers.tick(12001)
    await flush()
    assert.equal(calls, 2)
})

test('permission errors stop polling for all supported request wrappers', async t => {
    browser(t)
    t.mock.timers.enable({ apis: ['setTimeout', 'setInterval', 'Date'], now: 100000 })
    let calls = 0
    const task = createRefreshTask({ load: async () => { calls++; throw { code: 403 } } })
    t.cleanup(() => task.dispose())
    t.mock.timers.tick(0)
    await flush()
    task.refresh('invalidate')
    t.mock.timers.tick(120000)
    await flush()
    assert.equal(calls, 1)
})

test('offline and hidden pages do not poll', async t => {
    const { document } = browser(t)
    t.mock.timers.enable({ apis: ['setTimeout', 'setInterval', 'Date'], now: 100000 })
    let calls = 0
    document.hidden = true
    const task = createRefreshTask({ load: async () => ++calls })
    t.cleanup(() => task.dispose())
    t.mock.timers.tick(30000)
    await flush()
    assert.equal(calls, 0)
    document.hidden = false
    document.dispatchEvent(new Event('visibilitychange'))
    t.mock.timers.tick(0)
    await flush()
    assert.equal(calls, 1)
})
