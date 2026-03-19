import { computed, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { storeToRefs } from 'pinia'
import { Message, InfoBox } from 'bkui-vue'
import { useFlowLabelGroupStore } from '@/stores/flowLabelGroup'
import type { GroupLabelItem } from '@/api/flowLabelGroup'

/**
 * 标签分组管理 Hook - 包含computed和业务逻辑方法
 */
export function useFlowLabelGroup() {
  const { t } = useI18n()
  const route = useRoute()
  const store = useFlowLabelGroupStore()

  // 使用 storeToRefs 确保响应式
  const {
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
  } = storeToRefs(store)

  // ==================== Computed ====================
  const projectId = computed(() => route.params.projectId as string)

  // 是否显示"新增标签组"按钮
  const isShowGroupBtn = computed(() => {
    if (tagList.value.length > 0 && tagList.value.length < 10) {
      return tagList.value.every((item) => item.labels.length > 0)
    }
    return false
  })

  // ==================== Methods: Utilities ====================
  // 生成编辑key
  function getEditKey(groupIndex: number, tagIndex: number) {
    return `g${groupIndex}-t${tagIndex}`
  }

  // 判断是否聚焦
  function isFocus(groupIndex: number, tagIndex: number) {
    return activeEditKey.value === getEditKey(groupIndex, tagIndex)
  }

  // ==================== Methods: Group Operations ====================
  function showGroupInput(index: number, name: string) {
    resetTag()
    activeEditKey.value = null
    labelValue.value = name
    isShowInputIndex.value = index
    btnDisabled.value = true
  }

  function handleGroupCancel(groupIndex: number, val: string) {
    btnDisabled.value = false
    labelValue.value = val
    isShowInputIndex.value = -1
  }

  async function handleGroupSave(groupIndex: number) {
    if (!labelValue.value) return

    const group = tagList.value[groupIndex]
    if (!group) return

    try {
      loading.value = true
      await store.modifyGroupAPI(projectId.value, group.id, labelValue.value)

      group.name = labelValue.value
      Message({
        message: t('flow.labelGroup.modifySuccess'),
        theme: 'success',
      })
    } catch (error: any) {
      Message({
        message: error.message || error,
        theme: 'error',
      })
    } finally {
      loading.value = false
      btnDisabled.value = false
      isShowInputIndex.value = -1
    }
  }

  function showGroupDialog() {
    groupDialog.value = {
      isShow: true,
      title: t('flow.labelGroup.addGroup'),
      value: '',
      error: '',
    }
    nextTick(() => {
      // 聚焦输入框
    })
  }

  async function handleGroupDialogConfirm() {
    // 校验标签组名称
    if (!groupDialog.value.value || !groupDialog.value.value.trim()) {
      groupDialog.value.error = t('flow.labelGroup.groupNameRequired')
      groupDialog.value.isShow = true
      return false
    }

    groupDialog.value.error = ''

    try {
      loading.value = true
      const newGroup = await store.addGroupAPI(projectId.value, groupDialog.value.value)
      tagList.value.push(newGroup!)

      Message({
        message: t('flow.labelGroup.addSuccess'),
        theme: 'success',
      })
      groupDialog.value.isShow = false
      return true
    } catch (error: any) {
      Message({
        message: error.message || error,
        theme: 'error',
      })
      return false
    } finally {
      loading.value = false
    }
  }

  function deleteGroup(groupIndex: number) {
    resetTag()
    btnDisabled.value = false

    InfoBox({
      title: t('flow.labelGroup.deleteGroupConfirm'),
      onConfirm: async () => {
        try {
          loading.value = true
          const group = tagList.value[groupIndex]
          if (!group) return

          await store.deleteGroupAPI(projectId.value, group.id)

          tagList.value.splice(groupIndex, 1)
          Message({
            message: t('flow.labelGroup.deleteSuccess'),
            theme: 'success',
          })
        } catch (error: any) {
          Message({
            message: error.message || error,
            theme: 'error',
          })
        } finally {
          loading.value = false
        }
      },
    })
  }

  // ==================== Methods: Tag Operations ====================
  function tagEdit(groupIndex: number, tagIndex: number) {
    resetTag()
    handleGroupCancel(groupIndex, '')

    const group = tagList.value[groupIndex]
    if (!group) return

    const tag = group.labels[tagIndex]
    if (!tag) return

    tagOriginalValue.value = tag.name
    tagOriginalGroupIndex.value = groupIndex
    tagOriginalTagIndex.value = tagIndex
    tagValue.value = tag.name

    activeEditKey.value = getEditKey(groupIndex, tagIndex)
    btnDisabled.value = true
    addTagGroupIndex.value = null
    addTagIndex.value = null
  }

  function tagAdd(groupIndex: number) {
    resetTag()
    isShowInputIndex.value = -1
    tagOriginalGroupIndex.value = null
    tagOriginalTagIndex.value = null

    const group = tagList.value[groupIndex]
    if (!group) return

    addTagGroupIndex.value = groupIndex
    addTagIndex.value = group.labels.length
    tagValue.value = ''
    btnDisabled.value = true

    // 添加临时标签
    const tempTag: GroupLabelItem = {
      id: '',
      name: '',
      groupId: group.id,
      createTime: Date.now(),
      uptimeTime: Date.now(),
      createUser: '',
      updateUser: '',
    }
    group.labels.push(tempTag)

    activeEditKey.value = getEditKey(groupIndex, group.labels.length - 1)
  }

  function tagSave(groupIndex: number, tagIndex: number) {
    tagModify(groupIndex, tagIndex)
  }

  function tagCancel(groupIndex: number, tagIndex: number) {
    const group = tagList.value[groupIndex]
    if (!group) return

    const tag = group.labels[tagIndex]
    if (!tag) return

    if (tagOriginalValue.value) {
      tag.name = tagOriginalValue.value
    }

    btnDisabled.value = false
    activeEditKey.value = null

    // 如果是新增标签且未保存，移除临时标签
    if (!tag.id && !isAddTagEnter.value) {
      group.labels.splice(tagIndex, 1)
    }

    addTagGroupIndex.value = null
    addTagIndex.value = null
    isAddTagEnter.value = false
  }

  async function tagModify(groupIndex: number, tagIndex: number) {
    if (!tagValue.value) return

    const group = tagList.value[groupIndex]
    if (!group) return

    const tag = group.labels[tagIndex]
    if (!tag) return

    try {
      loading.value = true

      if (tag.id) {
        // 修改标签
        await store.modifyTagAPI(projectId.value, tag.id, tag.groupId, tagValue.value)
        tag.name = tagValue.value
        Message({
          message: t('flow.labelGroup.modifySuccess'),
          theme: 'success',
        })
      } else {
        // 新增标签
        const newTag = await store.addTagAPI(projectId.value, group.id, tagValue.value)
        group.labels[tagIndex] = newTag!
        isAddTagEnter.value = true
        Message({
          message: t('flow.labelGroup.addSuccess'),
          theme: 'success',
        })
      }

      tagValue.value = ''
      activeEditKey.value = null
      btnDisabled.value = false
    } catch (error: any) {
      Message({
        message: error.message || error,
        theme: 'error',
      })
      if (tagOriginalGroupIndex.value !== null && tagOriginalTagIndex.value !== null) {
        const originalGroup = tagList.value[tagOriginalGroupIndex.value]
        if (originalGroup) {
          const originalTag = originalGroup.labels[tagOriginalTagIndex.value]
          if (originalTag) {
            originalTag.name = tagOriginalValue.value
          }
        }
      }
    } finally {
      loading.value = false
    }
  }

  function deleteTag(groupIndex: number, tagIndex: number) {
    resetTag()
    activeEditKey.value = null
    addTagGroupIndex.value = null
    addTagIndex.value = null

    InfoBox({
      title: t('flow.labelGroup.deleteTagConfirm'),
      onConfirm: async () => {
        try {
          loading.value = true
          const group = tagList.value[groupIndex]
          if (!group) return

          const tag = group.labels[tagIndex]
          if (!tag?.id) return

          await store.deleteTagAPI(projectId.value, tag.id)

          group.labels.splice(tagIndex, 1)
          Message({
            message: t('flow.labelGroup.deleteSuccess'),
            theme: 'success',
          })
        } catch (error: any) {
          Message({
            message: error.message || error,
            theme: 'error',
          })
        } finally {
          loading.value = false
        }
      },
    })
  }

  function resetTag() {
    if (
      typeof addTagGroupIndex.value === 'number' &&
      addTagGroupIndex.value !== null &&
      addTagIndex.value !== null
    ) {
      const group = tagList.value[addTagGroupIndex.value]
      if (!group) return

      const tag = group.labels[addTagIndex.value]
      if (!tag) return

      btnDisabled.value = false
      activeEditKey.value = null

      // 移除未保存的临时标签
      if (!tag.id) {
        group.labels.splice(addTagIndex.value, 1)
      }
      isAddTagEnter.value = false
    }
    addTagGroupIndex.value = null
    addTagIndex.value = null
  }

  async function init() {
    try {
      loading.value = true
      const data = await store.requestTagList(projectId.value)
      tagList.value = data
      showContent.value = true
    } catch (error: any) {
      Message({
        message: error.message || error,
        theme: 'error',
      })
    } finally {
      loading.value = false
    }
  }

  // 清除弹窗错误提示
  function clearGroupDialogError() {
    if (groupDialog.value.error) {
      groupDialog.value.error = ''
    }
  }

  return {
    // State (from storeToRefs - 响应式)
    loading,
    showContent,
    tagList,
    isShowInputIndex,
    labelValue,
    activeEditKey,
    tagValue,
    btnDisabled,
    groupDialog,
    // Computed
    isShowGroupBtn,
    // Utilities
    getEditKey,
    isFocus,
    // Group Operations
    showGroupInput,
    handleGroupCancel,
    handleGroupSave,
    showGroupDialog,
    handleGroupDialogConfirm,
    deleteGroup,
    clearGroupDialogError,
    // Tag Operations
    tagEdit,
    tagAdd,
    tagSave,
    tagCancel,
    tagModify,
    deleteTag,
    resetTag,
    // Init
    init,
  }
}
