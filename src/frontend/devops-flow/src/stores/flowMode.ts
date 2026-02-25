import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { UI_MODE, CODE_MODE } from '@/utils/flowConst'

export const useModeStore = defineStore('mode', () => {
  const currentMode = ref<string>(localStorage.getItem('flowModelType') || UI_MODE)
  const isCodeMode = computed(() => currentMode.value === CODE_MODE)
  const isUIMode = computed(() => currentMode.value === UI_MODE)

  function setMode(mode: string) {
    currentMode.value = mode
    localStorage.setItem('flowModelType', mode)
  }

  function toggleMode() {
    const newMode = currentMode.value === CODE_MODE ? UI_MODE : CODE_MODE
    setMode(newMode)
  }

  function reset() {
    setMode(UI_MODE)
  }

  return {
    currentMode,
    isCodeMode,
    isUIMode,
    setMode,
    toggleMode,
    reset,
  }
})
