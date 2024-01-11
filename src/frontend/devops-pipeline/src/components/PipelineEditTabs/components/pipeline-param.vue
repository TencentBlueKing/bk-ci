<template>
    <div class="variable-container">
        <bk-alert type="info" :title="$t('可通过表达式 ${{ variables.<var_name> }} 引用变量')" closable></bk-alert>
        <div class="operate-row">
            <bk-button class="var-btn" :disabled="!editable" @click="handleAdd">{{$t('添加变量')}}</bk-button>
            <bk-button class="var-btn" :disabled="!editable" @click="handleAdd('constant')">{{$t('添加常量')}}</bk-button>
            <bk-input
                v-model="searchStr"
                :clearable="true"
                :placeholder="'变量名/变量别名/变量描述'"
                :right-icon="'bk-icon icon-search'"
            />
        </div>
        <template v-if="!showSlider">
            <param-group
                v-for="group in pipelineParamGroups"
                :editable="editable"
                :key="group.key"
                :title="group.title"
                :list="group.list"
                :handle-edit="handleEdit"
                :handle-delete="handleDelete"
            />
        </template>

        <div v-else-if="editable" class="current-edit-param-item">
            <div class="edit-var-header">
                <bk-icon style="font-size: 28px;" type="arrows-left" class="back-icon" @click="showSlider = false" />
                {{sliderTitle}}
            </div>
            <div class="edit-var-content">
                <pipeline-param-form
                    :edit-item="sliderEditItem"
                    :global-params="globalParams"
                    :edit-index="editIndex"
                    :param-type="paramType"
                    :update-param="updateEditItem"
                    :reset-edit-item="resetEditItem" />
            </div>
            <div class="edit-var-footer" slot="footer">
                <bk-button theme="primary" @click="handleSaveVar">
                    {{ editIndex === -1 ? $t('添加') : $t('确定') }}
                </bk-button>
                <bk-button style="margin-left: 8px;" @click="hideSlider">
                    {{ $t('cancel') }}
                </bk-button>
            </div>
        </div>
    </div>
</template>

<script>
    import { allVersionKeyList } from '@/utils/pipelineConst'
    import ParamGroup from './children/param-group'
    import PipelineParamForm from './pipeline-param-form'

    export default {
        components: {
            PipelineParamForm,
            ParamGroup
        },
        props: {
            params: {
                type: Object,
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
                searchStr: ''
            }
        },
        computed: {
            versions () {
                return this.params.filter(p => allVersionKeyList.includes(p.id))
            },
            globalParams: {
                get () {
                    return this.params.filter(p => !allVersionKeyList.includes(p.id) && p.id !== 'BK_CI_BUILD_MSG')
                },
                set (params) {
                    this.updateContainerParams('params', [...params, ...this.versions])
                }
            },
            renderParams () {
                return !this.searchStr ? this.globalParams : this.globalParams.filter(item => (item.id.includes(this.searchStr) || item.name.includes(this.searchStr) || item.desc.includes(this.searchStr)))
            },
            pipelineParamGroups () {
                return [
                    {
                        key: 'requiredParam',
                        title: '入参',
                        list: this.renderParams.filter(item => !item.constant && item.required)
                    },
                    {
                        key: 'constantParam',
                        title: '常量',
                        list: this.renderParams.filter(item => item.constant === true)
                    },
                    {
                        key: 'otherParam',
                        title: '其它变量',
                        list: this.renderParams.filter(item => !item.constant && !item.required)
                    }
                ]
            },
            sliderTitle () {
                return `${this.editIndex === -1 ? this.$t('添加') : this.$t('编辑')}${this.paramType === 'constant' ? this.$t('常量') : this.$t('变量')}`
            }
        },
        methods: {
            handleDelete (paramId) {
                if (!this.editable) return
                const index = this.globalParams.findIndex(item => item.id === paramId)
                this.globalParams.splice(index, 1)
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
                this.sliderEditItem = this.globalParams.find(item => item.id === paramId) || {}
                this.paramType = this.sliderEditItem?.constant === true ? 'constant' : 'var'
            },
            handleSaveVar () {
                this.$validator.validate('pipelineParam.*').then((result) => {
                    if (result) {
                        if (this.editIndex > -1) {
                            this.globalParams[this.editIndex] = this.sliderEditItem
                        } else {
                            this.globalParams.push(this.sliderEditItem)
                        }
                        this.updateContainerParams('params', [...this.globalParams, ...this.versions])
                        this.hideSlider()
                    }
                })
            },
            updateEditItem (name, value) {
                Object.assign(this.sliderEditItem, { [name]: value })
                console.log(this.sliderEditItem, 'editing')
            },
            resetEditItem (param = {}) {
                this.sliderEditItem = param
            },
            hideSlider () {
                this.showSlider = false
                this.editIndex = -1
                this.sliderEditItem = {}
            }
        }
    }
</script>

<style lang="scss">
    @import "@/scss/mixins/ellipsis.scss";
    .variable-container {
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
                width: 88px;
                min-width: 88px;
                margin-right: 8px;
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
