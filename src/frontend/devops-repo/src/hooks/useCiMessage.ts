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
import { ObjectMap } from '@/utils/vue-ts';
import { onBeforeUnmount, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import useRouteParams from './useRouteParam';

export default function useCiMessage() {
  const router = useRouter();
  const repoParams = useRouteParams();

  onMounted(() => {
    window.addEventListener('change::$currentProjectId', handleProjectIdChange);

    window.addEventListener('change::$routePath', updateUrl);

    window.addEventListener('order::backHome', goHome);
  });

  onBeforeUnmount(() => {
    window.removeEventListener('change::$currentProjectId', handleProjectIdChange);

    window.removeEventListener('change::$routePath', updateUrl);

    window.removeEventListener('order::backHome', goHome);
  });

  function updateUrl(e: any) {
    const { detail } = e;
    router.push({
      name: detail.routePath.englishName,
      path: detail.routePath.path.replace(/^\/[a-zA-Z]+/, process.env.VUE_APP_BASE_URL),
    });
  }

  function handleProjectIdChange(e: any) {
    const { detail } = e;
    const { currentProjectId } = detail;
    localStorage.setItem('projectId', currentProjectId);
    console.log('receive projectId', repoParams.value.projectId, currentProjectId);
    if (repoParams.value.projectId !== currentProjectId) {
      goHome({
        projectId: currentProjectId,
      });
    }
  }

  function goHome({ projectId }: ObjectMap) {
    router.push({
      name: 'repoList',
      params: projectId ? {
        projectId,
      } : {},
    });
  }
}

