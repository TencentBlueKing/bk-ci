<template>
    <div class="variable-container">
        <bk-alert type="info" :title="$t('可在插件中通过表达式 ${{ xxx }} 引用变量')" closable></bk-alert>
        <div class="add-and-desc">
            <bk-button @click="handleEdit(-1)">新增变量</bk-button>
            <div class="status-desc">
                <div class="desc-item">
                    <div class="circle" style="background-color: #2DCB9D;"></div>
                    <span class="status-desc">入参</span>
                </div>
                <div class="desc-item">
                    <div class="circle" style="background-color: #FF5656;"></div>
                    <span class="status-desc">必填</span>
                </div>
                <div class="desc-item">
                    <div class="circle" style="background-color: #C4C6CC;"></div>
                    <span class="status-desc">运行时只读</span>
                </div>
            </div>
        </div>
        <div class="variable-content">
            <div
                v-for="(param, index) in globalParams"
                :key="param.id"
                class="variable-item"
                @mouseenter="hoverIndex = index"
                @mouseleave="hoverIndex = -1"
            >
                <div class="var-con">
                    <div class="var-names" v-bk-tooltips="{ content: param.desc, disabled: !param.desc }">
                        <span>{{ param.id }}</span>
                        <span>({{ param.name || param.id }})</span>
                    </div>
                    <div class="default-value">
                        {{ param.defaultValue }}
                    </div>
                </div>
                <div class="var-operate">
                    <div v-if="hoverIndex === index" class="operate-btns">
                        <i @click="handleEdit(index)" class="bk-icon icon-edit-line" style="margin-right: 12px;"></i>
                        <i @click="handleDelete(index)" class="bk-icon icon-minus-circle"></i>
                    </div>
                    <div v-else class="var-status">
                        <div v-if="param.required" class="circle" style="background-color: #2DCB9D;"></div>
                        <div v-if="param.necessary" class="circle" style="background-color: #FF5656;"></div>
                        <div v-if="param.readOnly" class="circle" style="background-color: #C4C6CC;"></div>
                    </div>
                </div>
            </div>
            <div class="variable-empty" v-if="globalParams.length === 0">
                {{$t('暂无变量')}}
            </div>
        </div>

        <bk-sideslider
            quick-close
            :transfer="true"
            :width="640"
            :title="sliderTitle"
            :is-show.sync="showSlider"
            ext-cls="edit-var-container"
            @hidden="closeSlider"
        >
            <div class="edit-var-content" slot="content">
                <pipeline-param-form :edit-item="sliderEditItem" :global-params="globalParams" :edit-index="editIndex" :update-param="updateEditItem" />
            </div>
            <div class="edit-var-footer" slot="footer">
                <bk-button theme="primary" @click="handleSaveVar">
                    新增
                </bk-button>
                <bk-button style="margin-left: 4px;" @click="hideSlider">
                    取消
                </bk-button>
            </div>
        </bk-sideslider>
    </div>
</template>

<script>
    import { allVersionKeyList } from '@/utils/pipelineConst'
    import PipelineParamForm from './pipeline-param-form'

    export default {
        components: {
            PipelineParamForm
        },
        props: {
            params: {
                type: Object,
                required: true
            },
            updateContainerParams: {
                type: Function,
                required: true
            }
        },
        data () {
            return {
                showSlider: false,
                hoverIndex: -1,
                editIndex: -1,
                sliderEditItem: {}
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
            sliderTitle () {
                return '新增变量'
            }
        },
        methods: {
            handleDelete (index) {
                this.globalParams = this.globalParams.splice(index, 1)
            },
            handleEdit (index) {
                this.showSlider = true
                this.editIndex = index
                if (index > -1 && this.globalParams[index]) {
                    this.sliderEditItem = this.globalParams[index]
                } else {
                    this.sliderEditItem = {}
                }
            },
            handleSaveVar () {
                console.log(this.sliderEditItem, this.editIndex)
                if (this.editIndex > -1) {
                    this.globalParams[this.editIndex] = this.sliderEditItem
                } else {
                    this.globalParams.push(this.sliderEditItem)
                }
                this.hideSlider()
            },
            updateEditItem (name, value) {
                Object.assign(this.sliderEditItem, { [name]: value })
                console.log(this.sliderEditItem, 'editing')
            },
            hideSlider () {
                this.showSlider = false
                this.editIndex = -1
                this.sliderEditItem = {}
            }
        }
    }
</script>
