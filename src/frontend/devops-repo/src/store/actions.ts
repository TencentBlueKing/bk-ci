/*
* Tencent is pleased to support the open source community by making
* 蓝鲸智云PaaS平台社区版 (BlueKing PaaS Community Edition) available.
*
* Copyright (C) 2021 Tencent.  All rights reserved.
*
* 蓝鲸智云PaaS平台社区版 (BlueKing PaaS Community Edition) is licensed under the MIT License.
*
* License for 蓝鲸智云PaaS平台社区版 (BlueKing PaaS Community Edition):
*
* ---------------------------------------------------
* Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
* documentation files (the "Software"), to deal in the Software without restriction, including without limitation
* the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
* to permit persons to whom the Software is furnished to do so, subject to the following conditions:
*
* The above copyright notice and this permission notice shall be included in all copies or substantial portions of
* the Software.
*
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
* THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
* CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
* IN THE SOFTWARE.
*/
import * as StoreConst from './constants';
import JSEncrypt from 'jsencrypt';
import request from '@/utils/request';
import { ActionContext } from 'vuex';
import { DomainKey, State } from '.';
import { RepoItem, LOG_REPO_NAME, REPORT_REPO_NAME, PageApiResponse, MoveOrCopyParam } from '@/utils/vue-ts';
import { formatArtifactNode, formatPath, formatTreeNode, generateRule, generateRuleObject, getRepoParams, IS_CI_MODE, OPERATION, RELATION, SORT_TYPE } from '@/utils';
import { DOCKER_REPO, GENERIC_REPO, NPM_REPO } from '@/utils/conf';

export const REPO_API_PREFIX = '/repository/api';
export const SCANNER_API_PREFIX = 'scanner/api';
export const AUTH_API_PREFIX = 'auth/api';

export default {
  [StoreConst.LOGIN]: async (_: ActionContext<State, State>, {
    username,
    pwd = '',
  }: any) => {
    const rsaKey = await request.get<any, any>(`${AUTH_API_PREFIX}/user/rsa`);
    const formData = new FormData();
    formData.append('uid', username);
    const encrypt = new JSEncrypt();
    encrypt.setPublicKey(rsaKey);
    formData.append('token', encrypt.encrypt(pwd) as string);
    return request.post(
      `${AUTH_API_PREFIX}/user/login`, formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      },
    );
  },
  [StoreConst.FETCH_USER_INFO]: async ({ commit }: ActionContext<State, State>) => {
    try {
      const { userId } = await request.get<any, any>(`${AUTH_API_PREFIX}/user/info`);
      commit(StoreConst.SET_USER_INFO, {
        username: userId,
      });
    } catch (error) {
      console.error('fetch user Error', error);
    }
  },
  [StoreConst.FETCH_USER_SETTING]: async ({ commit, state }: ActionContext<State, State>) => {
    try {
      const info = await request.get<any, any>(`${AUTH_API_PREFIX}/user/userinfo/${state.currentUser?.username}`);
      commit(StoreConst.SET_USER_INFO, info);
    } catch (error) {
      console.error('fetch user Error', error);
    }
  },

  [StoreConst.FETCH_PROJECT_LIST]: async ({ commit }: ActionContext<State, State>) => {
    try {
      const res = await request.get<any, any>(`${REPO_API_PREFIX}/project/list`);
      commit(StoreConst.SET_PROJECT_LIST, res);
    } catch (error) {
      console.error('fetch user Error', error);
    }
  },
  [StoreConst.FETCH_USER_LIST]: async ({ commit }: ActionContext<State, State>) => {
    try {
      const res = await request.get<any, any>(`${AUTH_API_PREFIX}/user/list`);
      commit(StoreConst.SET_USER_MAP, res);
    } catch (error) {
      console.error('fetch user Error', error);
    }
  },
  [StoreConst.FETCH_REPO_LIST]: async ({ commit }: ActionContext<State, State>, param: any) => {
    try {
      const { projectId, current, limit, ...restBody } = param;
      const res = await request.get<any, PageApiResponse>(`${REPO_API_PREFIX}/repo/page/${projectId}/${current}/${limit}`, {
        params: restBody,
      });
      const { records } = res;
      // 前端隐藏report仓库/log仓库
      commit(StoreConst.SET_REPO_LIST, records.filter((item: RepoItem) => ![
        LOG_REPO_NAME,
        REPORT_REPO_NAME,
      ].includes(item.name)));
      return res;
    } catch (error) {
      console.trace(error);
    }
  },
  [StoreConst.CHECK_REPO_EXIST]: async (_: ActionContext<State, State>, {
    projectId,
    name,
  }: any) => request.get(`${REPO_API_PREFIX}/repo/exist/${projectId}/${name}`),
  [StoreConst.CREATE_REPO]: async (_: ActionContext<State, State>, {
    projectId,
    repoFormData,
  }: any) => request.post(`${REPO_API_PREFIX}/repo/create`, getRepoParams(projectId, repoFormData)),
  [StoreConst.FETCH_REPO_INFO]: (_: ActionContext<State, State>, {
    projectId,
    repoName,
    repoType,
  }: any) => request.get(`${REPO_API_PREFIX}/repo/info/${projectId}/${repoName}/${repoType}`),

  [StoreConst.UPDATE_REPO_INFO]: (_: ActionContext<State, State>, {
    projectId,
    repoName,
    repoFormData,
  }: any) => request.post(`${REPO_API_PREFIX}/repo/update/${projectId}/${repoName}`, getRepoParams(projectId, repoFormData)),
  [StoreConst.DELETE_REPO]: (_: ActionContext<State, State>, params: any) => {
    const { projectId, name, forced = false } = params;
    return request.delete(`${REPO_API_PREFIX}/repo/delete/${projectId}/${name}?forced=${forced}`);
  },
  [StoreConst.INIT_REPO_TREE]: ({ commit }: ActionContext<State, State>, rootRepos: any) => {
    commit(StoreConst.UPDATE_REPO_MAP, rootRepos);
  },
  [StoreConst.FETCH_TREE_CHILDREN]: async ({ commit }: ActionContext<State, State>, {
    projectId,
    repoName,
    fullPath = '/',
  }: any) => {
    try {
      const path = formatPath(fullPath);
      const body = generateRule({
        ruleMap: {
          projectId: [projectId],
          repoName: [repoName],
          path: [path],
          folder: [true],
        },
        sortProperties: ['name'],
        sortType: SORT_TYPE.ASC,
      });
      const { records } = await request.post<any, any>(`${REPO_API_PREFIX}/node/search`, body);

      const list = records.map((item: any) => formatTreeNode(item, fullPath));;
      commit(StoreConst.UPDATE_REPO_MAP, list);
      return list;
    } catch (error) {
      console.trace(error);
      return [];
    }
  },
  [StoreConst.EXPAND_TREE_ITEM]: ({ commit }: ActionContext<State, State>, fullPath = '/') => {
    commit(StoreConst.UPDATE_REPO_TREE_ITEM, {
      key: fullPath,
      isOpen: true,
      fetched: true,
    });
  },
  [StoreConst.FETCH_ARTIFACTORIES]: async (_: ActionContext<State, State>, {
    projectId,
    repoName,
    fullPath,
    searchName,
    current,
    limit,
    sortProp = 'lastModifiedDate',
    sortType = SORT_TYPE.DESC,
  }: any) => {
    const hasSearchName = !!searchName;
    const body = generateRule({
      page: current,
      pageSize: limit,
      ruleMap: {
        projectId: [projectId],
        repoName: [repoName],
        ...(hasSearchName ? {
          name: [`*${searchName}*`, OPERATION.MATCH],
        } : {
          path: [formatPath(fullPath)],
        }),
      },
      sortProperties: ['folder', sortProp],
      sortType,
    });
    const { records, count } = await request.post<any, any>(`${REPO_API_PREFIX}/node/search`, body);
    const list = records.map((item: any) => formatArtifactNode(item, fullPath));

    return [
      list,
      count,
    ];
  },
  [StoreConst.CALCULATE_FOLDER_SIZE]: async (_: ActionContext<State, State>, {
    projectId,
    repoName,
    fullPath,
  }: any) => {
    const size = await request.get(`${REPO_API_PREFIX}/node/size/${projectId}/${repoName}/${fullPath}`);
    return size;
  },
  [StoreConst.GET_ARTIFACTORY_INFO]: async (_: ActionContext<State, State>, {
    projectId,
    repoName,
    fullPath,
  }: any) => {
    const info = await request.get(`${REPO_API_PREFIX}/node/detail/${projectId}/${repoName}/${fullPath}`);
    return info;
  },
  [StoreConst.CREATE_TOKEN]: async (_: ActionContext<State, State>, {
    projectId,
    username,
    name,
    expiredAt,
  }: any) => {
    const { id } = await request.post<any, any>(`${AUTH_API_PREFIX}/user/token/${username}/${name}`, {
      params: {
        projectId,
        expiredAt,
      },
    });
    return id;
  },
  [StoreConst.DELETE_META_DATA]: async (_: ActionContext<State, State>, {
    projectId,
    repoName,
    fullPath,
    data,
  }: any) => {
    const result = await request.delete(`${REPO_API_PREFIX}/metadata/${projectId}/${repoName}/${fullPath}`, {
      data,
    });
    return result;
  },
  [StoreConst.ADD_META_DATA]: async (_: ActionContext<State, State>, {
    projectId,
    repoName,
    fullPath,
    body,
  }: any) => {
    const result = await request.post(`${REPO_API_PREFIX}/metadata/${projectId}/${repoName}/${fullPath}`, body);
    return result;
  },
  [StoreConst.GET_DOWNLOAD_URL]: async (_: ActionContext<State, State>, {
    projectId,
    repoName,
    fullPath,
  }: any) => {
    try {
      const url = `/generic/${projectId}/${repoName}/${fullPath}?download=true`;
      await request.head(url);
      return url;
    } catch (error) {
      throw error;
    }
  },
  [StoreConst.SHARE_ARTIFACTORY]: (_: ActionContext<State, State>, body: any) => request.post('/generic/temporary/url/create', body),
  [StoreConst.RENAME]: (_: ActionContext<State, State>, {
    projectId,
    repoName,
    fullPath,
    newFullPath,
  }: any) => request.post(`${REPO_API_PREFIX}/node/rename/${projectId}/${repoName}/${fullPath}?newFullPath=${newFullPath}`),
  [StoreConst.CREATE_FOLDER]: (_: ActionContext<State, State>, {
    projectId,
    repoName,
    fullPath,
  }: any) => request.post(`${REPO_API_PREFIX}/node/mkdir/${projectId}/${repoName}/${fullPath}`),
  [StoreConst.MOVE]: (_: ActionContext<State, State>, body: MoveOrCopyParam) => request.post(`${REPO_API_PREFIX}/node/move/`, body),
  [StoreConst.COPY]: (_: ActionContext<State, State>, body: MoveOrCopyParam) => request.post(`${REPO_API_PREFIX}/node/copy/`, body),
  [StoreConst.DELETE]: (_: ActionContext<State, State>, {
    projectId,
    repoName,
    fullPath,
  }: any) => request.delete(`${REPO_API_PREFIX}/node/delete/${projectId}/${repoName}/${fullPath}`),
  [StoreConst.GET_FOLDER_FILES_COUNT]: async (_: ActionContext<State, State>, {
    projectId,
    repoName,
    fullPath,
  }: any) => {
    try {
      const body = generateRule({
        page: 1,
        pageSize: 1,
        ruleMap: {
          projectId: [projectId],
          repoName: [repoName],
          path: [formatPath(fullPath), OPERATION.PREFIX],
          folder: [false],
        },
      });
      const { totalRecords } = await request.post<any, PageApiResponse>(`${REPO_API_PREFIX}/node/search`, body);
      return totalRecords;
    } catch (error) {
      throw error;
    }
  },
  [StoreConst.FETCH_SEARCH_REPO]: (_: ActionContext<State, State>, {
    projectId,
    repoType,
    packageName,
  }: any) => {
    const isGeneric = repoType === GENERIC_REPO;
    const searchType = isGeneric ? 'node' : 'package';
    const field = isGeneric ? 'name' : 'packageName';
    const params = {
      projectId,
      repoType: repoType.toUpperCase(),
      [field]: `*${packageName}*`,
    };
    const optionParams = IS_CI_MODE && isGeneric ? {
      exRepo: 'report,log',
    } : {};
    return request.get(`${REPO_API_PREFIX}/${searchType}/search/overview`, {
      params: {
        ...params,
        ...optionParams,
      },
    });
  },
  [StoreConst.SEARCH]: async (_: ActionContext<State, State>, {
    projectId,
    repoType,
    repoName,
    packageName,
    property = ['name'],
    direction = 'ASC',
    current = 1,
    limit = 20,
  }: any) => {
    const isGeneric = repoType === GENERIC_REPO;
    const subUrl =  isGeneric ? 'node/query' : 'package/search';
    const i = IS_CI_MODE && isGeneric;
    const body = generateRule({
      page: current,
      pageSize: limit,
      sortProperties: property,
      sortType: direction,
      ruleMap: {
        projectId: [projectId],
        repoType: [repoType.toUpperCase()],
        repoName: [repoName],
        name: [packageName ? `*${packageName}*` : undefined, OPERATION.MATCH],
        ...(isGeneric ? { folder: [false] } : {}),
      },
    });
    if (i) {
      body.rule.rules.unshift({
        field: 'repoName',
        value: ['report', 'log'],
        operation: OPERATION.NIN,
      });
    }
    return request.post(`${REPO_API_PREFIX}/${subUrl}`, body);
  },
  [StoreConst.FETCH_SEARCH_REPO]: (_: ActionContext<State, State>, {
    projectId,
    repoType,
    packageName,
  }: any) => {
    const isGeneric = repoType === GENERIC_REPO;
    const searchType = isGeneric ? 'node' : 'package';
    const field = isGeneric ? 'name' : 'packageName';
    const params = {
      projectId,
      repoType: repoType.toUpperCase(),
      [field]: `*${packageName}*`,
    };
    const optionParams = IS_CI_MODE && isGeneric ? {
      exRepo: 'report,log',
    } : {};
    return request.get(`${REPO_API_PREFIX}/${searchType}/search/overview`, {
      params: {
        ...params,
        ...optionParams,
      },
    });
  },
  [StoreConst.FETCH_USER_TOKEN]: (_: ActionContext<State, State>, {
    username,
  }: any) => request.get(`${AUTH_API_PREFIX}/user/list/token/${username}`),
  [StoreConst.DELETE_USER_TOKEN]: (_: ActionContext<State, State>, {
    username,
    name,
  }: any) => request.delete(`${AUTH_API_PREFIX}/user/token/${username}/${name}`),
  [StoreConst.UPDATE_USER_INFO]: (_: ActionContext<State, State>, {
    username,
    body,
  }: any) => request.put(`${AUTH_API_PREFIX}/user/${username}`, body),
  [StoreConst.MODIFY_USER_PWD]: (_: ActionContext<State, State>, {
    username,
    body,
  }: any) => request.put(`${AUTH_API_PREFIX}/user/update/password/${username}`, body, {
    headers: { 'Content-Type': 'multipart/form-data' },
  }),
  [StoreConst.GET_PACKAGE_INFO]: (_: ActionContext<State, State>, {
    projectId,
    repoName,
    packageKey,
  }: any) => request.get(`${REPO_API_PREFIX}/package/info/${projectId}/${repoName}`, {
    params: {
      packageKey,
    },
  }),
  [StoreConst.GET_PACKAGE_VERSIONS]: (_: ActionContext<State, State>, {
    projectId,
    repoName,
    packageKey,
    current,
    limit,
    version,
  }: any) => request.get(`${REPO_API_PREFIX}/version/page/${projectId}/${repoName}`, {
    params: {
      pageNumber: current,
      pageSize: limit,
      packageKey,
      version,
    },
  }),
  [StoreConst.DELETE_PACKAGE_VERSION]: (_: ActionContext<State, State>, {
    projectId,
    repoType,
    repoName,
    version,
    packageKey,
  }: any) => request.delete(`${repoType}/ext/version/delete/${projectId}/${repoName}`, {
    params: {
      packageKey,
      version,
    },
  }),
  [StoreConst.FETCH_PACKAGE_DETAIL]: (_: ActionContext<State, State>, {
    projectId,
    repoType,
    repoName,
    version,
    packageKey,
  }: any) => request.get(`${repoType}/ext/version/detail/${projectId}/${repoName}`, {
    params: {
      packageKey,
      version,
    },
  }),
  [StoreConst.GET_ARTIFACTORY_URL]: async (_: ActionContext<State, State>, {
    projectId,
    repoName,
    version,
    packageKey,
  }: any) => {
    const url = `${REPO_API_PREFIX}/version/download/${projectId}/${repoName}?packageKey=${packageKey}&version=${version}&download=true`;
    await request.head(url);
    return url;
  },
  [StoreConst.DELETE_PACKAGE]: (_: ActionContext<State, State>, {
    projectId,
    repoType,
    repoName,
    packageKey,
  }: any) => request.delete(`${repoType}/ext/package/delete/${projectId}/${repoName}`, {
    params: {
      packageKey,
    },
  }),
  [StoreConst.DELETE_PACKAGE_VERSION]: (_: ActionContext<State, State>, {
    projectId,
    repoType,
    repoName,
    packageKey,
    version,
  }: any) => request.delete(`${repoType}/ext/version/delete/${projectId}/${repoName}`, {
    params: {
      packageKey,
      version,
    },
  }),
  [StoreConst.PACKAGE_UPGRADE]: (_: ActionContext<State, State>, {
    projectId,
    repoName,
    packageKey,
    version,
    tag,
  }: any) => request.post(`${REPO_API_PREFIX}/stage/upgrade/${projectId}/${repoName}`, null, {
    params: {
      packageKey,
      version,
      tag,
    },
  }),
  [StoreConst.FETCH_SEC_SCAN_LIST]: (_: ActionContext<State, State>, {
    projectId,
    type,
  }: any) => request.get(`${SCANNER_API_PREFIX}/scan/plan/all/${projectId}`, {
    params: {
      type,
    },
  }),
  [StoreConst.SCAN]: (_: ActionContext<State, State>, {
    projectId,
    repoName,
    id,
    isPackage,
    packageKey,
    version,
    fullPath,
  }: any) => {
    const repoRules = generateRuleObject({
      projectId: [projectId],
      repoName: [[repoName], OPERATION.IN],
    });
    const rule = isPackage ? {
      version: [version],
      key: [packageKey],
    } : {
      fullPath: [fullPath],
    };
    const packageRules = generateRuleObject(rule);
    const body = {
      id,
      force: true,
      rule: {
        rules: [
          ...repoRules.rules,
          {
            rules: [
              packageRules,
            ],
            relation: RELATION.OR,
          },
        ],
        relation: RELATION.AND,
      },
    };

    return request.post(`${SCANNER_API_PREFIX}/scan`, body);
  },
  [StoreConst.GET_DOMAIN]: async ({ commit }: ActionContext<State, State>, type: DomainKey) => {
    let domainUrl;
    switch (type) {
      case DOCKER_REPO:
        domainUrl = '/docker/ext/addr';
        break;
      case NPM_REPO:
        domainUrl = '/npm/ext/address';
        break;
    }
    if (domainUrl) {
      const domain = await request.get(domainUrl);
      commit(StoreConst.SET_DOMAIN, {
        type,
        domain,
      });
    }
  },
};


