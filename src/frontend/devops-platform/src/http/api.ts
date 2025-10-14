import fetch from './fetch';

const repositoryApiPerfix = '/repository/api/user';
const projectPerfix = '/project/api/user';

export default {
  // 获取代码库配置
  fetchRepoConfigList ({ page, pageSize }) {
    return fetch.get(`${repositoryApiPerfix}/repositories/config/listConfig?page=${page}&pageSize=${pageSize}`);
  },
  // 启用/禁用代码库配置
  deleteRepoConfig (scmCode: string) {
    return fetch.delete(`${repositoryApiPerfix}/repositories/config/${scmCode}`);
  },
  // 启用/禁用代码库配置
  toggleEnableRepoConfig (scmCode: string, type: string) {
    return fetch.put(`${repositoryApiPerfix}/repositories/config/${scmCode}/${type}`);
  },
  // 更新代码库配置
  updateRepoConfig (scmCode: string, param: object) {
    return fetch.put(`${repositoryApiPerfix}/repositories/config/${scmCode}`, param);
  },
  // 创建代码库配置
  createRepoConfig (param: object) {
    return fetch.post(`${repositoryApiPerfix}/repositories/config`, param);
  },
  fetchListProvider () {
    return fetch.get(`${repositoryApiPerfix}/repositories/config/listProvider`);
  },
  uploadConfigLog ({ formData }) {
    return fetch.post(`${repositoryApiPerfix}/repositories/config/uploadLogo`, formData, {
      disabledResponseType: true
    });
  },
  requestVisibleList (scmCode: string) {
    return fetch.get(`${repositoryApiPerfix}/repositories/config/${scmCode}/dept`);
  },
  setVisibleDept (scmCode: string, param: object) {
    return fetch.post(`${repositoryApiPerfix}/repositories/config/${scmCode}/dept`, param);
  },
  deleteVisibleDept (scmCode: string, param: Array) {
    return fetch.delete(`${repositoryApiPerfix}/repositories/config/${scmCode}/dept`, param);
  },
  getOrganizationsList(type: string, id: string) {
    return fetch.get(`${projectPerfix}/organizations/types/${type}/ids/${id}`);
  },
};
