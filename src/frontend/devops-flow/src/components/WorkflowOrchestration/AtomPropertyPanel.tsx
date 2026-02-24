import type { Element } from '@/api/flowModel'
import { SvgIcon } from '@/components/SvgIcon'
import { useUIStore } from '@/stores/ui'
import { Input, Sideslider } from 'bkui-vue'
import { storeToRefs } from 'pinia'
import { computed, defineComponent, type PropType, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import AtomPropertyContent from './AtomPropertyContent'
import sharedStyles from './shared.module.css'

export default defineComponent({
    name: 'AtomPropertyPanel',
    props: {
        visible: {
            type: Boolean,
            default: false,
        },
        currentElement: {
            type: Object as PropType<Element | null>,
            default: null,
        },
        editable: {
            type: Boolean,
            default: true,
        },
    },
    emits: ['update:visible', 'chooseAtom', 'updateAtom'],
    setup(props, { emit }) {
        // ========== Hooks ==========
        const { t } = useI18n()
        const uiStore = useUIStore()
        const { isVariablePanelOpen } = storeToRefs(uiStore)

        // ========== Refs ==========
        const nameEditing = ref(false)

        // ========== Computed ==========
        const atomCode = computed(() => {
            const element = props.currentElement
            if (!element) return ''
            const isThird = element.atomCode && element['@type'] !== element.atomCode
            return isThird ? element.atomCode : element['@type'] || ''
        })

        const isAtomSelected = computed(() => {
            return !!atomCode.value && !!props.currentElement
        })

        // ========== Functions ==========
        function getAtomName() {
            if (props.currentElement?.name) {
                return props.currentElement.name
            }
            if (atomCode.value) {
                return atomCode.value
            }
            return ''
        }

        function handleClose() {
            emit('update:visible', false)
        }

        function toggleEditName(show: boolean) {
            nameEditing.value = show
        }

        function handleEditName(value: string) {
            if (!props.currentElement) return
            const element = { ...props.currentElement, name: value }
            emit('updateAtom', element)
        }

        function handleElementChange(element: Element) {
            debugger
            emit('updateAtom', element)
        }

        function handleChooseAtom() {
            emit('chooseAtom')
        }

        function handleBlur() {
            toggleEditName(false)
        }

        function handleEnter() {
            toggleEditName(false)
        }

        function handleNameChange(val: string) {
            handleEditName(val)
        }

        function handleEditIconClick() {
            toggleEditName(true)
        }

        return () => (
            <Sideslider
                isShow={props.visible}
                width={640}
                transfer
                onClosed={handleClose}
                class={['bkci-property-panel', isVariablePanelOpen.value && 'with-variable-open']}
            >
                {{
                    header: () => (
                        <div class={sharedStyles.propertyPanelHeader}>
                            <div class={sharedStyles.atomNameEdit}>
                                {props.editable && nameEditing.value ? (
                                    <Input
                                        value={getAtomName()}
                                        maxlength={30}
                                        placeholder={t('flow.orchestration.atomNamePlaceholder')}
                                        onBlur={handleBlur}
                                        onEnter={handleEnter}
                                        onChange={handleNameChange}
                                        class={sharedStyles.nameInput}
                                    />
                                ) : (
                                    <>
                                        <p class={sharedStyles.atomNameText} title={getAtomName()}>
                                            {isAtomSelected.value
                                                ? getAtomName()
                                                : t('flow.orchestration.waitingSelectAtom')}
                                        </p>
                                        {props.editable && isAtomSelected.value && (
                                            <span class={sharedStyles.editIcon} onClick={handleEditIconClick}>
                                                <SvgIcon name="edit" size={16} />
                                            </span>
                                        )}
                                    </>
                                )}
                            </div>
                        </div>
                    ),
                    default: () => (
                        <AtomPropertyContent
                            element={props.currentElement}
                            editable={props.editable}
                            showAtomSelector={props.editable}
                            showStepIdField={true}
                            showVersionSelector={true}
                            showCustomEnvSection={true}
                            showFlowControlSection={true}
                            onChange={handleElementChange}
                            onChooseAtom={handleChooseAtom}
                        />
                    ),
                }}
            </Sideslider>
        )
    },
})
