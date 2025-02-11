import {
  TargetNetBehavior,
} from '@/types';
import { STORE_TYPE, BASE_PREFIX } from '@/common/constants';
import fetch from './fetch';

const repositoryApiPerfix = '/repository/api/user';

export default {
  fetchRepoConfigList () {
    return fetch.get(`${repositoryApiPerfix}/repository/config`);
  }
};
