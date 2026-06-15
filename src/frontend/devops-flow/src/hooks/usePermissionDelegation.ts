import { computed } from 'vue'
import { storeToRefs } from 'pinia'
import { usePermissionDelegationStore } from '@/stores/permissionDelegation'

/**
 * 权限代持业务逻辑Hook
 */
export function usePermissionDelegation() {
  const store = usePermissionDelegationStore()
  const { resourceAuthData, loading, resetLoading, showResetDialog, showFailedDialog, failedArr } =
    storeToRefs(store)

  /**
   * 是否显示过期标签
   */
  const showExpiredTag = computed(() => {
    return !resourceAuthData.value.executePermission && !loading.value
  })

  return {
    // State
    resourceAuthData,
    loading,
    resetLoading,
    showResetDialog,
    showFailedDialog,
    failedArr,
    // Computed
    showExpiredTag,
    // Methods
    fetchResourceAuth: store.fetchResourceAuth,
    handleReset: store.handleReset,
    handleShowResetDialog: store.handleShowResetDialog,
    handleToggleShowResetDialog: store.handleToggleShowResetDialog,
  }
}
