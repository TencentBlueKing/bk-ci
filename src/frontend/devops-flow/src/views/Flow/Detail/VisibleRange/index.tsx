import { computed, defineComponent, ref, onMounted, watch, onBeforeUnmount } from 'vue'
import { useI18n } from 'vue-i18n'
import { Button, Table, Loading, Input, Tag, InfoBox } from 'bkui-vue'
import { Search } from 'bkui-vue/lib/icon'
import { SvgIcon } from '@/components/SvgIcon'
import EmptyTableStatus from '@/components/EmptyTable'
import type { Column } from 'bkui-vue/lib/table/props'
import { useTableHeight } from '@/hooks/useTableHeight'
import { useVisibleRange } from '@/hooks/useVisibleRange'
import type { AddVisibleRangeItem } from '@/api/visibleRange'
import BkOrgSelector, { type TreeItem } from '@blueking/bk-org-selector'
import '@blueking/bk-org-selector/vue3/vue3.css'
import styles from './VisibleRange.module.css'

export default defineComponent({
  name: 'VisibleRange',
  props: {
    tableMaxHeight: {
      type: [Number, String],
      default: undefined,
    },
    source: {
      type: String,
      default: 'detail',
    },
  },
  setup(props) {
    const { t } = useI18n()
    const tableContainerRef = ref<HTMLDivElement>()
    const orgSelectorRef = ref<InstanceType<typeof BkOrgSelector>>()
    const selectedOrgs = ref<TreeItem[]>([])
    const avatarBaseUrl = 'https://r.hrc.woa.com/photo/150/'
    const apiBaseUrl = 'https://bk-user-web.apigw.o.woa.com/prod'
    const { maxHeight } = useTableHeight(tableContainerRef)

    const effectiveMaxHeight = computed(() => {
      return props.tableMaxHeight !== undefined ? props.tableMaxHeight : maxHeight.value
    })
    const {
      visibleRangeList,
      loading,
      pagination,
      searchValue,
      init,
      handleSearch,
      handlePageChange,
      handleLimitChange,
      addVisibleRange,
      handleRemove: removeVisibleRange,
    } = useVisibleRange()

    watch(searchValue, (val) => {
      handleSearch(val)
    })

    // 表格列配置
    const tableColumn = computed(
      () =>
        [
          {
            field: 'scopeName',
            label: t('flow.visibleRange.userOrg'),
            render: ({ row }: { row: any }) => (
              <div class={styles.userCell}>
                <SvgIcon
                  name={row.type === 'ORG' ? 'organization' : 'user'}
                  size={14}
                />
                <span v-bk-tooltips={{ content: row.fullName, disabled: !row.fullName }}>
                  {row.scopeName}
                </span>
              </div>
            ),
          },
          {
            field: 'type',
            label: t('flow.stageReviewEdit.type'),
            minWidth: 120,
            render: ({ row }: { row: any }) => {
              const typeMap: Record<string, { theme: 'info' | 'warning'; label: string }> = {
                ORG: { theme: 'warning', label: t('flow.visibleRange.org') },
                USER: { theme: 'info', label: t('flow.visibleRange.user') },
              }
              const mapped = typeMap[row.type] || { theme: 'info' as const, label: row.type }
              return <Tag theme={mapped.theme}>{mapped.label}</Tag>
            },
          },
          {
            field: 'groupName',
            label: t('flow.visibleRange.organization'),
            showOverflowTooltip: true,
            render: ({ row }: { row: any }) => (
              <span >{row.type === 'USER' ? (row.userDepartments?.join('/') || '--') : '--'}</span>
            ),
          },
          {
            field: 'updater',
            label: t('flow.versionHistory.updater'),
            width: 255,
          },
          {
            field: 'updateTime',
            label: t('flow.versionHistory.updateTime'),
            width: 255,
          },
          {
            field: 'operation',
            label: t('flow.versionHistory.operate'),
            width: 100,
            render: ({ row }: { row: any }) => (
              <Button text theme="danger" onClick={() => handleRemove(row)}>
                {t('flow.common.remove')}
              </Button>
            ),
          },
        ].filter((col) => {
          if (props.source === 'release') return !['updater', 'updateTime'].includes(col.field)
          return true
        }) as Column[],
    )

    const handleAdd = () => {
      orgSelectorRef.value?.openEdit()
      selectedOrgs.value = []
    }

    const handleClosed = () => {
      orgSelectorRef.value?.destroy()
    }

    const handleRemove = (row: any) => {
      InfoBox({
        theme: 'danger',
        title: t('flow.common.remove'),
        content: t('flow.visibleRange.removeConfirm', [row.scopeName]),
        confirmText: t('flow.common.confirm'),
        cancelText: t('flow.common.cancel'),
        onConfirm: () => removeVisibleRange([row.scopeId]),
      })
    }
    /** 点击确认按钮时触发 */
    async function handleAddConfirm(result: { name: string; type: string; data: TreeItem[] }[]) {
      const items: AddVisibleRangeItem[] = result.flatMap((group) => {
        const isOrg = group.type === 'org'
        return group.data.map((item: Record<string, any>) => ({
          type: (isOrg ? 'ORG' : 'USER') as 'ORG' | 'USER',
          scopeId: item.id,
          scopeName: item.name ?? '',
          fullName: isOrg ? item.orgPath : (item.name ?? ''),
          userDepartments: isOrg ? null : (item.organization_paths || (item._displayPath ? [item._displayPath] : [])),
        }))
      })

      await addVisibleRange(items)
    }

    onMounted(() => {
      init()
    })

    onBeforeUnmount(() => {
      searchValue.value = ''
    })

    return () => (
      <div class={styles.visibleRange} style={ props.source === 'release' ? { padding: 0 } : { padding: '20px' }}>
        {/* 提示信息 */}
        {props.source === 'release' ? (
          <bk-tag theme="info" type="stroke" style={{ height: '100%' }}>
            {{
              default: () => (
                <p class={styles.release}>
                  <SvgIcon name="info-line" size={14} />
                  <p>
                    <i18n-t
                      tag="span"
                      keypath="flow.visibleRange.visibleRangeDesc"
                      class={styles.releaseDesc}
                    >
                      <span class={styles.noticeBold}>{t('flow.visibleRange.trigger')}</span>
                    </i18n-t>
                    <p class={styles.releaseDesc}>{t('flow.visibleRange.visibleRangeDescEg')}</p>
                  </p>
                </p>
              ),
            }}
          </bk-tag>
        ) : (
          <div class={styles.noticeContent}>
            <div class={styles.noticeTitle}>
              <SvgIcon class={styles.helpDocumentFill} name="help-document-fill" size={20} />
              {t('flow.content.visibleRange')}
            </div>
            <p class={styles.noticeDesc}>
              <i18n-t
                tag="p"
                keypath="flow.visibleRange.visibleRangeDesc"
              >
                <span class={styles.noticeBold}>{t('flow.visibleRange.trigger')}</span>
              </i18n-t>
              <p>{t('flow.visibleRange.visibleRangeDescEg')}</p>
            </p>
          </div>
        )}

        {/* 操作栏 */}
        <div class={styles.toolbar}>
          <Button theme="primary" onClick={handleAdd}>
            <SvgIcon name="add-small" class={styles.addIcon} size={16} /> {t('flow.common.add')}
          </Button>
          <Input
            v-model={searchValue.value}
            class={styles.searchInput}
            placeholder={t('flow.visibleRange.search')}
            clearable
            onClear={() => handleSearch('')}
          >
            {{
              suffix: () => <Search class={styles.searchIcon} />,
            }}
          </Input>
        </div>

        {/* 表格 */}
        <div ref={tableContainerRef} class={styles.tableContainer}>
          <Loading loading={loading.value} mode="spin" theme="primary" size="small">
            <Table
              data={visibleRangeList.value}
              columns={tableColumn.value}
              max-height={effectiveMaxHeight.value}
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
                    onClear={() => searchValue.value = ''}
                  />
                ),
              }}
            </Table>
          </Loading>
        </div>

        {/* 组织选择器（通过 ref 按需打开弹窗） */}
        <BkOrgSelector
          style={{ display: 'none' }}
          ref={orgSelectorRef}
          v-model={selectedOrgs.value}
          api-base-url={apiBaseUrl}
          avatarBaseUrl={avatarBaseUrl}
          has-user
          virtual-render
          display-mode="simple"
          onConfirm={handleAddConfirm}
          onClosed={handleClosed}
        />
      </div>
    )
  },
})
