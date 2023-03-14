import fetch from './fetch';

const apiPerfix = '/api';
const applyFix = 'ms/auth/api/user/auth/apply';
const IAM_PERFIX = '/ms/auth/api/user/auth/resource';
const projectPerfix = 'ms/project/api/user'
export default {
  getUser() {
    return fetch.get(`${projectPerfix}/users`);
  },
  getAllProjectList() {
    return fetch.get(`${projectPerfix}/projects?enabled=true`)
  },
  // 获取资源类型列表
  getResourceTypesList() {
    return fetch.get(`${applyFix}/listResourceTypes`);
  },
  // 获取动作列表
  getActionsList(resourceType: any) {
    return fetch.get(`${applyFix}/listActions?resourceType=${resourceType}`);
  },
  // 获取资源列表
  getResourceList(params: any) {
    const { projectId, resourceType, resourceName } = params;
    return fetch.get(`${IAM_PERFIX}/${projectId}/${resourceType}/listResources`)
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
  }
}
