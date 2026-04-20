/**
 * TAPD iframe postMessage 通用桥接服务
 *
 * 场景：当前工程作为 iframe 嵌入 TAPD 主站（tapd.woa.com 等）时，
 *      通过 postMessage 与主站进行双向通信（路由同步、生命周期通知、导航指令等）。
 *
 * 设计要点：
 * - 统一信封格式：{ type: 'tapdFeIframe', event, payload }
 * - 统一 origin 白名单校验，避免来源不可信的消息
 * - 订阅/发布模型（on/off/post），支持多组件独立订阅
 * - 单例 + 单次 window.message 监听，避免重复注册
 */

// 与 TAPD 主站约定的 postMessage 信封 type（切勿改动）
export const TAPD_MSG_TYPE = 'tapdFeIframe'

// 与主站约定的事件名常量，业务代码禁止硬编码字符串
export const TAPD_EVENTS = {
    ROUTE_CHANGE: 'bkPipelineRouteChange',
    READY: 'bkPipelineReady',
    CLOSE: 'bkPipelineClose',
    NAVIGATE: 'bkPipelineNavigate',
    BACK_TO_HOST: 'bkBackToHost'
}

// 主站允许的 origin 白名单（按实际域名维护）
export const TAPD_ORIGIN_WHITELIST = [
    'https://tapd.woa.com',
    'https://wolf.woa.com',
    'https://lion.woa.com'
]

class TapdIframeBridge {
    constructor () {
        this.listeners = new Map()
        this._started = false
        this._parentOrigin = undefined
        this._onMessage = this._handleMessage.bind(this)
    }

    /**
     * 启动监听（幂等：多次调用只会注册一次）
     */
    start () {
        if (this._started) return
        if (typeof window === 'undefined') return
        window.addEventListener('message', this._onMessage)
        this._started = true
    }

    /**
     * 停止监听并清理所有订阅
     */
    stop () {
        if (!this._started) return
        window.removeEventListener('message', this._onMessage)
        this.listeners.clear()
        this._parentOrigin = undefined
        this._started = false
    }

    /**
     * 解析并缓存父窗口 origin：优先使用 document.referrer，若在白名单内则复用，
     * 否则退化到全量广播。这样可避免浏览器在 origin 不匹配时打印告警。
     */
    _resolveParentOrigin () {
        if (this._parentOrigin !== undefined) return this._parentOrigin
        let origin = ''
        try {
            const ref = (typeof document !== 'undefined' && document.referrer) || ''
            if (ref) {
                const url = new URL(ref)
                origin = `${url.protocol}//${url.host}`
            }
        } catch (e) {
            origin = ''
        }
        this._parentOrigin = this.isTrustedOrigin(origin) ? origin : ''
        return this._parentOrigin
    }

    /**
     * 向主站（parent）发送消息
     * @param {string} eventName 事件名（建议使用 TAPD_EVENTS 常量）
     * @param {*} [payload] 负载数据
     */
    post (eventName, payload) {
        if (typeof window === 'undefined') return
        if (!window.parent || window.parent === window) return
        const data = {
            type: TAPD_MSG_TYPE,
            event: eventName,
            payload: payload !== undefined ? payload : null
        }
        // 优先使用已识别的父 origin 精准投递，避免不必要的告警
        const parentOrigin = this._resolveParentOrigin()
        if (parentOrigin) {
            try {
                window.parent.postMessage(data, parentOrigin)
            } catch (e) {
                // 忽略非法 origin 导致的异常
            }
            return
        }
        // 兜底：对白名单中的 origin 各发一次（postMessage 会按 origin 过滤）
        TAPD_ORIGIN_WHITELIST.forEach(origin => {
            try {
                window.parent.postMessage(data, origin)
            } catch (e) {
                // 忽略非法 origin 导致的异常
            }
        })
    }

    /**
     * 订阅指定事件
     * @param {string} eventName
     * @param {(payload:any, event:MessageEvent)=>void} handler
     * @returns {Function} unsubscribe 函数
     */
    on (eventName, handler) {
        if (typeof handler !== 'function') return () => {}
        if (!this.listeners.has(eventName)) {
            this.listeners.set(eventName, new Set())
        }
        this.listeners.get(eventName).add(handler)
        return () => this.off(eventName, handler)
    }

    /**
     * 取消订阅
     */
    off (eventName, handler) {
        const set = this.listeners.get(eventName)
        if (set) set.delete(handler)
    }

    /**
     * 校验 origin 是否在白名单内
     */
    isTrustedOrigin (origin) {
        return !!origin && TAPD_ORIGIN_WHITELIST.indexOf(origin) !== -1
    }

    _handleMessage (event) {
        const data = event && event.data
        if (!data || data.type !== TAPD_MSG_TYPE || !data.event) return
        if (!this.isTrustedOrigin(event.origin)) return
        const set = this.listeners.get(data.event)
        if (!set || !set.size) return
        set.forEach(fn => {
            try {
                fn(data.payload, event)
            } catch (e) {
                // eslint-disable-next-line no-console
                console.error('[tapd-iframe-bridge] handler error:', e)
            }
        })
    }
}

// 单例导出：全局只注册一次 message 监听，避免重复分发
export default new TapdIframeBridge()
