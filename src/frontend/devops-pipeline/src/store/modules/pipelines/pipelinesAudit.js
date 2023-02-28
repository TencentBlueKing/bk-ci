import ajax from '@/utils/request'
import {
    PROCESS_API_URL_PREFIX,
    AUTH_URL_PREFIX
} from '@/store/constants'

const prefix = `/${PROCESS_API_URL_PREFIX}/user/pipelines/audit`
const AUTH_PREFIX = `/${AUTH_URL_PREFIX}/user/auth/resource`

const state = {

}

const getters = {

}

const mutations = {

}

const actions = {
    getUserAudit (_, { projectId, userId, resourceName, status, startTime, endTime, current, limit }) {
        return ajax.get(
            `${prefix}/${projectId}/pipeline`, {
                params: {
                    page: current,
                    pageSize: limit,
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
    },
    /**
   * 获取是否为资源的管理员
   */
    async fetchHasManagerPermission (_, params) {
        const { projectCode, resourceType, resourceCode } = params
        return ajax.get(`${AUTH_PREFIX}/${projectCode}/${resourceType}/${resourceCode}/hasManagerPermission`)
    },
    /**
   * 是否启用权限管理
   */
    async fetchEnablePermission (_, params) {
        const { projectCode, resourceType, resourceCode } = params
        return ajax.get(`${AUTH_PREFIX}/${projectCode}/${resourceType}/${resourceCode}/isEnablePermission`)
    },
    /**
   * 流水线/流水线组 开启用户组权限管理
   */
    async enableGroupPermission (_, params) {
        const { projectCode, resourceType, resourceCode } = params
        return ajax.put(`${AUTH_PREFIX}/${projectCode}/${resourceType}/${resourceCode}/enable`)
    },
    /**
   * 流水线/流水线组 关闭用户组权限管理
   */
    async disableGroupPermission (_, params) {
        const { projectCode, resourceType, resourceCode } = params
        return ajax.put(`${AUTH_PREFIX}/${projectCode}/${resourceType}/${resourceCode}/disable`)
    },
    /**
   * 获取用户组列表
   */
    async fetchUserGroupList (_, params) {
        const { projectCode, resourceType, resourceCode } = params
        return ajax.get(`${AUTH_PREFIX}/${projectCode}/${resourceType}/${resourceCode}/listGroup`)
    },
    /**
     * 获取用户所属组
     */
    async fetchGroupMember (_, params) {
        const { projectCode, resourceType, resourceCode } = params
        return ajax.get(`${AUTH_PREFIX}/${projectCode}/${resourceType}/${resourceCode}/groupMember`)
    },
    /**
     * 删除用户组
     */
    deleteGroup (_, params) {
        const { projectCode, resourceType, groupId } = params
        return ajax.delete(`${AUTH_PREFIX}/group/group/${projectCode}/${resourceType}/${groupId}`)
    }
}

export default {
    state,
    getters,
    mutations,
    actions
}
