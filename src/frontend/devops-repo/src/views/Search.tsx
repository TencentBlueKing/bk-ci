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
import Icon from '@/components/Icon';
import { Button, Dropdown, Exception, Input, Loading, Select, Tree } from 'bkui-vue';
import { useI18n } from 'vue-i18n';
import { REPO_TYPE_LIST, SORT_PROPERTY } from '@/utils/conf';
import { classes } from '@/utils';
import { Artifact, ObjectMap } from '@/utils/vue-ts';
import { useSearchArtifact } from '@/hooks';
import InfiniteScroll from '@/components/InfiniteScroll';
import { useRoute, useRouter } from 'vue-router';
import PackageItem from '@/components/PackageItem';

const { Option } = Select;
export default defineComponent({
  setup() {
    const { t } = useI18n();
    const isRepoTreeLoading = ref(false);
    const currentRoute = useRoute();
    const router = useRouter();
    const treeData = ref<ObjectMap[]>();
    const {
      isSearching,
      artifactList,
      searchArtifact,
      getRepoTreeData,
      searchParams,
      pagination,
      toggleDirection,
      updateRouteQuery,
    }  = useSearchArtifact();
    const isEmpty = computed(() => artifactList.value.length === 0 && !isSearching.value);

    onBeforeMount(() => {
      initTree();
    });

    async function initTree() {
      isRepoTreeLoading.value = true;
      const repoTreeData = await getRepoTreeData({
        repoType: searchParams.repoType,
        packageName: searchParams.packageName,
      });
      treeData.value = repoTreeData;
      isRepoTreeLoading.value = false;
    }

    async function loadMore(done: Function) {
      console.log('loadMore');
      const nextPage = pagination.current + 1;
      Object.assign(pagination, {
        current: nextPage,
      });
      await searchArtifact({
        current: nextPage,
      });
      done();
    }

    function handleRepoTypeChange(repoType: string) {
      searchParams.repoType = repoType;
    }

    function handleRepoSelect(item: any) {
      searchParams.repoName = item.key;
    }

    const repoTypeOptions = computed(() => REPO_TYPE_LIST.map(type => ({
      ...type,
      handler: () => handleRepoTypeChange(type.id),
      cls: classes({
        selected: searchParams.repoType === type.id,
      }, 'repo-type-item link'),
    })));

    function handlePackageNameChange() {
      initTree();
      updateRouteQuery();
    }

    function goDetail(artifact: Artifact) {
      console.log(artifact);
      if (artifact.fullPath) {
        // TODO: 判断条件优化
        router.push({
          name: 'genericRepo',
          params: {
            ...currentRoute.params,
            repoType: searchParams.repoType,
            folders: artifact.path.substring(1).split('/'),
          },
          query: {
            repoName: artifact.repoName,
          },
        });
        return;
      }
      router.push({
        name: 'repoPackage',
        params: {
          repoType: artifact.type.toLowerCase(),
        },
        query: {
          repoName: artifact.repoName,
          package: artifact.key,
          versionName: artifact.versionName,
          version: artifact.latest,
        },
      });
    }

    return () => (
      <div class="repo-search">
        <header class="repo-search-header">
          <div class="repo-search-input">
            <Dropdown trigger='click'>
              {
                {
                  default: () => (
                    <div class="repo-serach-type-reference">
                      <span class="repo-serach-type-icon">
                        <Icon size={20} name={searchParams.repoType}/>
                        {searchParams.repoType}
                      </span>
                      <Icon size={16} name="angle-down"/>
                    </div>
                  ),
                  content: () => (
                    <div class="repo-type-select-menu">
                      {
                        repoTypeOptions.value.map(type => (
                          <div
                            onClick={type.handler}
                            class={type.cls}>
                            <Icon size={32} name={type.id} />
                            <span>{type.name}</span>
                          </div>
                        ))
                      }
                    </div>
                  ),
                }
              }
            </Dropdown>
            <Input
              class="repo-search-key"
              v-model={searchParams.packageName}
              onEnter={handlePackageNameChange}
              placeholder={t('searchRepoPlaceholder')}
            >
              {{
                suffix: () => (
                  <span onClick={handlePackageNameChange} class="repo-search-btn">
                    <Icon size={16} name="repoSearch" />
                  </span>
                ),
              }}
            </Input>
          </div>
        </header>
        <main class="repo-search-result">
          {
            isEmpty.value ? (
              <Exception type="search-empty" scene="part">
                <p>{t('noSearchData')}</p>
              </Exception>
            ) : (
              <>
                <header class="repo-search-info">
                  <span class="repo-search-count">
                    {isSearching.value ?  '' : t('searchCountTips', [pagination.count])}
                  </span>
                  <span class="repo-search-sort">
                    <Select multiple v-model={searchParams.property}>
                      {Object.entries(SORT_PROPERTY).map(([, value]) => (
                          <Option label={t(`${value}Sort`)} value={value} />
                      ))}
                    </Select>
                    <Button hoverTheme='primary' onClick={toggleDirection}>
                      <Icon size={16} name={`order-${searchParams.direction.toLowerCase()}`} />
                    </Button>
                  </span>
                </header>

                <section class="repo-search-content">
                  <Loading loading={isRepoTreeLoading.value} class="repo-search-construct">
                    <Tree
                      label="name"
                      nodeKey="key"
                      data={treeData.value}
                      onNodeClick={handleRepoSelect}
                      lineHeight={40}
                      selected={searchParams.repoName}
                    >
                      {{
                        nodeAction: ({ isLeaf, __attr__: { isOpen } }: any) => (!isLeaf ? (
                            <span class="repo-tree-expand-icon">
                              <Icon name={isOpen ? 'angle-down' : 'angle-right'} />
                            </span>
                        ) : null),
                        nodeType: () => null,
                        nodeAppend: (repo: any) => (
                          <span class="repo-tree-count-append">
                            {repo.packages}
                          </span>
                        ),
                      }}
                      </Tree>
                  </Loading>
                  <Loading loading={isSearching.value} class="repo-search-list">
                  <InfiniteScroll
                    onSrollEnd={loadMore}
                    hasNext={pagination.hasNext}
                  >
                      {artifactList.value.map((item: Artifact) => (
                          <PackageItem
                            isPackage={!!item.type}
                            readonly
                            pacakgeInfo={item}
                            onClick={goDetail}
                          />
                      ))}
                  </InfiniteScroll>
                  </Loading>
                </section>
              </>
            )
          }
        </main>
      </div>
    );
  },
});
