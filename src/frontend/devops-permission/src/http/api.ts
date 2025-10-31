import fetch from './fetch';
import { OPERATE_CHANNEL } from "@/utils/constants";

const apiPerfix = '/ms/auth/api/user';
const projectPerfix = 'ms/project/api/user'
const repositoryPerfix = 'ms/repository/api/user'

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
    return fetch.get(`${repositoryPerfix}/repositories/oauth`)
  },
  // 删除oauth授权
  deleteOauth(type: any, username: string) {
    return fetch.delete(`${repositoryPerfix}/repositories/oauth/delete?scmCode=${type}&oauthUserId=${username}`)
  },
  getOauthRelSource(params: any) {
    const query = new URLSearchParams({
      ...params,
    }).toString();
    return fetch.get(`${repositoryPerfix}/repositories/oauth/relSource?${query}`)
  },
  refreshOauth(oauthType: any, username: string, redirectUrl: any) {
    return fetch.post(`${repositoryPerfix}/repositories/oauth/reset?scmCode=${oauthType}&oauthUserId=${username}&redirectUrl=${redirectUrl}`)
  },
  /**
   * 获取（代码库、流水线、部署节点）授权列表
   */
  getResourceAuthList(projectId: string, params: any) {
    return fetch.post(`${apiPerfix}/auth/authorization/${projectId}/listResourceAuthorization?operateChannel=PERSONAL`, params);
  },
  /**
   * 批量交接用户组成员
   */
  batchHandover(projectId: string, params?: any) {
    return fetch.put(`${apiPerfix}/auth/resource/member/${projectId}/batch/personal/handover`, params);
  },
  /**
   * 批量移除用户组成员
   */
  batchRemove(projectId: string, params?: any) {
    return fetch.DELETE(`${apiPerfix}/auth/resource/member/${projectId}/batch/personal/remove`, params);
  },
  /**
   * 重置资源授权管理
   */
  batchOperateCheck(projectId: string, batchOperateType: string, params: any) {
    return fetch.post(`${apiPerfix}/auth/resource/member/${projectId}/batch/${batchOperateType}/check`, params);
  },
  /**
   * 获取项目成员有权限的用户组数量
   */
  getMemberGroups(projectId: string, params: any) {
    const query = new URLSearchParams({
      ...params,
    }).toString();
    return fetch.get(`${apiPerfix}/auth/resource/member/${projectId}/getMemberGroupCount?${query}`);
  },
  /**
   * 获取项目成员有权限的用户组
   */
  getMemberGroupsDetails(projectId: string, resourceType: string, params: any) {
    const query = new URLSearchParams({
      ...params,
    }).toString();
    return fetch.get(`${apiPerfix}/auth/resource/group/${projectId}/${resourceType}/getMemberGroupsDetails?${query}`);
  },
  /**
   * 获取项目下全体成员(简单查询)
   */
  async getProjectMembers(projectId: string, params?: any) {
    const query = new URLSearchParams({
      ...params,
    }).toString();
    return fetch.get(`${apiPerfix}/auth/resource/member/${projectId}/listProjectMembers?${query}`, {
      globalError: false
    });
  },
  /**
   * 获取用户已加入的项目列表
   */
  fetchProjectList() {
    return fetch.get(`${projectPerfix}/projects?enabled=true&queryAuthorization=true`)
  },
  /**
   * 获取用户有授权的项目列表
   */
  fetchProjectsWithAuthorization() {
    return fetch.get(`${apiPerfix}/auth/authorization/listUserProjectsWithAuthorization`)
  },
  /**
   * 重置授权（代码库、流水线、部署节点） 
   */
  resetAuthorization(projectId: string, params: any) {
    return fetch.post(`${apiPerfix}/auth/handover/${projectId}/handoverAuthorizationsApplication`, params)
  },
  /**
   * 校验是否可交接
   */
  checkAuthorization(projectId: string, params: any) {
    return fetch.post(`${apiPerfix}/auth/authorization/${projectId}/resetResourceAuthorization`, params)
  },
  /**
   * 展示动作列表
   */
  getListActions(resourceType: string) {
    return fetch.get(`${apiPerfix}/auth/apply/listActions?resourceType=${resourceType}`);
  },
  /**
   * 获取资源列表
   */
  getListResource(projectId: string, resourceType: string, params: any) {
    const query = new URLSearchParams({
      ...params,
    }).toString();
    return fetch.get(`${apiPerfix}/auth/resource/${projectId}/${resourceType}/listResources?${query}`);
  },
  /**
   * 单条续期
   */
  async renewal(projectId: string, resourceType: string, groupId: number, params: any) {
    return fetch.put(`${apiPerfix}/auth/resource/group/${projectId}/${resourceType}/${groupId}/member/renewal`, params);
  },
  /**
   * 获取资源授权管理数量
   */
  getResourceType2CountOfHandover(params: any) {
    return fetch.post(`${apiPerfix}/auth/handover/getResourceType2CountOfHandover`, params);
  },
  /**
   * 获取交接单中授权相关
   */
  listAuthorizationsOfHandover(params: any) {
    return fetch.post(`${apiPerfix}/auth/handover/listAuthorizationsOfHandover`, params);
  },
  /**
   * 获取交接单中用户组相关
   */
  listGroupsOfHandover(params: any) {
    return fetch.post(`${apiPerfix}/auth/handover/listGroupsOfHandover`, params);
  },
  /**
   * 获取移交退出时的单条数据
   */
  getMemberGroupDetails(projectId: string, resourceType: string, groupId: number, memberId: string) {
    return fetch.get(`${apiPerfix}/auth/resource/group/${projectId}/${resourceType}/${groupId}/getMemberGroupDetails?memberId=${memberId}`);
  },
  /**
   * 获取交接单列表
   */
  fetchHandoverOverviewList(params: any) {
    return fetch.post(`${apiPerfix}/auth/handover/listHandoverOverviews`, params);
  },
  /**
   * 处理交接审批单
   */
  handleHanoverApplication(params: any) {
    return fetch.post(`${apiPerfix}/auth/handover/handleHanoverApplication`, params);
  },
  /**
  * 单条退出
  */
  getIsDirectRemove(projectId: string, groupId: number, params: any) {
    return fetch.DELETE(`${apiPerfix}/auth/resource/member/${projectId}/single/${groupId}/${OPERATE_CHANNEL}/remove`, params);
  },
  /**
   * 批量处理交接审批单
   */
  handleBatchHandovers(params: any) {
    return fetch.post(`${apiPerfix}/auth/handover/batchHandleHanoverApplications`, params);
  }
}
