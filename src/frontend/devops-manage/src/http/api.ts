import {
  IAM_PERFIX,
  ITSM_PERFIX,
  PROJECT_PERFIX,
  STORE_PERFIX,
  USER_PERFIX,
  OPERATE_CHANNEL,
  PIPELINES_PERFIX,
  ARTIFACTORY_PERFIX,
} from './constants';
import http from './fetch';
export default {
  getUser() {
    return http.get(`${PROJECT_PERFIX}/user/users`);
  },

  validateProjectName(name: string) {
    return http.put(`${PROJECT_PERFIX}/user/projects/project_name/names/validate/?name=${name}`, { globalError: false })
  },

  validateEnglishName(name: string) {
    return http.put(`${PROJECT_PERFIX}/user/projects/english_name/names/validate/?name=${name}`, { globalError: false })
  },

  getUserDetail() {
    return http.get(`${PROJECT_PERFIX}/user/users/detail/`);
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
    return http.put(`${PROJECT_PERFIX}/user/projects/${projectId}`, projectData, { globalError: false });
  },
  /**
   * 获取项目详情
   */
  requestProjectData(params: any) {
    const { englishName } = params;
    return http.get(`${PROJECT_PERFIX}/user/projects/${englishName}/show`, { globalError: false });
  },

  /**
   * 获取更新项目信息 -- diff详情
   */
  requestDiffProjectData(params: any) {
    const { englishName } = params;
    return http.get(`${PROJECT_PERFIX}/user/projects/${englishName}/diff`);
  },

  requestApprovalInfo(projectCode: string) {
    return http.get(`${ITSM_PERFIX}/${projectCode}`)
  },

  /**
   * 取消更新项目信息
   */
  cancelUpdateProject(params: any) {
    const { projectId } = params;
    return http.put(`${PROJECT_PERFIX}/user/projects/${projectId}/cancelUpdateProject`);
  },

  /**
   * 停用/启用项目
   */
  enabledProject(params: any) {
    const { projectId, enable } = params;
    return http.put(`${PROJECT_PERFIX}/user/projects/${projectId}/enable?enabled=${enable}`);
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
  async fetchUserGroupList(params: any, { page, pageSize }: any) {
    const { projectCode, resourceType, resourceCode } = params;
    return http.get(`${IAM_PERFIX}/${projectCode}/${resourceType}/${resourceCode}/listGroup?page=${page}&pageSize=${pageSize}`);
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

  async renameGroupName(params: any) {
    const { groupName, groupId, projectCode, resourceType } = params;
    return http.put(`${IAM_PERFIX}/group/${projectCode}/${resourceType}/${groupId}/rename`, {
      groupName
    });
  },

  async getResource(params: any) {
    const { projectCode, resourceType, resourceCode } = params;
    return http.get(`${IAM_PERFIX}/${projectCode}/${resourceType}/${resourceCode}/getResource`);
  },

  /**
   * 获取项目下全体成员(简单查询)
   */
  async getProjectMembers(projectId: string, params?: any) {
    const query = new URLSearchParams({
      ...params,
    }).toString();
    return http.get(`${IAM_PERFIX}/member/${projectId}/listProjectMembers?${query}`, {
      globalError: false
    });
  },
  /**
   * 获取项目下全体成员(复杂查询)
   */
  async getProjectMembersByCondition(projectId: string, params: any) {
    return http.post(`${IAM_PERFIX}/member/${projectId}/listProjectMembersByCondition`, {
      ...params,
      globalError: false
    });
  },
  /**
   * 获取项目成员有权限的用户组数量
   */
  async getMemberGroups(projectId: string, params: any) {
    const query = new URLSearchParams({
      ...params,
    }).toString();
    return http.get(`${IAM_PERFIX}/member/${projectId}/getMemberGroupCount?${query}`);
  },
  /**
   * 获取项目成员有权限的用户组
   */
  async getMemberGroupsDetails(projectId: string, resourceType: string, params: any) {
    const query = new URLSearchParams({
      ...params,
    }).toString();
    return http.get(`${IAM_PERFIX}/group/${projectId}/${resourceType}/getMemberGroupsDetails?${query}`);
  },
  /**
   * 批量续期组成员权限--无需进行审批
   */
  async batchRenewal(projectId: string, params?: any) {
    return http.put(`${IAM_PERFIX}/member/${projectId}/batch/renewal`, params);
  },
  /**
   * 批量移除用户组成员
   */
  async batchRemove(projectId: string, params?: any) {
    return http.DELETE(`${IAM_PERFIX}/member/${projectId}/batch/remove`, params);
  },
  /**
   * 单条续期
   */
  async renewal(projectId: string, params?: any) {
    return http.put(`${IAM_PERFIX}/member/${projectId}/renewal`, params);
  },
  /**
   * 批量交接用户组成员
   */
  async batchHandover(projectId: string, params?: any) {
    return http.put(`${IAM_PERFIX}/member/${projectId}/batch/handover`, params);
  },
  /**
   * 根据组织ID获取成员
   */
  async deptUsers(deptId: string) {
    return http.get(`${USER_PERFIX}/dept/${deptId}/users`);
  },
  /**
   * 获取（代码库、流水线、部署节点）授权列表
   */
  getResourceAuthList (projectId: string, params: any) {
    return http.post(`${USER_PERFIX}/auth/authorization/${projectId}/listResourceAuthorization`, params);
  },

  /**
   * 重置授权（代码库、流水线、部署节点） 
   */
  resetAuthorization (projectId: string, params: any) {
    return http.post(`${USER_PERFIX}/auth/authorization/${projectId}/resetResourceAuthorization`, params)
  },

  /**
   * 用户态-iam用户组_同步
   */
  syncGroupAndMember (projectId: string) {
    return http.put(`${IAM_PERFIX}/group/sync/${projectId}/syncGroupAndMember`);
  },
  syncGroupMember (projectId: string, groupId: any) {
    return http.put(`${IAM_PERFIX}/group/sync/${projectId}/${groupId}/syncGroupMember`);
  },
  /**
   * 用户移出项目
   */
  removeMemberFromProject (projectId: string, params: any) {
    return http.put(`${IAM_PERFIX}/member/${projectId}/batchRemoveMemberFromProject`, params);
  },
  /**
   * 重置资源授权管理
   */
  resetAllResourceAuthorization (projectId: string, params: any) {
    return http.post(`${USER_PERFIX}/auth/authorization/${projectId}/resetAllResourceAuthorization`, params);
  },
  /**
   * 重置资源授权管理
   */
  batchOperateCheck (projectId: string, batchOperateType: string , params: any) {
    return http.post(`${IAM_PERFIX}/member/${projectId}/batch/${batchOperateType}/check`, params);
  },
  /**
   * 获取所以成员同步状态
   */
  getSyncStatusOfAllMember (projectId: string) {
    return http.get(`${IAM_PERFIX}/group/sync/${projectId}/getStatusOfSync`);
  },

  removeMemberFromProjectCheck (projectId: string, params: any) {
    return http.post(`${IAM_PERFIX}/member/${projectId}/batchRemoveMemberFromProjectCheck`, params);
  },
  /**
   * 获取资源类型列表
   */
  getListResourceTypes () {
    return http.get(`${USER_PERFIX}/auth/apply/listResourceTypes`);
  },
  /**
   * 获取资源列表
   */
  getListResource (projectId: string, resourceType: string, params: any) {
    const query = new URLSearchParams({
      ...params,
    }).toString();
    return http.get(`${USER_PERFIX}/auth/resource/${projectId}/${resourceType}/listResources?${query}`);
  },
  /**
   * 展示动作列表
   */
  getListActions (resourceType: string) {
    return http.get(`${USER_PERFIX}/auth/apply/listActions?resourceType=${resourceType}`);
  },

  syncGroupPermissions (projectId: string, groupId: any) {
    return http.put(`${IAM_PERFIX}/group/sync/${projectId}/${groupId}/syncGroupPermissions`);
  },

  syncDeleteGroupPermissions (projectId: string, groupId: any) {
    return http.delete(`${IAM_PERFIX}/group/sync/${projectId}/${groupId}/deleteGroupPermissions`);
  },
  /**
  * 单条移出
  */
  getIsDirectRemove(projectId: string, groupId: number, params: any) {
    return http.DELETE(`${IAM_PERFIX}/member/${projectId}/single/${groupId}/${OPERATE_CHANNEL}/remove`, params);
  },
  /**
   * 获取资源授权管理数量
   */
  getResourceType2CountOfHandover(params: any) {
    return http.post(`${USER_PERFIX}/auth/handover/getResourceType2CountOfHandover`, params);
  },
  /**
   * 获取交接单中授权相关
   */
  listAuthorizationsOfHandover(params: any) {
    return http.post(`${USER_PERFIX}/auth/handover/listAuthorizationsOfHandover`, params);
  },
  /**
   * 获取交接单中用户组相关
   */
  listGroupsOfHandover(params: any) {
    return http.post(`${USER_PERFIX}/auth/handover/listGroupsOfHandover`, params);
  },
  /**
   * 根据流水线方言获取流水线数量
   */
  countInheritedDialectPipeline(projectId: string) {
    return http.get(`${PIPELINES_PERFIX}/${projectId}/countInheritedDialectPipeline`);
  },
  /**
   * 根据流水线方言获取流水线列表
   */
  listInheritedDialectPipelines(projectId: string, params: any) {
    const query = new URLSearchParams({
      ...params,
    }).toString();
    return http.get(`${PIPELINES_PERFIX}/${projectId}/listInheritedDialectPipelines?${query}`);
  },
  /**
   * 获取项目制品质量元数据标签列表
   */
  getMetadataList (projectId : string) {
    return http.get(`${ARTIFACTORY_PERFIX}/quality/metadata/${projectId}`);
  },
  /**
   * 创建项目制品质量元数据标签
   */
  createdMetadata (projectId : string, params: any) {
    return http.post(`${ARTIFACTORY_PERFIX}/quality/metadata/${projectId}`, params);
  },
  /**
   * 更新项目制品质量元数据标签
   */
  updateMetadata (projectId : string, labelKey: string, params: any) {
    return http.put(`${ARTIFACTORY_PERFIX}/quality/metadata/${projectId}/${labelKey}`, params);
  },
  /**
   * 删除项目制品质量元数据标签
   */
  deleteMetadata (projectId : string, labelKey: string) {
    return http.delete(`${ARTIFACTORY_PERFIX}/quality/metadata/${projectId}/${labelKey}`);
  },
  /**
   * 批量保存项目制品质量元数据标签
   */
  batchUpdateMetadata(projectId : string, params: any) {
    return http.post(`${ARTIFACTORY_PERFIX}/quality/metadata/${projectId}/batch`, params);
  },
};
