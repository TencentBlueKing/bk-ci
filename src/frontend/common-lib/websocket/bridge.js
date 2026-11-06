import { BRIDGE, createRefreshTask, directConnection, closeRealtimeConnection } from './realtime.js'

const makeId = () => `${Date.now().toString(36)}-${Math.random().toString(36).slice(2)}`
const parentOrigin = () => {
    try { return new URL(document.referrer).origin } catch (_) { return window.location.origin }
}

let bridge
class ParentConnection {
    constructor () {
        this.id = makeId()
        this.origin = parentOrigin()
        this.entries = new Map()
        this.ready = false
        this.suspended = false
        this.epoch = null
        this.listener = event => {
            if (event.source !== window.parent || event.origin !== this.origin || event.data?.namespace !== BRIDGE || event.data?.clientId !== this.id) return
            const message = event.data
            if (message.op === 'ready' && !this.suspended) {
                this.epoch = message.epoch
                this.ready = true
                this.entries.forEach((entry, id) => this.post({ op: 'subscribe', id, topics: entry.topics }))
            } else if (message.op === 'event' && message.epoch === this.epoch) {
                this.entries.get(message.id)?.callback(message.type, message.topic)
            } else if (message.op === 'reset') {
                this.ready = false
                this.suspended = true
                this.entries.forEach(entry => entry.callback('polling'))
                this.entries.clear()
            }
        }
        window.addEventListener('message', this.listener)
        this.helloTimer = setInterval(() => { if (!this.ready && !this.suspended && this.entries.size) this.hello() }, 10000)
        window.addEventListener('pagehide', () => this.post({ op: 'dispose' }))
    }

    post (message) { window.parent.postMessage({ namespace: BRIDGE, clientId: this.id, epoch: this.epoch, ...message }, this.origin) }
    hello () { this.post({ op: 'hello' }) }

    subscribe (topics, callback) {
        if (this.suspended) { this.id = makeId(); this.suspended = false; this.epoch = null }
        const owner = this.id
        const id = makeId()
        this.entries.set(id, { topics, callback })
        if (this.ready) this.post({ op: 'subscribe', id, topics })
        else this.hello()
        return () => {
            if (owner !== this.id) return
            this.entries.delete(id)
            this.post({ op: 'unsubscribe', id })
            if (!this.entries.size) { this.post({ op: 'idle' }); this.ready = false }
        }
    }
}

export function subscribeRealtime (topics, callback) {
    if (window.BK_CI_WEBSOCKET_V2 === false || !topics.length) return () => {}
    if (window.parent === window) return directConnection().subscribe(topics, callback)
    if (!bridge) bridge = new ParentConnection()
    return bridge.subscribe(topics, callback)
}

export function watchRealtime (options) {
    const task = createRefreshTask(options)
    const topics = options.topics || []
    const unsubscribe = subscribeRealtime(topics, (type, topic) => {
        if (type === 'forbidden' && options.onForbidden) options.onForbidden(topic)
        else task.refresh(type)
    })
    return {
        refresh: task.refresh,
        dispose () { unsubscribe(); task.dispose() }
    }
}

export function installRealtimeHost ({ getFrame, onActive = () => {}, onIdle = () => {} }) {
    const entries = new Map()
    const retired = new Set()
    let clientId
    let source
    let origin
    let epoch = makeId()
    const post = message => source?.postMessage({ namespace: BRIDGE, clientId, epoch, ...message }, origin)
    const clear = () => {
        entries.forEach(dispose => dispose())
        entries.clear()
        closeRealtimeConnection()
    }
    const release = () => {
        clear()
        clientId = null
        source = null
        epoch = makeId()
        onIdle()
    }
    const listener = event => {
        if (window.BK_CI_WEBSOCKET_V2 === false) return
        const frame = getFrame()
        if (!frame || event.source !== frame.contentWindow || event.data?.namespace !== BRIDGE) return
        let frameOrigin
        try { frameOrigin = new URL(frame.src, window.location.href).origin } catch (_) { return }
        if (event.origin !== frameOrigin) return
        const message = event.data
        if (typeof message.clientId !== 'string' || message.clientId.length > 128 || retired.has(message.clientId)) return
        if (message.op === 'hello') {
            if (clientId !== message.clientId) { clear(); epoch = makeId() }
            clientId = message.clientId
            source = event.source
            origin = event.origin
            onActive()
            post({ op: 'ready' })
            return
        }
        if (message.clientId !== clientId || message.epoch !== epoch) return
        if (message.op === 'subscribe') {
            if (typeof message.id !== 'string' || !Array.isArray(message.topics) || message.topics.length > 128 || (!entries.has(message.id) && entries.size >= 128)) return
            if (!message.topics.every(topic => typeof topic === 'string' && topic.length <= 600)) return
            entries.get(message.id)?.()
            entries.set(message.id, directConnection().subscribe(message.topics, (type, topic) => post({ op: 'event', id: message.id, type, topic })))
        } else if (message.op === 'unsubscribe') {
            entries.get(message.id)?.()
            entries.delete(message.id)
        } else if (message.op === 'dispose' || message.op === 'idle') {
            release()
        }
    }
    window.addEventListener('message', listener)
    return {
        reset () {
            if (clientId) {
                retired.add(clientId)
                if (retired.size > 16) retired.delete(retired.values().next().value)
            }
            post({ op: 'reset' })
            release()
        },
        dispose () { release(); window.removeEventListener('message', listener) }
    }
}
