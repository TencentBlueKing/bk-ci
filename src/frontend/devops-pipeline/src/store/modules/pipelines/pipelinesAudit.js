import ajax from '@/utils/request'
import {
    PROCESS_API_URL_PREFIX
} from '@/store/constants'

const prefix = `/${PROCESS_API_URL_PREFIX}/user/pipelines/audit`

const state = {

}

const getters = {

}

const mutations = {

}

const actions = {
    getUserAudit (_, { projectId, userId, resourceName, resourceId, status, startTime, endTime, current, limit }) {
        return ajax.get(
            `${prefix}/${projectId}/pipeline`, {
                params: {
                    page: current,
                    pageSize: limit,
                    resourceId: resourceId || undefined,
                    resourceName: resourceName || undefined,
                    userId: userId || undefined,
                    status: status || undefined,
                    startTime: startTime || undefined,
                    endTime: endTime || undefined
                }
            }
        ).then(response => {
            return response.data
        })
    }
}

export default {
    state,
    getters,
    mutations,
    actions
}
