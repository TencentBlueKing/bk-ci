import {
  TargetNetBehavior,
} from '@/types';
import { STORE_TYPE, BASE_PREFIX } from '@/common/constants';
import fetch from './fetch';

const projectPerfix = '/project/api/user';
const storeApiPerfix = '/store/api/user';
const repositoryApiPerfix = '/repository/api/user';
const remotedevApiPerfix = '/remotedev/api';

export default {
  getUser() {
    return fetch.get(`${projectPerfix}/users`);
  },
};
