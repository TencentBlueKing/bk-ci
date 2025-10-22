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
import { computed, defineComponent, onBeforeMount, ref } from 'vue';
import { Input, Loading } from 'bkui-vue';
import { useI18n } from 'vue-i18n';
import { useRoute, useRouter } from 'vue-router';

import InfiniteScroll from '@/components/InfiniteScroll';
import OperationMenu from '@/components/OperationMenu';
import PackageDetail from '@/components/PackageDetail';
import { useOperation, usePagination, usePackageOperation, useRouteParams } from '@/hooks';
import { useStore } from '@/store';
import { GET_PACKAGE_INFO, GET_PACKAGE_VERSIONS } from '@/store/constants';
import { classes } from '@/utils';
import { OperateName, Operation } from '@/utils/vue-ts';
import RepoHeader from '@/components/RepoHeader';

export default defineComponent({

  setup() {
    const { t } = useI18n();
    const store = useStore();
    const router  = useRouter();
    const currentRoute = useRoute();
    const isLoading = ref(true);
    const searching = ref(false);
    const routeParams = useRouteParams();
    const info = ref();
    const { activeOperation } = useOperation();
    const versionList = ref<any[]>([]);
    const versionKey = ref('');
    const { pagination, updatePagination, handleCurrentChange } = usePagination();

    const versionOperation = computed<Record<string, Operation[]>>(() => versionList.value.reduce((acc, v) => {
      const packageOperations = usePackageOperation(
        routeParams.value.repoType,
        info.value?.name,
        v,
      );
      acc[v.name] = packageOperations;
      return acc;
    }, {}));

    onBeforeMount(async () => {
      isLoading.value = true;
      const [packageInfo] = await Promise.all([
        store.dispatch(GET_PACKAGE_INFO, {
          projectId: routeParams.value.projectId,
          repoName: routeParams.value.repoName,
          packageKey: routeParams.value.package,
        }),
        getVersions(),
      ]);
      info.value = packageInfo;
      isLoading.value = false;
    });

    async function getVersions() {
      const res = await store.dispatch(GET_PACKAGE_VERSIONS, {
        projectId: routeParams.value.projectId,
        repoName: routeParams.value.repoName,
        packageKey: routeParams.value.package,
        ...pagination,
        version: versionKey.value,
      });
      const list = res.records.map((item: any) => ({
        name: item.name,
        stageTag: item.stageTag,
      }));
      if (pagination.current === 1) {
        versionList.value = list;
      } else {
        versionList.value = [
          ...versionList.value,
          ...list,
        ];
      }
      updatePagination({
        current: res.page,
        count: res.count,
        hasNext: res.totalPages > res.page,
      });
      searching.value = false;
    }

    async function handleSearchVersion() {
      if (searching.value) return;
      searching.value = true;
      handleCurrentChange(1);
      await getVersions();
      searching.value = false;
    }

    async function loadMore(done: Function) {
      const nextCurrent = pagination.current + 1;
      handleCurrentChange(nextCurrent);
      await getVersions();
      done();
    }

    function getVersionCls(v: string) {
      return classes({
        active: v === routeParams.value.version,
      }, 'version-item');
    }

    function switchVersion(version: string) {
      router.push({
        query: {
          ...currentRoute.query,
          version,
        },
      });
    }

    function handlerVersionOperation(operation: Operation, version: any) {
      activeOperation(operation, version, () => {
        switch (operation.id) {
          case OperateName.DELETE:
            debugger;
            versionList.value = versionList.value.filter(v => v.name !== version.name);
            if (routeParams.value.version === version.name) { // 删除的是当前版本，切换到最新版本
              switchVersion(versionList.value[0]?.name);
            }
            break;
          case OperateName.UPGRADE:
            getVersions();
            break;
        }
      });
    }

    return () => (
      <Loading class="bk-repo-package-info" loading={isLoading.value}>
        <RepoHeader
          repoType={routeParams.value.repoType}
          repoName={info.value?.name}
          repoDesc={info.value?.description ?? t('packageDesc')}
        />
        <div class="bk-repo-package-info-main">

          <aside class="bk-repo-package-info-aside">
              <h3 class="aside-title">{t('artifactVersion')}</h3>
              <p class="version-search">
                <Input
                  type="search"
                  v-model={versionKey.value}
                  clearable
                  disabled={searching.value}
                  placeholder={t('versionPlaceholder')}
                  onEnter={handleSearchVersion}
                  onClear={handleSearchVersion}
                />
              </p>
              <Loading loading={searching.value} class="version-list">
                <p class="version-sum">{t('versionSum', [pagination.count])}</p>
                <InfiniteScroll
                  onSrollEnd={loadMore}
                  hasNext={pagination.hasNext}
                >
                  {
                    versionList.value.map(v => (
                      <p class={getVersionCls(v.name)} onClick={() => switchVersion(v.name)}>
                        {v.name}
                        <OperationMenu
                          class="version-operation"
                          operationList={versionOperation.value[v.name]}
                          handleOperation={(_: any, operation: Operation) => handlerVersionOperation(operation, v)}
                        >
                        </OperationMenu>
                      </p>
                    ))
                  }
                </InfiniteScroll>
              </Loading>
          </aside>
          <section class="bk-repo-package-info-content">
            <PackageDetail
              operations={versionOperation.value?.[routeParams.value.version]}
              handleOperation={handlerVersionOperation}
            />
          </section>
        </div>
      </Loading>
    );
  },
});
