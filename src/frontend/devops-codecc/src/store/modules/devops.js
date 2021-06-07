import request from '@/common/request'

export default {
    namespaced: true,
    state: {
    },
    mutations: {
    },
    actions: {
        // 第一次拉取日志
        getInitLog ({ commit }, { projectId, pipelineId, buildId, tag, currentExe }) {
            let url = `/log/api/user/logs/${projectId}/${pipelineId}/${buildId}`
            if (tag || currentExe) url += '?'
            if (tag) url += `tag=${tag}`
            if (tag && currentExe) url += '&'
            if (currentExe) url += `executeCount=${currentExe}`
            return request.get(url)
        },

        getAfterLog ({ commit }, { projectId, pipelineId, buildId, tag, currentExe, lineNo }) {
            return request.get(`/log/api/user/logs/${projectId}/${pipelineId}/${buildId}/after?start=${lineNo}${currentExe ? '&executeCount=' + currentExe : ''}${tag ? '&tag=' + tag : ''}`)
        }
    }
}
