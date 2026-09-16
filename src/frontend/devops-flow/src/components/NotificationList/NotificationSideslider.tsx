/**
 * NotificationSideslider Component
 * A sideslider for adding/editing notification configurations
 */
import type { Subscription } from '@/types/flow'
import { get } from '@/utils/http'
import { Button, Checkbox, Form, Input, Popover, Sideslider, Switcher, TagInput } from 'bkui-vue'
import { computed, defineComponent, onMounted, ref, watch, type PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import styles from './NotificationSideslider.module.css'

interface ProjectGroup {
  groupId: string
  groupName: string
  users: string[]
}

// Notification type options
const { Group: CheckboxGroup } = Checkbox

const NOTIFICATION_TYPES = [
  { id: 'EMAIL', name: 'flow.content.emailNotice' },
  { id: 'WEWORK', name: 'flow.content.weworkNotice' },
  // { id: 'RTX', name: 'flow.content.rtxNotice' },
  { id: 'WEWORK_GROUP', name: 'flow.content.weworkGroup' },
  { id: 'VOICE', name: 'flow.content.voiceNotice' },
  // { id: 'WECHAT', name: 'flow.content.wechatNotice' },
  // { id: 'SMS', name: 'flow.content.smsNotice' },
]

// Default subscription object
const createDefaultSubscription = (defaultContent = '', defaultUsers = ''): Subscription => ({
  types: [],
  groups: [],
  users: defaultUsers,
  wechatGroupFlag: false,
  wechatGroup: '',
  wechatGroupMarkdownFlag: false,
  content: defaultContent,
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
    defaultContent: {
      type: String,
      default: '',
    },
    defaultUsers: {
      type: String,
      default: '',
    },
  },
  emits: ['update:visible', 'save'],
  setup(props, { emit }) {
    const { t } = useI18n()
    const route = useRoute()
    const { FormItem } = Form
    const formRef = ref<InstanceType<typeof Form> | null>(null)

    // Project notification groups (same data source as pipeline notify setting)
    const projectGroupAndUsers = ref<ProjectGroup[]>([])
    const loadProjectGroupAndUsers = async () => {
      const projectId = route.params.projectId as string
      if (!projectId) return
      try {
        const res = await get<any[]>(
          `/quality/api/user/groups/${projectId}/projectGroupAndUsers`,
        )
        // Filter out invalid/duplicate groups (e.g. IAM custom groups with empty name)
        const groupMap = new Map<string, ProjectGroup>()
        ;(res || []).forEach((item: any) => {
          const groupId = item.groupId ?? ''
          const groupName = item.groupName ?? ''
          if (!groupId || !groupName || groupMap.has(groupId)) return
          groupMap.set(groupId, { groupId, groupName, users: item.users ?? [] })
        })
        projectGroupAndUsers.value = [...groupMap.values()]
      } catch {
        projectGroupAndUsers.value = []
      }
    }
    onMounted(loadProjectGroupAndUsers)

    // Group options, keeping legacy manual-entered groups as extra options when editing
    const groupOptions = computed<ProjectGroup[]>(() => {
      const options = [...projectGroupAndUsers.value]
      const ids = new Set(options.map((option) => option.groupId))
      const extra = localNotification.value.groups
        .filter((group) => !ids.has(group))
        .map((group) => ({ groupId: group, groupName: group, users: [] as string[] }))
      return [...options, ...extra]
    })

    // Candidate users for fuzzy search, collected from project notification group members
    const userOptions = computed<Array<{ id: string; name: string }>>(() => {
      const userMap = new Map<string, string>()
      projectGroupAndUsers.value.forEach((group) => {
        ;(group.users || []).forEach((user) => {
          if (user && !userMap.has(user)) userMap.set(user, user)
        })
      })
      return [...userMap].map(([id, name]) => ({ id, name }))
    })

    // Users as array for TagInput
    const usersArray = computed(() =>
      (localNotification.value.users || '').split(',').map((u) => u.trim()).filter(Boolean),
    )

    // Local notification state
    const localNotification = ref<Subscription>(createDefaultSubscription())

    // Reset local notification when props change
    watch(
      () => props.notification,
      (newVal) => {
        if (newVal) {
          localNotification.value = JSON.parse(JSON.stringify(newVal))
        } else {
          localNotification.value = createDefaultSubscription(props.defaultContent, props.defaultUsers)
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
            localNotification.value = createDefaultSubscription(props.defaultContent, props.defaultUsers)
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

    return () => (
      <Sideslider isShow={props.visible} width={640} onClosed={handleClose}>
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
                  <CheckboxGroup
                    class={styles.groupCheckboxGroup}
                    modelValue={localNotification.value.groups}
                    onUpdate:modelValue={(val: string[]) => {
                      localNotification.value.groups = val
                    }}
                  >
                    {groupOptions.value.map((group) => (
                      <Checkbox
                        key={group.groupId}
                        label={group.groupId}
                        class={styles.groupCheckbox}
                      >
                        {group.groupName}
                        <Popover placement="top">
                          {{
                            default: () => (
                              <span class={styles.groupUserCount}>
                                ({group.users.length})
                              </span>
                            ),
                            content: () => (
                              <div class={styles.groupUserPopover}>
                                {group.users.length
                                  ? group.users.join(';')
                                  : t('flow.content.emptyNoticeGroup')}
                              </div>
                            ),
                          }}
                        </Popover>
                      </Checkbox>
                    ))}
                  </CheckboxGroup>
                </FormItem>

                {/* Notification Users */}
                <FormItem label={t('flow.content.noticeUser')}>
                  <TagInput
                    class={styles.userTagInput}
                    modelValue={usersArray.value}
                    list={userOptions.value}
                    searchKey={['id', 'name']}
                    allowCreate
                    allowAutoMatch
                    copyable={false}
                    separator=","
                    placeholder={t('flow.content.noticeUserPlaceholder')}
                    onChange={(val: string[]) => {
                      localNotification.value.users = val.join(',')
                    }}
                  />
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
