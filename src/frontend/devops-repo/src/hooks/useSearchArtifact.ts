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
import { FETCH_SEARCH_REPO, ROOT_KEY, SEARCH } from '@/store/constants';
import { formatArtifactNode, replaceRepoName, SORT_TYPE } from '@/utils';
import { GENERIC_REPO, MAVEN_REPO, SORT_PROPERTY } from '@/utils/conf';
import { Artifact, ObjectMap, PageApiResponse, SearchArtifactParam } from '@/utils/vue-ts';
import { Message } from 'bkui-vue';
import { reactive, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRoute, useRouter } from 'vue-router';
import usePagination from './usePagination';
import useRouteParams from './useRouteParam';

export default function useSearchArtifact() {
  const store = useStore();
  const router = useRouter();
  const currentRoute = useRoute();
  const repoParams = useRouteParams();
  const isSearching = ref(true);
  const { pagination, updatePagination, handleCurrentChange } = usePagination();
  const { t } = useI18n();
  const artifactList = ref<Artifact[]>([]);

  const searchParams = reactive({
    packageName: (repoParams.value.packageName ?? ''),
    repoName: (repoParams.value.repoName ?? ROOT_KEY),
    property: [(repoParams.value.property ?? SORT_PROPERTY.MOD_DATE)],
    direction: (repoParams.value.direction ?? SORT_TYPE.ASC),
    repoType: (repoParams.value.repoType ?? GENERIC_REPO),
  });

  watch(() => ({
    repoName: searchParams.repoName,
    direction: searchParams.direction,
    repoType: searchParams.repoType,
    property: searchParams.property,
  }), updateRouteQuery);

  watch(() => currentRoute.query, () => {
    searchArtifact({
      current: 1,
    });
  }, { immediate: true });

  function updateRouteQuery() {
    const { repoName, ...query } = searchParams;
    router.replace({
      query: {
        ...query,
        ...(repoName === ROOT_KEY ? {} : {
          repoName,
        }),
      },
    });
  }

  function toggleDirection() {
    searchParams.direction = searchParams.direction === SORT_TYPE.ASC ? SORT_TYPE.DESC : SORT_TYPE.ASC;
  }

  async function searchArtifact(param?: Partial<SearchArtifactParam>) {
    try {
      isSearching.value = param?.current === 1;
      const repoName = param?.repoName ?? searchParams.repoName;

      const res: PageApiResponse = await store.dispatch(SEARCH, {
        ...repoParams.value,
        ...pagination,
        ...searchParams,
        ...param,
        repoName: repoName === ROOT_KEY || !repoName ? undefined : repoName,
      });

      if (!res) {
        return artifactList.value;
      }

      const artifacts = Array.isArray(res.records) ? res.records.map((item: ObjectMap) => {
        const artifact = formatArtifactNode(item, item.path);

        return {
          ...artifact,
          isMaven: item.type?.toLowerCase() === MAVEN_REPO,
        };
      }) : [];

      if (res.page === 1) {
        artifactList.value = artifacts;
      } else {
        artifactList.value = [
          ...artifactList.value,
          ...artifacts,
        ];
      }
      updatePagination({
        count: res.count ?? 0,
        current: res.page ?? 1,
        limit: res.pageSize ?? 20,
        hasNext: res.totalPages > res.page,
      });

      return artifactList.value;
    } catch (error: any) {
      Message({
        message: error.message ?? error,
        theme: 'error',
      });
    } finally {
      isSearching.value = false;
    }
  }

  async function getRepoTreeData({
    repoType,
    packageName,
  }: ObjectMap) {
    const res = await store.dispatch(FETCH_SEARCH_REPO, {
      projectId: repoParams.value.projectId,
      repoType,
      packageName,
    });
    if (Array.isArray(res) && res[0]) {
      return [{
        key: ROOT_KEY,
        name: t('all'),
        isOpen: true,
        packages: res[0].sum,
        children: res[0].repos?.map((repo: any) => ({
          ...repo,
          isLeaf: true,
          key: repo.repoName,
          name: `${replaceRepoName(repo.repoName, t)}`,
        })) ?? [],
      }];
    }
    return [];
  }


  return {
    isSearching,
    artifactList,
    searchArtifact,
    toggleDirection,
    pagination,
    updatePagination,
    handleCurrentChange,
    searchParams,
    updateRouteQuery,
    getRepoTreeData,
  };
}
