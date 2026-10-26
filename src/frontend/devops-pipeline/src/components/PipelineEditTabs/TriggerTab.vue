<template>
    <div>
        <span
            v-if="$route.meta.edit"
            v-bk-tooltips="{
                disabled: !pipeline?.instanceFromTemplate,
                content: $t('constraintTriggerTips')
            }"
        >

            <bk-button
                :disabled="!editable || pipeline?.instanceFromTemplate"
                theme="primary"
                @click="addTrigger"
            >
                {{ $t('settings.addTrigger') }}
            </bk-button>
        </span>
        <div class="trigger-list-wrapper">
            <bk-table :data="triggerList">
                <bk-table-column
                    :label="$t('settings.trigger')"
                    prop="name"
                    show-overflow-tooltip
                >
                    <template slot-scope="props">
                        <span
                            @click="editTriggerAtom(props.$index)"
                            :class="{ 'text-link': true, 'is-error': !props.row.name || props.row.isError }"
                        >
                            <template v-if="isYamlBranchInactiveRow(props.row)">
                                <span
                                    class="yaml-branch-inactive-tag"
                                >
                                    {{ $t('editPage.yamlBranchInactiveTag') }}
                                </span>
                            </template>
                            <template v-else>
                                {{ props.row.name || $t('settings.selectTrigger') }}
                            </template>
                        </span>
                    </template>
                </bk-table-column>
                <bk-table-column :label="$t('settings.enableStatus')">
                    <template slot-scope="props">
                        <ConstraintWraper
                            classify="triggerStepIds"
                            :field="props.row.stepId"
                            :space-between="false"
                            :show-constraint-area="false"
                            @toggleConstraint="(isOverride) => setIsOverrideField(props.$index, isOverride)"
                        >
                            <template #constraint-title="{ props: { isOverride } }">
                                <bk-switcher
                                    :disabled="!(editable || isOverride)"
                                    :value="getIsEnable(props.row)"
                                    theme="primary"
                                    size="small"
                                    @change="(val) => handleUpdateOptions(props.$index, 'enable', val)"
                                ></bk-switcher>
                            </template>
                        </ConstraintWraper>
                    </template>
                </bk-table-column>
                <bk-table-column
                    :label="$t('operate')"
                    width="150"
                    class-name="handler-btn"
                >
                    <span
                        slot-scope="props"
                        v-bk-tooltips="{
                            disabled: !pipeline?.instanceFromTemplate,
                            content: $t('constraintTriggerUnDeleteTips')
                        }"
                    >
                        
                        <bk-button
                            text
                            :disabled="!editable || pipeline?.instanceFromTemplate"
                            @click="deleteTriggerAtom(props.$index)"
                        >
                            {{ $t('delete') }}
                        </bk-button>
                    </span>
                </bk-table-column>
            </bk-table>
        </div>
        <atom-selector
            v-if="container"
            :container="container"
            :element="element"
            v-bind="editingElementPos"
        />
        <template v-if="editingElementPos">
            <atom-property-panel
                :element-index="editingElementPos.elementIndex"
                :container-index="0"
                :stage-index="0"
                :editable="editable"
                :stages="pipeline.stages"
                :is-instance-template="pipeline?.instanceFromTemplate"
            />
        </template>
    </div>
</template>

<script>
    import ConstraintWraper from '@/components/ConstraintWraper.vue'
    import { mapActions, mapGetters, mapState } from 'vuex'
    import AtomPropertyPanel from '../AtomPropertyPanel'
    import AtomSelector from '../AtomSelector'
    import { getCurrentYamlBranch, isYamlBranchInactive } from '../AtomPropertyPanel/TimerTrigger/yamlBranchMixin'


    export default {
        components: {
            AtomPropertyPanel,
            AtomSelector,
            ConstraintWraper
        },
        props: {
            editable: {
                type: Boolean,
                default: true
            },
            pipeline: {
                type: Object,
                required: true
            }
        },
        data () {
            return {
                element: null
            }
        },
        computed: {
            ...mapState('atom', [
                'editingElementPos',
                'showAtomSelectorPopup',
                'pipelineInfo',
                'activePipelineVersion'
            ]),
            ...mapGetters('atom', ['isBranchVersion']),
            container () {
                return this.pipeline?.stages[0]?.containers[0] || {}
            },
            triggerList () {
                try {
                    return this.container?.elements || []
                } catch (err) {
                    return []
                }
            },
            // 当前查看/编辑的 YAML 配置所在分支名（仅分支版本场景有值）
            currentYamlBranch () {
                return getCurrentYamlBranch({
                    isBranchVersion: this.isBranchVersion,
                    activePipelineVersion: this.activePipelineVersion,
                    pipelineInfo: this.pipelineInfo
                })
            }
        },
        watch: {
            showAtomSelectorPopup: {
                handler (show) {
                    if (!show && !this.editingElementPos) {
                        this.triggerList.forEach((element, index) => {
                            if (!element.atomCode) {
                                this.deleteAtom({
                                    container: this.container,
                                    atomIndex: index
                                })
                            }
                        })
                    }
                }
            },
            editingElementPos (val) {
                if (!val) {
                    this.element = null
                }
            }
        },
        methods: {
            ...mapActions('atom', [
                'toggleAtomSelectorPopup',
                'addAtom',
                'updateAtom',
                'deleteAtom',
                'togglePropertyPanel'
            ]),
            setIsOverrideField (index, isOverride) {
                this.handleAtomChange(index, 'isOverride', isOverride)
            },
            getIsEnable (row) {
                return row?.additionalOptions?.enable ?? true
            },
            // 定时触发器配置了 yaml-branch 且与当前配置所在分支不一致：标记未生效定时
            isYamlBranchInactiveRow (row) {
                if (this.currentYamlBranch === '') {
                    return false
                }
                return isYamlBranchInactive({
                    value: (row?.yamlBranch || '').trim(),
                    currentBranch: this.currentYamlBranch,
                    defaultBranch: this.pipelineInfo?.yamlInfo?.defaultBranch || ''
                })
            },
            handleUpdateOptions (index, key, val) {
                const element = this.triggerList[index]
                const options = element?.additionalOptions || {}
                Object.assign(options, { [key]: val })
                this.handleAtomChange(index, 'additionalOptions', options)
            },
            handleAtomChange (index, key, val) {
                const element = this.triggerList[index]
                
                this.updateAtom({
                    element: element,
                    newParam: {
                        [key]: val
                    }
                })
            },
            editTriggerAtom (index) {
                this.element = this.triggerList[index]
                this.togglePropertyPanel({
                    isShow: true,
                    editingElementPos: {
                        stageIndex: 0,
                        containerIndex: 0,
                        elementIndex: index
                    }
                })
            },
            deleteTriggerAtom (index) {
                if (this.triggerList.length <= 1) {
                    this.$bkMessage({
                        theme: 'error',
                        message: this.$t('triggerLeastOne'),
                        limit: 1
                    })
                    return
                }
                this.deleteAtom({
                    container: this.container,
                    atomIndex: index
                })
            },
            addTrigger () {
                this.addAtom({
                    stageIndex: 0,
                    containerIndex: 0,
                    atomIndex: this.triggerList.length - 1,
                    container: this.container
                })
                this.toggleAtomSelectorPopup(true)
            }
        }
    }
</script>

<style lang="scss" scoped>
    .is-error {
        color: #ff5656;
    }
    .yaml-branch-inactive-tag {
        display: inline-block;
        color: #ea3636;
        font-size: 12px;
        line-height: 18px;
        cursor: pointer;
    }
    .link-btn {
        cursor: pointer;
        color: #3a84ff;
    }
    .trigger-list-wrapper {
        width: 800px;
        margin: 16px 0 30px;
        cursor: default;
    }
</style>
