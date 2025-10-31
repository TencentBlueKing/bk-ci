<template>
    <div class="variable-container">
        <div
            class="container-top"
        >
            <bk-alert
                v-if="editable"
                type="info"
                :title="$t('newui.pipelineParam.useTips')"
                closable
                @close="alertClose"
            ></bk-alert>
            <div class="operate-row">
                <template v-if="editable">
                    <bk-button
                        class="var-btn"
                        v-enStyle="'min-width:100px'"
                        @click="handleAdd"
                    >
                        {{ $t('newui.pipelineParam.addVar') }}
                    </bk-button>
                    <bk-button
                        class="var-btn"
                        v-enStyle="'min-width:100px'"
                        @click="handleAdd('constant')"
                    >
                        {{ $t('newui.pipelineParam.addConst') }}
                    </bk-button>
                </template>
                <bk-input
                    class="search-input"
                    v-model="searchStr"
                    :clearable="true"
                    :placeholder="$t('newui.pipelineParam.searchPipelineVar')"
                    :right-icon="'bk-icon icon-search'"
                />
            </div>
        </div>

        <div
            class="container-bottom"
            :style="{ marginTop: `${offsetData}px` }"
            v-if="!showSlider"
        >
            <param-group
                v-for="group in pipelineParamGroups"
                v-bind="group"
                :key="group.key"
                :editable="editable"
                :handle-edit="handleEdit"
                :handle-update="handleUpdate"
                :handle-sort="handleSort"
            />
        </div>

        <div
            v-else-if="editable"
            class="current-edit-param-item"
        >
            <div class="edit-var-header">
                <bk-icon
                    style="font-size: 28px;"
                    type="arrows-left"
                    class="back-icon"
                    @click="hideSlider"
                />
                {{ sliderTitle }}
            </div>
            <div class="edit-var-content">
                <pipeline-param-form
                    ref="pipelineParamFormRef"
                    :edit-item="sliderEditItem"
                    :global-params="globalParams"
                    :edit-index="editIndex"
                    :param-type="paramType"
                    :update-param="updateEditItem"
                    :reset-edit-item="resetEditItem"
                />
            </div>
            <div
                class="edit-var-footer"
                slot="footer"
            >
                <bk-button
                    theme="primary"
                    @click="handleSaveVar"
                >
                    {{ editIndex === -1 ? $t('editPage.append') : $t('confirm') }}
                </bk-button>
                <bk-button
                    style="margin-left: 8px;"
                    @click="hideSlider"
                >
                    {{ $t('cancel') }}
                </bk-button>
            </div>
        </div>
    </div>
</template>

<script>
    import {
        getParamsGroupByLabel
    } from '@/store/modules/atom/paramsConfig'
    import { allVersionKeyList } from '@/utils/pipelineConst'
    import { deepCopy, navConfirm } from '@/utils/util'
    import ParamGroup from './children/param-group'
    import PipelineParamForm from './pipeline-param-form'

    export default {
        components: {
            PipelineParamForm,
            ParamGroup
        },
        props: {
            params: {
                type: Array,
                required: true
            },
            updateContainerParams: {
                type: Function,
                required: true
            },
            editable: {
                type: Boolean,
                default: true
            }
        },
        data () {
            return {
                showSlider: false,
                editIndex: -1,
                paramType: 'var',
                sliderEditItem: {},
                searchStr: '',
                confirmMsg: this.$t('editPage.closeConfirmMsg'),
                cancelText: this.$t('cancel'),
                isAlertTips: true
            }
        },
        computed: {
            offsetData () {
                return this.editable && this.isAlertTips ? 98 : 63
            },
            versions () {
                return this.params.filter(p => allVersionKeyList.includes(p.id))
            },
            globalParams: {
                get () {
                    return this.params.filter(p => !allVersionKeyList.includes(p.id) && p.id !== 'BK_CI_BUILD_MSG').map(i => ({
                        ...i,
                        category: i.category ?? ''
                    }))
                },
                set (params) {
                    this.updateContainerParams('params', [...params, ...this.versions])
                }
            },
            renderParams () {
                return !this.searchStr ? this.globalParams : this.globalParams.filter(item => (item.id?.includes(this.searchStr) || item.name?.includes(this.searchStr) || item.desc?.includes(this.searchStr)))
            },
            requiredParamList () {
                return this.renderParams.filter(item => !item.constant && item.required)
            },
            constantParamList () {
                return this.renderParams.filter(item => item.constant === true)
            },
            otherParamList () {
                return this.renderParams.filter(item => !item.constant && !item.required)
            },
            pipelineParamGroups () {
                return [
                    {
                        key: 'requiredParam',
                        title: this.$t('newui.pipelineParam.buildParam'),
                        tips: this.$t('newui.pipelineParam.buildParamTips'),
                        listNum: this.requiredParamList.length,
                        listMap: getParamsGroupByLabel(this.requiredParamList).listMap ?? {},
                        sortedCategories: getParamsGroupByLabel(this.requiredParamList).sortedCategories ?? []
                    },
                    {
                        key: 'constantParam',
                        title: this.$t('newui.pipelineParam.constParam'),
                        listNum: this.constantParamList.length,
                        listMap: getParamsGroupByLabel(this.constantParamList).listMap ?? {},
                        sortedCategories: getParamsGroupByLabel(this.constantParamList).sortedCategories ?? []
                    },
                    {
                        key: 'otherParam',
                        title: this.$t('newui.pipelineParam.otherVar'),
                        listNum: this.otherParamList.length,
                        listMap: getParamsGroupByLabel(this.otherParamList).listMap ?? {},
                        sortedCategories: getParamsGroupByLabel(this.otherParamList).sortedCategories ?? []
                    }
                ]
            },
            sliderTitle () {
                return `${this.editIndex === -1 ? this.$t('editPage.append') : this.$t('edit')}${this.paramType === 'constant' ? this.$t('newui.pipelineParam.constTitle') : this.$t('newui.pipelineParam.varTitle')}`
            },
            sortParamsList () {
                return [
                    ...this.flattenMultipleObjects(getParamsGroupByLabel(this.requiredParamList)),
                    ...this.flattenMultipleObjects(getParamsGroupByLabel(this.constantParamList)),
                    ...this.flattenMultipleObjects(getParamsGroupByLabel(this.otherParamList))
                ]
            }
        },
        methods: {
            flattenMultipleObjects (objects) {
                return Object.values(objects).flat()
            },
            initParamsSort () {
                this.updateContainerParams('params', [...this.sortParamsList, ...this.versions])
            },
         
            handleSort (preEleId, newEleId, isPrefix) {
                // 从原列表找出被拖拽的element
                const newEle = this.globalParams.find(item => item.id === newEleId)
                // 从原列表中删除该element
                const oldIndex = this.globalParams.findIndex(item => item.id === newEleId)
                this.globalParams.splice(oldIndex, 1)
                // 把拖拽的element插入到preEleId对应的element前面或后面
                const preEleIndex = this.globalParams.findIndex(item => item.id === preEleId)
                this.globalParams.splice((isPrefix ? preEleIndex : preEleIndex + 1), 0, newEle)
                this.initParamsSort()
                this.updateContainerParams('params', [...this.globalParams, ...this.versions])
            },
            // toTop为true，表示移到最前, 为false为delete操作
            handleUpdate (paramId, toTop = false) {
                if (!this.editable) return
                const index = this.globalParams.findIndex(item => item.id === paramId)
                const item = this.globalParams.find(item => item.id === paramId)
                const preEleIndex = this.globalParams.findIndex(i => i.category === item.category)
                this.globalParams.splice(index, 1)
                toTop && this.globalParams.splice(preEleIndex, 0, item)
                this.updateContainerParams('params', [...this.globalParams, ...this.versions])
            },
            handleAdd (type = 'var') {
                this.editIndex = -1
                this.showSlider = true
                this.sliderEditItem = {}
                this.paramType = type
            },
            handleEdit (paramId) {
                if (!this.editable) return
                this.showSlider = true
                this.editIndex = this.globalParams.findIndex(item => item.id === paramId)
                this.sliderEditItem = deepCopy(this.globalParams.find(item => item.id === paramId) || {})
                this.paramType = this.sliderEditItem?.constant === true ? 'constant' : 'var'
            },
            async validParamOptions () {
                let optionValid = true
                if ((this.sliderEditItem?.type === 'ENUM' || this.sliderEditItem?.type === 'MULTIPLE') && this.sliderEditItem?.payload?.type !== 'remote') {
                    // value为空， 则默认等于key
                    this.sliderEditItem.options?.forEach(item => {
                        if (!item.value) {
                            item.value = item.key
                        }
                    })
                    for (const index of this.sliderEditItem?.options?.keys()) {
                        optionValid = await this.$validator.validate(`option-${index}.*`)
                        if (!optionValid) return optionValid
                    }
                }
                return optionValid
            },
            async handleSaveVar () {
                // 单选、复选类型， 需要先校验options
                const optionValid = await this.validParamOptions()
                this.$validator.validate('pipelineParam.*').then((result) => {
                    const {isInvalid, ...param} = this.sliderEditItem
                    if (result && optionValid) {
                        if (this.editIndex > -1) {
                            this.globalParams[this.editIndex] = param
                        } else {
                            this.globalParams.push(param)
                        }
                        this.updateContainerParams('params', [...this.globalParams, ...this.versions])
                        this.hideSlider(false)
                    }
                })
            },
            updateEditItem (name, value) {
                Object.assign(this.sliderEditItem, { [name]: value })
            },
            resetEditItem (param = {}) {
                this.sliderEditItem = param
            },
            // 关闭前需要check是否需要弹窗确认离开
            hideSlider (needCheckChange = true) {
                const hasChange = this.$refs.pipelineParamFormRef?.isParamChanged()

                const close = () => {
                    this.showSlider = false
                    this.editIndex = -1
                    this.sliderEditItem = {}
                }
                if (needCheckChange && hasChange) {
                    navConfirm({ content: this.confirmMsg, type: 'warning', cancelText: this.cancelText })
                        .then((leave) => {
                            leave && close()
                        })
                } else {
                    close()
                }
            },
            alertClose () {
                this.isAlertTips = false
            }
        }
    }
</script>

<style lang="scss">
    @import "@/scss/mixins/ellipsis.scss";
    .variable-container {
        position: relative;
        display: flex;
        flex-direction: column;
        height: 100%;

        .container-top {
            position: absolute;
            width: 100%;
        }

        .container-bottom {
            width: 100%;
            position: absolute;
            height: calc(100% - 89px);
            overflow-y: auto;
        }
        
        .current-edit-param-item {
            position: fixed;
            top: 48px;

            .edit-var-content {
                height: calc(100% - 138px);
            }

            .edit-var-footer {
                bottom: 48px;
                background-color: #fff;
            }
        }

        .circle {
            width: 10px;
            height: 10px;
            border-radius: 50%;
        }
        .operate-row {
            margin: 16px 0;
            display: flex;
            align-items: center;
            justify-content: space-between;
            .var-btn {
                min-width: 88px;
                width: -webkit-fill-available;
                margin-right: 8px;
            }
            .search-input {
                min-width: 215px;
            }
        }
        .variable-content {
            width: 100%;
            min-height: 64px;
            border: 1px solid #DCDEE5;
            border-bottom: none;
            .variable-empty {
                height: 200px;
                border-bottom: 1px solid #DCDEE5;
                display: flex;
                align-items: center;
                justify-content: center;
                font-size: 14px;
                color:#63656E;
            }
            .variable-item {
                position: relative;
                height: 64px;
                border-bottom: 1px solid #DCDEE5;
                padding-left: 24px;
                display: flex;
                justify-content: space-between;
                align-items: center;
                .var-con {
                    font-size: 12px;
                    letter-spacing: 0;
                    line-height: 20px;
                    flex: 1;
                    overflow: hidden;
                    .var-names {
                        color: #313238;
                        flex-shrink: 0;
                    }
                    .default-value {
                        color: #979BA5;
                        @include ellipsis();
                        width: 100%;
                    }
                }
                .var-operate {
                    .var-status {
                        margin-right: 16px;
                        display: flex;
                        align-items: center;
                        .circle {
                            margin-left: 8px;
                        }
                    }
                    .operate-btns {
                        width: 76px;
                        height: 62px;
                        background-color: #F5F7FA;
                        display: flex;
                        align-items: center;
                        padding: 0 18px;
                        i {
                            cursor: pointer;
                            font-size: 14px;
                            color: #63656E;
                        }
                    }
                }
            }
        }
    }
</style>
