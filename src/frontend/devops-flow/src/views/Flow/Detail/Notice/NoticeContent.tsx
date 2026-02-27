/**
 * NoticeContent Component
 * Readonly notification display using shared NotificationList component
 */
import type { FlowSettings } from '@/api/flowModel'
import NotificationList, { type NotifyItem } from '@/components/NotificationList'
import { computed, defineComponent, type PropType } from 'vue'
import { useI18n } from 'vue-i18n'

export default defineComponent({
  name: 'NoticeContent',
  props: {
    flowSetting: {
      type: Object as PropType<FlowSettings>,
      default: null,
    },
  },
  setup(props) {
    const { t } = useI18n()

    // Get notification lists from flowSetting
    const successList = computed(() => props.flowSetting?.successSubscriptionList || [])
    const failList = computed(() => props.flowSetting?.failSubscriptionList || [])
    const cancelList = computed(() => (props.flowSetting as any)?.cancelSubscriptionList || [])
    const publishList = computed(() => (props.flowSetting as any)?.publishSubscriptionList || [])

    // Notification list configuration
    const notifyList = computed<NotifyItem[]>(() => [
      {
        type: 'successSubscriptionList',
        name: t('flow.content.runSuccess'),
        notifications: successList.value,
      },
      {
        type: 'failSubscriptionList',
        name: t('flow.content.runFailed'),
        notifications: failList.value,
      },
      {
        type: 'cancelSubscriptionList',
        name: t('flow.content.runCanceled'),
        notifications: cancelList.value,
      },
      {
        type: 'publishSubscriptionList',
        name: t('flow.content.newVersionPublished'),
        notifications: publishList.value,
      },
    ])

    // Auto-expand panels that have notifications
    const defaultActiveIndex = computed(() =>
      notifyList.value
        .map((item, index) => (item.notifications.length > 0 ? index : -1))
        .filter((i) => i >= 0),
    )

    return () => (
      <NotificationList notifyList={notifyList.value} editable={false} defaultActiveIndex={defaultActiveIndex.value} />
    )
  },
})
