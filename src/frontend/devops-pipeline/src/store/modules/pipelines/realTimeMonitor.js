import {
    MERTICS_URL_PREFIX,
} from '@/store/constants'
import ajax from '@/utils/request'


const prefix = `/${MERTICS_URL_PREFIX}/user/metrics/query/`
const prefixBuild ='/ms/environment/api/user/envnode/'
const stateParams = {
    format: "table",
    type: "instant"
}

const state = {
    selectDataRange:[]
}

const actions = {
    getMetrics (_, params) {
        const paramsMerge = {
            ...stateParams,
            ...params
        }
        return ajax.post(`${prefix}`, paramsMerge).then(response => {
            return response.data
        })
    },

    getBuildResource (_,params) {

        return ajax.post(`${prefixBuild}/${params}`, {}).then(response => {
            return response.data
        })
    }
}

export default {
    namespaced: true,
    actions,
    state,
}