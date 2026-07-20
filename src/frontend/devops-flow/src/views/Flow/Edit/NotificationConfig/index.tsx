/**
 * EditNotificationConfig Component
 * Editable notification configuration page using shared NotificationList component
 */
import NotificationList, { type NotifyItem } from '@/components/NotificationList'
import NotificationSideslider from '@/components/NotificationList/NotificationSideslider'
import { useFlowModel } from '@/hooks/useFlowModel'
import { Loading, Message } from 'bkui-vue'
import { computed, defineComponent, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import type { Subscription } from '@/types/flow'
import sharedStyles from '../shared.module.css'

export default defineComponent({
  name: 'EditNotificationConfig',
  setup() {
    const { t } = useI18n()
    const route = useRoute()
    const flowId = route.params.flowId as string
    const projectId = route.params.projectId as string

    // Use flowModel to get and update settings
    const { flowSetting, updateFlowSetting, loading } = useFlowModel()

    // Notification data (from flowSetting)
    const successSubscriptionList = ref<Subscription[]>([])
    const failSubscriptionList = ref<Subscription[]>([])


    // Sideslider state
    const sidesliderVisible = ref(false)
    const currentNotifyType = ref('')
    const currentNotifyTypeName = ref('')
    const currentEditIndex = ref(-1)
    const currentNotification = ref<Subscription | null>(null)
    const isEditMode = ref(false)

    // Initialize data from flowSetting
    watch(
      flowSetting,
      (setting) => {
        if (setting) {
          successSubscriptionList.value = setting.successSubscriptionList || []
          failSubscriptionList.value = setting.failSubscriptionList || []
        }
      },
      { immediate: true },
    )

    // Update flowSetting when notification data changes
    const updateNotificationSetting = () => {
      if (!flowSetting.value) return

      updateFlowSetting({
        ...flowSetting.value,
        successSubscriptionList: successSubscriptionList.value,
        failSubscriptionList: failSubscriptionList.value,
      })
    }

    // Notification list configuration
    const notifyList = computed<NotifyItem[]>(() => [
      {
        type: 'successSubscriptionList',
        name: t('flow.content.runSuccess'),
        notifications: successSubscriptionList.value,
      },
      {
        type: 'failSubscriptionList',
        name: t('flow.content.runFailed'),
        notifications: failSubscriptionList.value,
      },
    ])

    // Auto-expand panels that have notifications
    const defaultActiveIndex = computed(() =>
      notifyList.value
        .map((item, index) => (item.notifications.length > 0 ? index : -1))
        .filter((i) => i >= 0),
    )

    // Get notification list by type
    const getNotificationList = (type: string): Subscription[] => {
      switch (type) {
        case 'successSubscriptionList':
          return successSubscriptionList.value
        case 'failSubscriptionList':
          return failSubscriptionList.value
        default:
          return []
      }
    }

    // Get notify type name by type
    const getNotifyTypeName = (type: string): string => {
      const item = notifyList.value.find((n) => n.type === type)
      return item?.name || ''
    }

    // Handle add notification
    const handleAddNotification = (type: string) => {
      currentNotifyType.value = type
      currentNotifyTypeName.value = getNotifyTypeName(type)
      currentEditIndex.value = -1
      currentNotification.value = null
      isEditMode.value = false
      sidesliderVisible.value = true
    }

    // Handle edit notification
    const handleEditNotification = (type: string, index: number) => {
      const list = getNotificationList(type)
      currentNotifyType.value = type
      currentNotifyTypeName.value = getNotifyTypeName(type)
      currentEditIndex.value = index
      currentNotification.value = list[index] || null
      isEditMode.value = true
      sidesliderVisible.value = true
    }

    // Handle save notification (from sideslider)
    const handleSaveNotification = (notification: Subscription) => {
      const list = getNotificationList(currentNotifyType.value)

      if (isEditMode.value && currentEditIndex.value >= 0) {
        // Edit existing notification
        list[currentEditIndex.value] = notification
        Message({
          theme: 'success',
          message: t('flow.content.updateSuccess'),
        })
      } else {
        // Add new notification
        list.push(notification)
        Message({
          theme: 'success',
          message: t('flow.content.addSuccess'),
        })
      }

      // Update flowSetting
      updateNotificationSetting()
    }

    // Handle delete notification
    const handleDeleteNotification = (type: string, index: number) => {
      const list = getNotificationList(type)
      list.splice(index, 1)

      // Update flowSetting after deletion
      updateNotificationSetting()

      Message({
        theme: 'success',
        message: t('flow.content.deleteSuccess'),
      })
    }

    return () => (
      <Loading loading={loading.value} class={[sharedStyles.tabContainer, sharedStyles.tabPadding]}>
        <NotificationList
          notifyList={notifyList.value}
          editable={true}
          defaultActiveIndex={defaultActiveIndex.value}
          onAdd={handleAddNotification}
          onEdit={handleEditNotification}
          onDelete={handleDeleteNotification}
        />
        {/* Notification Sideslider */}
        <NotificationSideslider
          visible={sidesliderVisible.value}
          notification={currentNotification.value}
          notifyTypeName={currentNotifyTypeName.value}
          isEdit={isEditMode.value}
          onUpdate:visible={(val: boolean) => {
            sidesliderVisible.value = val
          }}
          onSave={handleSaveNotification}
        />
      </Loading>
    )
  },
})
