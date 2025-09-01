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
import { useStore } from '@/store';
import { computed } from 'vue';
import { useRoute } from 'vue-router';

export default function useRouteParams() {
  const route = useRoute();
  const store = useStore();
  const repoParams = computed<any>(() => ({
    projectId: route.params.projectId as string,
    repoType: route.params.repoType as string,
    ...route.query,
    repoName: route.query.repoName as string ?? '',
    packageName: route.query.packageName as string,
    property: route.query.property as string,
    direction: route.query.direction as string,
    packageDisplayName: (route.query.package as string ?? '').split(':').pop() || '<PACKAGE_NAME>',
    userName: store.state.currentUser?.username ?? '<USERNAME>',
    repoUrl: `${location.origin}/${route.params.repoType}/${route.params.projectId}/${route.query.repoName}`,
    versionLabel: route.query.version || '<PACKAGE_VERSION>',
  }));

  return repoParams;
}
