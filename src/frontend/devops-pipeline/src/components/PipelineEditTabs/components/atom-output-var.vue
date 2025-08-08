<template>
    <div class="variable-container">
        <bk-alert
            v-if="editable"
            type="info"
            :title="$t('newui.atomVarTips')"
            closable
        ></bk-alert>
        <div class="operate-row">
            <bk-input
                v-model="searchStr"
                :clearable="true"
                :placeholder="$t('newui.searchAtomVar')"
                :right-icon="'bk-icon icon-search'"
            />
        </div>
        <param-group
            v-for="(group, index) in renderOutputList"
            :key="group.key"
            :show-content="group.isOpen === true || index === 0"
        >
            <div
                class="atom-group-header"
                slot="header"
                :class="{ 'disabled-header': group.disableHeader }"
            >
                <div class="env-name flex-item">
                    <bk-icon
                        class="toggle-icon"
                        type="right-shape"
                    />
                    <span
                        class="group-title"
                        :class="{ 'title-overflow': !group.stepId }"
                        v-bk-tooltips="{ content: group.title, maxWidth: 300, disabled: group.stepId, allowHTML: false }"
                    >
                        {{ group.title }}
                    </span>
                </div>
                <div
                    v-if="!group.stepId && editable"
                    @click.stop
                    class="flex-item step-tips"
                >
                    <bk-icon type="exclamation-circle-shape" />
                    <span>{{ $t('newui.noStepidTips') }}
                        <bk-popconfirm
                            trigger="click"
                            ext-cls="step-pop-confirm"
                            :ext-popover-cls="{ 'disabled-confirm-button': !editStepId || errors.has('step.editStepId') }"
                            width="280"
                            :confirm-text="$t('save')"
                            @confirm="handleUpdateStepId"
                            @cancel="resetStep"
                        >
                            <div slot="content">
                                <form-field
                                    label="Step ID"
                                    style="margin-bottom: 16px;"
                                    :hide-colon="true"
                                    :is-error="errors.has('step.editStepId')"
                                    :desc="$t('editPage.stepIdDesc')"
                                    :error-msg="errors.first('step.editStepId')"
                                >
                                    <vuex-input
                                        style="margin-top: 6px;"
                                        name="editStepId"
                                        :value.sync="editStepId"
                                        :handle-change="(name, value) => editStepId = value"
                                        data-vv-scope="step"
                                        v-validate="`required|varRule|notInList:${allStepId}`"
                                    >
                                    </vuex-input>
                                </form-field>
                            </div>
                            <a
                                class="edit-step-span"
                                @click="location = group.location"
                            >{{ $t('newui.setNow') }}</a>
                        </bk-popconfirm>
                    </span>
                </div>
                <span
                    v-else
                    class="flex-item item-num"
                >{{ group.params.length }}</span>
            </div>
            <section slot="content">
                <template v-for="env in group.params">
                    <env-item
                        :key="env.name"
                        :name="env.name"
                        :desc="env.desc"
                        :editable="editable"
                        :copy-prefix="group.envPrefix"
                        :disabled-copy="!group.stepId || group.disableHeader"
                        :disabled-copy-tips="group.disableHeader ? $t('newui.stepUseWarn') : ''"
                    />
                </template>
            </section>
        </param-group>
    </div>
</template>

<script>
    import FormField from '@/components/AtomPropertyPanel/FormField'
    import VuexInput from '@/components/atomFormField/VuexInput'
    import { mapState, mapGetters, mapActions } from 'vuex'
    import ParamGroup from './children/param-group'
    import EnvItem from './children/env-item'
    export default {
        components: {
            FormField,
            VuexInput,
            ParamGroup,
            EnvItem
        },
        props: {
            stages: {
                type: Array,
                default: () => ([])
            },
            editable: {
                type: Boolean,
                default: true
            }
        },
        data () {
            return {
                searchStr: '',
                editStepId: '',
                location: {}
            }
        },
        computed: {
            ...mapState('atom', [
                'editingElementPos',
                'atomsOutputMap'
            ]),
            ...mapGetters('atom', [
                'getStage',
                'getContainer',
                'getElement'
            ]),
            editingEleIndex () {
                if (!this.editingElementPos) {
                    return 0
                } else {
                    const { stageIndex = 0, containerIndex = 0, elementIndex = 0 } = this.editingElementPos
                    return parseInt(`${stageIndex}${containerIndex}${elementIndex}`)
                }
            },
            currentStage () {
                return this.getStage(this.stages, this.location?.stageIndex) || {}
            },
            currentContainer () {
                return this.getContainer(this.currentStage?.containers, this.location.containerIndex) || {}
            },
            currentElement () {
                return this.getElement(this.currentContainer, this.location.elementIndex) || {}
            },
            allStepId () {
                const stepIdList = []
                const elements = this.currentContainer?.elements || []
                elements.forEach(ele => {
                    if (ele.stepId) {
                        stepIdList.push(ele.stepId)
                    }
                })
                return stepIdList
            },
            outputAtomList () {
                const list = []
                this.stages.forEach((stage, stageIndex) => {
                    if (stage) {
                        (stage.containers || []).forEach((container, containerIndex) => {
                            (container.elements || []).forEach((element, elementIndex) => {
                                // 从api获取的output信息
                                const apiOutput = this.atomsOutputMap[`${element.atomCode}@${element.version}`] || {}
                                // 从model解析的output信息
                                const modelOutput = element?.data?.output || {}
                                if (Object.keys(modelOutput).length || Object.keys(apiOutput).length) {
                                    const realOutput = Object.keys(apiOutput).length > 0 ? apiOutput : modelOutput
                                    list.push({
                                        id: element.id,
                                        location: {
                                            stageIndex,
                                            containerIndex,
                                            elementIndex
                                        },
                                        totalIndex: parseInt(`${stageIndex}${containerIndex}${elementIndex}`),
                                        title: `${stageIndex + 1}-${containerIndex + 1}-${elementIndex + 1}-${element.name}`,
                                        version: element.version,
                                        stepId: element.stepId,
                                        stepName: element.name,
                                        envPrefix: `jobs.${container.jobId}.steps.${element.stepId}.outputs.`,
                                        params: Object.keys(realOutput).map(item => ({
                                            name: item,
                                            desc: realOutput[item]?.description
                                        }))
                                    })
                                }
                            })
                        })
                    }
                })
                return list
            },
            renderOutputList () {
                return this.outputAtomList.map((group, index) => ({
                    ...group,
                    disableHeader: this.editingElementPos && group.totalIndex >= this.editingEleIndex,
                    ...(
                        this.searchStr
                            ? {
                                params: group.stepName?.includes(this.searchStr) ? group.params : group.params?.filter(item => item.name.includes(this.searchStr) || item.desc.includes(this.searchStr)),
                                isOpen: group.stepName?.includes(this.searchStr) || group.params?.filter(item => item.name.includes(this.searchStr) || item.desc.includes(this.searchStr)).length > 0
                            }
                            : {}
                    )
                }))
            }
        },
        created () {
            this.fetchAtomsOutput()
        },
        methods: {
            ...mapActions('atom', [
                'updateAtom',
                'fetchAtomsOutput'
            ]),
            async handleUpdateStepId () {
                const valid = await this.$validator.validate('step.*')
                if (valid) {
                    this.updateAtom({
                        element: this.currentElement,
                        newParam: {
                            stepId: this.editStepId
                        }
                    })
                    this.resetStep()
                }
            },
            resetStep () {
                this.location = {}
                this.editStepId = ''
            }
        }
    }
</script>

<style lang="scss">
    .step-pop-confirm {
        z-index: 2019 !important;
    }
    .disabled-confirm-button.bk-popconfirm-content .popconfirm-operate button[type=button].primary {
        pointer-events: none;
        opacity: 0.5;
    }
    .edit-step-span {
        font-size: 12px;
        color: #3A84FF;
        cursor: pointer;
    }
    .atom-group-header.disabled-header {
        cursor: not-allowed;
        background-color: #EAEBF0;
        color: #C4C6CC;
        .toggle-icon {
            color: #DCDEE5;
        }
    }
    .atom-group-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        width: 100%;
        .flex-item {
            display: flex;
            align-items: center;
        }
        .env-name {
            flex: 1;
            .group-title {
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;
                max-width: 350px;
            }
            .group-title.title-overflow {
                max-width: 208px;
            }
        }
        .step-tips {
            cursor: default;
            i {
                font-size: 14px;
                color: #FF9C01;
            }
            span {
                font-size: 12px;
                color: #979BA5;
            }
        }
    }
</style>
