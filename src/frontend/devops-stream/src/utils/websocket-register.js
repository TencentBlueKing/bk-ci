import { getWSpath } from './index'
import { createRefreshTask } from '../../../common-lib/websocket/realtime'

const register = {
    callBacks: {},
    refreshTasks: {},

    execCallBacks (res) {
        const keys = Object.keys(register.callBacks)
        keys.forEach((key) => {
            const cb = register.callBacks[key]
            cb(res)
        })
    },

    installWsMessage (callBack, key, id, refresh) {
        register.unInstallWsMessage(id)
        if (refresh) register.refreshTasks[id] = createRefreshTask({ load: refresh, interval: 2000 })
        register.callBacks[id] = (res = {}) => {
            if (res.source !== window || res.origin !== window.location.origin) return
            const { webSocketType, module, page, message } = res.data || {}
            const wsKey = webSocketType + module
            const wsPath = getWSpath(location.href.replace(/[\?#].+$/, ''))
            if (wsKey === key && wsPath.includes(page)) {
                if (refresh) register.refreshTasks[id].refresh()
                else {
                    try { callBack(JSON.parse(message || '{}')) } catch (_) {}
                }
            }
        }
    },

    unInstallWsMessage (id) {
        register.refreshTasks[id]?.dispose()
        delete register.refreshTasks[id]
        if (register.callBacks[id]) {
            delete register.callBacks[id]
        }
    }
}

window.addEventListener('message', register.execCallBacks)

export default register
