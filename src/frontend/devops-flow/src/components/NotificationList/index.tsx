/**
 * NotificationList Component
 * A shared component for displaying notification configurations
 * Supports both editable and readonly modes through the `editable` prop
 */
import { SvgIcon } from '@/components/SvgIcon'
import type { Subscription } from '@/types/flow'
import { Button, Card, Collapse, Exception } from 'bkui-vue'
import { computed, defineComponent, ref, type PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import styles from './NotificationList.module.css'

// Notification type mapping
const NOTIFY_TYPE_MAP: Record<string, string> = {
  EMAIL: 'flow.content.emailNotice',
  WEWORK: 'flow.content.weworkNotice',
  RTX: 'flow.content.rtxNotice',
  WEWORK_GROUP: 'flow.content.weworkGroup',
  VOICE: 'flow.content.voiceNotice',
  WECHAT: 'flow.content.wechatNotice',
  SMS: 'flow.content.smsNotice',
}

export interface NotifyItem {
  type: string
  name: string
  notifications: Subscription[]
}

export default defineComponent({
  name: 'NotificationList',
  props: {
    /**
     * List of notification items to display
     * Each item contains type, name, and notifications array
     */
    notifyList: {
      type: Array as PropType<NotifyItem[]>,
      required: true,
    },
    /**
     * Whether the list is editable
     * When true, shows add/edit/delete buttons
     */
    editable: {
      type: Boolean,
      default: false,
    },
    /**
     * Default active collapse panels (by index)
     */
    defaultActiveIndex: {
      type: Array as PropType<number[]>,
      default: () => [],
    },
    /**
     * Whether to use block theme for collapse
     */
    useBlockTheme: {
      type: Boolean,
      default: true,
    },
  },
  emits: ['add', 'edit', 'delete'],
  setup(props, { emit }) {
    const { t } = useI18n()
    const activeIndex = ref<number[]>(props.defaultActiveIndex)

    // Collapse list data for rendering
    const collapseList = computed(() =>
      props.notifyList.map((item) => ({
        ...item,
        displayName: `${item.name} (${item.notifications.length})`,
      }))
    )

    // Format notification types for display
    const formatNotificationTypes = (types: string[]): string => {
      if (!types || types.length === 0) return '--'
      return types.map((type) => t(NOTIFY_TYPE_MAP[type]!) || type).join(', ')
    }

    // Format groups for display
    const formatGroups = (groups: string[]): string => {
      if (!groups || groups.length === 0) return '--'
      return groups.join(', ')
    }

    // Format users for display
    const formatUsers = (users: string): string => {
      if (!users || users.trim() === '') return '--'
      return users
    }

    // Format content for display
    const formatContent = (content: string): string => {
      if (!content || content.trim() === '') return '--'
      return content
    }

    // Handle add notification
    const handleAdd = (type: string, e: MouseEvent) => {
      e.stopPropagation()
      emit('add', type)
    }

    // Handle edit notification
    const handleEdit = (type: string, index: number) => {
      emit('edit', type, index)
    }

    // Handle delete notification
    const handleDelete = (type: string, index: number) => {
      emit('delete', type, index)
    }

    // Render single notification card
    const renderNotificationCard = (
      notification: Subscription,
      notifyType: string,
      index: number
    ) => (
      <Card showHeader={false} class={styles.notificationCard}>
        {props.editable && (
          <div class={styles.notificationActions}>
            <Button
              text
              theme="primary"
              onClick={() => handleEdit(notifyType, index)}
              class={styles.editBtn}
              aria-label={t('flow.common.edit')}
              tabindex={0}
            >
              <SvgIcon name="edit" size={14} />
            </Button>
            <Button
              text
              theme="primary"
              onClick={() => handleDelete(notifyType, index)}
              class={styles.deleteBtn}
              aria-label={t('flow.common.delete')}
              tabindex={0}
            >
              <SvgIcon name="trash-bin" size={14} />
            </Button>
          </div>
        )}
        <div class={styles.notificationInfo}>
          <div class={styles.infoRow}>
            <span class={styles.infoLabel}>{t('flow.content.noticeType')}</span>
            <span class={styles.infoValue}>
              {formatNotificationTypes(notification.types)}
            </span>
          </div>
          <div class={styles.infoRow}>
            <span class={styles.infoLabel}>{t('flow.content.noticeGroup')}</span>
            <span class={styles.infoValue}>{formatGroups(notification.groups)}</span>
          </div>
          <div class={styles.infoRow}>
            <span class={styles.infoLabel}>{t('flow.content.noticeUser')}</span>
            <span class={styles.infoValue}>{formatUsers(notification.users)}</span>
          </div>
          <div class={styles.infoRow}>
            <span class={styles.infoLabel}>{t('flow.content.noticeContent')}</span>
            <span class={styles.infoValue}>{formatContent(notification.content)}</span>
          </div>
        </div>
      </Card>
    )

    return () => (
      <div class={styles.notificationList}>
        <Collapse
          v-model={activeIndex.value}
          list={collapseList.value}
          useBlockTheme={props.useBlockTheme}
          headerIcon="right-shape"
          class={[styles.collapse, styles.collapseContainer]}
        >
          {{
            title: (notify: NotifyItem & { displayName: string }) =>
              props.editable ? (
                <div class={styles.cardHeader}>
                  <span class={styles.cardTitle}>{notify.displayName}</span>
                  <Button
                    text
                    theme="primary"
                    onClick={(e: MouseEvent) => handleAdd(notify.type, e)}
                    class={styles.addBtn}
                    aria-label={t('flow.content.addNotification')}
                    tabindex={0}
                  >
                    <SvgIcon name="add-small" size={16} class={styles.addIcon} />
                    {t('flow.content.addNotification')}
                  </Button>
                </div>
              ) : (
                <span class={styles.cardTitle}>{notify.displayName}</span>
              ),
            content: (notify: NotifyItem) => (
              <div class={styles.cardContent}>
                {notify.notifications.length > 0 ? (
                  <div class={styles.notificationItems}>
                    {notify.notifications.map((notification, index) =>
                      renderNotificationCard(notification, notify.type, index)
                    )}
                  </div>
                ) : (
                  <Exception scene="part" type="empty">
                    {t('flow.content.noNotifications')}
                  </Exception>
                )}
              </div>
            ),
          }}
        </Collapse>
      </div>
    )
  },
})
