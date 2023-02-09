import http from './fetch';
import {
  API_PERFIX,
  STORE_PERFIX,
  PROJECT_PERFIX,
  IAM_PERFIX,
} from './constants';
export default {
  getUser() {
    return http.get(`${PROJECT_PERFIX}/users`);
  },
  /** *
   * 获取已安装的扩展列表
   */
  requestInstalledServiceList(projectCode: any) {
    return http.get(`${STORE_PERFIX}/user/market/service/project/${projectCode}/installed/service`);
  },
  /** *
   * 获取微扩展点列表
   */
  requestServiceItemList() {
    return http.get(`${PROJECT_PERFIX}/user/ext/items`);
  },
  /** *
   * 卸载扩展服务
   */
  uninstallService(params: any) {
    const { serviceCode, projectCode } = params;
    const reasonList = [{
      reasonId: '',
      note: '',
    }];
    return http.put(`${STORE_PERFIX}/user/market/service/project/${projectCode}/serviceCodes/${serviceCode}/uninstalled`, { reasonList });
  },
  /**
   * 创建项目
   */
  requestCreateProject(params: any) {
    const { projectData } = params;
    return http.post(`${PROJECT_PERFIX}/user/projects/`, projectData);
  },
  /**
   * 修改项目信息
   */
  requestUpdateProject(params: any) {
    const { projectId, projectData } = params;
    return http.put(`${PROJECT_PERFIX}/user/projects/${projectId}`, projectData);
  },
  /**
   * 获取项目详情
   */
  requestProjectData(params: any) {
    const { englishName } = params;
    return http.get(`${PROJECT_PERFIX}/user/projects/${englishName}`);
  },

  /**
   * 获取更新项目信息 -- diff详情
   */
  requestDiffProjectData(params: any) {
    const { englishName } = params;
    return http.get(`${PROJECT_PERFIX}/user/projects/${englishName}/diff`);
  },

  /**
   * 取消创建项目
   */
  cancelCreateProject(params: any) {
    const { projectId } = params;
    return http.put(`${PROJECT_PERFIX}/user/projects/${projectId}/cancelCreateProject`);
  },
  /**
   * 获取公司组织列表
   */
  getOrganizations(params: any) {
    const { type, id } = params;
    return http.get(`${PROJECT_PERFIX}/user/organizations/types/${type}/ids/${id}`);
  },

  /**
   * 上传项目logo
   */
  async uploadProjectLogo(params: any) {
    const { formData } = params;
    return http.post(`${PROJECT_PERFIX}/user/projects/upload/logo`, formData, {
      disabledResponseType: true,
    });
  },

  /**
   * 用户组-用户退出
   */
  signOutGroup(params: any) {
    const { projectCode, resourceType, groupId } = params;
    return http.delete(`${IAM_PERFIX}/group/${projectCode}/${resourceType}/${groupId}/member`);
  },

  /**
   * 用户组-用户续期
   */
  renewalGroup(params: any) {
    const { projectCode, resourceType, groupId } = params;
    return http.delete(`${IAM_PERFIX}/group/${projectCode}/${resourceType}/${groupId}/member/renewal`);
  },

  /**
   * 流水线/流水线组 开启用户组权限管理
   */
  async enableGroupPermission(params: any) {
    const { projectCode, resourceType, resourceCode } = params;
    return http.put(`${IAM_PERFIX}/${projectCode}/${resourceType}/${resourceCode}/enable`);
  },
  /**
   * 流水线/流水线组 关闭用户组权限管理
   */
  async disableGroupPermission(params: any) {
    const { projectCode, resourceType, resourceCode } = params;
    return http.put(`${IAM_PERFIX}/${projectCode}/${resourceType}/${resourceCode}/disable`);
  },

  /**
   * 获取是否为资源的管理员
   */
  async fetchHasManagerPermission(params: any) {
    const { projectCode, resourceType, resourceCode } = params;
    return http.get(`${IAM_PERFIX}/${projectCode}/${resourceType}/${resourceCode}/hasManagerPermission`);
  },

  /**
   * 是否启用权限管理
   */
  async fetchEnablePermission(params: any) {
    const { projectCode, resourceType, resourceCode } = params;
    return http.get(`${IAM_PERFIX}/${projectCode}/${resourceType}/${resourceCode}/isEnablePermission`);
  },

  /**
   * 获取用户组列表
   */
  async fetchUserGroupList(params: any) {
    const { projectCode, resourceType, resourceCode } = params;
    return http.get(`${IAM_PERFIX}/${projectCode}/${resourceType}/${resourceCode}/listGroup`);
  },

  /**
   * 获取用户所属组
   */
  async fetchGroupMember(params: any) {
    const { projectCode, resourceType, resourceCode } = params;
    return http.get(`${IAM_PERFIX}/${projectCode}/${resourceType}/${resourceCode}/groupMember`);
  },

  /**
   * 获取组策略详情
   */
  async fetchGroupPolicies(params: any) {
    const { projectCode, resourceType, groupId } = params;
    return http.get(`${IAM_PERFIX}/group/${projectCode}/${resourceType}/${groupId}/groupPolicies`);
  },

  /**
   * 获取资源列表
   */
  async fetchResourceList(params: any) {
    const { projectCode, resourceType } = params;
    return http.get(`${IAM_PERFIX}/${projectCode}/${resourceType}/listResources`);
  },

  /**
   * 删除组
   */
  async deleteGroup(params: any) {
    const { projectCode, resourceType, groupId } = params;
    return http.delete(`${IAM_PERFIX}/group/${projectCode}/${resourceType}/${groupId}`);
  },
};
