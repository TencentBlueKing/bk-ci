export const BRIDGE = 'bk-ci:websocket:v2'

const makeId = () => `${Date.now().toString(36)}-${Math.random().toString(36).slice(2)}`
const jitter = (delay) => delay * (0.8 + Math.random() * 0.4)

export class RealtimeConnection {
    constructor (options = {}) {
        this.options = options
        this.topics = new Map()
        this.pending = new Map()
        this.socket = null
        this.ready = false
        this.retry = 0
        this.disposed = false
        this.hiddenTimer = null
        this.hidden = false
        this.lastSeen = 0
        this.heartbeat = 30000
        this.onVisibility = () => {
            clearTimeout(this.hiddenTimer)
            if (document.hidden) {
                this.hiddenTimer = setTimeout(() => { this.hidden = true; this.disconnect() }, 60000)
            } else {
                this.hidden = false
                this.notifyAll('sync')
                this.connect()
            }
        }
        this.onOnline = () => { this.retry = 0; this.connect(); this.notifyAll('sync') }
        this.onOffline = () => this.disconnect()
        document.addEventListener('visibilitychange', this.onVisibility)
        window.addEventListener('online', this.onOnline)
        window.addEventListener('offline', this.onOffline)
        this.watchdog = setInterval(() => {
            if (this.socket && Date.now() - this.lastSeen > this.heartbeat * 2) this.disconnect(true)
            for (const [id, request] of this.pending) {
                if (Date.now() - request.time > 15000) {
                    this.pending.delete(id)
                    this.disconnect(true)
                    break
                }
            }
        }, 5000)
    }

    subscribe (topics, callback) {
        const unique = [...new Set(topics)].slice(0, 128)
        if (!this.topics.size && this.socket && unique[0]?.split(':')[1] !== this.projectId) this.disconnect()
        const added = []
        unique.forEach(topic => {
            let entry = this.topics.get(topic)
            if (!entry) {
                entry = { callbacks: new Set(), generation: makeId(), accepted: false }
                this.topics.set(topic, entry)
                added.push(topic)
            }
            entry.callbacks.add(callback)
            if (entry.accepted) callback('sync', topic)
        })
        clearTimeout(this.idleTimer)
        if (this.ready) this.sendTopics('sub', added)
        this.connect()
        return () => {
            unique.forEach(topic => {
                const entry = this.topics.get(topic)
                if (!entry) return
                entry.callbacks.delete(callback)
                if (!entry.callbacks.size) {
                    if (this.ready) this.sendTopics('unsub', [topic])
                    this.topics.delete(topic)
                }
            })
            if (!this.topics.size) this.idleTimer = setTimeout(() => this.disconnect(), 1000)
        }
    }

    connect () {
        if (this.disposed || this.hidden || !navigator.onLine || !this.topics.size || this.socket) return
        clearTimeout(this.retryTimer)
        const endpoint = new URL(this.options.url || '/websocket/ws/v2', window.location.href)
        endpoint.protocol = endpoint.protocol === 'https:' ? 'wss:' : 'ws:'
        const project = [...this.topics.keys()][0]?.split(':')[1]
        this.projectId = project
        if (project) endpoint.searchParams.set('projectId', project)
        let socket
        try { socket = new WebSocket(endpoint.href) } catch (_) { this.scheduleRetry(); return }
        this.socket = socket
        this.lastSeen = Date.now()
        socket.onmessage = event => {
            if (this.socket !== socket) return
            try { this.handle(JSON.parse(event.data)) } catch (_) { this.disconnect(true) }
        }
        socket.onerror = () => { if (this.socket === socket) this.disconnect(true) }
        socket.onclose = () => {
            if (this.socket !== socket) return
            this.socket = null
            this.ready = false
            this.pending.clear()
            this.topics.forEach(entry => { entry.accepted = false })
            this.notifyAll('polling')
            this.scheduleRetry()
        }
    }

    handle (frame) {
        if (frame.v !== 2) return
        this.lastSeen = Date.now()
        if (frame.op === 'hello') {
            this.ready = true
            this.retry = 0
            this.heartbeat = Math.max(1000, Number(frame.heartbeatInterval) || 30000)
            this.sendTopics('sub', [...this.topics.keys()])
        } else if (frame.op === 'ping') {
            this.send({ op: 'pong', nonce: frame.nonce })
        } else if (frame.op === 'sub_ack') {
            const request = this.pending.get(frame.requestId)
            if (!request || request.generation !== frame.generation) return
            this.pending.delete(frame.requestId)
            for (const topic of frame.accepted || []) {
                const entry = this.topics.get(topic)
                if (entry?.generation === frame.generation) {
                    entry.accepted = true
                    this.notify(topic, 'sync')
                }
            }
            Object.entries(frame.rejected || {}).forEach(([topic, code]) => {
                if (request.topics.includes(topic) && this.topics.get(topic)?.generation === frame.generation) {
                    this.notify(topic, code === 'FORBIDDEN' ? 'forbidden' : 'polling')
                }
            })
        } else if (frame.op === 'invalidate') {
            if (this.topics.get(frame.topic)?.generation === frame.generation) this.notify(frame.topic, 'invalidate')
        } else if (frame.op === 'resync_required') {
            this.notifyAll('sync')
        } else if (frame.op === 'error' && frame.code === 'FORBIDDEN') {
            this.notify(frame.topic, 'forbidden')
        } else if (frame.op === 'draining') {
            this.disconnect(true)
        }
    }

    sendTopics (op, topics) {
        const groups = new Map()
        const generation = makeId()
        topics.forEach(topic => {
            const entry = this.topics.get(topic)
            if (!entry) return
            if (op === 'sub') entry.generation = generation
            if (!groups.has(entry.generation)) groups.set(entry.generation, [])
            groups.get(entry.generation).push(topic)
        })
        groups.forEach((values, generation) => {
            for (let index = 0; index < values.length; index += 32) {
                const requestId = makeId()
                if (op === 'sub') this.pending.set(requestId, { time: Date.now(), generation, topics: values.slice(index, index + 32) })
                this.send({ op, requestId, generation, topics: values.slice(index, index + 32) })
            }
        })
    }

    send (frame) {
        if (this.socket?.readyState === WebSocket.OPEN) this.socket.send(JSON.stringify({ v: 2, ...frame }))
    }

    notify (topic, type) {
        this.topics.get(topic)?.callbacks.forEach(callback => {
            try { callback(type, topic) } catch (_) { /* isolate subscribers */ }
        })
    }

    notifyAll (type) { this.topics.forEach((_, topic) => this.notify(topic, type)) }

    scheduleRetry () {
        clearTimeout(this.retryTimer)
        if (!this.disposed && !this.hidden && navigator.onLine && this.topics.size) {
            this.retryTimer = setTimeout(() => this.connect(), jitter(Math.min(60000, 1000 * 2 ** Math.min(this.retry++, 6))))
        }
    }

    disconnect (retry = false) {
        clearTimeout(this.retryTimer)
        const socket = this.socket
        this.socket = null
        this.ready = false
        this.pending.clear()
        this.topics.forEach(entry => { entry.accepted = false })
        if (socket) { socket.onclose = null; socket.onerror = null; socket.close() }
        this.notifyAll('polling')
        if (retry) this.scheduleRetry()
    }

    dispose () {
        this.disposed = true
        this.disconnect()
        clearInterval(this.watchdog)
        clearTimeout(this.idleTimer)
        clearTimeout(this.hiddenTimer)
        document.removeEventListener('visibilitychange', this.onVisibility)
        window.removeEventListener('online', this.onOnline)
        window.removeEventListener('offline', this.onOffline)
        this.topics.clear()
    }
}

export function createRefreshTask ({ load, apply = () => {}, onError = () => {}, interval = 1000, liveInterval = 45000, pollingInterval = 15000 }) {
    let disposed = false
    let dirty = 0
    let running = false
    let timer = null
    let lastStarted = 0
    let failureCount = 0
    let live = false
    let forbidden = false
    const schedule = (delay) => {
        if (disposed || forbidden || timer) return
        timer = setTimeout(run, Math.max(0, delay))
    }
    const run = async () => {
        timer = null
        if (disposed || forbidden || running) return
        if (document.hidden || !navigator.onLine) { schedule(pollingInterval); return }
        const version = dirty
        running = true
        lastStarted = Date.now()
        try {
            const result = await load()
            if (!disposed && !forbidden) apply(result)
            failureCount = 0
        } catch (error) {
            failureCount++
            const status = error?.status || error?.httpStatus || error?.response?.status || error?.code
            if (Number(status) === 401 || Number(status) === 403) forbidden = true
            if (!disposed) onError(error)
        } finally {
            running = false
            if (!disposed && !forbidden) {
                const wait = failureCount
                    ? jitter(Math.min(60000, pollingInterval * 2 ** Math.min(failureCount - 1, 3)))
                    : version !== dirty ? Math.max(0, interval - (Date.now() - lastStarted)) : jitter(live ? liveInterval : pollingInterval)
                schedule(wait)
            }
        }
    }
    const refresh = (type = 'invalidate') => {
        if (disposed) return
        if (type === 'forbidden') {
            forbidden = true
            clearTimeout(timer)
            timer = null
            onError(Object.assign(new Error('Forbidden'), { status: 403 }))
            return
        }
        if (type === 'sync') live = true
        if (type === 'polling') live = false
        dirty++
        clearTimeout(timer)
        timer = null
        if (!running) schedule(Math.max(0, interval - (Date.now() - lastStarted)))
    }
    const visible = () => { if (!document.hidden) refresh('invalidate') }
    document.addEventListener('visibilitychange', visible)
    window.addEventListener('online', visible)
    schedule(0)
    return {
        refresh,
        dispose () {
            disposed = true
            clearTimeout(timer)
            document.removeEventListener('visibilitychange', visible)
            window.removeEventListener('online', visible)
        }
    }
}

let connection
export function directConnection () {
    if (!connection) connection = new RealtimeConnection()
    return connection
}

export function closeRealtimeConnection () {
    connection?.disconnect()
}
