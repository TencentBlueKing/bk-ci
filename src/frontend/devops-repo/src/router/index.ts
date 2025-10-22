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
// import { GENERIC_REPO } from '@/utils/conf';
import { GENERIC_REPO } from '@/utils/conf';
import { createRouter, createWebHistory } from 'vue-router';

declare module 'vue-router' {
  interface RouteMeta {
    isAdmin?: boolean
    requiresAuth?: boolean
    label: string
    icon?: string
    breadLabel?: string
    activeKey?: string
    query?: string[]
  }
}
const routes = [
  {
    path: '/:projectId',
    component: () => import('@/views/App'),
    meta: {
      label: 'artifactManage',
      breadLabel: 'artifactManage',
      icon: 'repolist',
    },
    children: [
      {
        path: '',
        component: () => import('@/views/RootView'),
        name: 'repoRoot',
        meta: {
          label: 'repoList',
          icon: 'repolist',
          breadLabel: 'repoManageList',
        },
        redirect: {
          name: 'repoList',
        },
        children: [
          {
            path: 'repolist',
            name: 'repoList',
            component: () => import('@/views/RepoList'),
            meta: {
              label: 'repoList',
              icon: 'repolist',
              activeKey: 'repoList',
            },
          },
          {
            path: 'repo',
            name: 'repo',
            component: () => import('@/views/Repo'),
            meta: {
              label: 'repo',
              activeKey: 'repoList',
            },
            children: [
              {
                path: `:repoType(${GENERIC_REPO})/:folders*`,
                name: 'genericRepo',
                component: () => import('@/views/RepoGeneric'),
                meta: {
                  label: 'genericRepo',
                  breadLabel: '{repoName}',
                  activeKey: 'repoList',
                  query: ['repoName'],
                },
              },
              {
                path: ':repoType',
                name: 'commonRepo',
                component: () => import('@/views/RootView'),
                meta: {
                  label: 'repoCommon',
                  activeKey: 'repoList',
                  breadLabel: '{repoName}',
                  query: ['repoName'],
                },
                redirect: {
                  name: 'packageList',
                },
                children: [
                  {
                    path: '',
                    component: () => import('@/views/RepoCommon'),
                    name: 'packageList',
                    meta: {
                      label: 'packageList',
                      icon: 'packagelist',
                      query: ['repoName'],
                    },
                  },
                  {
                    path: 'package',
                    name: 'repoPackage',
                    component: () => import('@/views/Package'),
                    meta: {
                      label: 'repoPackage',
                      breadLabel: '{package}',
                      activeKey: 'repoList',
                      query: ['repoName', 'package', 'version'],
                    },
                  },
                ],
              },
            ],
          },
          {
            path: 'repoConfig/:repoType',
            name: 'repoConfig',
            component: () => import('@/views/RepoConfig'),
            meta: {
              label: 'repoConfig',
              breadLabel: 'repoConfig',
              activeKey: 'repoList',
              query: ['repoName'],
            },
          },
        ],
      },
      {
        path: 'reposearch',
        name: 'repoSearch',
        component: () => import('@/views/Search'),
        meta: {
          label: 'repoSearch',
          breadLabel: 'repoSearch',
          icon: 'reposearch',
          activeKey: 'repoSearch',
        },
      },
      {
        path: 'repoToken',
        name: 'repoToken',
        component: () => import('@/views/RepoToken'),
        meta: {
          label: 'repoToken',
          breadLabel: 'repoToken',
          icon: 'repotoken',
          activeKey: 'repoToken',
        },
      },
      {
        path: 'user',
        name: 'userCenter',
        component: () => import('@/views/UserCenter'),
        meta: {
          label: 'userCenter',
          breadLabel: 'userCenter',
        },
      },
    ],
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/Login'),
    meta: {
      label: 'login',
      breadLabel: 'login',
    },
  },
];
const router = createRouter({
  history: createWebHistory(process.env.VUE_APP_BASE_URL),
  routes,
});

export default router;

