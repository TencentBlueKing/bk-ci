import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { Message } from 'bkui-vue'
import { useAuthStore } from '@/stores/auth'
import {
  getResourceAuthorization,
  resetResourceAuthorization,
  type ResetResourceAuthParams,
} from '@/api/permissionDelegation'
import { RESOURCE_TYPES } from '@/components/Permission/constants'

/**
 * 权限代持状态管理
 */
export const usePermissionDelegationStore = defineStore('permissionDelegation', () => {
  const route = useRoute()
  const authStore = useAuthStore()
  const { t } = useI18n()

  // 状态定义
  const resourceAuthData = ref({
    id: 0,
    projectCode: '',
    resourceType: '',
    resourceName: '',
    resourceCode: '',
    handoverTime: 0,
    handoverFrom: '',
    handoverFromCnName: '',
    executePermission: false,
  })
  const loading = ref(false)
  const resetLoading = ref(false)
  const flowId = computed(() => route.params.flowId as string)
  const projectId = computed(() => route.params.projectId as string)

  const showResetDialog = ref(false)
  const showFailedDialog = ref(false)
  const failedArr = ref<string[]>([])

  /**
   * 获取资源授权信息
   */
  async function fetchResourceAuth() {
    try {
      loading.value = true
      const data = await getResourceAuthorization({
        projectId: projectId.value,
        flowId: flowId.value,
      })
      resourceAuthData.value = data
    } catch (error) {
      console.error(error)
    } finally {
      loading.value = false
    }
  }

  /**
   * 重置资源授权
   * @param handoverTo 交接给谁（当前用户）
   */
  async function handleReset() {
    const params: ResetResourceAuthParams = {
      projectCode: projectId.value,
      resourceType: RESOURCE_TYPES.CREATIVE_STREAM,
      handoverChannel: 'OTHER',
      resourceAuthorizationHandoverList: [
        {
          projectCode: projectId.value,
          resourceType: RESOURCE_TYPES.CREATIVE_STREAM,
          resourceName: resourceAuthData.value.resourceName,
          resourceCode: resourceAuthData.value.resourceCode,
          handoverFrom: resourceAuthData.value.handoverFrom,
          handoverTo: authStore.username,
        },
      ],
    }

    try {
      resetLoading.value = true
      const res = await resetResourceAuthorization(projectId.value, params)

      showResetDialog.value = false
      if (res?.FAILED?.length) {
        const message = res.FAILED[0]?.handoverFailedMessage || ''
        if (message.includes('<br/>')) {
          failedArr.value = message.split('<br/>')
          showFailedDialog.value = true
        } else {
          Message({
            theme: 'error',
            message,
          })
        }
      } else {
        fetchResourceAuth()
        Message({
          theme: 'success',
          message: t('flow.delegation.resetSuc'),
        })
      }
    } catch (error) {
      console.error('重置资源授权失败:', error)
      throw error
    } finally {
      resetLoading.value = false
    }
  }

  function handleShowResetDialog() {
    showResetDialog.value = true
  }

  function handleToggleShowResetDialog(val: boolean) {
    if (!val) {
      showResetDialog.value = false
    }
  }
  return {
    // State
    resourceAuthData,
    loading,
    resetLoading,
    showResetDialog,
    showFailedDialog,
    failedArr,
    // Actions
    fetchResourceAuth,
    handleReset,
    handleShowResetDialog,
    handleToggleShowResetDialog,
  }
})
