import http from './fetch';
import {
  MANAGE_PERFIX,
  API_PERFIX,
  STORE_PERFIX,
  PROJECT_PERFIX,
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
};
