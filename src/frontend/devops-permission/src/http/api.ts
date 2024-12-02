import fetch from './fetch';

const apiPerfix = '/api';
const applyFix = 'ms/auth/api/user/auth/apply';
const IAM_PERFIX = '/ms/auth/api/user/auth/resource';
const projectPerfix = 'ms/project/api/user'
export default {
  getUser() {
    return fetch.get(`${projectPerfix}/users`);
  },
  getAllProjectList(params) {
    return fetch.get(`${projectPerfix}/projects/listProjectsForApply`, params)
  },
  /**
   * 获取资源类型列表
   */
  getResourceTypesList() {
    return fetch.get(`${applyFix}/listResourceTypes`);
  },
  // 获取动作列表
  getActionsList(resourceType: any) {
    return fetch.get(`${applyFix}/listActions?resourceType=${resourceType}`);
  },
  // 获取资源列表
  getResourceList(params: any, pageInfo: any) {
    const { projectId, resourceType, resourceName } = params;
    return fetch.get(`${IAM_PERFIX}/${projectId}/${resourceType}/listResources`, {
      ...pageInfo,
      resourceName
    })
  },
  // 获取用户组列表
  getUserGroupList(params: any) {
    const { projectId } = params;
    delete params.projectId;
    return fetch.post(`${applyFix}/${projectId}/listGroups`, params);
  },
  // 申请加入用户组实体
  applyToJoinGroup(params: any) {
    return fetch.post(`${applyFix}/applyToJoinGroup`, params);
  },
  // 查询用户组权限详情
  getGroupPermissionDetail(groupId: any) {
    return fetch.get(`${applyFix}/${groupId}/getGroupPermissionDetail`)
  },
  getProjectsList() {
    return fetch.get(`${projectPerfix}/projects?queryAuthorization=true`)
  },
  /**
   * 批量交接用户组成员
   */
  batchHandover(projectId: string, params?: any) {
    return fetch.put(`${IAM_PERFIX}/member/${projectId}/batch/personal/handover`, params);
  },
  /**
   * 批量移除用户组成员
   */
  batchRemove(projectId: string, params?: any) {
    return fetch.DELETE(`${IAM_PERFIX}/member/${projectId}/batch/personal/remove`, params);
  },
  /**
   * 重置资源授权管理
   */
  batchOperateCheck(projectId: string, batchOperateType: string, params: any) {
    return fetch.post(`${IAM_PERFIX}/member/${projectId}/batch/${batchOperateType}/check`, params);
  },
  /**
   * 获取项目成员有权限的用户组数量
   */
  getMemberGroups(projectId: string, params: any) {
    const query = new URLSearchParams({
      ...params,
    }).toString();
    return fetch.get(`${IAM_PERFIX}/member/${projectId}/getMemberGroupCount?${query}`);
  },
  /**
   * 获取项目成员有权限的用户组
   */
  getMemberGroupsDetails(projectId: string, resourceType: string, params: any) {
    const query = new URLSearchParams({
      ...params,
    }).toString();
    return fetch.get(`${IAM_PERFIX}/group/${projectId}/${resourceType}/getMemberGroupsDetails?${query}`);
  },
  /**
   * 获取项目下全体成员(简单查询)
   */
  getProjectMembers(projectId: string, params?: any) {
    const query = new URLSearchParams({
      ...params,
    }).toString();
    return fetch.get(`${IAM_PERFIX}/member/${projectId}/listProjectMembers?${query}`, {
      globalError: false
    });
  },
  /**
   * 展示动作列表
   */
  getListActions(resourceType: string) {
    return fetch.get(`${applyFix}/listActions?resourceType=${resourceType}`);
  },
  /**
   * 获取资源列表
   */
  getListResource(projectId: string, resourceType: string, params: any) {
    const query = new URLSearchParams({
      ...params,
    }).toString();
    return fetch.get(`${IAM_PERFIX}/${projectId}/${resourceType}/listResources?${query}`);
  },
  /**
   * 单条续期
   */
  async renewal(projectId: string, resourceType: string, groupId: number, params: any) {
    return fetch.put(`${IAM_PERFIX}/group/${projectId}/${resourceType}/${groupId}/member/renewal`, params);
  },
}
