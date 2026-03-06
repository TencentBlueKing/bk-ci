/**
 * NotificationSideslider Component
 * A sideslider for adding/editing notification configurations
 */
import type { Subscription } from '@/types/flow'
import { Button, Checkbox, Form, Input, Sideslider, Switcher } from 'bkui-vue'
import { computed, defineComponent, ref, watch, type PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import styles from './NotificationSideslider.module.css'

// Notification type options
const NOTIFICATION_TYPES = [
  { id: 'EMAIL', name: 'flow.content.emailNotice' },
  { id: 'WEWORK', name: 'flow.content.weworkNotice' },
  // { id: 'RTX', name: 'flow.content.rtxNotice' },
  { id: 'WEWORK_GROUP', name: 'flow.content.weworkGroup' },
  { id: 'VOICE', name: 'flow.content.voiceNotice' },
  // { id: 'WECHAT', name: 'flow.content.wechatNotice' },
  { id: 'SMS', name: 'flow.content.smsNotice' },
]

// Default subscription object
const createDefaultSubscription = (): Subscription => ({
  types: [],
  groups: [],
  users: '',
  wechatGroupFlag: false,
  wechatGroup: '',
  wechatGroupMarkdownFlag: false,
  content: '',
})

export default defineComponent({
  name: 'NotificationSideslider',
  props: {
    visible: {
      type: Boolean,
      default: false,
    },
    notification: {
      type: Object as PropType<Subscription | null>,
      default: null,
    },
    notifyTypeName: {
      type: String,
      default: '',
    },
    isEdit: {
      type: Boolean,
      default: false,
    },
  },
  emits: ['update:visible', 'save'],
  setup(props, { emit }) {
    const { t } = useI18n()
    const { FormItem } = Form
    const formRef = ref<InstanceType<typeof Form> | null>(null)

    // Local notification state
    const localNotification = ref<Subscription>(createDefaultSubscription())

    // Reset local notification when props change
    watch(
      () => props.notification,
      (newVal) => {
        if (newVal) {
          localNotification.value = JSON.parse(JSON.stringify(newVal))
        } else {
          localNotification.value = createDefaultSubscription()
        }
      },
      { immediate: true },
    )

    // Reset when sideslider opens
    watch(
      () => props.visible,
      (visible) => {
        if (visible) {
          if (props.notification) {
            localNotification.value = JSON.parse(JSON.stringify(props.notification))
          } else {
            localNotification.value = createDefaultSubscription()
          }
        }
      },
    )

    // Title for sideslider
    const title = computed(() => {
      if (props.isEdit) {
        return `${t('flow.content.editNotification')} - ${props.notifyTypeName}`
      }
      return `${t('flow.content.addNotification')} - ${props.notifyTypeName}`
    })

    // Handle type checkbox change
    const handleTypeChange = (typeId: string, checked: boolean) => {
      if (checked) {
        if (!localNotification.value.types.includes(typeId)) {
          localNotification.value.types.push(typeId)
        }
      } else {
        const index = localNotification.value.types.indexOf(typeId)
        if (index > -1) {
          localNotification.value.types.splice(index, 1)
        }
      }
    }

    // Check if type is selected
    const isTypeSelected = (typeId: string): boolean => {
      return localNotification.value.types.includes(typeId)
    }

    // Show wechat group input when WEWORK_GROUP is selected
    const showWechatGroup = computed(() => {
      return localNotification.value.types.includes('WEWORK_GROUP')
    })

    // Handle close
    const handleClose = () => {
      emit('update:visible', false)
    }

    // Handle save
    const handleSave = async () => {
      // Validate form
      if (localNotification.value.types.length === 0) {
        return
      }

      emit('save', JSON.parse(JSON.stringify(localNotification.value)))
      handleClose()
    }

    // Handle users input change
    const handleUsersChange = (value: string) => {
      localNotification.value.users = value
    }

    // Handle groups input change (comma separated)
    const handleGroupsChange = (value: string) => {
      localNotification.value.groups = value
        .split(',')
        .map((g) => g.trim())
        .filter((g) => g)
    }

    // Handle content change
    const handleContentChange = (value: string) => {
      localNotification.value.content = value
    }

    // Handle wechat group change
    const handleWechatGroupChange = (value: string) => {
      localNotification.value.wechatGroup = value
    }

    // Handle wechat group flag change
    const handleWechatGroupFlagChange = (value: boolean) => {
      localNotification.value.wechatGroupFlag = value
    }

    // Handle wechat group markdown flag change
    const handleWechatGroupMarkdownFlagChange = (value: boolean) => {
      localNotification.value.wechatGroupMarkdownFlag = value
    }

    // Get groups as string for display
    const groupsAsString = computed(() => {
      return localNotification.value.groups.join(', ')
    })

    return () => (
      <Sideslider isShow={props.visible} width={560} onClosed={handleClose}>
        {{
          header: () => (
            <div class={styles.header}>
              <span class={styles.title}>{title.value}</span>
            </div>
          ),
          default: () => (
            <div class={styles.content}>
              <Form ref={formRef} form-type="vertical" model={localNotification.value}>
                {/* Notification Types */}
                <FormItem
                  label={t('flow.content.noticeType')}
                  required
                  property="types"
                  rules={[
                    {
                      validator: () => localNotification.value.types.length > 0,
                      message: t('flow.content.selectNoticeType'),
                      trigger: 'change',
                    },
                  ]}
                >
                  <div class={styles.typeCheckboxGroup}>
                    {NOTIFICATION_TYPES.map((type) => (
                      <Checkbox
                        key={type.id}
                        modelValue={isTypeSelected(type.id)}
                        onChange={(checked: boolean) => handleTypeChange(type.id, checked)}
                        class={styles.typeCheckbox}
                        tabindex={0}
                      >
                        {t(type.name)}
                      </Checkbox>
                    ))}
                  </div>
                </FormItem>

                {/* Wechat Group Settings - Only show when WEWORK_GROUP is selected */}
                {showWechatGroup.value && (
                  <>
                    <FormItem label={t('flow.content.wechatGroupId')}>
                      <Input
                        modelValue={localNotification.value.wechatGroup}
                        placeholder={t('flow.content.wechatGroupIdPlaceholder')}
                        onUpdate:modelValue={handleWechatGroupChange}
                      />
                    </FormItem>
                    <FormItem>
                      <div class={styles.switchRow}>
                        <Switcher
                          size="small"
                          theme="primary"
                          modelValue={localNotification.value.wechatGroupFlag}
                          onChange={handleWechatGroupFlagChange}
                        />
                        <span class={styles.switchLabel}>{t('flow.content.wechatGroupAtAll')}</span>
                      </div>
                    </FormItem>
                    <FormItem>
                      <div class={styles.switchRow}>
                        <Switcher
                          size="small"
                          theme="primary"
                          modelValue={localNotification.value.wechatGroupMarkdownFlag}
                          onChange={handleWechatGroupMarkdownFlagChange}
                        />
                        <span class={styles.switchLabel}>
                          {t('flow.content.wechatGroupMarkdown')}
                        </span>
                      </div>
                    </FormItem>
                  </>
                )}

                {/* Notification Groups */}
                <FormItem label={t('flow.content.noticeGroup')}>
                  <Input
                    modelValue={groupsAsString.value}
                    placeholder={t('flow.content.noticeGroupPlaceholder')}
                    onUpdate:modelValue={handleGroupsChange}
                  />
                  <div class={styles.fieldTip}>{t('flow.content.noticeGroupTip')}</div>
                </FormItem>

                {/* Notification Users */}
                <FormItem label={t('flow.content.noticeUser')}>
                  <Input
                    modelValue={localNotification.value.users}
                    placeholder={t('flow.content.noticeUserPlaceholder')}
                    onUpdate:modelValue={handleUsersChange}
                  />
                  <div class={styles.fieldTip}>{t('flow.content.noticeUserTip')}</div>
                </FormItem>

                {/* Notification Content */}
                <FormItem label={t('flow.content.noticeContent')}>
                  <Input
                    type="textarea"
                    rows={4}
                    modelValue={localNotification.value.content}
                    placeholder={t('flow.content.noticeContentPlaceholder')}
                    onUpdate:modelValue={handleContentChange}
                  />
                  <div class={styles.fieldTip}>{t('flow.content.noticeContentTip')}</div>
                </FormItem>
              </Form>
            </div>
          ),
          footer: () => (
            <div class={styles.footer}>
              <Button
                theme="primary"
                onClick={handleSave}
                disabled={localNotification.value.types.length === 0}
              >
                {t('flow.content.save')}
              </Button>
              <Button onClick={handleClose}>{t('flow.common.cancel')}</Button>
            </div>
          ),
        }}
      </Sideslider>
    )
  },
})
