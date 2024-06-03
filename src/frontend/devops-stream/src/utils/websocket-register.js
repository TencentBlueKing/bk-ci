import { getWSpath } from './index'

const register = {
    callBacks: {},

    execCallBacks (res) {
        const keys = Object.keys(register.callBacks)
        keys.forEach((key) => {
            const cb = register.callBacks[key]
            cb(res)
        })
    },

    installWsMessage (callBack, key, id) {
        register.callBacks[id] = (res = {}) => {
            const { webSocketType, module, page, message } = res.data || {}
            const wsKey = webSocketType + module
            const wsPath = getWSpath(location.href.replace(/[\?#].+$/, ''))
            if (wsKey === key && wsPath.includes(page)) {
                const parseMessage = JSON.parse(message || '{}')
                callBack(parseMessage)
            }
        }
    },

    unInstallWsMessage (id) {
        if (register.callBacks[id]) {
            delete register.callBacks[id]
        }
    }
}

window.addEventListener('message', register.execCallBacks)

export default register
