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
import { computed, defineComponent, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import Icon from '@/components/Icon';
import { useRoute, useRouter } from 'vue-router';
import { formatDate, formatSize, getIconNameByFileName, replaceRepoName } from '@/utils';
import {
  Breadcrumb,
  Input,
  Table,
  Loading,
  Button,
} from 'bkui-vue';
import { useI18n } from 'vue-i18n';
import { useStore } from '@/store';
import {
  CALCULATE_FOLDER_SIZE,
  CLEAR_TREE,
  FETCH_ARTIFACTORIES,
  UPDATE_REPO_MAP,
} from '@/store/constants';
import {  Artifact, OperateName, Operation, PaginationConfFieldType, PaginationConfType } from '@/utils/vue-ts';
import { useArtifactOperation, useFolderOperation, useRepoInfo, useRepoTree, useRouteParams } from '@/hooks';
import RepoTree from '@/components/RepoTree';
import { useOperation } from '@/hooks/artifactOperation';
import OperationMenu from '@/components/OperationMenu';
import RepoHeader from '@/components/RepoHeader';

const BreadcrumbItem = Breadcrumb.Item;
export default defineComponent({
  setup() {
    const { t } = useI18n();
    const currentRoute = useRoute();
    const router = useRouter();
    const store = useStore();
    const displayName = computed(() => replaceRepoName(currentRoute.query.repoName as string, t));
    const isSearching = computed(() => !!currentRoute.query.fileName);
    const isLoading = ref(false);
    const isTreeLoading = ref(false);
    const selectedTreePath = ref<string>('/');
    const { activeOperation } = useOperation<Artifact>();
    const tableData = ref<Artifact[]>([]);
    const treeRef = ref();
    const repoParams = useRouteParams();
    const repoTreeParams = computed(() => ({
      projectId: repoParams.value.projectId,
      repoName: repoParams.value.repoName,
    }));
    const repoInfo = useRepoInfo();
    const { fetchTreeChildren, expandTreeNode } = useRepoTree(repoTreeParams.value);
    const paginationConf = reactive<PaginationConfType>({
      count: 0,
      limit: 20,
      current: 1,
      align: 'right',
    });
    const state = reactive({
      description: '',
      searchKey: currentRoute.query.fileName,
      filesCount: 0,
    });
    const columns = [
      {
        label: t('fileName'),
        field: 'name',
        sort: true,
        render: ({ row }: any) => {
          const iconName = row.folder ? 'folder' : getIconNameByFileName(row.displayName);
          return (
            <div class="bk-repo-flex">
              <Icon size="20" name={iconName} fallback="file" />
              <span class="artifact-file-name text-overflow" title={row.displayName}>
                {row.displayName}
              </span>
            </div>
          );
        },
      },
      ...(isSearching.value ? [{
        label: t('path'),
        field: 'fullPath',
      }] : []),
      {
        label: t('lastModifiedDate'),
        field: 'lastModifiedDate',
        sort: true,
        render: ({ cell }: any) => formatDate(cell),
      },
      {
        label: t('lastModifiedBy'),
        field: 'lastModifiedBy',
      },
      {
        label: t('size'),
        field: 'size',
        width: 100,
        render: ({ cell, row }: any) => {
          if (cell || row.calculating === false) {
            return formatSize(cell);
          }
          return row.calculating
            ? <Loading size='mini' loading mode='spin'></Loading>
            : (
            <span
              class="link"
              onClick={() => handleCalcFolderSize(row.fullPath)}
            >
              {t('calculate')}
            </span>
            );
        },
      },
      {
        label: t('operation'),
        field: 'folder',
        width: 80,
        render: ({ row }: any) => {
          const operationList = useArtifactOperation(row);
          return operationList.length > 0 ? (
            <OperationMenu
              operationList={operationList}
              handleOperation={(e: MouseEvent, operation: Operation) => handleOperation(operation, row)}
            >
            </OperationMenu>
          ) : null;
        },
      },
    ];

    const handleLimitChange = handlePaginationChange('limit');
    const handlePageChange = handlePaginationChange('current');
    const folderBreadCrumbs = computed<string[]>(() => {
      const { folders } = currentRoute.params;
      if (Array.isArray(folders) && folders.length >= 1) {
        const trimFolders = folders.filter((folder: string) => typeof folder === 'string' && folder !== '');
        return trimFolders.reduce<string[]>((acc, folder) => {
          const last = acc[acc.length - 1];
          const subPath = `${last}/${folder}`.replace(/\/+/g, '/');
          acc.push(subPath);
          return acc;
        }, ['/']);
      }
      return ['/'];
    });

    watch(() => currentRoute.params.folders, async () => {
      await nextTick();
      selectedTreePath.value = folderBreadCrumbs.value[folderBreadCrumbs.value.length - 1];
      paginationConf.current = 1;
      fetchArtifactories({
        fullPath: selectedTreePath.value,
        searchName: state.searchKey,
      }, 1);
    });

    onMounted(async () => {
      isTreeLoading.value = true;
      const { query } = currentRoute;
      const repoName = query.repoName as string;
      const rootRepo: Artifact = {
        folder: true,
        children: [],
        displayName: displayName.value,
        name: repoName,
        fullPath: '/',
        isRoot: true,
        isOpen: true,
      };
      await nextTick();
      selectedTreePath.value = folderBreadCrumbs.value[folderBreadCrumbs.value.length - 1];
      store.commit(UPDATE_REPO_MAP, [
        rootRepo,
      ]);
      await Promise.all([
        ...folderBreadCrumbs.value.map(async key => fetchTreeChildren(key)),
        fetchArtifactories({
          fullPath: selectedTreePath.value,
          searchName: state.searchKey,
        }, 1),
      ]);
      folderBreadCrumbs.value.map(item => expandTreeNode(item!));
      isTreeLoading.value = false;
    });

    onBeforeUnmount(() => {
      store.commit(CLEAR_TREE);
    });

    const searchInput = () => (
      <Input
        type="search"
        clearable
        v-model={state.searchKey}
        onChange={handleSearch}
      />
    );

    function handleSearch(fileName: string | number) {
      router.push({
        ...currentRoute,
        query: {
          ...currentRoute.query,
          fileName,
        },
      });
      fetchArtifactories({
        searchName: state.searchKey,
      });
    };

    async function fetchArtifactories(params: any, current?: number, limit?: number) {
      try {
        isLoading.value = true;

        const [artifactories, count] = await store.dispatch(FETCH_ARTIFACTORIES, {
          ...repoParams.value,
          ...params,
          current: current ?? paginationConf.current,
          limit: limit ?? paginationConf.limit,
        });
        tableData.value = artifactories;

        paginationConf.count = count;
      } catch (error) {
        console.trace(error);
      } finally {
        isLoading.value = false;
      }
    }

    function handleFolderClick(fullPath: string) {
      router.push({
        ...currentRoute,
        params: {
          ...currentRoute.params,
          folders: fullPath.substring(1).split('/'),
        },
      });
    }

    function handlePaginationChange(field: PaginationConfFieldType) {
      return (value: any) => {
        if (value !== paginationConf[field]) {
          paginationConf[field] = value;
          nextTick(() => {
            fetchArtifactories({
              fullPath: selectedTreePath.value,
              searchName: state.searchKey,
            });
          });
        }
      };
    }

    function handleRowDblClick(e: Event, row: Artifact) {
      if (row.folder) {
        handleFolderClick(row.fullPath);
      }
    }

    function handleRefresh() {
      fetchArtifactories({
        fullPath: selectedTreePath.value,
        searchName: state.searchKey,
      });
    }

    async function handleCalcFolderSize(fullPath: string): Promise<void> {
      const tableItem = tableData.value.find(item => item.fullPath === fullPath);
      if (tableItem) {
        try {
          tableItem.calculating = true;
          const { size } = await store.dispatch(CALCULATE_FOLDER_SIZE, {
            ...repoParams.value,
            fullPath: encodeURIComponent(fullPath),
          });

          tableItem.size = size;
        } catch (error) {
          console.trace(error);
        } finally {
          tableItem.calculating = false;
        }
      }
    }

    function handleUpload() {
      const folders = currentRoute.params.folders as string[];

      const fullPath = Array.isArray(folders) ? `/${(folders).join('/')}` : '/';
      const repo = store.state.repoMap.get(fullPath);
      if (repo) {
        const operation: Operation = {
          id: OperateName.UPLOAD,
          name: OperateName.UPLOAD,
          message: 'uploadSuccess',
          show: true,
        };
        handleOperation(operation, repo);
      }
    }

    function handleSort({ column, type }: any) {
      console.log(column, type);
      fetchArtifactories({
        fullPath: selectedTreePath.value,
        searchName: state.searchKey,
        sortProp: column.field,
        sortType: type.toUpperCase(),
      });
    }

    function handleOperation(operation: Operation, artifact: Artifact) {
      activeOperation(operation, artifact, handleRefresh);
    }

    return () => (
      <>
        <RepoHeader
          showGuide={repoInfo.showGuide}
          repoType={repoInfo.type}
          repoName={repoInfo.name}
          repoDesc={repoInfo.desc}
        />
        <section class="generic-repo-content">
          {
          !isSearching.value && (
            <aside class="generic-repo-aside">
              <div class="generic-repo-aside-search">
                {searchInput()}
              </div>
              <Loading
                class="repo-constrct-tree-box"
                loading={isTreeLoading.value}
              >
                <RepoTree
                  showOperationEntry
                  ref={treeRef}
                  tree={store.getters.repoTree}
                  activeNode={selectedTreePath.value}
                  handleNodeClick={handleFolderClick}
                  onOperate={handleOperation}
                />
              </Loading>
            </aside>
          )}
          <main class="generic-repo-main">
            <header class="repo-artifactories-header">
              {
                isTreeLoading.value && <Loading size='small' mode='spin' loading />
              }
              {
                // eslint-disable-next-line no-nested-ternary
                isSearching.value ? (
                  <div class="repo-search-input">
                    { searchInput() }
                  </div>
                ) : (
                  !isTreeLoading.value && (
                    <Breadcrumb extCls="artifactories-bread-crumb">
                      {
                        folderBreadCrumbs.value?.map((fullPath: string, index: number) => (
                            <BreadcrumbItem>
                              <Button
                                text
                                disabled={index === folderBreadCrumbs.value.length - 1}
                                onClick={() => handleFolderClick(fullPath)}
                              >
                                {store.state.repoMap.get(fullPath)?.displayName ?? '--'}
                              </Button>
                            </BreadcrumbItem>
                        ))
                      }
                    </Breadcrumb>
                  )
                )
              }
              <Button onClick={handleRefresh}>{t('refresh')}</Button>
            </header>
            <Loading class="repo-dir-table" loading={isLoading.value}>
              <Table
                height="100%"
                columns={columns}
                data={tableData.value}
                rowHeight={40}
                onRowDblClick={handleRowDblClick}
                pagination={paginationConf}
                remotePagination
                onPageLimitChange={handleLimitChange}
                onPageValueChange={handlePageChange}
                onColumnSort={handleSort}
              >
                {{
                  empty: () => {
                    let showUploadNow = false;
                    const currentPath = folderBreadCrumbs.value[folderBreadCrumbs.value.length - 1];
                    const repo = store.state.repoMap.get(currentPath);
                    if (repo) {
                      const operation = useFolderOperation(repo, repoTreeParams.value)
                        .find(operation => operation.id === OperateName.UPLOAD);;
                      showUploadNow = !isSearching.value && !!operation;
                    }
                    return (
                      <span class="repo-table-empty-placeholder">
                        {
                          showUploadNow ? (
                            <>
                              <Icon name="empty-data" size={30} />
                              {t('noFile')}
                              <Button
                                text
                                theme='primary'
                                onClick={handleUpload}
                              >
                                {t('uploadNow')}
                              </Button>
                            </>
                          ) : (
                            <>
                              <Icon name="empty-search" size={30} />
                              {t('noData')}
                            </>
                          )
                        }
                      </span>
                    )
                    ;
                  },
                }}
              </Table>
            </Loading>
          </main>
        </section>
      </>
    );
  },
});
