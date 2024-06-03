import api from './ajax'
import { STREAM_PERFIX } from './perfix'

export default {
    getUserMessages (params) {
        return api.get(`${STREAM_PERFIX}/user/messages`, { params })
    },

    readAllMessages (projectId) {
        return api.put(`${STREAM_PERFIX}/user/messages/read?projectId=${projectId}`)
    },

    readMessage (id, projectId) {
        return api.put(`${STREAM_PERFIX}/user/messages/${id}/read?projectId=${projectId}`)
    },

    getUnreadNotificationNum (projectId) {
        return api.get(`${STREAM_PERFIX}/user/messages/noread`, { params: { projectId } })
    }
}
