<template>
    <div class="variable-container">
        <bk-alert type="info" :title="$t('可在插件中通过表达式 ${{ xxx }} 引用变量')" closable></bk-alert>
        <div class="add-and-desc">
            <bk-button @click="handleEdit(-1)">{{$t('新增变量')}}</bk-button>
            <div class="status-desc">
                <div class="desc-item">
                    <div class="circle" style="background-color: #2DCB9D;"></div>
                    <span class="status-desc">{{$t('入参')}}</span>
                </div>
                <div class="desc-item">
                    <div class="circle" style="background-color: #FF5656;"></div>
                    <span class="status-desc">{{$t('必填')}}</span>
                </div>
                <div class="desc-item">
                    <div class="circle" style="background-color: #C4C6CC;"></div>
                    <span class="status-desc">{{$t('运行时只读')}}</span>
                </div>
            </div>
        </div>
        <div class="variable-content">
            <div v-for="(param, index) in globalParams" :key="param.id" class="variable-item"
                @mouseenter="hoverIndex = index" @mouseleave="hoverIndex = -1">
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
                        <div v-if="param.valueNotEmpty" class="circle" style="background-color: #FF5656;"></div>
                        <div v-if="param.readOnly" class="circle" style="background-color: #C4C6CC;"></div>
                    </div>
                </div>
            </div>
            <div class="variable-empty" v-if="globalParams.length === 0">
                {{$t('暂无变量')}}
            </div>
        </div>

        <bk-sideslider :quick-close="false" :transfer="true" :width="640" :title="sliderTitle"
            :is-show.sync="showSlider" ext-cls="edit-var-container" @hidden="closeSlider">
            <div class="edit-var-content" slot="content">
                <pipeline-param-form :edit-item="sliderEditItem" :global-params="globalParams" :edit-index="editIndex"
                    :update-param="updateEditItem" :reset-edit-item="resetEditItem" />
            </div>
            <div class="edit-var-footer" slot="footer">
                <bk-button theme="primary" @click="handleSaveVar">
                    {{ $t('添加') }}
                </bk-button>
                <bk-button style="margin-left: 4px;" @click="hideSlider">
                    {{ $t('cancel') }}
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
                return this.editIndex === -1 ? this.$t('添加变量') : this.$t('编辑变量')
            }
        },
        methods: {
            handleDelete (index) {
                this.globalParams.splice(index, 1)
                this.updateContainerParams('params', [...this.globalParams, ...this.versions])
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
                if (this.editIndex > -1) {
                    this.globalParams[this.editIndex] = this.sliderEditItem
                } else {
                    this.globalParams.push(this.sliderEditItem)
                }
                this.updateContainerParams('params', [...this.globalParams, ...this.versions])
                this.hideSlider()
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
