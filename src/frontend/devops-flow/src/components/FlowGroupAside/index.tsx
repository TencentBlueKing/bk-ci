import { ROUTE_NAMES } from '@/constants/routes'
import { Loading, Message, Tag } from 'bkui-vue'
import { computed, defineComponent, nextTick, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import type { EditGroupParams, FlowGroupItem } from '../../api/flowGroup'
import { FLOW_GROUP_TYPES } from '../../constants/flowGroup'
import { useDeleteConfirm } from '../../hooks/useDeleteConfirm'
import { useFlowGroupData } from '../../hooks/useFlowGroupData'
import { SvgIcon } from '../SvgIcon'
import { CreateGroupDialog } from './CreateGroupDialog'
import styles from './FlowGroupAside.module.css'
import { GroupActionMenu } from './GroupActionMenu'
import { RenameGroupDialog } from './RenameGroupDialog'

export enum GroupSectionType {
  MY_FLOWS = 'myFlowGroups',
  PROJECT_FLOWS = 'projectFlowGroups',
}

export const FlowGroupAside = defineComponent({
  name: 'FlowGroupAside',
  components: {
    SvgIcon,
    Tag,
    CreateGroupDialog,
    RenameGroupDialog,
    GroupActionMenu,
  },
  setup() {
    const { t } = useI18n()
    const router = useRouter()
    const route = useRoute()
    const flowGroupData = useFlowGroupData() // 获取处理后的数据
    // 组件本地状态
    const selectedItem = computed(() => {
      return router.currentRoute.value.params.groupId as string
    })
    const projectId = computed(() => {
      return route.params.projectId as string
    })
    const collapsed = ref({
      myFlowGroups: false,
      projectFlowGroups: false,
    })

    const showDialog = ref(false)
    const currentGroupType = ref<GroupSectionType>(GroupSectionType.MY_FLOWS)

    const dialogLoading = ref(false)

    // 重命名弹窗状态
    const showRenameDialog = ref(false)
    const renameGroupId = ref('')
    const renameGroupName = ref('')

    // 当前操作的分组项
    const currentGroupItem = ref<FlowGroupItem | null>(null)

    // 删除确认 hook
    const { showDeleteConfirm } = useDeleteConfirm()

    watch(() => projectId.value, () => {
      nextTick(() => {
        flowGroupData.loadAllData()
      })
    })
    /**
     * 组件挂载时加载数据
     */
    onMounted(() => {
      if (flowGroupData.flowGroups.value.length === 0 && !flowGroupData.loading.value) {
        flowGroupData.loadAllData()
      }
    })

    const handleItemClick = (key: string) => {
      router.push({
        name: ROUTE_NAMES.FLOW_LIST,
        params: { groupId: key },
        query: route.query,
      })
    }

    const handleGroupToggle = (key: GroupSectionType) => {
      collapsed.value[key] = !collapsed.value[key]
    }

    const handleGroupAction = (e: MouseEvent, key: GroupSectionType) => {
      e.stopPropagation()
      currentGroupType.value = key
      showDialog.value = true
    }

    const handleDialogConfirm = async (data: { name: string; projected: boolean }) => {
      try {
        dialogLoading.value = true
        const params: EditGroupParams = {
          ...data,
          viewType: 2,
          logic: 'AND',
          filters: [],
          pipelineIds: [],
        }
        const res = await flowGroupData.createFlowGroup(params)
        if (res.id) {
          Message({ theme: 'success', message: t('flow.dialog.createGroup.addPipelineGroupSuc')})
          flowGroupData.loadAllData()
        }
      } catch (error) {
        console.error('Failed to create flow group:', error)
      } finally {
        dialogLoading.value = false
      }
    }

    // 处理操作菜单点击
    const handleOperationClick = (item: FlowGroupItem, operationId: string) => {
      currentGroupItem.value = item

      if (operationId === 'rename') {
        renameGroupId.value = item.id
        renameGroupName.value = item.name
        showRenameDialog.value = true
      } else if (operationId === 'pinToTop') {
        handlePinToTop(item)
      } else if (operationId === 'delete') {
        handleDelete(item)
      } else if (operationId === 'permissionManage') {
        handlePermissionManage(item)
      }
    }

    // 处理重命名确认
    const handleRenameConfirm = async (data: EditGroupParams) => {
      try {
        dialogLoading.value = true
        const params: EditGroupParams = {
          ...data,
          projected: currentGroupItem.value?.projected
        }
        const res = await flowGroupData.renameFlowGroup(params)
        if (res) {
          Message({ theme: 'success', message: t('flow.actions.rename') + t('flow.common.success') })
          flowGroupData.loadAllData()
        }
      } catch (error) {
        console.error('Failed to rename flow group:', error)
        Message({ theme: 'error', message: t('flow.actions.rename') + t('flow.common.failed') })
      } finally {
        dialogLoading.value = false
      }
    }

    // 处理置顶
    const handlePinToTop = async (item: FlowGroupItem) => {
      try {
        const newTopState = !item.top
        const res = await flowGroupData.pinFlowGroup(item.id, newTopState)
        if (res) {
          Message({
            theme: 'success',
            message: newTopState
              ? t('flow.actions.pinToTop') + t('flow.common.success')
              : t('flow.actions.unpin') + t('flow.common.success'),
          })
          flowGroupData.loadAllData()
        }
      } catch (error) {
        console.error('Failed to pin flow group:', error)
        Message({ theme: 'error', message: t('flow.actions.pinToTop') + t('flow.common.failed') })
      }
    }

    // 处理删除
    const handleDelete = (item: FlowGroupItem) => {
      showDeleteConfirm({
        message: t('flow.actions.confirmDelete', { name: item.name }),
        onConfirm: async () => {
          try {
            await flowGroupData.removeFlowGroup(item.id)
            Message({
              theme: 'success',
              message: t('flow.actions.delete') + t('flow.common.success'),
            })
          } catch (error) {
            console.error('Failed to delete flow group:', error)
            Message({ theme: 'error', message: t('flow.actions.delete') + t('flow.common.failed') })
            throw error // 重新抛出错误，让 InfoBox 不关闭
          }
        },
      })
    }

    // 处理权限管理
    const handlePermissionManage = (item: FlowGroupItem) => {
      // TODO: 跳转到权限管理页面
      // 这里需要根据实际的路由配置来实现
      // 暂时使用 window.open 或路由跳转
      const projectCode = (router.currentRoute.value.params.projectCode as string) || ''
      const permissionUrl = `/manage/${projectCode}/permission?groupId=${item.id}`
      window.open(permissionUrl, '_blank')
    }

    // 获取操作菜单列表
    const getOperations = (item: FlowGroupItem) => {
      const operations = [
        {
          id: 'rename',
          label: t('flow.actions.rename'),
        },
        {
          id: 'pinToTop',
          label: item.top ? t('flow.actions.unpin') : t('flow.actions.pinToTop'),
        },
        {
          id: 'delete',
          label: t('flow.actions.delete'),
        },
      ]

      // 项目组添加权限管理入口
      if (item.projected === true) {
        operations.splice(3, 0, {
          id: 'permissionManage',
          label: t('flow.actions.permissionManage'),
        })
      }

      return operations
    }

    const renderMenuItem = (item: FlowGroupItem) => {
      // 判断是否为配置对象还是分组项
      const { id, name, icon, pipelineCount, showAction = false, top } = item
      const sticky = id === FLOW_GROUP_TYPES.ALL_FLOWS
      const isTrash = id === FLOW_GROUP_TYPES.RECYCLE_BIN
      const operations = showAction ? getOperations(item) : []

      return (
        <div
          key={id}
          class={[
            styles.menuItem,
            selectedItem.value === id && styles.active,
            sticky && styles.stickyMenuItem,
            isTrash && styles.trashItem,
            top && styles.pinnedItem, // 置顶项添加背景色
          ]}
          onClick={() => handleItemClick(id)}
        >
          {icon && <SvgIcon name={icon} class={styles.icon} />}
          <span class={styles.text}>{name}</span>
          <div class={styles.countContainer}>
            {pipelineCount !== undefined && (
              <Tag class={styles.countTag} radius="round" size="small">
                {pipelineCount}
              </Tag>
            )}
            {showAction && operations.length > 0 ? (
              <div class={styles.itemAction}>
                <GroupActionMenu
                  operations={operations}
                  onOperationClick={(operationId: string) =>
                    handleOperationClick(item, operationId)
                  }
                />
              </div>
            ) : (
              <div class={styles.itemActionPlaceholder}></div>
            )}
          </div>
        </div>
      )
    }

    const renderSectionHeader = (key: GroupSectionType, title: string, total: number) => {
      const isCollapsed = collapsed.value[key]
      // "项目创作流组"需要不同的 sticky top 值，避免与"我的创作流"重叠
      const stickyClass = key === 'projectFlowGroups' ? styles.stickyProjectHeader : styles.sticky
      return (
        <div class={[styles.groupHeader, stickyClass]} onClick={() => handleGroupToggle(key)}>
          <SvgIcon
            name="right-shape"
            size={14}
            class={[styles.icon, styles.toggleIcon, !isCollapsed && styles.expanded]}
          />
          <span class={styles.groupTitle}>
            {title} ({total})
          </span>
          {/* TODO: 二期功能，一期暂时屏蔽 */}
          {/* <div onClick={(e) => handleGroupAction(e, key)}>
            <SvgIcon name="increase" class={[styles.icon, styles.increaseIcon]} />
          </div> */}
        </div>
      )
    }

    // 渲染分组部分 - 已移除，因为需要将 header 和 content 分开以保持 sticky 在同一层级

    return () =>
      flowGroupData.loading.value ? (
        <div class={styles.sidebar}>
          <div class={styles.sidebarContent}>
            <div class={styles.stickyPlaceholder}></div>
            <div class={styles.loadingWrapper}>
              <Loading loading size="small" mode="spin" theme="primary" />
            </div>
          </div>
        </div>
      ) : (
        <div class={styles.sidebar}>
          <div class={styles.sidebarContent}>
            {/* sticky 占位块 - 覆盖 sidebarContent 的 padding-top */}
            <div class={styles.stickyPlaceholder}></div>
            {/* 全部创作流 - sticky，直接子元素 */}
            {renderMenuItem({
              id: FLOW_GROUP_TYPES.ALL_FLOWS,
              icon: 'all',
              name: t('flow.common.allFlows'),
              pipelineCount: flowGroupData.counts.value.totalCount,
            })}
            <div class={[styles.divider, styles.sticky]}></div>
            {/* 我的创作流 header - sticky，直接子元素 */}
            {renderSectionHeader(
              GroupSectionType.MY_FLOWS,
              t('flow.sidebar.myFlows'),
              flowGroupData.myFlowGroupsTotal.value,
            )}
            {/* 我的创作流内容 - 在 groupContent 内 */}
            {!collapsed.value.myFlowGroups && (
              <div class={styles.groupContent}>
                {flowGroupData.myFlowGroupMenuItems.value.map(renderMenuItem)}
              </div>
            )}
            <div class={styles.divider}></div>
            {/* 项目创作流组 header - sticky，直接子元素 */}
            {renderSectionHeader(
              GroupSectionType.PROJECT_FLOWS,
              t('flow.sidebar.projectGroups'),
              flowGroupData.projectFlowGroupsTotal.value,
            )}
            {!collapsed.value.projectFlowGroups && (
              <div class={styles.groupContent}>
                {flowGroupData.projectFlowGroups.value.map(renderMenuItem)}
              </div>
            )}
          </div>
          {/* 回收站 - 固定在底部 */}
          <div class={styles.sidebarFooter}>
            {renderMenuItem({
              id: FLOW_GROUP_TYPES.RECYCLE_BIN,
              icon: 'trash-bin',
              name: t('flow.sidebar.recycleBin'),
              pipelineCount: flowGroupData.counts.value.recycleCount,
              showAction: false, // 回收站不需要操作按钮，但需要保留空间以对齐
            })}
          </div>

          {/* 创建分组弹窗 */}
          <CreateGroupDialog
            isShow={showDialog.value}
            isLoading={dialogLoading.value}
            projected={currentGroupType.value === GroupSectionType.PROJECT_FLOWS}
            onUpdate:isShow={(val: boolean) => {
              showDialog.value = val
            }}
            onConfirm={handleDialogConfirm}
          />

          {/* 重命名弹窗 */}
          <RenameGroupDialog
            isShow={showRenameDialog.value}
            groupId={renameGroupId.value}
            isLoading={dialogLoading.value}
            currentName={renameGroupName.value}
            onUpdate:isShow={(val: boolean) => {
              showRenameDialog.value = val
            }}
            onConfirm={handleRenameConfirm}
          />
        </div>
      )
  },
})
