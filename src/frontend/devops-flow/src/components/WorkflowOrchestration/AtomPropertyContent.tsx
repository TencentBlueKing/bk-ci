import type { AtomModal, AtomVersion } from '@/api/atom'
import type { AdditionalOptions, CustomVariable, Element } from '@/api/flowModel'
import AtomCheckbox from '@/components/AtomForm/AtomCheckbox'
import AtomCheckboxList from '@/components/AtomForm/AtomCheckboxList'
import AtomForm, { type AtomPropsModel } from '@/components/AtomForm/AtomForm'
import KeyValueMap from '@/components/AtomForm/KeyValueMap'
import { SvgIcon } from '@/components/SvgIcon'
import { getAtomFailControlList, getAtomRunConditionList } from '@/constants/flowOptionConfig'
import { DEFAULT_VERSION, useAtomVersion } from '@/hooks/useAtomVersion'
import { useAtomStore } from '@/stores/atom'
import { AtomRunCondition } from '@/utils/flowDefaults'
import { Button, Collapse, Form, Input, Loading, Popover, Select } from 'bkui-vue'
import { computed, defineComponent, type PropType, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import styles from './AtomPropertyPanel.module.css'
import sharedStyles from './shared.module.css'
const { FormItem } = Form
const { Option } = Select
const { CollapsePanel } = Collapse

export interface AtomPropertyContentProps {
    /** Element data */
    element: Element | null
    /** Whether the form is editable */
    editable?: boolean
    /** Whether to show atom selector button */
    showAtomSelector?: boolean
    /** Whether to show step id field */
    showStepIdField?: boolean
    /** Whether to show version selector */
    showVersionSelector?: boolean
    /** Whether to show custom env section */
    showCustomEnvSection?: boolean
    /** Whether to show flow control section */
    showFlowControlSection?: boolean
}

export default defineComponent({
    name: 'AtomPropertyContent',
    props: {
        element: {
            type: Object as PropType<Element | null>,
            default: null,
        },
        editable: {
            type: Boolean,
            default: true,
        },
        showAtomSelector: {
            type: Boolean,
            default: true,
        },
        showStepIdField: {
            type: Boolean,
            default: true,
        },
        showVersionSelector: {
            type: Boolean,
            default: true,
        },
        showCustomEnvSection: {
            type: Boolean,
            default: true,
        },
        showFlowControlSection: {
            type: Boolean,
            default: true,
        },
    },
    emits: ['change', 'chooseAtom'],
    setup(props, { emit }) {
        // ========== Hooks ==========
        const { t } = useI18n()
        const route = useRoute()
        const atomStore = useAtomStore()
        const projectCode = route.params.projectId as string
        const atomVersion = useAtomVersion({ projectCode })

        // ========== Refs ==========
        const versionList = ref<AtomVersion[]>([])
        const isLoadingVersion = ref(false)
        const atomModal = ref<AtomModal | null>(null)
        // 默认展开流程控制选项面板
        const activeIndex = ref(['flowControl'])

        // ========== Computed ==========
        const atomCode = computed(() => {
            const element = props.element
            if (!element) return ''
            // If it's a third-party plugin, use atomCode, otherwise use @type
            const isThird = element.atomCode && element['@type'] !== element.atomCode
            return isThird ? element.atomCode : element['@type'] || ''
        })

        const atomVersionValue = computed(() => {
            return props.element?.version || DEFAULT_VERSION
        })

        const isLoadingModal = computed(() => {
            const code = atomCode.value
            const version = atomVersionValue.value
            if (!code || !version) return false
            return atomStore.isLoadingAtomModal(code, version)
        })

        const isAtomSelected = computed(() => {
            return !!atomCode.value && !!props.element
        })

        const atomPropsModel = computed(() => {
            const modal = atomModal.value
            if (!modal) return null

            return modal.props as AtomPropsModel
        })

        // Check if it's a new template version
        const isNewTemplate = computed(() => {
            const modal = atomModal.value
            if (!modal) return false
            const { htmlTemplateVersion } = modal
            return htmlTemplateVersion && htmlTemplateVersion !== '1.0'
        })

        const atomValue = computed(() => {
            const element = props.element
            if (!element) return {}

            // New version template: get from element.data.input
            if (isNewTemplate.value) {
                return element.data?.input || {}
            }

            // Old version template: use element directly
            return element
        })

        const customEnv = computed(() => {
            return props.element?.customEnv || []
        })

        const additionalOptions = computed(() => {
            return props.element?.additionalOptions || ({} as AdditionalOptions)
        })

        const failControlValue = computed(() => {
            const options = additionalOptions.value
            const control: string[] = []
            if (options.continueWhenFailed) control.push('continueWhenFailed')
            if (options.retryWhenFailed) control.push('retryWhenFailed')
            if (options.manualRetry) control.push('MANUAL_RETRY')
            return control
        })

        // Check if custom variables input should be shown
        const showCustomVariables = computed(() => {
            const runCondition = additionalOptions.value.runCondition
            return (
                runCondition === AtomRunCondition.CUSTOM_VARIABLE_MATCH ||
                runCondition === AtomRunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN
            )
        })

        // Check if custom condition expression input should be shown
        const showCustomCondition = computed(() => {
            return additionalOptions.value.runCondition === AtomRunCondition.CUSTOM_CONDITION_MATCH
        })

        // Get custom variables list
        const customVariables = computed(() => {
            return additionalOptions.value.customVariables || []
        })

        // Get custom condition expression
        const customCondition = computed(() => {
            return additionalOptions.value.customCondition || ''
        })

        const computedVersionList = computed(() => {
            const currentVersion = atomVersionValue.value
            const list = versionList.value.map((v) => ({
                value: v.versionValue,
                label: v.versionName,
            }))

            // If current version is not in the list, add it
            if (currentVersion && !list.find((v) => v.value === currentVersion)) {
                list.push({
                    value: currentVersion,
                    label: currentVersion.replace('.*', '.latest'),
                })
            }
            return list
        })

        const atomDisplayName = computed(() => {
            if (atomModal.value?.name) {
                return atomModal.value.name
            }
            return getAtomName()
        })

        const docsLink = computed(() => {
            return atomModal.value?.docsLink || ''
        })

        // ========== Lifecycle Hooks ==========
        watch(
            atomCode,
            (newCode) => {
                if (newCode) {
                    loadVersionList()
                    getAtomModal()
                } else {
                    versionList.value = []
                }
            },
            { immediate: true },
        )

        // ========== Functions ==========
        async function getAtomModal() {
            const code = atomCode.value
            const version = atomVersionValue.value
            if (!code || !version) return null
            const modal = await atomStore.getAtomModal(code, version, projectCode)
            atomModal.value = modal
            return modal
        }

        function getAtomName() {
            if (props.element?.name) {
                return props.element.name
            }
            if (atomCode.value) {
                return atomCode.value
            }
            return ''
        }

        function handleChooseAtom() {
            emit('chooseAtom')
        }

        function handleConfigChange(key: string, value: any) {
            if (!props.element) return

            const element = { ...props.element }

            // New version template: update element.data.input[key]
            if (isNewTemplate.value) {
                if (!element.data) {
                    element.data = { input: {}, output: [] }
                }
                if (!element.data.input) {
                    element.data.input = {}
                }
                element.data.input[key] = value
            } else {
                // Old version template: update element[key] directly
                element[key] = value
            }

            emit('change', element)
        }

        function handleCustomEnvChange(value: CustomVariable[]) {
            if (!props.element) return
            const element = { ...props.element, customEnv: value }
            emit('change', element)
        }

        function handleAdditionalOptionsChange(key: string, value: any) {
            if (!props.element) return
            const currentOptions = additionalOptions.value
            const newOptions = { ...currentOptions, [key]: value } as AdditionalOptions

            // Handle failControl linkage logic
            if (key === 'failControl') {
                const failControl = value as string[]
                newOptions.continueWhenFailed = failControl.includes('continueWhenFailed')
                newOptions.retryWhenFailed = failControl.includes('retryWhenFailed')
                newOptions.manualRetry = failControl.includes('MANUAL_RETRY')
                newOptions.failControl = failControl
            }
            debugger
            const element = { ...props.element, additionalOptions: newOptions }
            emit('change', element)
        }

        function handleStepIdChange(value: string) {
            if (!props.element) return
            const element = { ...props.element, stepId: value }
            emit('change', element)
        }

        async function handleVersionChange(version: string) {
            if (!props.element || props.element.version === version) return

            const element = { ...props.element, version }

            // Load config for new version
            const code = atomCode.value
            if (code) {
                try {
                    await atomStore.getAtomModal(code, version, projectCode)
                } catch (error) {
                    console.error('Failed to load atom modal:', error)
                }
            }

            emit('change', element)
        }

        async function loadVersionList() {
            const code = atomCode.value
            if (!code) return

            isLoadingVersion.value = true
            try {
                const versions = await atomVersion.loadVersionList(code)
                versionList.value = versions
            } catch (error) {
                console.error('Failed to load version list:', error)
                versionList.value = []
            } finally {
                isLoadingVersion.value = false
            }
        }

        function handleDocLinkClick(e: MouseEvent) {
            e.stopPropagation()
        }

        function handleCustomEnvChangeWrapper(name: string, value: CustomVariable[]) {
            handleCustomEnvChange(value)
        }

        function handleEnableChange(name: string, value: boolean) {
            handleAdditionalOptionsChange('enable', value)
        }

        function handleFailControlChange(name: string, value: string[]) {
            handleAdditionalOptionsChange('failControl', value)
        }

        function handleTimeoutChange(val: string) {
            handleAdditionalOptionsChange('timeoutVar', val)
        }

        function handleRunConditionChange(val: string) {
            const currentOptions = additionalOptions.value
            const newOptions = { ...currentOptions, runCondition: val } as AdditionalOptions

            // Clean up fields based on new runCondition
            if (
                val !== AtomRunCondition.CUSTOM_VARIABLE_MATCH &&
                val !== AtomRunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN
            ) {
                newOptions.customVariables = []
            } else if (!newOptions.customVariables || newOptions.customVariables.length === 0) {
                newOptions.customVariables = [{ key: 'param1', value: '' }]
            }

            if (val !== 'CUSTOM_CONDITION_MATCH') {
                newOptions.customCondition = ''
            }

            const element = { ...props.element, additionalOptions: newOptions }
            emit('change', element)
        }

        function handleCustomConditionChange(value: string) {
            handleAdditionalOptionsChange('customCondition', value)
        }

        return () => (
            <div class={styles.atomPropertyPanel}>
                <Loading loading={isLoadingModal.value}>
                    <div class={styles.content}>
                        <Form formType="vertical">
                            {/* Step ID config */}
                            {isAtomSelected.value && props.showStepIdField && (
                                <FormItem>
                                    {{
                                        label: () => (
                                            <div class={styles.labelWithIcon}>
                                                <span>{t('flow.orchestration.stepId')}</span>
                                                <Popover content={t('flow.orchestration.stepIdDesc')} placement="top">
                                                    <span class={sharedStyles.infoIcon}>
                                                        <SvgIcon name="info-circle" size={14} />
                                                    </span>
                                                </Popover>
                                            </div>
                                        ),
                                        default: () => (
                                            <Input
                                                value={props.element?.stepId || ''}
                                                placeholder={t('flow.orchestration.stepIdPlaceholder')}
                                                onChange={handleStepIdChange}
                                                class={styles.stepIdInput}
                                                disabled={!props.editable}
                                            />
                                        ),
                                    }}
                                </FormItem>
                            )}

                            {/* Plugin type and version displayed side by side */}
                            <div class={styles.flexRow}>
                                <FormItem>
                                    {{
                                        label: () => (
                                            <div class={styles.labelWithIcon}>
                                                <span>{t('flow.orchestration.atomLabel')}</span>
                                                {isAtomSelected.value && docsLink.value && (
                                                    <a
                                                        href={docsLink.value}
                                                        target="_blank"
                                                        rel="noopener noreferrer"
                                                        class={sharedStyles.atomLink}
                                                        onClick={handleDocLinkClick}
                                                    >
                                                        {t('flow.orchestration.atomHelpDoc')}
                                                        <SvgIcon name="tiaozhuan" size={14} />
                                                    </a>
                                                )}
                                            </div>
                                        ),
                                        default: () => (
                                            <div class={styles.atomTypeSelector}>
                                                {isAtomSelected.value ? (
                                                    <div class={styles.atomSelectEntry}>
                                                        <Input
                                                            modelValue={atomDisplayName.value}
                                                            readonly
                                                            class={styles.atomNameInput}
                                                        />
                                                        {props.showAtomSelector && props.editable && (
                                                            <Button
                                                                theme="primary"
                                                                class={styles.reselectBtn}
                                                                onClick={handleChooseAtom}
                                                            >
                                                                {t('flow.orchestration.reSelect')}
                                                            </Button>
                                                        )}
                                                    </div>
                                                ) : (
                                                    props.showAtomSelector &&
                                                    props.editable && (
                                                        <Button theme="primary" onClick={handleChooseAtom}>
                                                            {t('flow.orchestration.choosePlugin')}
                                                        </Button>
                                                    )
                                                )}
                                            </div>
                                        ),
                                    }}
                                </FormItem>

                                {isAtomSelected.value &&
                                    props.showVersionSelector &&
                                    computedVersionList.value.length > 0 && (
                                        <FormItem>
                                            {{
                                                label: () => (
                                                    <div class={styles.labelWithIcon}>
                                                        <span>{t('flow.content.version')}</span>
                                                        <Popover
                                                            content={t('flow.orchestration.atomVersionDesc')}
                                                            placement="top"
                                                        >
                                                            <span class={sharedStyles.infoIcon}>
                                                                <SvgIcon name="info-circle" size={14} />
                                                            </span>
                                                        </Popover>
                                                    </div>
                                                ),
                                                default: () => (
                                                    <Select
                                                        modelValue={atomVersionValue.value}
                                                        onChange={handleVersionChange}
                                                        loading={isLoadingVersion.value}
                                                        list={computedVersionList.value}
                                                        clearable={false}
                                                        disabled={!props.editable}
                                                    ></Select>
                                                ),
                                            }}
                                        </FormItem>
                                    )}
                            </div>

                            {isAtomSelected.value && (
                                <>
                                    {/* Plugin config form */}
                                    {atomPropsModel.value && Object.keys(atomPropsModel.value).length > 0 && (
                                        <AtomForm
                                            atomPropsModel={atomPropsModel.value}
                                            atomValue={atomValue.value}
                                            element={props.element!}
                                            onChange={handleConfigChange}
                                            disabled={!props.editable}
                                        />
                                    )}

                                    {atomModal.value &&
                                        (!atomPropsModel.value ||
                                            Object.keys(atomPropsModel.value).length === 0) && (
                                            <div class={styles.noConfig}>{t('flow.orchestration.noConfig')}</div>
                                        )}

                                    {!atomModal.value && !isLoadingModal.value && (
                                        <div class={styles.loadError}>
                                            {t('flow.orchestration.loadConfigFailed')}
                                        </div>
                                    )}

                                    {/* Custom environment variables */}
                                    {props.showCustomEnvSection && (
                                        <div class={styles.customEnvSection}>
                                            <Collapse useBlockTheme>
                                                <CollapsePanel>
                                                    {{
                                                        default: () => (
                                                            <div class={sharedStyles.collapseHeader}>
                                                                <span>{t('flow.orchestration.customEnv')}</span>
                                                            </div>
                                                        ),
                                                        content: () => (
                                                            <div class={sharedStyles.collapseContent}>
                                                                <KeyValueMap
                                                                    value={customEnv.value}
                                                                    name="customEnv"
                                                                    handleChange={handleCustomEnvChangeWrapper}
                                                                    addBtnText={t('flow.orchestration.addVariable')}
                                                                    keyPlaceholder={t('flow.orchestration.envKeyPlaceholder')}
                                                                    valuePlaceholder={t('flow.orchestration.envValuePlaceholder')}
                                                                    disabled={!props.editable}
                                                                />
                                                            </div>
                                                        ),
                                                    }}
                                                </CollapsePanel>
                                            </Collapse>
                                        </div>
                                    )}

                                    {/* Flow control options */}
                                    {props.showFlowControlSection && (
                                        <div class={styles.processControlSection}>
                                            <Collapse modelValue={activeIndex.value} useBlockTheme>
                                                <CollapsePanel name="flowControl">
                                                    {{
                                                        default: () => (
                                                            <div class={sharedStyles.collapseHeader}>
                                                                <span>{t('flow.orchestration.flowControlOptions')}</span>
                                                            </div>
                                                        ),
                                                        content: () => (
                                                            <div class={sharedStyles.collapseContent}>
                                                                <Form labelWidth={120} formType="vertical">
                                                                    {/* Enable this plugin */}
                                                                    <FormItem>
                                                                        <AtomCheckbox
                                                                            value={additionalOptions.value.enable ?? true}
                                                                            name="enable"
                                                                            text={t('flow.orchestration.enableAtom')}
                                                                            handleChange={handleEnableChange}
                                                                            disabled={!props.editable}
                                                                        />
                                                                    </FormItem>

                                                                    {/* When this plugin fails */}
                                                                    {additionalOptions.value.enable && (
                                                                        <FormItem>
                                                                            <div class={styles.failControlLabel}>
                                                                                {t('flow.orchestration.whenAtomFailed')}
                                                                            </div>
                                                                            <AtomCheckboxList
                                                                                value={failControlValue.value}
                                                                                name="failControl"
                                                                                list={getAtomFailControlList(t)}
                                                                                handleChange={handleFailControlChange}
                                                                                disabled={!props.editable}
                                                                            />
                                                                        </FormItem>
                                                                    )}

                                                                    {/* Plugin execution timeout */}
                                                                    {additionalOptions.value.enable && (
                                                                        <FormItem>
                                                                            {{
                                                                                label: () => (
                                                                                    <div class={sharedStyles.labelWithIcon}>
                                                                                        <span>{t('flow.orchestration.atomTimeout')}</span>
                                                                                        <Popover
                                                                                            content={t('flow.orchestration.timeoutDesc')}
                                                                                            placement="top"
                                                                                        >
                                                                                            <span class={sharedStyles.infoIcon}>
                                                                                                <SvgIcon name="info-circle" size={14} />
                                                                                            </span>
                                                                                        </Popover>
                                                                                    </div>
                                                                                ),
                                                                                default: () => (
                                                                                    <Input
                                                                                        value={additionalOptions.value.timeoutVar || '900'}
                                                                                        placeholder={t('flow.orchestration.timeoutPlaceholder')}
                                                                                        onChange={handleTimeoutChange}
                                                                                        disabled={!props.editable}
                                                                                    />
                                                                                ),
                                                                            }}
                                                                        </FormItem>
                                                                    )}

                                                                    {/* When to run this plugin */}
                                                                    {additionalOptions.value.enable && (
                                                                        <>
                                                                            <FormItem>
                                                                                {{
                                                                                    label: () => (
                                                                                        <div class={sharedStyles.labelWithIcon}>
                                                                                            <span>
                                                                                                {t('flow.orchestration.atomRunCondition')}
                                                                                            </span>
                                                                                        </div>
                                                                                    ),
                                                                                    default: () => (
                                                                                        <Select
                                                                                            modelValue={
                                                                                                additionalOptions.value.runCondition ||
                                                                                                'PRE_TASK_SUCCESS'
                                                                                            }
                                                                                            onChange={handleRunConditionChange}
                                                                                            clearable={false}
                                                                                            disabled={!props.editable}
                                                                                        >
                                                                                            {getAtomRunConditionList(t).map((item) => (
                                                                                                <Option
                                                                                                    key={item.value}
                                                                                                    value={item.value}
                                                                                                    label={item.label}
                                                                                                />
                                                                                            ))}
                                                                                        </Select>
                                                                                    ),
                                                                                }}
                                                                            </FormItem>

                                                                            {/* Custom variables input - shown when variable match is selected */}
                                                                            {showCustomVariables.value && (
                                                                                <FormItem>
                                                                                    {{
                                                                                        label: () => (
                                                                                            <div class={sharedStyles.labelWithIcon}>
                                                                                                <span>
                                                                                                    {t('flow.orchestration.customVar')}
                                                                                                </span>
                                                                                            </div>
                                                                                        ),
                                                                                        default: () => (
                                                                                            <KeyValueMap
                                                                                                value={customVariables.value}
                                                                                                name="customVariables"
                                                                                                handleChange={handleAdditionalOptionsChange}
                                                                                                addBtnText={t('flow.orchestration.addVariable')}
                                                                                                keyPlaceholder={t(
                                                                                                    'flow.orchestration.envKeyPlaceholder',
                                                                                                )}
                                                                                                valuePlaceholder={t(
                                                                                                    'flow.orchestration.envValuePlaceholder',
                                                                                                )}
                                                                                                allowNull={false}
                                                                                                disabled={!props.editable}
                                                                                            />
                                                                                        ),
                                                                                    }}
                                                                                </FormItem>
                                                                            )}

                                                                            {/* Custom condition expression input - shown when expression is selected */}
                                                                            {showCustomCondition.value && (
                                                                                <FormItem>
                                                                                    {{
                                                                                        label: () => (
                                                                                            <div class={sharedStyles.labelWithIcon}>
                                                                                                <span>
                                                                                                    {t('flow.orchestration.customConditionExp')}
                                                                                                </span>
                                                                                            </div>
                                                                                        ),
                                                                                        default: () => (
                                                                                            <Input
                                                                                                value={customCondition.value}
                                                                                                placeholder={t(
                                                                                                    'flow.orchestration.customConditionExpPlaceholder',
                                                                                                )}
                                                                                                onChange={handleCustomConditionChange}
                                                                                                disabled={!props.editable}
                                                                                            />
                                                                                        ),
                                                                                    }}
                                                                                </FormItem>
                                                                            )}
                                                                        </>
                                                                    )}
                                                                </Form>
                                                            </div>
                                                        ),
                                                    }}
                                                </CollapsePanel>
                                            </Collapse>
                                        </div>
                                    )}
                                </>
                            )}
                        </Form>
                    </div>
                </Loading>
            </div>
        )
    },
})
