import { ref, computed } from 'vue'

export interface EditingPos {
  stageIndex: number
  containerIndex?: number
  elementIndex?: number
}

export function useEditingPos() {
  const editingPos = ref<EditingPos>({
    stageIndex: -1,
    containerIndex: undefined,
    elementIndex: undefined,
  })

  const setEditingPos = (pos: EditingPos) => {
    editingPos.value = pos
  }

  const clearEditingPos = () => {
    editingPos.value = {
      stageIndex: -1,
      containerIndex: undefined,
      elementIndex: undefined,
    }
  }

  // 是否正在编辑（任何内容）
  const isEditing = computed(() => editingPos.value.stageIndex !== -1)

  // 是否正在编辑 Stage (只有 stageIndex)
  const isEditingStage = computed(() => {
    const { stageIndex, containerIndex, elementIndex } = editingPos.value
    return stageIndex !== -1 && containerIndex === undefined && elementIndex === undefined
  })

  // 是否正在编辑 Job (有 stageIndex 和 containerIndex，但没有 elementIndex)
  const isEditingJob = computed(() => {
    const { stageIndex, containerIndex, elementIndex } = editingPos.value
    return stageIndex !== -1 && containerIndex !== undefined && elementIndex === undefined
  })

  // 是否正在编辑 Plugin (三个 index 都有)
  const isEditingPlugin = computed(() => {
    const { stageIndex, containerIndex, elementIndex } = editingPos.value
    return stageIndex !== -1 && containerIndex !== undefined && elementIndex !== undefined
  })

  const realEditingPos = computed(() => {
    const { stageIndex, containerIndex, elementIndex } = editingPos.value
    return {
      stageIndex: stageIndex + 1,
      containerIndex: containerIndex,
      elementIndex: elementIndex,
    }
  })

  return {
    editingPos,
    realEditingPos,
    setEditingPos,
    clearEditingPos,
    isEditing,
    isEditingStage,
    isEditingJob,
    isEditingPlugin,
  }
}
