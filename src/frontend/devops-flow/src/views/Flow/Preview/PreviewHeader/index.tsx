import { CommonHeader } from '@/components/CommonHeader'
import { SvgIcon } from '@/components/SvgIcon'
import { useFlowInfo } from '@/hooks/useFlowInfo'
import type { FlowVersion } from '@/types/flow'
import { Button, Loading, Select, Tag } from 'bkui-vue'
import { computed, defineComponent, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { ROUTE_NAMES } from '../../../../constants/routes'
import styles from './PreviewHeader.module.css'

const { Option } = Select

/**
 * Find latest version from version list (pure function)
 */
const findLatestVersion = (versions: FlowVersion[]): FlowVersion | undefined => {
  return versions.find((v) => v.latestReleasedFlag)
}

// ============================================
// 3. Component Definition
// ============================================

export default defineComponent({
  name: 'PreviewHeader',
  props: {
    executing: {
      type: Boolean,
      default: false,
    },
    canExecute: {
      type: Boolean,
      default: true,
    },
    flowName: {
      type: String,
      default: '',
    },
  },
  emits: ['execute', 'versionChange'],
  setup(props, { emit }) {
    const { t } = useI18n()
    const route = useRoute()
    const router = useRouter()

    // ----------------------------------------
    // Use hook to get version list (filtered out COMMITTING versions)
    // ----------------------------------------
    const { releasedVersionList, loading: loadingVersions, flowInfo } = useFlowInfo()

    // ----------------------------------------
    // Local State
    // ----------------------------------------
    const selectedVersion = ref<number | undefined>(undefined)

    // ----------------------------------------
    // Route Params (Computed)
    // ----------------------------------------
    const routeVersion = computed(() => {
      const v = route.params.version
      return v ? Number(v) : undefined
    })

    // ----------------------------------------
    // Computed (Derived from Props)
    // ----------------------------------------
    const isDebugMode = computed(() => Object.prototype.hasOwnProperty.call(route.query, 'debug'))

    const currentVersionOption = computed(() =>
      releasedVersionList.value.find((v) => v.version === selectedVersion.value),
    )

    // ----------------------------------------
    // Actions
    // ----------------------------------------

    const handleExecute = (): void => {
      emit('execute')
    }

    const handleCancel = (): void => {
      router.back()
    }

    const goToFlow = (): void => {
      router.push({
        name: ROUTE_NAMES.FLOW_DETAIL_EXECUTION_RECORD,
        params: {
          ...route.params,
          version: flowInfo.value?.releaseVersion,
        },
      })
    }

    const handleVersionChange = (version: number): void => {
      selectedVersion.value = version
      emit('versionChange', version)
    }

    // ----------------------------------------
    // Render Functions (Pure View Logic)
    // ----------------------------------------

    const renderTag = () => (
      <Tag theme="success" size="small" class={styles.tag}>
        {t('flow.content.latest')}
      </Tag>
    )

    const renderCheckIcon = (isLatest = false) => (
      <SvgIcon name="check-circle" class={[styles.checkIcon, isLatest && styles.latestCheckIcon]} />
    )

    const renderVersionSelector = () => {
      const currentVersion = currentVersionOption.value

      return (
        <div class={styles.versionSelectorWrapper}>
          <span class={styles.versionLabel}>{t('flow.preview.flowVersion')}</span>
          <Select
            modelValue={selectedVersion.value}
            onChange={handleVersionChange}
            class={styles.versionSelector}
            clearable={false}
            loading={loadingVersions.value}
          >
            {{
              trigger: () => (
                <span class={styles.versionTrigger}>
                  {renderCheckIcon(currentVersion?.latestReleasedFlag)}
                  <span class={styles.versionText}>{currentVersion?.versionName}</span>
                  {currentVersion?.latestReleasedFlag && renderTag()}
                  <SvgIcon name="angle-down" class={styles.versionSelectToggleIcon} />
                </span>
              ),
              default: () =>
                releasedVersionList.value.map((version) => (
                  <Option key={version.version} value={version.version} label={version.versionName}>
                    <div class={styles.versionOption}>
                      <div class={styles.versionOptionName}>
                        {renderCheckIcon(version.latestReleasedFlag)}
                        <span>{version.versionName}</span>
                        {version.latestReleasedFlag && renderTag()}
                      </div>
                      <span class={styles.versionOptionDesc}>
                        {version.description || '--'}
                      </span>
                    </div>
                  </Option>
                )),
            }}
          </Select>
        </div>
      )
    }

    const renderExecutionTitle = () => (
      <span class={styles.executionTitle}>
        {t(isDebugMode.value ? 'flow.preview.debug' : 'flow.preview.execute')}
      </span>
    )

    const renderActions = () => (
      <>
        {/* Cancel button */}
        <Button disabled={props.executing} onClick={handleCancel}>
          {t('flow.common.cancel')}
        </Button>

        {/* Execute/Debug button */}
        <Button
          theme="primary"
          disabled={props.executing || !props.canExecute}
          loading={props.executing}
          onClick={handleExecute}
        >
          {t(isDebugMode.value ? 'flow.preview.debug' : 'flow.preview.execute')}
        </Button>
      </>
    )

    // ----------------------------------------
    // Watchers
    // ----------------------------------------

    // Initialize selected version when version list is loaded
    watch(
      releasedVersionList,
      (list) => {
        if (list.length > 0 && selectedVersion.value === undefined) {
          // Set selected version from route or latest
          if (routeVersion.value) {
            selectedVersion.value = routeVersion.value
          } else {
            const latestVersion = findLatestVersion(list)
            selectedVersion.value = latestVersion?.version ?? list[0]?.version
          }
        }
      },
      { immediate: true },
    )

    // ----------------------------------------
    // Main Render
    // ----------------------------------------
    return () => {
      // Early return for loading state
      if (!props.flowName) {
        return (
          <header class={styles.previewHeader}>
            <Loading size="mini" mode="spin" />
          </header>
        )
      }

      return (
        <CommonHeader workflowName={props.flowName} onWorkflowNameClick={goToFlow}>
          {{
            'version-selector': renderVersionSelector,
            'execution-detail': renderExecutionTitle,
            actions: renderActions,
          }}
        </CommonHeader>
      )
    }
  },
})
