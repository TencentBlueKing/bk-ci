import fetch from './fetch';

const apiPerfix = '/ms/auth/api/user';
const projectPerfix = 'ms/project/api/user'
export default {
  getUser() {
    return fetch.get(`${projectPerfix}/users`);
  },
  getAllProjectList(params) {
    return fetch.get(`${projectPerfix}/projects/listProjectsForApply`, params)
  },
  // 获取资源类型列表
  getResourceTypesList() {
    return fetch.get(`${apiPerfix}/auth/apply/listResourceTypes`);
  },
  // 获取动作列表
  getActionsList(resourceType: any) {
    return fetch.get(`${apiPerfix}/auth/apply/listActions?resourceType=${resourceType}`);
  },
  // 获取资源列表
  getResourceList(params: any, pageInfo: any) {
    const { projectId, resourceType, resourceName } = params;
    return fetch.get(`${apiPerfix}/auth/resource/${projectId}/${resourceType}/listResources`, {
      ...pageInfo,
      resourceName
    })
  },
  // 获取用户组列表
  getUserGroupList(params: any) {
    const { projectId } = params;
    delete params.projectId;
    return fetch.post(`${apiPerfix}/auth/apply/${projectId}/listGroups`, params);
  },
  // 申请加入用户组实体
  applyToJoinGroup(params: any) {
    return fetch.post(`${apiPerfix}/auth/apply/applyToJoinGroup`, params);
  },
  // 查询用户组权限详情
  getGroupPermissionDetail(groupId: any) {
    return fetch.get(`${apiPerfix}/auth/apply/${groupId}/getGroupPermissionDetail`)
  },
  // 获取oauth授权列表
  getOauthResource() {
    return fetch.get(`${apiPerfix}/oauth/resource`)
  },
  // 删除oauth授权
  deleteOauth(type: any) {
    return fetch.delete(`${apiPerfix}/oauth/resource/delete?oauthType=${type}`)
  },
  getOauthRelSource(type: any, page: Number, pageSize: Number) {
    return fetch.get(`${apiPerfix}/oauth/resource/relSource?oauthType=${type}&page=${page}&pageSize=${pageSize}`)
  },
  refreshOauth(oauthType: any, redirectUrl: any) {
    return fetch.post(`${apiPerfix}/oauth/resource/reOauth?oauthType=${oauthType}&redirectUrl=${redirectUrl}`)
  },
  /**
   * 获取（代码库、流水线、部署节点）授权列表
   */
  getResourceAuthList (projectId: string, params: any) {
    return fetch.post(`${apiPerfix}/auth/authorization/${projectId}/listResourceAuthorization`, params);
  },
}
