import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  getProjectGroups,
  updateProjectGroups,
  addProjectGroups,
  deleteProjectGroups,
  updateProjectTags,
  addProjectTags,
  deleteProjectTags,
  type GroupResponse,
} from '@/api/flowLabelGroup'

/**
 * 标签分组 Store - 包含状态和API请求方法
 */
export const useFlowLabelGroupStore = defineStore('flowLabelGroup', () => {
  // ==================== State ====================
  const loading = ref(false)
  const showContent = ref(false)
  const tagList = ref<GroupResponse[]>([])

  // 分组编辑状态
  const isShowInputIndex = ref(-1)
  const labelValue = ref('')

  // 标签编辑状态
  const activeEditKey = ref<string | null>(null)
  const tagValue = ref('')
  const tagOriginalValue = ref('')
  const tagOriginalGroupIndex = ref<number | null>(null)
  const tagOriginalTagIndex = ref<number | null>(null)

  // 新增标签状态
  const addTagGroupIndex = ref<number | null>(null)
  const addTagIndex = ref<number | null>(null)
  const isAddTagEnter = ref(false)

  // 按钮禁用状态
  const btnDisabled = ref(false)

  // 弹窗设置
  const groupDialog = ref({
    isShow: false,
    title: '',
    value: '',
    error: '', // 错误提示信息
  })

  // ==================== Methods: API Requests ====================
  async function requestTagList(projectId: string) {
    try {
      return await getProjectGroups(projectId)
    } catch (error) {
      console.error('获取标签组列表失败:', error)
      throw error
    }
  }

  async function addGroupAPI(projectId: string, name: string) {
    try {
      await addProjectGroups(projectId, {
        id: '',
        projectId,
        name,
      })
      // 添加成功后重新获取列表以获取新分组的ID
      const updatedList = await requestTagList(projectId)
      const newGroup = updatedList.find((g) => g.name === name)
      if (newGroup) {
        return newGroup
      }
    } catch (error) {
      console.error('新增标签组失败:', error)
      throw error
    }
  }

  async function modifyGroupAPI(projectId: string, id: string, name: string) {
    try {
      return await updateProjectGroups(projectId, {
        id,
        projectId,
        name,
      })
    } catch (error) {
      console.error('修改标签组失败:', error)
      throw error
    }
  }

  async function deleteGroupAPI(projectId: string, groupId: string) {
    try {
      return await deleteProjectGroups(projectId, groupId)
    } catch (error) {
      console.error('删除标签组失败:', error)
      throw error
    }
  }

  async function addTagAPI(projectId: string, groupId: string, name: string) {
    try {
      await addProjectTags(projectId, {
        groupId,
        name,
      })
      const updatedList = await requestTagList(projectId)
      const group = updatedList.find((g) => g.id === groupId)
      const newTag = group?.labels.find((l) => l.name === name)
      if (newTag) {
        return newTag
      }
    } catch (error) {
      console.error('新增标签失败:', error)
      throw error
    }
  }

  async function modifyTagAPI(projectId: string, id: string, groupId: string, name: string) {
    try {
      return await updateProjectTags(projectId, {
        id,
        groupId,
        name,
      })
    } catch (error) {
      console.error('修改标签失败:', error)
      throw error
    }
  }

  async function deleteTagAPI(projectId: string, labelId: string) {
    try {
      return await deleteProjectTags(projectId, labelId)
    } catch (error) {
      console.error('删除标签失败:', error)
      throw error
    }
  }

  return {
    // State
    loading,
    showContent,
    tagList,
    isShowInputIndex,
    labelValue,
    activeEditKey,
    tagValue,
    tagOriginalValue,
    tagOriginalGroupIndex,
    tagOriginalTagIndex,
    addTagGroupIndex,
    addTagIndex,
    isAddTagEnter,
    btnDisabled,
    groupDialog,
    // API Methods
    requestTagList,
    addGroupAPI,
    modifyGroupAPI,
    deleteGroupAPI,
    addTagAPI,
    modifyTagAPI,
    deleteTagAPI,
  }
})
