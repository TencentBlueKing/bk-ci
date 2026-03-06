import {
  prefetchReleaseVersion,
  releaseFlowVersion,
  type PrefetchVersionInfo,
  type ReleaseParams,
  type ReleaseResponse,
} from '@/api/release'
import { SvgIcon } from '@/components/SvgIcon'
import { useFlowInfoStore } from '@/stores/flowInfoStore'
import { Button, Dialog, Form, Input, Loading, Message, Sideslider } from 'bkui-vue'
import { storeToRefs } from 'pinia'
import { computed, defineComponent, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { FLOW_DETAIL_TABS, ROUTE_NAMES } from '../../constants/routes'
import styles from './ReleaseSlider.module.css'

const { FormItem } = Form

export interface ReleaseSliderProps {
  isShow: boolean
  projectId: string
  flowId: string
  version: number
  baseVersionName?: string
  canRelease?: boolean
}

export const ReleaseSlider = defineComponent({
  name: 'ReleaseSlider',
  props: {
    isShow: {
      type: Boolean,
      default: false,
    },
    projectId: {
      type: String,
      required: true,
    },
    flowId: {
      type: String,
      required: true,
    },
    version: {
      type: Number,
      required: true,
    },
    baseVersionName: {
      type: String,
      default: '--',
    },
    canRelease: {
      type: Boolean,
      default: true,
    },
  },
  emits: ['update:isShow', 'released'],
  setup(props, { emit }) {
    const { t } = useI18n()
    const route = useRoute()
    const router = useRouter()
    const flowInfoStore = useFlowInfoStore()
    const { flowInfo } = storeToRefs(flowInfoStore)

    const isExecuteDisabled = computed(() => {
      return flowInfo.value?.locked || !flowInfo.value?.canManualStartup
    })

    const executeTooltip = computed(() => {
      if (!isExecuteDisabled.value) return ''
      return flowInfo.value?.locked
        ? t('flow.content.pipelineLockTips')
        : t('flow.content.pipelineManualDisable')
    })

    // State
    const isLoading = ref(false)
    const isReleasing = ref(false)
    const showSuccessDialog = ref(false)
    const releaseResult = ref<ReleaseResponse | null>(null)
    const prefetchInfo = ref<PrefetchVersionInfo | null>(null)

    // Form state
    const releaseForm = ref({
      description: '',
    })

    // Computed
    const newVersionName = computed(() => {
      return prefetchInfo.value?.newVersionName || '--'
    })

    const isVisible = computed({
      get: () => props.isShow,
      set: (val) => emit('update:isShow', val),
    })

    // Methods
    const handleClose = () => {
      isVisible.value = false
      resetForm()
    }

    const resetForm = () => {
      releaseForm.value = {
        description: '',
      }
      releaseResult.value = null
    }

    const handleSliderShown = async () => {
      if (!props.projectId || !props.flowId || !props.version) return

      isLoading.value = true
      try {
        prefetchInfo.value = await prefetchReleaseVersion(
          props.projectId,
          props.flowId,
          props.version,
        )
      } catch (error: any) {
        console.error('Failed to prefetch release version:', error)
        Message({
          theme: 'error',
          message: error?.message || t('flow.release.prefetchFailed'),
        })
      } finally {
        isLoading.value = false
      }
    }

    const handleRelease = async () => {
      if (isReleasing.value) return

      isReleasing.value = true
      try {
        const params: ReleaseParams = {
          description: releaseForm.value.description,
          enablePac: false,
          yamlInfo: null,
        }

        releaseResult.value = await releaseFlowVersion(
          props.projectId,
          props.flowId,
          props.version,
          params,
        )

        // Show success dialog
        showSuccessDialog.value = true
        isVisible.value = false

        // Emit released event
        emit('released', releaseResult.value)
      } catch (error: any) {
        console.error('Failed to release flow:', error)
        Message({
          theme: 'error',
          message: error?.message || t('flow.release.releaseFailed'),
        })
      } finally {
        isReleasing.value = false
      }
    }

    const handleGoToExecution = () => {
      showSuccessDialog.value = false
      router.push({
        name: ROUTE_NAMES.FLOW_PREVIEW,
        params: {
          projectId: props.projectId,
          flowId: props.flowId,
          version: String(props.version),
        },
      })
    }

    const handleGoToDetail = () => {
      showSuccessDialog.value = false
      router.push({
        name: FLOW_DETAIL_TABS.WORKFLOW_ORCHESTRATION,
        params: {
          projectId: props.projectId,
          flowId: props.flowId,
          version: String(props.version),
        },
      })
    }

    const handleCloseSuccessDialog = () => {
      showSuccessDialog.value = false
    }

    // Watch for props changes
    watch(
      () => props.isShow,
      (newVal) => {
        if (newVal) {
          handleSliderShown()
        }
      },
    )

    return () => (
      <>
        <Sideslider isShow={isVisible.value} width={640} title="" onClosed={handleClose}>
          {{
            header: () => (
              <div class={styles.releaseHeader}>
                <span>{t('flow.release.releaseFlow')}</span>
                <div class={styles.releaseVersionInfo}>
                  <span class={styles.newVersion}>
                    {t('flow.release.newVersion')}: {newVersionName.value}
                  </span>
                  <span>
                    {t('flow.release.baseVersion')}: {props.baseVersionName}
                  </span>
                </div>
              </div>
            ),
            default: () => (
              <Loading loading={isLoading.value}>
                <div class={styles.releaseContent}>
                  <Form class={styles.releaseForm} model={releaseForm.value} labelWidth={0}>
                    <FormItem label="" property="description">
                      <div class={styles.formItem}>
                        <label class={styles.formLabel}>{t('flow.release.releaseNote')}</label>
                        <Input
                          v-model={releaseForm.value.description}
                          type="textarea"
                          rows={6}
                          maxlength={200}
                          showWordLimit
                          placeholder={t('flow.release.releaseNotePlaceholder')}
                        />
                      </div>
                    </FormItem>
                  </Form>
                </div>
              </Loading>
            ),
            footer: () => (
              <div class={styles.releaseFooter}>
                <Button
                  theme="primary"
                  loading={isReleasing.value}
                  disabled={isReleasing.value || !props.canRelease}
                  onClick={handleRelease}
                >
                  {t('flow.release.release')}
                </Button>
                <Button disabled={isReleasing.value} onClick={handleClose}>
                  {t('flow.common.cancel')}
                </Button>
              </div>
            ),
          }}
        </Sideslider>

        {/* Success Dialog */}
        <Dialog
          isShow={showSuccessDialog.value}
          title=""
          width={600}
          showFooter={false}
          onClosed={handleCloseSuccessDialog}
        >
          {{
            header: () => (
              <div class="text-center">
                <div class={styles.successIcon}>
                  <SvgIcon name="check-circle" size={42} />
                </div>
                <div class={styles.successTitle}>{t('flow.release.releaseSuccess')}</div>
              </div>
            ),
            default: () => (
              <div class="text-center">
                <div class={styles.successVersion}>
                  {t('flow.release.releaseSuccessTip', {
                    version: releaseResult.value?.versionName || newVersionName.value,
                  })}
                </div>
              </div>
            ),
            footer: () => (
              <div class={styles.successActions}>
                <span
                  v-bk-tooltips={{
                    content: executeTooltip.value,
                    disabled: !isExecuteDisabled.value,
                  }}
                >
                  <Button
                    theme="primary"
                    disabled={isExecuteDisabled.value}
                    onClick={handleGoToExecution}
                  >
                    {t('flow.release.goToExecute')}
                  </Button>
                </span>
                <Button onClick={handleGoToDetail}>{t('flow.release.viewFlow')}</Button>
              </div>
            ),
          }}
        </Dialog>
      </>
    )
  },
})

export default ReleaseSlider
