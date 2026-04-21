import { computed, defineComponent, ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { Button, Table, Loading, Input, Tag } from 'bkui-vue'
import { Search } from 'bkui-vue/lib/icon'
import { SvgIcon } from '@/components/SvgIcon'
import EmptyTableStatus from '@/components/EmptyTable'
import type { Column } from 'bkui-vue/lib/table/props'
import { useTableHeight } from '@/hooks/useTableHeight'
import { useVisibleRange } from '@/hooks/useVisibleRange'
import BkOrgSelector, { type TreeItem } from '@blueking/bk-org-selector';
import '@blueking/bk-org-selector/vue3/vue3.css';
import styles from './VisibleRange.module.css'

export default defineComponent({
  name: 'VisibleRange',
  setup() {
    const { t } = useI18n()
    const tableContainerRef = ref<HTMLDivElement>()
    const orgSelectorRef = ref<InstanceType<typeof BkOrgSelector>>()
    const selectedOrgs = ref<TreeItem[]>([])
    const avatarBaseUrl = 'https://r.hrc.woa.com/photo/150/'
    const apiBaseUrl = 'https://bk-user-web.apigw.o.woa.com/prod'
    const { maxHeight } = useTableHeight(tableContainerRef)
    
    // 使用数据 Hook
    const {
      filteredList,
      loading,
      pagination,
      searchValue,
      init,
      handleSearch,
      handlePageChange,
      handleLimitChange,
      clearFilters,
    } = useVisibleRange()
    
    // 表格列配置
    const tableColumn = computed(
      () =>
        [
          {
            field: 'userName',
            label: '用户/组织',
            width: 200,
            render: ({ row }: { row: any }) => (
              <div class={styles.userCell}>
                <span class={styles.userIcon}>
                  {row.userType === '组织' ? '📦' : '👤'}
                </span>
                <span>{row.userName}</span>
              </div>
            ),
          },
          {
            field: 'userType',
            label: '类型',
            width: 120,
            render: ({ row }: { row: any }) => {
              const typeMap: Record<string, 'success' | 'info' | 'warning'> = {
                组织: 'success',
                内部用户: 'info',
                用户组: 'warning',
              }
              return <Tag theme={typeMap[row.userType] || 'info'}>{row.userType}</Tag>
            },
          },
          {
            field: 'groupName',
            label: '所属组织',
          },
          {
            field: 'updatedBy',
            label: '更新人',
          },
          {
            field: 'updatedAt',
            label: '更新时间',
            width: 180,
          },
          {
            field: 'operation',
            label: '操作',
            width: 100,
            render: ({ row }: { row: any }) => (
              <Button text theme="primary" onClick={() => handleRemove(row.id)}>
                {t('flow.common.remove')}
              </Button>
            ),
          },
        ] as Column[],
    )

    const handleAdd = () => {
      orgSelectorRef.value?.openEdit()
    }

    const handleChange = (value: TreeItem[]) => {
      console.log('选中数据变化:', value)
    }

    const handleChangeResult = (result: { name: string; type: string; data: TreeItem[] }[]) => {
      console.log('格式化结果变化:', result)
    }

    const handleClosed = () => {
      console.log('弹窗已关闭')
    }

    const handleRemove = (id: string) => {
      console.log('移除:', id)
      // TODO: 显示确认弹窗
      // 确认后调用：
      // await handleRemoveAPI([id]) // 传入ID数组
      // 然后刷新列表
    }
    /** 点击确认按钮时触发 */
    const handleAddConfirm = (result: { name: string; type: string; data: TreeItem[] }[]) => {
      // result 按 type 分组：org(组织) / user(用户) / virtual(虚拟账号)
      console.log('确认选择:', result)
      // TODO: 调用添加接口，然后刷新列表
      init()
    }

    onMounted(() => {
      init()
    })
    
    return () => (
      <div class={styles.visibleRange}>
        {/* 提示信息 */}
        <div class={styles.noticeContent}>
            <div class={styles.noticeTitle}>
                <SvgIcon class={styles.helpDocumentFill} name="help-document-fill" size={20} />
                {t('flow.content.visibleRange')}
            </div>
            <i18n-t
                tag="div"
                keypath="flow.visibleRange.visibleRangeDesc"
                class={styles.noticeDesc}
            >
                <span class={styles.noticeBold}>{t('flow.content.trigger')}</span>
            </i18n-t>
        </div>
        
        {/* 操作栏 */}
        <div class={styles.toolbar}>
          <Button theme="primary" onClick={handleAdd}>
            + {t('flow.common.add')}
          </Button>
          <div class={styles.filters}>
            <Input
              v-model={searchValue.value}
              class={styles.searchInput}
              placeholder="搜索用户/组织名称"
              clearable
              onClear={() => handleSearch('')}
              onEnter={(value: string) => handleSearch(value)}
            >
              {{
                suffix: () => <Search class={styles.searchIcon} />,
              }}
            </Input>
          </div>
        </div>
        
        {/* 表格 */}
        <div ref={tableContainerRef} class={styles.tableContainer}>
          <Loading loading={loading.value} mode="spin" theme="primary" size="small">
            <Table
              data={filteredList.value}
              columns={tableColumn.value}
              max-height={maxHeight.value}
              border={['row', 'outer']}
              remote-pagination
              pagination={pagination.value}
              onPageValueChange={handlePageChange}
              onPageLimitChange={handleLimitChange}
            >
              {{
                empty: () => (
                  <EmptyTableStatus
                    type={searchValue.value ? 'search-empty' : 'empty'}
                    desc={searchValue.value ? undefined : t('flow.visibleRange.personalOnly')}
                    onClear={clearFilters}
                  />
                ),
              }}
            </Table>
          </Loading>
        </div>

        {/* 组织选择器（通过 ref 按需打开弹窗） */}
        <BkOrgSelector
          ref={orgSelectorRef}
          v-model={selectedOrgs.value}
          api-base-url={apiBaseUrl}
          avatarBaseUrl={avatarBaseUrl}
          has-user
          virtual-render
          display-mode="simple"
          onChange={handleChange}
          onConfirm={handleAddConfirm}
          onChangeResult={handleChangeResult}
          onClosed={handleClosed}
        />
      </div>
    )
  },
})
