import {
  type AddToFlowGroupParams,
  type ContentTableItem,
  type CopyFlowParams,
  type SaveAsTemplateParams,
} from '@/api/flowContentList'
import AddToGroupPopup from '@/components/AddToGroupPopup'
import CopyFlowPopup from '@/components/CopyFlowPopup'
import EmptyTableStatus from '@/components/EmptyTable'
import ExtMenu from '@/components/ExtMenu/index'
import ImportFlowPopup from '@/components/ImportFlowPopup'
import NewFlowPopup from '@/components/NewFlowPopup'
import { RESOURCE_ACTION, RESOURCE_TYPES, PROJECT_RESOURCE_ACTION } from '@/components/Permission/constants'
import SaveAsTemplatePopup from '@/components/SaveAsTemplatePopup'
import StageSteps from '@/components/StageSteps'
import StatusIcon from '@/components/StatusIcon'
import { SvgIcon } from '@/components/SvgIcon'
import { ROUTE_NAMES } from '@/constants/routes'
import { useDeleteConfirm } from '@/hooks/useDeleteConfirm'
import { useFlowListData, type Styles } from '@/hooks/useFlowListData'
import { useTableHeight } from '@/hooks/useTableHeight'
import { FLOW_SORT_FILED } from '@/utils/flowConst.ts'
import { handleFlowNoPermission } from '@/utils/permission'
import SearchSelect from '@blueking/search-select-v3'
import { Button, Dropdown, Loading, Message, Popover, Table, Tag } from 'bkui-vue'
import type { Column } from 'bkui-vue/lib/table/props'
import { websocketRegister } from '@/utils/websocketRegister'
import { useFlowHomeContentStore } from '@/stores/flowContentList'
import { computed, defineComponent, h, nextTick, onMounted, onUnmounted, ref, resolveDirective, watch, withDirectives } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import styles from './FlowTable.module.css'

export const FlowTable = defineComponent({
  name: 'FlowTable',
  components: {
    ExtMenu,
    EmptyTableStatus,
    SvgIcon,
    ImportFlowPopup,
    NewFlowPopup,
    SearchSelect,
    AddToGroupPopup,
    CopyFlowPopup,
    SaveAsTemplatePopup,
    StatusIcon,
    StageSteps,
  },
  props: {
    groupId: {
      type: String,
      required: true,
    },
  },
  setup(props) {
    const { t } = useI18n()
    const router = useRouter()
    const route = useRoute()
    const confirmLoading = ref(false)
    const tableContainerRef = ref<HTMLDivElement>()
    const { maxHeight } = useTableHeight(tableContainerRef)
    const { showDeleteConfirm } = useDeleteConfirm()
    const projectId = computed(() => route.params.projectId as string)

    const {
      pagination,
      tableLoading,
      flowTableList,
      isShowAddToDialog,
      isShowCopyDialog,
      isShowSaveAsTemplateDialog,
      currentActionData,

      sortList,
      newFlowList,
      sortShow,
      currentSortType,
      currentCollation,
      currentSortIconName,
      newFromTemplatePopupShow,
      importFlowPopupShow,
      isRecycleBin,
      latestExecIsStageProgress,
      searchValue,
      searchData,
      searchPlaceHolder,
      currentGroup,
      labelsGroup,

      goEdit,
      handleExecute,
      rowMouseEnter,
      rowMouseLeave,
      collectHandler,
      switchExecView,
      handleRestore,
      updateQuery,
      getProjectTagList,
      changeSortType,
      closeAllDialogs,
      handleTableSortChange,
      handlePageChange,
      handleLimitChange,
      handleSearchChange,
      handleClearSearch,
      removeContent,
      confirmEnableAction,
      copyContentItem,
      saveContentAsTemplate,
      addContentToFlowGroup,
      setDeleteActionCallback,
      setEnableActionCallback,
      loadContentData,
      loadContentDataWithGroupId,
      initSearchFromQuery,
    } = useFlowListData(styles as Styles)

    const permDirective = resolveDirective('perm')
    const contentStore = useFlowHomeContentStore()

    // ---- WebSocket real-time status updates from parent (devops-nav) ----
    const WS_ID = 'flowList'
    websocketRegister.installWsMessage(
      (data) => contentStore.updatePipelineStatusFromWs(data),
      WS_ID,
    )
    onUnmounted(() => websocketRegister.unInstallWsMessage(WS_ID))

    onMounted(async () => {
      updateQuery()
    })

    // 使用传入的 groupId 加载数据
    watch(
      () => props.groupId,
      async (newGroupId, oldGroupId) => {
        if (newGroupId) {
          // 切换分组时清空搜索条件
          if (oldGroupId && newGroupId !== oldGroupId) {
            searchValue.value = []
            updateQuery(true)
          }

          labelsGroup.value = await getProjectTagList()
          initSearchFromQuery()
          loadContentData(newGroupId)
        }
      },
      { immediate: true },
    )

    watch(
      () => projectId.value,
      () => {
        nextTick(() => {
          loadContentData(props.groupId)
        })
      },
    )

    watch([currentSortType, currentCollation], () => {
      loadContentData(props.groupId)
      updateQuery()
    })

    const fieldToSortTypeMap: Record<string, string> = {
      pipelineName: FLOW_SORT_FILED.flowName,
      latestBuildStartDate: FLOW_SORT_FILED.latestBuildStartDate,
      updateTime: FLOW_SORT_FILED.updateTime,
      createDate: FLOW_SORT_FILED.createDate,
    }

    const renderCollect = (row: ContentTableItem) => {
      if (row.delete) {
        return
      }
      return (
        <Button
          text
          class={[styles.iconStarBtn, row.hasCollect ? styles.isCollect : '']}
          theme={row.hasCollect ? 'warning' : ''}
          onClick={() => collectHandler(row.hasCollect, row.pipelineId)}
        >
          <SvgIcon name={!row.hasCollect ? 'star-line' : 'star-shape'} size={14} />
        </Button>
      )
    }

    const hasViewPermission = (row: ContentTableItem) => {
      return row?.permissions?.canView ?? true
    }

    const renderFlowName = (row: ContentTableItem) => {
      const handleFlowClick = () => {
        if (row.delete) return
        if (!hasViewPermission(row)) return

        router.push({
          name: ROUTE_NAMES.FLOW_DETAIL,
          params: {
            projectId: row.projectId,
            flowId: row.pipelineId,
          },
        })
      }

      return (
        <>
          <span
            class={row.delete ? 'text-disabled' : hasViewPermission(row) ? styles.nameLink : styles.noPermLink}
            v-overflow-title
            onClick={handleFlowClick}
          >
            {row.pipelineName}
          </span>
          {row.onlyDraftVersion ? (
            <Tag theme="success" class="draft-tag">
              {t('flow.content.draft')}
            </Tag>
          ) : null}
          {row.onlyBranchVersion ? (
            <Tag theme="success" class="draft-tag">
              {t('flow.content.branch')}
            </Tag>
          ) : null}
        </>
      )
    }

    const renderTags = (row: ContentTableItem) => {
      const tags = row.viewNames

      if (row.delete) {
        return <span class="text-disabled">{t('flow.content.deleteAlready')}</span>
      }

      if (!tags || !Array.isArray(tags) || tags.length === 0) {
        return <span>--</span>
      }

      const maxDisplayCount = 2
      const showMore = tags.length > 3

      const displayedTags = showMore ? tags.slice(0, maxDisplayCount) : tags
      const remainingTags = tags.slice(maxDisplayCount)
      const remainingCount = remainingTags.length

      return (
        <div class={styles.tagList}>
          {displayedTags.map((tag) => (
            <Tag key={tag} class={styles.tag} v-overflow-title>
              {tag}
            </Tag>
          ))}

          {showMore ? (
            <Popover theme="light" maxWidth={280} placement="bottom-end">
              {{
                default: () => <Tag class={styles.tag}>+{remainingCount}</Tag>,
                content: () => (
                  <div class={styles.popoverTagList}>
                    {remainingTags.map((tag) => (
                      <Tag key={tag} class={styles.tag} v-overflow-title>
                        {tag}
                      </Tag>
                    ))}
                  </div>
                ),
              }}
            </Popover>
          ) : null}
        </div>
      )
    }

    const DescItem = (icon?: string, content?: string) => {
      if (!content) return null

      return (
        <span class={styles.execDesc}>
          {icon && <SvgIcon name={icon} size={16} />}
          <span class="text-ellipsis" v-overflow-title>
            {content}
          </span>
        </span>
      )
    }

    const toLatestBuildRoute = (row: ContentTableItem) => {
      if (hasViewPermission(row) && row.latestBuildRoute) {
        router.push(row.latestBuildRoute)
      }
    }

    const renderLastExecLabel = () => {
      return isRecycleBin.value ? (
        t('flow.content.lastExecution')
      ) : (
        <div class={styles.lastExecHeader}>
          <span>{t('flow.content.lastExecution')}</span>
          <p onClick={switchExecView} class={styles.switchExec}>
            <SvgIcon name="exchange-line" size={14} />
            {!latestExecIsStageProgress.value
              ? t('flow.content.showStageProgress')
              : t('flow.content.showBuildInfo')}
          </p>
        </div>
      )
    }

    const renderLastExec = (row: ContentTableItem) => {
      if (row.delete) {
        return
      }
      const canView = hasViewPermission(row)
      return (
        <div class={styles.latestExecCell}>
          <StatusIcon status={row.latestBuildStatus} size={22} />
          <div class={styles.flowExecMsg}>
            {row.latestBuildNum ? (
              <>
                <div
                  class={[styles.flowExecMsgTitle, 'text-ellipsis']}
                  onClick={canView ? () => toLatestBuildRoute(row) : undefined}
                >
                  <b class={canView ? styles.flowCellLink : styles.flowCellLinkDisabled}>#{row.latestBuildNum}</b>
                  <b class={[canView ? styles.flowCellLink : styles.flowCellLinkDisabled, styles.line]}>|</b>
                  {!latestExecIsStageProgress.value ? (
                    <span class="lastBuildMsg">{row.lastBuildMsg}</span>
                  ) : (
                    <span style={{ display: 'inline-block' }}>
                      {row.latestBuildStageStatus && row.latestBuildId ? (
                        <StageSteps
                          class={styles.latestStageStatus}
                          steps={row.latestBuildStageStatus}
                          buildId={row.latestBuildId}
                        />
                      ) : (
                        <span>--</span>
                      )}
                    </span>
                  )}
                </div>
                <p class={styles.flowExecMsgDesc}>
                  {DescItem(row.startType, row.latestBuildUserId)}
                  {row.webhookAliasName && DescItem('branch', row.webhookAliasName)}
                  {row.webhookMessage && DescItem(row.startType, row.webhookMessage)}
                </p>
              </>
            ) : (
              <span class={styles.execDesc}>{t('flow.content.unexecute')}</span>
            )}
          </div>
        </div>
      )
    }

    const renderLatestBuildStartDate = (row: ContentTableItem) => {
      if (row.delete) {
        return
      }
      return (
        <p class={styles.columnTime}>
          <span>{row.latestBuildStartDate || ''}</span>
          {row.progress ? (
            <span class={styles.runningText}>{row.progress}</span>
          ) : (
            <span class="text-tertiary">{row.duration}</span>
          )}
        </p>
      ) as any
    }

    const renderLastModify = (row: ContentTableItem) => {
      if (row.delete) {
        return
      }
      return (
        <p class={styles.columnTime}>
          <span>{row.updater}</span>
          <span class="text-tertiary">{row.updateDate}</span>
        </p>
      )
    }

    const renderActions = (row: ContentTableItem) => {
      if (row.delete) {
        return
      }

      const permissions = row.permissions
      const canView = hasViewPermission(row)

      if (!canView && !isRecycleBin.value) {
        return (
          <Button
            theme="primary"
            size="small"
            onClick={() => applyPermission(row)}
          >
            {t('flow.content.applyPermission')}
          </Button>
        )
      }

      return (
        <>
          {isRecycleBin.value ? (
            withDirectives(
              <Button text theme="primary" onClick={() => handleRestore(row)}>
                {t('flow.restore.restore')}
              </Button>,
              permDirective
                ? [[permDirective, {
                  hasPermission: permissions?.canManage ?? true,
                  disablePermissionApi: true,
                  permissionData: {
                    projectId: projectId.value,
                    resourceType: RESOURCE_TYPES.PROJECT,
                    resourceCode: projectId.value,
                    action: PROJECT_RESOURCE_ACTION.MANAGE,
                  },
                }]]
                : [],
            )
          ) : (
            <div class={styles.actions}>
              {!(row.released || row.onlyBranchVersion) ? (
                withDirectives(
                  <Button text theme="primary" onClick={() => goEdit(row)}>
                    {t('flow.content.edit')}
                  </Button>,
                  permDirective
                    ? [[permDirective, {
                      hasPermission: permissions?.canEdit ?? true,
                      disablePermissionApi: true,
                      permissionData: {
                        projectId: projectId.value,
                        resourceType: RESOURCE_TYPES.CREATIVE_STREAM,
                        resourceCode: row.pipelineId,
                        action: RESOURCE_ACTION.EDIT,
                      },
                    }]]
                    : [],
                )
              ) : (
                withDirectives(
                  <Button
                    text
                    theme="primary"
                    disabled={row.disabled}
                    onClick={() => handleExecute(row)}
                    v-bk-tooltips={{
                      content: row.tooltips,
                      disabled: !row.disabled,
                    }}
                  >
                    {row.lock
                      ? t('flow.content.disabled')
                      : row.canManualStartup
                        ? t('flow.content.execute')
                        : t('flow.content.nonManual')}
                  </Button>,
                  permDirective
                    ? [[permDirective, {
                      hasPermission: permissions?.canExecute ?? true,
                      disablePermissionApi: true,
                      permissionData: {
                        projectId: projectId.value,
                        resourceType: RESOURCE_TYPES.CREATIVE_STREAM,
                        resourceCode: row.pipelineId,
                        action: RESOURCE_ACTION.EXECUTE,
                      },
                    }]]
                    : [],
                )
              )}
              <ExtMenu data={row} config={row.flowAction} />
            </div>
          )}
        </>
      )
    }

    const applyPermission = (row: ContentTableItem) => {
      handleFlowNoPermission({
        projectId: row.projectId,
        resourceType: RESOURCE_TYPES.CREATIVE_STREAM,
        resourceCode: row.pipelineId,
        action: RESOURCE_ACTION.VIEW,
      })
    }

    const setRowCls = ({ row }: { row: ContentTableItem }) => {
      const cls: string[] = []
      if (row?.delete) cls.push('has-delete')
      if (!hasViewPermission(row) && styles.noPermission) cls.push(styles.noPermission)
      return cls.join(' ')
    }

    const sortConfig = (field: string) => {
      return {
        value:
          currentSortType.value === field && currentCollation.value ? currentCollation.value : null,
        sortScope: 'all',
      }
    }

    const tableColumn = computed(() => {
      const column = [
        ...(!isRecycleBin.value
          ? [
            {
              fixed: 'left',
              minWidth: 30,
              render: ({ row }: { row: ContentTableItem }) => renderCollect(row),
            },
            {
              label: t('flow.content.name'),
              field: 'pipelineName',
              fixed: 'left',
              minWidth: 192,
              sort: sortConfig(FLOW_SORT_FILED.flowName),
              render: ({ row }: { row: ContentTableItem }) => renderFlowName(row),
            },
            {
              label: t('flow.content.groupName'),
              field: 'viewNames',
              minWidth: 280,
              render: ({ row }: { row: ContentTableItem }) => renderTags(row),
            },
            {
              label: renderLastExecLabel(),
              field: 'latestBuildStatus',
              minWidth: 300,
              render: ({ row }: { row: ContentTableItem }) => renderLastExec(row),
            },
            {
              label: t('flow.content.executionTime'),
              field: 'latestBuildStartDate',
              minWidth: 154,
              render: ({ row }: { row: ContentTableItem }) => renderLatestBuildStartDate(row),
              sort: sortConfig(FLOW_SORT_FILED.latestBuildStartDate),
            },
            {
              label: t('flow.content.lastModify'),
              field: 'updateTime',
              minWidth: 154,
              render: ({ row }: { row: ContentTableItem }) => renderLastModify(row),
              sort: sortConfig(FLOW_SORT_FILED.updateTime),
            },
            {
              label: t('flow.content.creator'),
              field: 'creator',
              minWidth: 100,
              render: ({ row }: { row: ContentTableItem }) => (
                <>{!row.delete ? <span>{row.creator}</span> : null}</>
              ),
            },
            {
              label: t('flow.content.createTime'),
              field: 'createDate',
              minWidth: 154,
              sort: sortConfig(FLOW_SORT_FILED.createDate),
              render: ({ row }: { row: ContentTableItem }) => (
                <>{!row.delete ? <span>{row.createDate}</span> : null}</>
              ),
            },
          ]
          : [
            {
              label: t('flow.content.name'),
              field: 'name',
              fixed: 'left',
              minWidth: 192,
              sort: sortConfig(FLOW_SORT_FILED.flowName),
            },
            {
              label: t('flow.content.createTime'),
              field: 'createDate',
              minWidth: 154,
              sort: sortConfig(FLOW_SORT_FILED.createDate),
            },
            {
              label: t('flow.restore.deleter'),
              field: 'lastModifyUser',
              minWidth: 100,
            },
            {
              label: t('flow.restore.deleteTime'),
              field: 'updateTime',
              minWidth: 154,
              sort: sortConfig(FLOW_SORT_FILED.updateTime),
              render: ({ row }: { row: ContentTableItem }) => (
                <>{!row.delete ? <span>{row.updateDate}</span> : null}</>
              ),
            },
          ]),
        {
          label: t('flow.content.actions'),
          field: 'actions',
          fixed: 'right',
          width: 160,
          render: ({ row }: { row: ContentTableItem }) => renderActions(row),
        },
      ] as Column[]
      return column
    })

    function handleSort({ column, type }: any) {
      const sortType = fieldToSortTypeMap[column.field] || ''
      const collation = type
      handleTableSortChange({ sortType, collation })
    }

    function pageChange(current: number) {
      handlePageChange(current)
      loadContentData(props.groupId)
    }

    function limitChange(limit: number) {
      handleLimitChange(limit)
      loadContentData(props.groupId)
    }

    // 处理删除操作
    function handleDeleteAction(data: any) {
      const objectName = data?.pipelineName || data?.pipelineId
      showDeleteConfirm({
        message: () => [
          `${t('flow.content.confirmDeleteFlow')}\n${t('flow.content.operationObject')}: `,
          h('strong', { style: 'font-weight: 700; color: var(--color-text-primary);' }, objectName),
        ],
        onConfirm: async () => {
          try {
            const res = await removeContent(data?.pipelineId)
            const allSuccess = Object.values(res).every((success) => success)

            if (allSuccess) {
              Message({ theme: 'success', message: t('flow.content.deleteSuccess') })
              loadContentDataWithGroupId(props.groupId)
            } else {
              const failedPipelineIds = Object.entries(res)
                .filter(([_, success]) => !success)
                .map(([pipelineId]) => pipelineId)

              Message({
                theme: 'error',
                message: `${failedPipelineIds.join(', ')} ${t('flow.content.deleteFail')}`,
              })
            }
          } catch (error: any) {
            Message({ theme: 'error', message: error?.message || error })
          }
        },
      })
    }

    // 处理启用/禁用操作
    function handleEnableAction(data: any) {
      const isEnable = data?.lock
      const objectName = data?.pipelineName || data?.pipelineId
      showDeleteConfirm({
        message: () => [
          `${!isEnable ? t('flow.content.confirmDisableFlow') : t('flow.content.confirmEnableFlow')}\n${t('flow.content.operationObject')}: `,
          h('strong', { style: 'font-weight: 700; color: var(--color-text-primary);' }, objectName),
        ],
        theme: 'primary',
        confirmText: t('flow.common.confirm'),
        onConfirm: async () => {
          try {
            const res = await confirmEnableAction(data?.pipelineId, data.lock)
            if (res) {
              Message({ theme: 'success', message: t('flow.common.success') })
              loadContentData(props.groupId)
            }
          } catch (error: any) {
            Message({ theme: 'error', message: error?.message || error })
          }
        },
      })
    }

    // 设置删除操作回调
    setDeleteActionCallback(handleDeleteAction)
    // 设置启用/禁用操作回调
    setEnableActionCallback(handleEnableAction)

    const handleAddTo = async (pipelineId: string, groupId: string[]) => {
      confirmLoading.value = true
      const params: AddToFlowGroupParams = {
        pipelineIds: [pipelineId],
        viewIds: groupId,
      }
      try {
        const res = await addContentToFlowGroup(params)
        if (res) {
          Message({
            theme: 'success',
            message: t('flow.content.addTo') + t('flow.common.success'),
          })
          loadContentDataWithGroupId(props.groupId)
          closeAllDialogs()
        }
      } catch (error: any) {
        Message({ theme: 'error', message: error?.message || error })
      } finally {
        confirmLoading.value = false
      }
    }

    const handleCopyFlow = async (pipelineId: string, param: CopyFlowParams) => {
      confirmLoading.value = true
      const { dynamicGroup, ...otherParams } = param
      try {
        const params = {
          ...otherParams,
          pipelineId: pipelineId,
        }
        const res = await copyContentItem(params)
        if (res) {
          Message({
            theme: 'success',
            message: t('flow.content.copyCreationFlow') + t('flow.common.success'),
          })
          loadContentDataWithGroupId(props.groupId)
          closeAllDialogs()
        }
      } catch (error: any) {
        Message({ theme: 'error', message: error?.message || error })
      } finally {
        confirmLoading.value = false
      }
    }

    const handleSaveAsTemplate = async (params: SaveAsTemplateParams) => {
      confirmLoading.value = true
      try {
        await saveContentAsTemplate(params)
        Message({
          theme: 'success',
          message: t('flow.content.saveAsTemplate') + t('flow.common.success'),
        })
        closeAllDialogs()
      } catch (error: any) {
        Message({ theme: 'error', message: error?.message || error })
      } finally {
        confirmLoading.value = false
      }
    }

    return () => (
      <div class={styles.content}>
        <div class={styles.toolbar}>
          <h2 class={styles.title}>{currentGroup.value?.name}</h2>
        </div>
        <div class={styles.tableContainer}>
          <div class={styles.toolbar}>
            {!isRecycleBin.value && (
              <Dropdown
                trigger="click"
                popover-options={{
                  clickContentAutoHide: true,
                }}
              >
                {{
                  default: () => (
                    <Button theme="primary">
                      <SvgIcon name="add-small" size={22} />
                      {t('flow.content.newFlow')}
                    </Button>
                  ),
                  content: () => (
                    <Dropdown.DropdownMenu>
                      {newFlowList.value.map((item) => (
                        <Dropdown.DropdownItem
                          key={item.text}
                          onClick={item.handler}
                          class={styles.newFlow}
                        >
                          {item.text}
                        </Dropdown.DropdownItem>
                      ))}
                    </Dropdown.DropdownMenu>
                  ),
                }}
              </Dropdown>
            )}
            {/* <Button>{t('flow.content.batchManage')}</Button> */}
            <div class={styles.searchBox}>
              <SearchSelect
                modelValue={searchValue.value}
                data={searchData.value}
                placeholder={searchPlaceHolder.value}
                class={styles.searchInput}
                uniqueSelect
                onUpdate:modelValue={handleSearchChange}
              />
              {!isRecycleBin.value ? (
                <Dropdown
                  trigger="click"
                  is-show={sortShow.value}
                  popover-options={{
                    clickContentAutoHide: true,
                  }}
                >
                  {{
                    default: () => (
                      <div class={styles.iconSortButton}>
                        <SvgIcon
                          name={currentSortIconName.value}
                          class={styles.sortIcon}
                          size={10}
                        />
                      </div>
                    ),
                    content: () => (
                      <Dropdown.DropdownMenu>
                        {sortList.value.map((item) => (
                          <Dropdown.DropdownItem
                            key={item.id}
                            class={`${styles.sortItem} ${item.active ? styles.active : ''}`}
                            onClick={() => changeSortType(item.id)}
                          >
                            {item.name}
                            <SvgIcon name={item.sortIcon} class={styles.sortItemIcon} size={10} />
                          </Dropdown.DropdownItem>
                        ))}
                      </Dropdown.DropdownMenu>
                    ),
                  }}
                </Dropdown>
              ) : null}
            </div>
          </div>
          <div class={styles.flowTable} ref={tableContainerRef}>
            <Loading loading={tableLoading.value} mode="spin" theme="primary" size="small">
              <Table
                data={flowTableList.value}
                columns={tableColumn.value}
                max-height={maxHeight.value}
                border={['row', 'outer']}
                pagination={pagination.value}
                remote-pagination
                rowClass={setRowCls}
                onColumnSort={handleSort}
                onPageValueChange={pageChange}
                onPageLimitChange={limitChange}
                onRowMouseEnter={rowMouseEnter}
                onRowMouseLeave={rowMouseLeave}
              >
                {{
                  empty: () => (
                    <EmptyTableStatus
                      type={searchValue.value.length > 0 ? 'search-empty' : 'empty'}
                      onClear={handleClearSearch}
                    />
                  ),
                }}
              </Table>
            </Loading>
          </div>
        </div>

        {newFromTemplatePopupShow.value && (
          <NewFlowPopup
            isShow={newFromTemplatePopupShow.value}
            onUpdate:isShow={(val: boolean) => {
              newFromTemplatePopupShow.value = val
            }}
          />
        )}

        {importFlowPopupShow.value && (
          <ImportFlowPopup
            isShow={importFlowPopupShow.value}
            onUpdate:isShow={(val: boolean) => {
              importFlowPopupShow.value = val
            }}
          />
        )}

        {isShowAddToDialog.value && (
          <AddToGroupPopup
            isShow={isShowAddToDialog.value}
            data={currentActionData.value}
            loading={confirmLoading.value}
            onUpdate:isShow={closeAllDialogs}
            onConfirm={handleAddTo}
          />
        )}

        {isShowCopyDialog.value && (
          <CopyFlowPopup
            isShow={isShowCopyDialog.value}
            data={currentActionData.value}
            loading={confirmLoading.value}
            onUpdate:isShow={closeAllDialogs}
            onConfirm={handleCopyFlow}
          />
        )}

        {isShowSaveAsTemplateDialog.value && (
          <SaveAsTemplatePopup
            isShow={isShowSaveAsTemplateDialog.value}
            data={currentActionData.value}
            loading={confirmLoading.value}
            onUpdate:isShow={closeAllDialogs}
            onConfirm={handleSaveAsTemplate}
          />
        )}
      </div>
    )
  },
})
