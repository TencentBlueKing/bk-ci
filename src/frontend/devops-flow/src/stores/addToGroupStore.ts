import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { defineStore } from 'pinia'
import { useRoute } from 'vue-router'
import { Message } from 'bkui-vue'
import { FLOW_GROUP_TYPES } from '@/constants/flowGroup'
import { useFlowGroupData } from '@/hooks/useFlowGroupData'
import { getSelectedTreeData, type SelectedTreeDataResponse } from '@/api/flowContentList'
import type { FlowGroupItem } from '@/api/flowGroup'

/**
 * 树节点数据结构
 * @description 定义流水线分组树形结构的节点类型
 */
export interface TreeNode {
  id: string
  name: string
  hasChild: boolean
  desc?: string
  checked?: boolean
  indeterminate?: boolean
  disabled?: boolean
  children?: TreeNode[]
  tooltips?: {
    content: string
    delay: number
    disabled: boolean
  }
  sortPos?: number
  isDynamicGroup?: boolean
  projected?: boolean
  viewType?: number
}

/**
 * 初始化弹窗参数
 */
interface InitPopupData {
  pipelineId?: string
}

/**
 * AddToGroupPopup状态管理
 */
export const useAddToGroupStore = defineStore('addToGroup', () => {
  const { t } = useI18n()
  const route = useRoute()
  const { flowGroups } = useFlowGroupData()
  
  // ========== 状态定义 ==========
  const loading = ref(false)
  const filterKeyword = ref('')
  const selectedGroups = ref<FlowGroupItem[]>([])
  const isManage = ref(true)
  /** 已保存的流水线分组数据 */
  const savedPipelineGroups = ref<SelectedTreeDataResponse[]>([])

  /**
   * 已保存分组的映射表
   * @description 将已保存的分组ID转换为哈希表，便于快速查找
   */
  const savedPipelineGroupMap = computed<Record<string, boolean>>(() => {
    return savedPipelineGroups.value.reduce(
      (acc, group) => {
        if (group.id) {
          acc[group.id] = true
        }
        return acc
      },
      {} as Record<string, boolean>,
    )
  })

  /**
   * 流水线分组树形结构
   * @description 构建包含个人分组和项目分组的两级树形结构
   * - 第一层：个人分组(personal)、项目分组(projected)
   * - 第二层：具体的分组节点
   * - 自动标记已添加、动态分组、无权限等状态
   */
  const pipelineGroupsTree = computed(() => {
    const tooltips = {
      content: t('groupEditDisableTips'),
      delay: 500,
      disabled: isManage.value,
    }
    const tree = flowGroups.value
      .reduce<TreeNode[]>(
        (acc, group) => {
          const index = group.projected ? 1 : 0
          const isDynamicGroup = group.viewType === 1
          const isUnclassifyGroup = group.id === FLOW_GROUP_TYPES.UNCLASSIFIED_FLOWS
          if (!isUnclassifyGroup) {
            const needManage = group.projected && !isManage.value
            let desc = null
            let sortPos = 0
            const isAdded = savedPipelineGroupMap.value[group.id]

            // 根据分组状态设置描述和排序优先级
            switch (true) {
              case isAdded:
                desc = 'flow.dialog.addGroup.added'
                sortPos = 1 // 已添加的排在最前
                break
              case isDynamicGroup:
                desc = 'flow.dialog.addGroup.dynamicGroup'
                sortPos = 2 // 动态分组排在中间
                break
              case needManage:
                desc = 'flow.dialog.addGroup.err403'
                sortPos = 3 // 无权限的排在最后
                break
            }

            // 确保父节点存在且有 children 数组
            const parentNode = acc[index]
            if (parentNode?.children) {
              parentNode.children.push({
                ...group,
                hasChild: false,
                indeterminate: false,
                checked: isAdded, // 已添加的显示为已选中
                disabled: isDynamicGroup || isAdded || needManage,
                tooltips: {
                  ...tooltips,
                  disabled: !needManage,
                },
                sortPos,
                ...(desc && { desc: t(desc) }),
                isDynamicGroup,
              })
            }
          }
          return acc
        },
        [
          {
            id: 'personal',
            name: t('flow.dialog.addGroup.personalFlowGroup'),
            hasChild: false,
            indeterminate: false,
            checked: false,
            disabled: false,
            children: [],
          },
          {
            id: 'projected',
            name: t('flow.sidebar.projectGroups'),
            hasChild: false,
            indeterminate: false,
            checked: false,
            disabled: !isManage.value,
            tooltips,
            children: [],
          },
        ],
      )
      .map((node) => {
        // 按排序权重对子节点排序
        node.children!.sort((a, b) => (a.sortPos || 0) - (b.sortPos || 0))

        // 根据子节点的选中状态更新父节点的状态
        const checkedChildren = node.children!.filter((child) => child.checked).length
        const totalChildren = node.children!.length

        if (checkedChildren === 0) {
          node.checked = false
          node.indeterminate = false
        } else if (checkedChildren === totalChildren) {
          node.checked = true
          node.indeterminate = false
        } else {
          node.checked = false
          node.indeterminate = true
        }

        return {
          ...node,
          hasChild: node.children!.length > 0,
          disabled: node.children!.length <= 0 || node.disabled,
        }
      })

    return tree
  })

  // ========== 工具函数 ==========

  /**
   * 根据ID查找节点
   * @description 递归遍历树形结构，查找指定ID的节点
   * @param nodes 节点数组
   * @param id 节点ID
   * @returns 找到的节点，未找到返回null
   */
  function findNodeById(nodes: TreeNode[], id: string): TreeNode | null {
    for (const node of nodes) {
      if (node.id === id) return node
      if (node.children) {
        const found = findNodeById(node.children, id)
        if (found) return found
      }
    }
    return null
  }

  /**
   * 更新节点状态
   * @description 根据子节点的选中情况更新父节点的checked和indeterminate状态
   * @param node 要更新的节点
   */
  function updateNodeState(node: TreeNode) {
    if (!node.children || node.children.length === 0) {
      node.indeterminate = false
      return
    }

    const checkedChildren = node.children.filter((child) => child.checked).length
    const totalChildren = node.children.length

    if (checkedChildren === 0) {
      node.checked = false
      node.indeterminate = false
    } else if (checkedChildren === totalChildren) {
      node.checked = true
      node.indeterminate = false
    } else {
      node.checked = false
      node.indeterminate = true
    }
  }

  /**
   * 递归更新所有父节点的状态
   * @description 从目标节点向上递归，更新所有父节点的选中状态
   * @param targetNode 目标节点
   * @param nodes 搜索范围的节点数组
   * @returns 是否找到并更新了父节点
   */
  function updateAllParentStates(
    targetNode: TreeNode,
    nodes: TreeNode[] = pipelineGroupsTree.value,
  ): boolean {
    for (const node of nodes) {
      if (node.children) {
        const isDirectParent = node.children.some((child) => child.id === targetNode.id)
        if (isDirectParent) {
          updateNodeState(node)
          return true
        }

        const found = updateAllParentStates(targetNode, node.children)
        if (found) {
          updateNodeState(node)
          return true
        }
      }
    }
    return false
  }

  // ========== 核心操作 ==========

  /**
   * 处理复选框变化
   * @description 处理节点选中/取消选中事件，同步更新子节点和父节点状态
   * - 如果是父节点：级联更新所有非禁用的子节点
   * - 如果是子节点：向上更新所有父节点的半选/全选状态
   * @param checked 是否选中
   * @param node 被操作的节点
   */
  function handleCheck(checked: boolean, node: TreeNode) {
    const targetNode = findNodeById(pipelineGroupsTree.value, node.id)
    if (!targetNode || targetNode.disabled) return

    // 更新目标节点状态
    targetNode.checked = checked
    targetNode.indeterminate = false

    if (targetNode.hasChild && targetNode.children) {
      // 父节点：级联更新所有非禁用的子节点
      targetNode.children.forEach((child) => {
        if (!child.disabled) {
          child.checked = checked
          child.indeterminate = false
        }
      })
      updateNodeState(targetNode)
    } else {
      // 子节点：向上更新所有父节点状态
      updateAllParentStates(targetNode)
    }

    updateSelectedGroups()
  }

  /**
   * 获取所有选中的叶子节点ID
   * @description 递归遍历树形结构，收集所有选中的叶子节点（不包括父节点）
   * @param nodes 要遍历的节点数组
   * @returns 选中的叶子节点ID数组
   */
  function getCheckedLeafNodes(nodes: TreeNode[] = pipelineGroupsTree.value): string[] {
    const result: string[] = []

    const traverse = (nodeList: TreeNode[]) => {
      nodeList.forEach((node) => {
        if (node.children && node.children.length > 0) {
          // 有子节点：继续递归遍历
          traverse(node.children)
        } else if (
          node.checked &&
          !node.disabled &&
          node.id !== 'personal' &&
          node.id !== 'projected'
        ) {
          // 叶子节点：满足条件则添加到结果
          // 排除 personal 和 projected 父节点
          result.push(node.id)
        }
      })
    }

    traverse(nodes)
    return result
  }

  /**
   * 更新选中组列表
   * @description 根据树形结构中的选中状态，更新 selectedGroups
   */
  function updateSelectedGroups() {
    const checkedIds = getCheckedLeafNodes()
    selectedGroups.value = checkedIds.map((id) => {
      const node = findNodeById(pipelineGroupsTree.value, id)
      return {
        id: node?.id || id,
        name: node?.name || id,
      } as FlowGroupItem
    })
  }

  /**
   * 清空所有选中状态
   * @description 递归清空所有非禁用节点的选中状态，并更新父节点
   */
  function emptySelectedGroups() {
    const clearCheckedState = (nodes: TreeNode[]) => {
      nodes.forEach((node) => {
        if (!node.disabled) {
          node.checked = false
          node.indeterminate = false
        }
        if (node.children) {
          clearCheckedState(node.children)
        }
      })
    }

    clearCheckedState(pipelineGroupsTree.value)
    selectedGroups.value = []

    // 清空后需要更新所有父节点的状态
    pipelineGroupsTree.value.forEach((parent) => {
      if (parent.children) {
        updateNodeState(parent)
      }
    })
  }

  /**
   * 移除单个分组
   * @description 从已选列表中移除指定分组，并更新树形结构状态
   * @param group 要移除的分组
   */
  function remove(group: FlowGroupItem) {
    const node = findNodeById(pipelineGroupsTree.value, group.id)
    if (node) {
      node.checked = false
      node.indeterminate = false

      updateAllParentStates(node)
      updateSelectedGroups()
    }
  }

  // ========== API 交互 ==========

  /**
   * 加载已选中的树数据
   * @description 根据流水线ID获取该流水线已添加的分组列表，并标记为禁用状态
   * @param flowId 流水线ID
   */
  async function loadSelectedTreeData(flowId: string) {
    loading.value = true
    try {
      const res = await getSelectedTreeData(route.params.projectId as string, flowId)
      savedPipelineGroups.value = res
    } catch (error) {
      Message({ message: t('flow.group.tree.load.error'), theme: 'error' })
    } finally {
      loading.value = false
    }
  }

  /**
   * 初始化弹窗
   * @description 重置弹窗状态，如果提供了流水线ID则加载已选数据
   * @param data 初始化参数
   */
  async function initPopup(data?: InitPopupData) {
    emptySelectedGroups()
    filterKeyword.value = ''

    if (data?.pipelineId) {
      await loadSelectedTreeData(data.pipelineId)
    }
  }

  return {
    // State
    loading,
    filterKeyword,
    selectedGroups,
    pipelineGroupsTree,

    // Actions
    findNodeById,
    updateNodeState,
    updateAllParentStates,
    handleCheck,
    getCheckedLeafNodes,
    updateSelectedGroups,
    emptySelectedGroups,
    remove,
    loadSelectedTreeData,
    initPopup,
  }
})
