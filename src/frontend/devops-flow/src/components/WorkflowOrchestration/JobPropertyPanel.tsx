import type { Container, Stage } from '@/api/flowModel'
import { SvgIcon } from '@/components/SvgIcon'
import { useUIStore } from '@/stores/ui'
import {
    Button,
    Form,
    InfoBox,
    Input,
    Sideslider,
} from 'bkui-vue'
import { storeToRefs } from 'pinia'
import { computed, defineComponent, ref, watch, type PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import JobPropertyContent from './JobPropertyContent'
import styles from './JobPropertyPanel.module.css'
import sharedStyles from './shared.module.css'

export default defineComponent({
    name: 'JobPropertyPanel',
    props: {
        modelValue: {
            type: Boolean,
            default: false,
        },
        editingContainer: {
            type: Object as PropType<Container | null>,
            default: null,
        },
        /** Current Job's parent Stage, for getting dependent Job list */
        stage: {
            type: Object as PropType<Stage | null>,
            default: null,
        },
        /** Current Job index in the Stage */
        containerIndex: {
            type: Number,
            default: -1,
        },
        editable: {
            type: Boolean,
            default: true,
        },
        isNew: {
            type: Boolean,
            default: false,
        },
        /** Whether it's a Finally Stage */
        isFinally: {
            type: Boolean,
            default: false,
        },
    },
    emits: ['update:modelValue', 'confirm'],
    setup(props, { emit }) {
        const { t } = useI18n()
        const { isVariablePanelOpen } = storeToRefs(useUIStore())

        // ========== State ==========
        const formRef = ref()
        const formData = ref<Container | null>(null)
        const nameEditing = ref(false)

        // ========== Computed ==========
        // Show different title based on Job type
        const title = computed(() => {
            if (!props.isNew) {
                return t('flow.orchestration.editJob')
            }
            // In new mode, show different title based on container's @type
            const containerType = formData.value?.['@type']
            if (containerType === 'normal') {
                return t('flow.orchestration.addCloudJob')
            }
            return t('flow.orchestration.addCreateJob')
        })

        // ========== Watchers ==========
        // Sync props.editingContainer to formData
        watch(
            () => props.editingContainer,
            (container) => {
                formData.value = Object.assign({}, (formData.value || {}), container)
            },
            { immediate: true },
        )

        // ========== Helpers ==========
        function closePanel() {
            emit('update:modelValue', false)
        }

        function exitNameEdit() {
            if (props.editable) nameEditing.value = false
        }

        // ========== Handlers ==========
        function handleContainerChange(container: Container) {
            // Prevent recursive updates by checking if the container has actually changed
            if (JSON.stringify(formData.value) !== JSON.stringify(container)) {
                formData.value = container
            }
        }

        async function handleConfirm() {
            try {
                await formRef.value?.validate()
            } catch {
                InfoBox({
                    title: t('flow.common.failed'),
                    subTitle: t('flow.orchestration.jobIdRequired'),
                    theme: 'danger',
                })
                return
            }

            if (formData.value) {
                emit('confirm', formData.value)
                closePanel()
            }
        }

        function beforeClose() {
            if (!props.isNew) {
                emit('confirm', formData.value)
            }
            return true
        }

        // ========== Render ==========
        return () => (
            <Sideslider
                isShow={props.modelValue}
                width={640}
                quick-close
                onUpdate:isShow={(val: boolean) => emit('update:modelValue', val)}
                class={['bkci-property-panel', isVariablePanelOpen.value && 'with-variable-open']}
                beforeClose={beforeClose}
            >
                {{
                    header: () => (
                        <div class={sharedStyles.propertyPanelHeader}>
                            {props.isNew ? (
                                <span>{title.value}</span>
                            ) : (
                                <div class={sharedStyles.nameEdit}>
                                    {nameEditing.value ? (
                                        <Input
                                            modelValue={formData.value?.name || ''}
                                            maxlength={30}
                                            placeholder={t('flow.orchestration.jobNamePlaceholder')}
                                            onBlur={exitNameEdit}
                                            onEnter={exitNameEdit}
                                            onChange={(val: string) => formData.value && (formData.value.name = val)}
                                            class={sharedStyles.nameInput}
                                            autoFocus
                                        />
                                    ) : (
                                        <>
                                            <p class={sharedStyles.nameText} title={formData.value?.name}>
                                                {formData.value?.name}
                                            </p>
                                            {props.editable && (
                                                <span class={sharedStyles.editIcon} onClick={() => (nameEditing.value = true)}>
                                                    <SvgIcon name="edit" size={16} />
                                                </span>
                                            )}
                                        </>
                                    )}
                                </div>
                            )}
                        </div>
                    ),

                    default: () =>
                        formData.value && (
                            <Form ref={formRef} form-type="vertical" model={formData.value}>
                                <JobPropertyContent
                                    container={formData.value}
                                    stage={props.stage}
                                    containerIndex={props.containerIndex}
                                    editable={props.editable}
                                    isNew={props.isNew}
                                    isFinally={props.isFinally}
                                    showNameField={props.isNew}
                                    showJobIdField={true}
                                    onChange={handleContainerChange}
                                />
                            </Form>
                        ),

                    footer: () =>
                        props.isNew && (
                            <div class={styles.jobPanelFooter}>
                                <Button theme="primary" onClick={handleConfirm} disabled={!props.editable}>
                                    {t('flow.orchestration.add')}
                                </Button>
                                <Button onClick={closePanel}>{t('flow.common.cancel')}</Button>
                            </div>
                        ),
                }}
            </Sideslider>
        )
    },
})
