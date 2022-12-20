import http from './fetch';
import {
  MANAGE_PERFIX,
  API_PERFIX,
  STORE_PERFIX,
  PROJECT_PERFIX,
  IAM_PERFIX,
} from './constants';
export default {
  getUser() {
    return http.get(`${API_PERFIX}/user`);
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
      disabledResponseType: true
    })
  },

  /**
   * 流水线/流水线组 开启用户组权限管理
   */
  async enableGroupPermission(params: any) {
    const { id } = params;
    return http.put(`${IAM_PERFIX}/${id}/enable`) 
  },
  /**
   * 流水线/流水线组 关闭用户组权限管理
   */
  async disableGroupPermission(params: any) {
    const { id } = params;
    return http.put(`${IAM_PERFIX}/${id}/disable`) 
  },

  /**
   * 获取是否为资源的管理员
   */
  async fetchResourceManager(params: any) {
    const { id } = params;
    delete params.id;
    return http.get(`${IAM_PERFIX}/${id}/isResourceManager`, params)
  },

  /**
   * 获取资源关联的二级管理员用户组信息
   */
  async fetchUserGroups(params: any) {
    const { id } = params;
    return http.get(`${IAM_PERFIX}/${id}/subsetGroups`)
  },

  /**
   * 获取用户归属组信息（普通用户）
   */
  async fetchUserBelongGroup(params: any) {
    const { id } = params;
    return http.get(`${IAM_PERFIX}/${id}/userBelongGroup`)
  },

  /**
   * 获取组权限详情（策略详情）（普通用户）
   */
  async fetchGroupPolicies(params: any) {
    const { id } = params;
    return http.get(`${IAM_PERFIX}/${id}/groupPolicies`)
  },
};
