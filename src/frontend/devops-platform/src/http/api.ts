import {
  TargetNetBehavior,
} from '@/types';
import { STORE_TYPE, BASE_PREFIX } from '@/common/constants';
import fetch from './fetch';

const repositoryApiPerfix = '/repository/api/user';

export default {
  // 获取代码库配置
  fetchRepoConfigList () {
    return fetch.get(`${repositoryApiPerfix}/repositories/config/listConfig`);
  },
  // 禁用代码库配置
  disableRepoConfig (scmCode: string) {
    return fetch.put(`${repositoryApiPerfix}/repositories/config/${scmCode}/disable`);
  },
  // 启用代码库配置
  enableRepoConfig (scmCode: string) {
    return fetch.put(`${repositoryApiPerfix}/repositories/config/${scmCode}/enable`);
  },
  // 删除代码库配置
  deleteRepoConfig (scmCode: string) {
    return fetch.delete(`${repositoryApiPerfix}/repositories/config/${scmCode}`);
  },
  // 禁用代码库配置
  updateRepoConfig (scmCode: string) {
    return fetch.put(`${repositoryApiPerfix}/repositories/config/${scmCode}`);
  },
  // 创建代码库配置
  createRepoConfig () {
    return fetch.post(`${repositoryApiPerfix}/repositories/config`);
  }
};
