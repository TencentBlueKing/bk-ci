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

import { DOCKER_REPO, NPM_REPO } from '@/utils/conf';
import { Artifact, OperationDialogProps, User, UserInfo } from '@/utils/vue-ts';
import { DomainKey, State } from '.';
import * as StoreConst from './constants';

export default {
  [StoreConst.SET_USER_INFO]: (state: State, userInfo: UserInfo) => {
    state.currentUser = {
      ...(state.currentUser ?? {}),
      ...userInfo,
    };
  },
  [StoreConst.SET_USER_MAP]: (state: State, userList: User[]) => {
    const userMap = userList.reduce((acc: Record<string, string>, user: User) => {
      acc[user.userId] = user.name;
      return acc;
    }, {});
    state.userMap = userMap;
  },
  [StoreConst.SET_PROJECT_LIST]: (state: State, res: any) => {
    state.projectList = res.map((item: any) => ({
      ...item,
      id: item.name ?? item.englishName,
      name: item.displayName ?? item.projectName,
    }));
  },
  [StoreConst.SET_REPO_LIST]: (state: State, res: any) => {
    state.repoList = res;
  },
  [StoreConst.UPDATE_REPO_MAP]: (state: State, repos: Artifact[]) => {
    repos.forEach((repoItem: Artifact) => {
      state.repoMap.set(repoItem.fullPath, repoItem);
    });
  },
  [StoreConst.UPDATE_REPO_TREE_ITEM]: (state: State, repo: Partial<Artifact>) => {
    const { key, ...rest } = repo;
    const item = state.repoMap.get(key);
    if (item) {
      Object.assign(item, rest);
    }
    console.log('after', item);
  },
  [StoreConst.CLEAR_TREE]: (state: State) => {
    state.repoMap = new Map();
  },
  [StoreConst.ACTIVE_OPERATION]: (state: State, operationProps: OperationDialogProps) => {
    Object.assign(state.operationProps, operationProps);
  },
  [StoreConst.CLOSE_OPERATION]: (state: State) => {
    Object.assign(state.operationProps, {
      isShow: false,
      operation: undefined,
      artifact: undefined,
      filesCount: 0,
    });
  },
  [StoreConst.SET_DOMAIN]: (state: State, { type, domain }: { type: DomainKey, domain: any }) => {
    switch (type) {
      case DOCKER_REPO:
        state.domain[type] = domain;
        break;
      case NPM_REPO:
        state.domain[type] = domain.domain ?? `${location.origin}/npm`;
        break;
    }
  },
};

