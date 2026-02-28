import StaffInput from '@/components/AtomForm/StaffInput'
import { SvgIcon } from '@/components/SvgIcon'
import { useUIStore } from '@/stores/ui'
import type { CheckConfig, ReviewGroup, ReviewParam, Stage } from '@/types/flow'
import { ParamType, VARIABLE_TYPE_LIST } from '@/types/variable'
import { get } from '@/utils/http'
import { Button, Checkbox, Dialog, Divider, Form, Input, Popover, Radio, Select, Sideslider, Table, TagInput } from 'bkui-vue'
import { storeToRefs } from 'pinia'
import { computed, defineComponent, onMounted, type PropType, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import styles from './StageReviewEditPanel.module.css'

const { FormItem } = Form
const { Option } = Select
const { Column } = Table

const SELECTOR_TYPES: ParamType[] = [ParamType.ENUM, ParamType.MULTIPLE]

const REVIEW_TYPE_OPTIONS = [
  { value: 'user', labelKey: 'flow.stageReviewEdit.reviewer' },
  { value: 'group', labelKey: 'flow.stageReviewEdit.userGroup' },
] as const

function createEmptyParam(): ReviewParam {
  return { key: '', chineseName: '', valueType: ParamType.STRING, value: '', required: false, options: [], desc: '' }
}

const DEFAULT_CHECK_CONFIG: CheckConfig = {
  manualTrigger: false,
  reviewGroups: [],
  notifyType: ['RTX'],
  markdownContent: true,
  timeout: 24,
}

export default defineComponent({
  name: 'StageReviewEditPanel',
  props: {
    isShow: {
      type: Boolean,
      default: false,
    },
    stage: {
      type: Object as PropType<Stage | null>,
      default: null,
    },
    checkType: {
      type: String as PropType<'checkIn' | 'checkOut'>,
      default: 'checkIn',
    },
  },
  emits: ['close', 'change'],
  setup(props, { emit }) {
    const { t } = useI18n()
    const route = useRoute()
    const { isVariablePanelOpen } = storeToRefs(useUIStore())
    const projectId = computed(() => route.params.projectId as string)

    const formData = reactive<CheckConfig>({ ...DEFAULT_CHECK_CONFIG })
    const reviewGroupsCopy = ref<ReviewGroup[]>([])
    const reviewParamsCopy = ref<ReviewParam[]>([])

    interface UserGroupOption { id: string; name: string }
    const userGroupList = ref<UserGroupOption[]>([])

    const loadUserGroupList = async () => {
      if (!projectId.value) return
      try {
        const res = await get<any[]>(
          `/quality/api/user/groups/${projectId.value}/projectGroupAndUsers`,
        )
        userGroupList.value = (res || []).map((item: any) => ({
          id: item.groupId ?? item.id ?? '',
          name: item.groupName ?? item.name ?? '',
        }))
      } catch {
        userGroupList.value = []
      }
    }

    onMounted(loadUserGroupList)

    const paramDialogVisible = ref(false)
    const editingParamIndex = ref(-1)
    const editingParam = reactive<ReviewParam>(createEmptyParam())

    const paramTypeOptions = computed(() =>
      VARIABLE_TYPE_LIST.map(({ id, nameKey }) => ({
        value: id,
        label: t(nameKey),
      })),
    )

    const stageTitle = computed(() => {
      const stageName = props.stage?.name || 'Stage'
      const typeLabel = props.checkType === 'checkIn'
        ? t('flow.stageReviewEdit.checkIn')
        : t('flow.stageReviewEdit.checkOut')
      return `${stageName} - ${typeLabel}`
    })

    const hasReviewerError = computed(() => {
      if (!formData.manualTrigger) return false
      if (reviewGroupsCopy.value.length === 0) return true
      return reviewGroupsCopy.value.some((g) => {
        if (!g.name) return true
        if (g.reviewType === 'group') {
          return !g.groups || g.groups.length === 0
        }
        return g.reviewers.length === 0
      })
    })

    const timeoutValid = computed(() => {
      const val = Number(formData.timeout)
      return Number.isFinite(val) && val >= 1 && val <= 720
    })

    const showNotifyGroup = computed(() => (formData.notifyType || []).includes('WEWORK_GROUP'))

    const notifyGroupStr = computed(() => (formData.notifyGroup || []).join(','))

    const validWeChatGroupID = computed(() => {
      if (!showNotifyGroup.value) return true
      return (formData.notifyGroup || []).length > 0
    })

    const isSelectorType = computed(() => SELECTOR_TYPES.includes(editingParam.valueType))

    const paramDialogTitle = computed(() =>
      editingParamIndex.value >= 0
        ? t('flow.stageReviewEdit.editVariable')
        : t('flow.stageReviewEdit.createVariable'),
    )

    watch(
      () => [props.isShow, props.stage, props.checkType],
      () => {
        if (props.isShow && props.stage) {
          const existing = (props.stage[props.checkType] as CheckConfig) || {}
          const merged = { ...DEFAULT_CHECK_CONFIG, ...existing }
          Object.assign(formData, merged)
          reviewGroupsCopy.value = JSON.parse(JSON.stringify(merged.reviewGroups || []))
          reviewParamsCopy.value = JSON.parse(JSON.stringify(merged.reviewParams || []))
        }
      },
      { immediate: true },
    )

    const emitChange = () => {
      if (!props.stage) return
      const isError = formData.manualTrigger && (
        hasReviewerError.value || !timeoutValid.value || !validWeChatGroupID.value
      )
      const updatedCheckConfig: CheckConfig = {
        ...formData,
        reviewGroups: reviewGroupsCopy.value.map((g) => ({
          name: g.name,
          reviewType: g.reviewType || 'user',
          reviewers: g.reviewers || [],
          groups: g.groups || [],
        })),
        reviewParams: reviewParamsCopy.value,
        isReviewError: isError,
      }
      emit('change', props.stage, props.checkType, updatedCheckConfig)
    }

    // ========== Basic field handlers ==========
    const handleManualTriggerChange = (val: boolean) => {
      formData.manualTrigger = val
      emitChange()
    }

    const handleTimeoutChange = (val: string) => {
      formData.timeout = Number(val)
      emitChange()
    }

    const handleDescChange = (val: string) => {
      formData.reviewDesc = val
      emitChange()
    }

    const handleNotifyTypeChange = (type: string, checked: boolean) => {
      const current = formData.notifyType || []
      if (checked) {
        formData.notifyType = [...current, type]
      } else {
        formData.notifyType = current.filter((t) => t !== type)
        if (type === 'WEWORK_GROUP') {
          formData.notifyGroup = undefined
        }
      }
      emitChange()
    }

    const handleNotifyGroupChange = (val: string) => {
      formData.notifyGroup = val ? val.split(',').map((s) => s.trim()).filter(Boolean) : []
      emitChange()
    }

    const handleMarkdownContentChange = (val: boolean) => {
      formData.markdownContent = val
      emitChange()
    }

    // ========== Review Group handlers ==========
    const addReviewGroup = () => {
      if (reviewGroupsCopy.value.length >= 5) return
      reviewGroupsCopy.value.push({
        name: `${reviewGroupsCopy.value.length + 1}`,
        reviewType: 'user',
        reviewers: [],
        groups: [],
      })
      emitChange()
    }

    const deleteReviewGroup = (index: number) => {
      reviewGroupsCopy.value.splice(index, 1)
      emitChange()
    }

    const updateReviewGroupName = (index: number, val: string) => {
      const group = reviewGroupsCopy.value[index]
      if (group) group.name = val
      emitChange()
    }

    const updateReviewGroupType = (index: number, val: string) => {
      const group = reviewGroupsCopy.value[index]
      if (!group) return
      group.reviewType = val as 'user' | 'group'
      if (val === 'user') {
        group.groups = []
      } else {
        group.reviewers = []
      }
      emitChange()
    }

    const updateReviewGroupUsers = (index: number, val: string[]) => {
      const group = reviewGroupsCopy.value[index]
      if (group) group.reviewers = val
      emitChange()
    }

    const updateReviewGroupGroups = (index: number, val: string[]) => {
      const group = reviewGroupsCopy.value[index]
      if (group) group.groups = val
      emitChange()
    }

    const handleClose = () => {
      emit('close')
    }

    // ========== Review Params CRUD ==========
    const openParamDialog = (param?: ReviewParam, index = -1) => {
      if (param) {
        Object.assign(editingParam, JSON.parse(JSON.stringify(param)))
      } else {
        Object.assign(editingParam, createEmptyParam())
      }
      editingParamIndex.value = index
      paramDialogVisible.value = true
    }

    const confirmParam = () => {
      if (!editingParam.key.trim()) return
      const paramCopy: ReviewParam = JSON.parse(JSON.stringify(editingParam))
      if (editingParamIndex.value >= 0) {
        reviewParamsCopy.value.splice(editingParamIndex.value, 1, paramCopy)
      } else {
        reviewParamsCopy.value.push(paramCopy)
      }
      paramDialogVisible.value = false
      emitChange()
    }

    const removeParam = (index: number) => {
      reviewParamsCopy.value.splice(index, 1)
      emitChange()
    }

    const handleParamTypeChange = (type: ParamType) => {
      editingParam.valueType = type
      editingParam.options = []
      editingParam.value = type === ParamType.MULTIPLE ? [] : ''
    }

    const getOptionsText = () => {
      return (editingParam.options || []).map((o) => (o.key === o.value ? o.key : `${o.key}=${o.value}`)).join('\n')
    }

    const handleOptionsTextChange = (val: string) => {
      if (!val || typeof val !== 'string') {
        editingParam.options = []
        return
      }
      editingParam.options = val.split('\n').map((line) => {
        const trimmed = line.trim()
        const match = trimmed.match(/^([\w./\\]+)=(\S+)$/)
        if (match) return { key: match[1]!, value: match[2]! }
        return { key: trimmed, value: trimmed }
      })
      editingParam.value = editingParam.valueType === ParamType.MULTIPLE ? [] : ''
    }

    const formatParamValue = (val: unknown) => {
      if (Array.isArray(val)) return val.length ? `[${val.join(', ')}]` : '--'
      return val !== undefined && val !== '' ? String(val) : '--'
    }

    const getParamTypeLabel = (type: string) => {
      const found = paramTypeOptions.value.find((o) => o.value === type)
      return found ? found.label : type
    }

    // ========== Render helpers ==========
    const renderReviewGroups = () => (
      <div>
        {reviewGroupsCopy.value.map((group, index) => (
          <div key={index} class={styles.reviewGroupItem}>
            <Input
              class={styles.reviewGroupName}
              modelValue={group.name}
              placeholder={`${index + 1}`}
              onInput={(val: string) => updateReviewGroupName(index, val)}
            />
            <Select
              class={styles.reviewGroupTypeSelect}
              modelValue={group.reviewType || 'user'}
              onChange={(val: string) => updateReviewGroupType(index, val)}
              clearable={false}
            >
              {REVIEW_TYPE_OPTIONS.map((opt) => (
                <Option key={opt.value} value={opt.value} label={t(opt.labelKey)} />
              ))}
            </Select>
            <div class={styles.reviewGroupUsers}>
              {(group.reviewType || 'user') === 'user' ? (
                <StaffInput
                  value={group.reviewers}
                  name={`reviewers-${index}`}
                  placeholder={t('flow.stageReviewEdit.reviewerPlaceholder')}
                  handleChange={(_: string, val: string[]) => updateReviewGroupUsers(index, val)}
                />
              ) : (
                <TagInput
                  modelValue={group.groups || []}
                  list={userGroupList.value}
                  displayKey="name"
                  saveKey="id"
                  searchKey={['id', 'name']}
                  trigger="focus"
                  placeholder={t('flow.stageReviewEdit.userGroupPlaceholder')}
                  onChange={(val: string[]) => updateReviewGroupGroups(index, val)}
                />
              )}
            </div>
            <Button
              text
              theme="primary"
              class={styles.reviewGroupDelete}
              onClick={() => deleteReviewGroup(index)}
            >
              {t('flow.actions.delete')}
            </Button>
          </div>
        ))}
        {hasReviewerError.value && formData.manualTrigger && (
          <span class={styles.errorTip}>{t('flow.stageReviewEdit.reviewerRequired')}</span>
        )}
        <Button
          text
          theme="primary"
          class={styles.addReviewGroupBtn}
          disabled={reviewGroupsCopy.value.length >= 5}
          onClick={addReviewGroup}
        >
          <SvgIcon name="add-small" size={16} />
          {t('flow.stageReviewEdit.addReviewFlow')}
        </Button>
      </div>
    )

    const renderNotifySection = () => (
      <>
        <FormItem label={t('flow.stageReviewEdit.notifyType')} class={styles.formSection}>
          <div class={styles.checkboxGroup}>
            <Checkbox
              modelValue={(formData.notifyType || []).includes('RTX')}
              onChange={(val: boolean) => handleNotifyTypeChange('RTX', val)}
            >
              {t('flow.stageReviewEdit.notifyRTX')}
            </Checkbox>
            <Checkbox
              modelValue={(formData.notifyType || []).includes('WEWORK_GROUP')}
              onChange={(val: boolean) => handleNotifyTypeChange('WEWORK_GROUP', val)}
            >
              {t('flow.stageReviewEdit.notifyWeworkGroup')}
            </Checkbox>
          </div>
        </FormItem>

        {showNotifyGroup.value && (
          <FormItem
            label={t('flow.stageReviewEdit.wechatGroupId')}
            required
            class={styles.formSection}
          >
            <Input
              modelValue={notifyGroupStr.value}
              placeholder={t('flow.stageReviewEdit.wechatGroupIdPlaceholder')}
              onBlur={(e: FocusEvent) => handleNotifyGroupChange((e.target as HTMLInputElement).value)}
            />
            {!validWeChatGroupID.value && (
              <span class={styles.errorTip}>{t('flow.stageReviewEdit.wechatGroupIdRequired')}</span>
            )}
          </FormItem>
        )}

        <FormItem class={styles.formSection}>
          <Checkbox
            modelValue={formData.markdownContent ?? true}
            onChange={handleMarkdownContentChange}
          >
            {t('flow.stageReviewEdit.markdownContent')}
          </Checkbox>
        </FormItem>
      </>
    )

    const renderParamValueInput = () => {
      const { valueType } = editingParam
      if (valueType === ParamType.BOOLEAN) {
        return (
          <Radio.Group v-model={editingParam.value}>
            <Radio label="true">true</Radio>
            <Radio label="false">false</Radio>
          </Radio.Group>
        )
      }
      if (valueType === ParamType.ENUM) {
        return (
          <Select v-model={editingParam.value} clearable>
            {(editingParam.options || []).map((opt) => (
              <Option key={opt.key} value={opt.key} label={opt.value} />
            ))}
          </Select>
        )
      }
      if (valueType === ParamType.MULTIPLE) {
        return (
          <Select v-model={editingParam.value} multiple clearable>
            {(editingParam.options || []).map((opt) => (
              <Option key={opt.key} value={opt.key} label={opt.value} />
            ))}
          </Select>
        )
      }
      if (valueType === ParamType.TEXTAREA) {
        return <Input type="textarea" v-model={editingParam.value} rows={3} />
      }
      return <Input v-model={editingParam.value} />
    }

    const renderReviewParams = () => (
      <div class={styles.reviewParamsSection}>
        {reviewParamsCopy.value.length > 0 && (
          <Table data={reviewParamsCopy.value} class={styles.paramsTable} border="outer">
            <Column label={t('flow.stageReviewEdit.variableName')} prop="key" showOverflowTooltip />
            <Column label={t('flow.stageReviewEdit.alias')} prop="chineseName" showOverflowTooltip />
            <Column label={t('flow.stageReviewEdit.type')}>
              {{ default: ({ row }: { row: ReviewParam }) => getParamTypeLabel(row.valueType) }}
            </Column>
            <Column label={t('flow.stageReviewEdit.defaultValue')} showOverflowTooltip>
              {{ default: ({ row }: { row: ReviewParam }) => formatParamValue(row.value) }}
            </Column>
            <Column label={t('flow.stageReviewEdit.required')} width={80}>
              {{ default: ({ row }: { row: ReviewParam }) => (row.required ? t('flow.actions.yes') : t('flow.actions.no')) }}
            </Column>
            <Column label={t('flow.stageReviewEdit.options')} showOverflowTooltip>
              {{ default: ({ row }: { row: ReviewParam }) => {
                if (!row.options || row.options.length === 0) return '--'
                return row.options.map((o) => o.key).join(', ')
              }}}
            </Column>
            <Column label={t('flow.stageReviewEdit.operation')} width={120}>
              {{ default: ({ row, index }: { row: ReviewParam; index: number }) => (
                <div class={styles.paramActions}>
                  <Button text theme="primary" onClick={() => openParamDialog(row, index)}>{t('flow.actions.edit')}</Button>
                  <Button text theme="primary" onClick={() => removeParam(index)}>{t('flow.actions.delete')}</Button>
                </div>
              )}}
            </Column>
          </Table>
        )}

        <Dialog
          isShow={paramDialogVisible.value}
          title={paramDialogTitle.value}
          width={600}
          zIndex={2100}
          onClosed={() => { paramDialogVisible.value = false }}
          onConfirm={confirmParam}
        >
          <Form formType="vertical" class={styles.paramForm}>
            <FormItem label={t('flow.stageReviewEdit.variableName')} required>
              <Input v-model={editingParam.key} placeholder={t('flow.stageReviewEdit.variableName')} />
            </FormItem>
            <FormItem label={t('flow.stageReviewEdit.alias')}>
              <Input v-model={editingParam.chineseName} placeholder={t('flow.stageReviewEdit.alias')} />
            </FormItem>
            <FormItem label={t('flow.stageReviewEdit.type')} required>
              <Select modelValue={editingParam.valueType} onChange={handleParamTypeChange} clearable={false}>
                {paramTypeOptions.value.map((opt) => (
                  <Option key={opt.value} value={opt.value} label={opt.label} />
                ))}
              </Select>
            </FormItem>
            {isSelectorType.value && (
              <FormItem label={t('flow.stageReviewEdit.listOptions')}>
                <Input
                  type="textarea"
                  modelValue={getOptionsText()}
                  placeholder={t('flow.stageReviewEdit.optionsTips')}
                  rows={4}
                  onBlur={(e: FocusEvent) => handleOptionsTextChange((e.target as HTMLTextAreaElement).value)}
                />
              </FormItem>
            )}
            <FormItem label={t('flow.stageReviewEdit.defaultValue')}>
              {renderParamValueInput()}
            </FormItem>
            <FormItem label={t('flow.stageReviewEdit.required')}>
              <Radio.Group v-model={editingParam.required}>
                <Radio label={true}>{t('flow.actions.yes')}</Radio>
                <Radio label={false} style={{ marginLeft: '40px' }}>{t('flow.actions.no')}</Radio>
              </Radio.Group>
            </FormItem>
            <FormItem label={t('flow.stageReviewEdit.description')}>
              <Input type="textarea" v-model={editingParam.desc} rows={2} />
            </FormItem>
          </Form>
        </Dialog>
      </div>
    )

    return () => (
      <Sideslider
        isShow={props.isShow}
        width={640}
        quickClose={true}
        transfer
        onClosed={handleClose}
        onUpdate:isShow={(val: boolean) => !val && handleClose()}
        class={['bkci-property-panel', isVariablePanelOpen.value && 'with-variable-open']}
      >
        {{
          header: () => <span>{stageTitle.value}</span>,
          default: () => (
            <div class={styles.stageReviewEditContent}>
              <Form formType="vertical">
                {/* 1. 准入规则 */}
                <FormItem label={t('flow.stageReviewEdit.stageCondition')}>
                  <Radio.Group
                    modelValue={formData.manualTrigger}
                    onChange={handleManualTriggerChange}
                    class={styles.reviewRadioGroup}
                  >
                    <Radio label={false}>{t('flow.stageReviewEdit.autoExecute')}</Radio>
                    <Radio label={true}>{t('flow.stageReviewEdit.manualReview')}</Radio>
                  </Radio.Group>
                </FormItem>

                {formData.manualTrigger && (
                  <>
                    <Divider class={styles.sectionDivider} />

                    {/* 2. 审核流 */}
                    <FormItem
                      label={t('flow.stageReviewEdit.approvalFlow')}
                      required
                      class={styles.formSection}
                    >
                      {renderReviewGroups()}
                    </FormItem>

                    {/* 3. 审核说明 */}
                    <FormItem label={t('flow.stageReviewEdit.reviewDesc')} class={styles.formSection}>
                      <Input
                        type="textarea"
                        modelValue={formData.reviewDesc || ''}
                        placeholder={t('flow.stageReviewEdit.reviewDescPlaceholder')}
                        rows={3}
                        onInput={handleDescChange}
                      />
                    </FormItem>

                    {/* 4. 审核通知 + Markdown */}
                    {renderNotifySection()}

                    {/* 5. 审批时间限制 */}
                    <FormItem required class={styles.formSection}>
                      {{
                        label: () => (
                          <div class={styles.labelWithIcon}>
                            <span>{t('flow.stageReviewEdit.timeout')}</span>
                            <Popover content={t('flow.stageReviewEdit.timeoutDesc')} placement="top">
                              <span class={styles.infoIcon}>
                                <SvgIcon name="info-circle" size={14} />
                              </span>
                            </Popover>
                          </div>
                        ),
                        default: () => (
                          <>
                            <Input
                              type="number"
                              modelValue={String(formData.timeout)}
                              min={1}
                              max={720}
                              onInput={handleTimeoutChange}
                            >
                              {{
                                suffix: () => (
                                  <span class={styles.timeoutSuffix}>
                                    {t('flow.stageReviewEdit.hours')}
                                  </span>
                                ),
                              }}
                            </Input>
                            {!timeoutValid.value && (
                              <span class={styles.errorTip}>
                                {t('flow.stageReviewEdit.timeoutError')}
                              </span>
                            )}
                          </>
                        ),
                      }}
                    </FormItem>

                    {/* 6. 自定义参数 */}
                    <FormItem class={styles.formSection}>
                      {{
                        label: () => (
                          <div class={styles.labelWithAction}>
                            <span>{t('flow.stageReviewEdit.reviewParams')}</span>
                            <Button
                              text
                              theme="primary"
                              class={styles.addParamBtn}
                              onClick={() => openParamDialog()}
                            >
                              <SvgIcon name="add-small" size={16} />
                              {t('flow.stageReviewEdit.createVariable')}
                            </Button>
                          </div>
                        ),
                        default: () => renderReviewParams(),
                      }}
                    </FormItem>
                  </>
                )}
              </Form>
            </div>
          ),
        }}
      </Sideslider>
    )
  },
})
