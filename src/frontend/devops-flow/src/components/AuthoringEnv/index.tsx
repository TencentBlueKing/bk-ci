import {
  getAuthoringNodeDisplayText,
  getEnvOsDisplayName,
} from '@/api/authoringEnvironmentApi'
import { SvgIcon } from '@/components/SvgIcon'
import useAuthoringEnvironment, { type EnvSelectItem } from '@/hooks/useAuthoringEnvironment'
import type { EnvironmentOsCompatibilityResult } from '@/hooks/useEnvironmentOsCompatibility'
import { Button, Dialog, Exception, Loading, Message, Select, Tag } from 'bkui-vue'
import { computed, defineComponent, nextTick, onMounted, ref, watch, type PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import styles from './AuthoringEnv.module.css'

export default defineComponent({
  name: 'AuthoringEnv',
  components: {
    SvgIcon,
  },
  props: {
    isEdit: {
      type: Boolean,
      default: false,
    },
    modelValue: {
      type: String,
      default: '',
    },
    envList: {
      type: Array as PropType<EnvSelectItem[]>,
      default: () => [],
    },
    nodeList: {
      type: Array,
      default: () => [],
    },
    envLoading: {
      type: Boolean,
      default: false,
    },
    nodeLoading: {
      type: Boolean,
      default: false,
    },
    selectLabel: {
      type: String,
      default: '',
    },
    selectRequired: {
      type: Boolean,
      default: false,
    },
    showEnvironmentManagement: {
      type: Boolean,
      default: false,
    },
    beforeChange: {
      type: Function as PropType<(
        currentEnv?: EnvSelectItem,
        nextEnv?: EnvSelectItem,
      ) => Promise<EnvironmentOsCompatibilityResult>>,
      default: undefined,
    },
  },
  emits: ['update:modelValue'],
  setup(props, { emit }) {
    const { t } = useI18n()
    const envHashId = ref(props.modelValue)
    const pendingEnvHashId = ref('')
    const osCompatibilityResult = ref<EnvironmentOsCompatibilityResult | null>(null)
    const isOsDialogShow = ref(false)
    const isCheckingOs = ref(false)
    const { goEnvironment, loadNodeList } = useAuthoringEnvironment()
    const selectedEnv = computed(() => {
      return props.envList.find((env) => env.envHashId === envHashId.value || env.value === envHashId.value)
    })
    const envName = computed(() => selectedEnv.value?.name || envHashId.value)
    const selectedOsLabel = computed(() => getEnvOsDisplayName(selectedEnv.value?.os))

    watch(
      () => props.modelValue,
      (newValue) => {
        if (newValue !== envHashId.value) {
          envHashId.value = newValue
          nextTick(() => {
            loadNodeList(newValue)
          })
        }
      },
    )

    onMounted(() => {
      if (envHashId.value) {
        loadNodeList(envHashId.value)
      }
    })

    function commitEnvironmentChange(value: string) {
      envHashId.value = value
      emit('update:modelValue', value)
      nextTick(() => {
        loadNodeList(value)
      })
    }

    async function handleChange(value: string) {
      if (!value || value === envHashId.value) return

      const currentEnv = props.envList.find(
        (env) => env.envHashId === envHashId.value || env.value === envHashId.value,
      )
      const nextEnv = props.envList.find(
        (env) => env.envHashId === value || env.value === value,
      )

      if (!props.beforeChange) {
        commitEnvironmentChange(value)
        return
      }

      isCheckingOs.value = true
      try {
        const result = await props.beforeChange(currentEnv, nextEnv)
        if (result.type === 'unchanged') {
          commitEnvironmentChange(value)
          return
        }

        pendingEnvHashId.value = value
        osCompatibilityResult.value = result
        isOsDialogShow.value = true
      } catch (error: any) {
        Message({
          theme: 'error',
          message: error?.message || t('flow.content.environmentOsCheckFailed'),
        })
      } finally {
        isCheckingOs.value = false
      }
    }

    function closeOsDialog() {
      isOsDialogShow.value = false
      pendingEnvHashId.value = ''
      osCompatibilityResult.value = null
    }

    function continueEnvironmentChange() {
      const value = pendingEnvHashId.value
      closeOsDialog()
      if (value) {
        commitEnvironmentChange(value)
      }
    }

    function goToEnvironment() {
      goEnvironment(envHashId.value)
    }

    function renderEnvironmentManagement() {
      return (
        <button type="button" class={styles.envManageEntry} onClick={goToEnvironment}>
          <SvgIcon name="jump" size={12} class={styles.envManageIcon} />
          <span>{t('flow.content.environmentManagement')}</span>
        </button>
      )
    }

    function renderEnvOption(env: EnvSelectItem) {
      const osLabel = getEnvOsDisplayName(env.os)
      return (
        <Select.Option key={env.value} id={env.value} name={env.label}>
          <div class={styles.envOption}>
            {osLabel ? (
              <Tag size="small" class={styles.envOsTag}>
                {osLabel}
              </Tag>
            ) : null}
            <span class={styles.envOptionName}>{env.label}</span>
          </div>
        </Select.Option>
      )
    }

    function renderSelectedOsTag() {
      if (!selectedOsLabel.value) return null
      return (
        <div class={styles.envSelectPrefix}>
          <Tag size="small" class={styles.envOsTag}>
            {selectedOsLabel.value}
          </Tag>
        </div>
      )
    }

    function renderEnvSelect() {
      const selectProps = {
        class: [styles.envSelect, selectedOsLabel.value && styles.envSelectWithOs],
        modelValue: envHashId.value,
        'onUpdate:modelValue': (value: string) => {
          handleChange(value)
        },
        filterable: true,
        loading: props.envLoading || isCheckingOs.value,
        placeholder: t('flow.orchestration.selectPlaceholder'),
        searchPlaceholder: t('flow.content.searchEnvironment'),
      }
      const selectSlots: Record<string, () => unknown> = {
        default: () => props.envList.map(renderEnvOption),
      }
      if (selectedOsLabel.value) {
        selectSlots.prefix = renderSelectedOsTag
      }
      if (props.showEnvironmentManagement) {
        selectSlots.extension = renderEnvironmentManagement
      }

      return (
        <div class={[styles.envSelectLine, !props.selectLabel && styles.envSelectLinePlain]}>
          {props.selectLabel ? (
            <label class={styles.envSelectLabel}>
              {props.selectLabel}
              {props.selectRequired ? <span class={styles.requiredMark}>*</span> : null}
            </label>
          ) : null}
          <div class={styles.envSelectControl}>
            <Select {...selectProps}>{selectSlots}</Select>
          </div>
        </div>
      )
    }

    function renderNodeEmpty() {
      return (
        <Exception type="empty" scene="part" class={styles.nodeEmpty}>
          {t('flow.content.noCreationNodeInEnvironment')}
        </Exception>
      )
    }

    function renderNodeList() {
      return (
        <div class={styles.nodeTag}>
          {props.nodeList.map((node: any) => (
            <Tag key={node.nodeId}>{getAuthoringNodeDisplayText(node)}</Tag>
          ))}
        </div>
      )
    }

    function renderEnvironmentDetails() {
      if (props.nodeList.length === 0) {
        return renderNodeEmpty()
      }

      return (
        <>
          <div class={styles.envItem}>
            <p class={styles.envItemTit}>
              {t('flow.content.creationNode')}
              {props.isEdit ? (
                <span
                  class={styles.envSettingBtn}
                  onClick={goToEnvironment}
                  v-bk-tooltips={{
                    content: t('flow.content.environmentSettings'),
                    placement: 'top',
                  }}
                >
                  <SvgIcon name="set-line" size={12} class={styles.setLine} />
                </span>
              ) : null}
            </p>
            {renderNodeList()}
          </div>
          <div class={styles.envItem}>
            <p class={styles.envItemTit}>{t('flow.content.workspace')}</p>
            <div>{t('flow.content.workSpaceDesc', [envHashId.value])}</div>
          </div>
        </>
      )
    }

    return () => {
      const result = osCompatibilityResult.value
      const previousOs = getEnvOsDisplayName(result?.previousOs)
      const nextOs = getEnvOsDisplayName(result?.nextOs)

      return (
        <>
          <div class={styles.authoringRoot}>
            {props.isEdit ? renderEnvSelect() : null}
            <div class={styles.authoringContent}>
              {!props.isEdit ? (
                <p class={styles.authoringHeader}>
                  <span class={styles.headerText}>{envName.value}</span>
                </p>
              ) : null}
              {envHashId.value ? (
                <Loading loading={props.nodeLoading} size="small" class="p-lg">
                  {renderEnvironmentDetails()}
                </Loading>
              ) : (
                <p class={styles.noData}>{t('flow.content.previewDetailsAfterEnvironmentSelection')}</p>
              )}
            </div>
          </div>

          <Dialog
            isShow={isOsDialogShow.value}
            title={result?.type === 'incompatible'
              ? t('flow.content.environmentOsIncompatibleTitle')
              : t('flow.content.environmentOsConfirmTitle')}
            width={680}
            quickClose={false}
            showFooter={false}
            onClosed={closeOsDialog}
          >
            {{
              default: () => (
                <div class={styles.osDialogContent}>
                  <p>
                    {t('flow.content.environmentOsChanged', [previousOs, nextOs])}
                    {result?.type === 'incompatible'
                      ? t('flow.content.environmentOsIncompatible')
                      : t('flow.content.environmentOsCompatible')}
                  </p>
                  {result?.type === 'incompatible' ? (
                    <>
                      <p>{t('flow.content.environmentOsChooseTip')}</p>
                      <ul class={styles.osIncompatibleList}>
                        {result.incompatiblePlugins.map((plugin, index) => (
                          <li key={`${plugin.atomCode}-${index}`}>
                            {t('flow.content.environmentOsPluginTip', [
                              plugin.name,
                              plugin.supportedOs.map(getEnvOsDisplayName).join(' / '),
                              nextOs,
                            ])}
                          </li>
                        ))}
                      </ul>
                    </>
                  ) : null}
                </div>
              ),
              footer: () => (
                <div class={styles.osDialogFooter}>
                  {result?.type === 'compatible' ? (
                    <>
                      <Button onClick={closeOsDialog}>{t('flow.common.cancel')}</Button>
                      <Button theme="primary" onClick={continueEnvironmentChange}>
                        {t('flow.content.continueSave')}
                      </Button>
                    </>
                  ) : (
                    <Button theme="primary" onClick={closeOsDialog}>
                      {t('flow.content.known')}
                    </Button>
                  )}
                </div>
              ),
            }}
          </Dialog>
        </>
      )
    }
  },
})
