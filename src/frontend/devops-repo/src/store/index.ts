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
import { Artifact, OperationDialogProps, Permission, Project, RepoItem, UserInfo } from '@/utils/vue-ts';
import { InjectionKey } from 'vue';
import { createStore, Store, useStore as vuexUseStore, createLogger } from 'vuex';
import actions from './actions';
import getters from './getters';
import mutations from './mutations';
const debug = process.env.NODE_ENV !== 'production';
const plugins = [];
if (debug) {
  plugins.push(createLogger({}));
}
const initDoamin = {
  docker: '',
  npm: '',
};
const repoInfo = {
  name: '',
  desc: '',
  type: '',
  showGuide: false,
};
export interface State {
  projectList: Project[]
  repoList: RepoItem[]
  userMap: Record<string, string>,
  operationProps: OperationDialogProps
  currentUser?: UserInfo
  domain: typeof initDoamin,
  repoMap: Map<string, Artifact>
  permission: Permission
  repoInfo: typeof repoInfo,
};
export type DomainKey = keyof typeof initDoamin;

export const key: InjectionKey<Store<State>>  = Symbol();

export default createStore<State>({
  state: {
    projectList: [],
    operationProps: {
      isShow: false,
      artifact: undefined,
      operation: undefined,
      filesCount: 0,
      done: undefined,
    },
    repoInfo,
    domain: initDoamin,
    repoList: [],
    userMap: {},
    repoMap: new Map(),
    permission: {
      write: true,
      edit: true,
      delete: true,
    },
  },
  actions,
  getters,
  mutations,
  plugins,
});

export function useStore() {
  return vuexUseStore(key);
}
