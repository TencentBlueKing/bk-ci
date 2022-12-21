import api from './ajax'
import { PROJECT_PERFIX, STREAM_PERFIX, REPOSITORY_PREFIX, AUTH_PERFIX } from './perfix'

export default {
    getUserInfo () {
        return api.get(`${PROJECT_PERFIX}/user/users`)
    },

    getProjectInfo (projectPath) {
        return api.get(`${STREAM_PERFIX}/user/gitcode/projects/info?gitProjectId=${projectPath}`)
    },

    oauth (redirectUrl) {
        return api.get(`${REPOSITORY_PREFIX}/user/git/isOauth?redirectUrl=${redirectUrl}`)
    },

    getStreamGroups (page = 1, limit = 20) {
        return api.get(`${STREAM_PERFIX}/user/groups/list?page=${page}&pageSize=${limit}`).then(response => {
            return response
        })
    },

    getStreamProjects (type = 'MY_PROJECT', page = 1, limit = 20, search = '', isAwait = false) {
        const querySearch = (search && search.trim()) ? `&search=${search.trim()}` : ''
        if (isAwait) {
            return api.get(`${STREAM_PERFIX}/user/projects/${type}/list?page=${page}&pageSize=${limit}${querySearch}`).then(response => {
                return response
            })
        } else {
            return api.get(`${STREAM_PERFIX}/user/projects/${type}/list?page=${page}&pageSize=${limit}${querySearch}`)
        }
    },

    getRecentProjects (size = 4) {
        return api.get(`${STREAM_PERFIX}/user/projects/history?size=${size}`)
    },

    getLoginUrl (type) {
        return api.get(`${STREAM_PERFIX}/external/stream/login/url?type=${type}`)
    },

    logout () {
        return api.delete(`${AUTH_PERFIX}/user/third/login/out`)
    }
}
