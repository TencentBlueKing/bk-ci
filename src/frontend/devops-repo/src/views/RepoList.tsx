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
import { computed, defineComponent, ref, onMounted, reactive, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import {
  Button,
  Input,
  Popover,
  Progress,
  Select,
  Table,
  Tag,
  Dropdown,
  Loading,
} from 'bkui-vue';
import Icon from '@/components/Icon';
import { useStore } from '@/store';
import * as StoreConsts from '@/store/constants';
import { formatDate, getRepoTypeTheme, convertBytesToGb, replaceRepoName } from '@/utils';
import { CommonOption, Operation, RepoItem } from '@/utils/vue-ts';
import { GENERIC_REPO, REPO_TYPE_ICON, REPO_TYPE_LIST } from '@/utils/conf';
import CreateRepoDialog from '@/components/CreateRepoDialog';
import { useRoute, useRouter } from 'vue-router';
import usePagination from '@/hooks/usePagination';
import { useOperation, useRepoOperation } from '@/hooks';

const { DropdownMenu, DropdownItem } = Dropdown;
const { Option } = Select;
export default defineComponent({
  setup() {
    const { t } = useI18n();
    const store = useStore();
    const route = useRoute();
    const router = useRouter();
    const showCreateDialog = ref(false);
    const isLoading = ref(false);
    const query = reactive({
      type: route.query.type ?? '',
      name: route.query.name ?? '',
    });
    const { activeOperation } = useOperation();
    const { pagination, updateCount, handleCurrentChange, handleLimitChange } = usePagination();

    const tableData = computed(() => store.state.repoList.map((repo: RepoItem) => {
      const username = store.state.userMap?.[repo.createdBy];
      const { theme, label } = getRepoTypeTheme(repo);
      const repoType = repo.type.toLowerCase();
      return {
        ...repo,
        repoType,
        icon: REPO_TYPE_ICON[repoType] ?? repoType,
        theme,
        isGenericRepo: repoType === GENERIC_REPO,
        date: formatDate(repo.createdDate),
        createdBy: username ?? repo.createdBy,
        displayName: replaceRepoName(repo.name, t),
        name: repo.name,
        label: t(label),
        showQuota: !!repo.quota,
        quotaProgress: (repo.used ?? 0) / (repo.quota ?? 1),
        usedGB: convertBytesToGb(repo.used),
        quotaGB: convertBytesToGb(repo.quota ?? 0),
      };
    }));

    const columns = [
      {
        label: t('repoName'),
        field: 'displayName',
        render: ({ cell, row }: any) => (
          <div class="repo-table-name-cell">
            <Icon size={20} name={row.icon} />
            <span class="name-cell-text" title={cell}>{cell}</span>
            {row.label && <Tag theme={row.theme}>{row.label}</Tag>}
          </div>
        ),
      },
      {
        label: t('repoQuota'),
        field: 'quotaProgress',
        render: ({ cell, row }: any) => (
          <div class="repo-quota-progress-cell">
            {
              row.showQuota ? (
                <Popover class="quota-progress" placement="right">
                  {{
                    default: () => (
                        <Progress type='circle' width={26} percent={cell} showText={false}></Progress>
                    ),
                    content: () => (
                      <>
                        <div>{ t('totalQuota') }: { row.quotaGB }</div>
                        <div>{ t('usedQuotaCapacity') }: { row.usedGB }</div>
                      </>
                    ),
                  }}
                </Popover>
              ) : <span>--</span>
            }
          </div>
        ),
      },
      {
        label: t('createdDate'),
        field: 'date',
      },
      {
        label: t('createdBy'),
        field: 'createdBy',
      },
      {
        label: t('operation'),
        field: 'repoName',
        render: ({ row }: any) => {
          const operationList = useRepoOperation(row);
          return (
            <Dropdown trigger="hover" placement="bottom-start">
              {{
                default: () => <span><Icon size={14} name="more" /></span>,
                content: () => (
                  <DropdownMenu>
                    {
                      operationList.map(operation => (
                        <DropdownItem
                          key={operation.id}
                          onClick={(e: MouseEvent) => handleOperateClick(operation, row, e)}
                        >
                            {t(operation.name)}
                        </DropdownItem>
                      ))
                    }
                  </DropdownMenu>
                ),
              }}
            </Dropdown>
          );
        },
      },
    ];

    const handleSearch = handleQueryChange('name');
    const handleRepoTypeChange = handleQueryChange('type');

    onMounted(() => {
      fetchRepoList(pagination);
    });

    watch(() => ({
      ...route.query,
      ...pagination,
    }), () => {
      fetchRepoList(pagination);
    });

    async function fetchRepoList({ current, limit } = pagination) {
      isLoading.value = true;
      const { count } = await store.dispatch(StoreConsts.FETCH_REPO_LIST, {
        projectId: route.params.projectId,
        current,
        limit,
        ...query,
      });
      updateCount(count);
      isLoading.value = false;
    }

    function handleRowClick(_: Event, { repoType, isGenericRepo, name }: any) {
      router.push({
        name: isGenericRepo ? 'genericRepo' : 'commonRepo',
        params: {
          repoType,
        },
        query: {
          repoName: name,
        },
      });
    }

    function handleQueryChange(key: keyof typeof query) {
      return (value: string | number) => {
        Object.assign(query, {
          [key]: value,
        });
        router.replace({
          query,
        });
      };
    }
    function handleOperateClick(operation: Operation, repo: RepoItem, e: MouseEvent) {
      e.stopPropagation();
      activeOperation(
        operation,
        repo,
        fetchRepoList,
      );
    }

    function toggleCreateRepoDialog() {
      showCreateDialog.value = !showCreateDialog.value;
    }

    return () => (
      <Loading loading={isLoading.value} class="repo-list-main repo-content-area">
          <header class="repo-list-header">
            <Button
              theme='primary'
              onClick={toggleCreateRepoDialog}
            >
              <span class="repo-list-create-button">
                <Icon name='add' size={20}/>
                <span>{t('create')}</span>
              </span>
            </Button>
            <aside>
              <Input
                v-model={query.name}
                class="repo-name-search-input"
                type='search'
                clearable
                onChange={handleSearch}
                placeholder={t('repoNameSearchPlaceholder')}
              >
              </Input>
              <Select
                modelValue={query.type}
                onChange={handleRepoTypeChange}
                class="repo-type-select"
              >
                {
                  REPO_TYPE_LIST.map((option: CommonOption) => (
                    <Option class="repo-type-option-item" value={option.id} label={option.name}>
                      <Icon name={option.id}></Icon>
                      <span class="repo-type-option-item-name">{option.name}</span>
                    </Option>
                  ))
                }
              </Select>
            </aside>
          </header>
          <div class="repo-list-table">
            <Table
              height="100%"
              columns={columns}
              data={tableData.value}
              onRowClick={handleRowClick}
              remotePagination
              pagination={pagination}
              onPageLimitChange={handleLimitChange}
              onPageValueChange={handleCurrentChange}
            >
            </Table>
          </div>
          <CreateRepoDialog
            v-model={showCreateDialog.value}
            onConfirm={fetchRepoList}
          >
          </CreateRepoDialog>
      </Loading>
    );
  },
});
