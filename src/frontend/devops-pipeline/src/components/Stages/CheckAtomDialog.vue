<template>
    <bk-dialog
        v-model="isShowCheckDialog"
        ext-cls="check-atom-form"
        :close-icon="false"
        :width="650"
        :position="{ top: '100' }"
        :auto-close="false"
        @confirm="handleAtomCheck"
        @cancel="cancleAtomCheck">
        <div v-bkloading="{ isLoading }">
            <bk-form form-type="vertical" :model="data" ref="checkForm">
                <bk-form-item v-if="data.desc" :label="$t('editPage.checkDesc')">
                    <div style="white-space: pre-wrap;word-break:break-all;">{{data.desc}}</div>
                </bk-form-item>
                <bk-form-item :label="$t('editPage.checkResult')"
                    required
                    error-display-type="normal"
                    property="status"
                    :rules="[requireRule]"
                >
                    <bk-radio-group v-model="data.status">
                        <bk-radio class="choose-item" :value="'PROCESS'">{{ $t('editPage.agree') }}</bk-radio>
                        <bk-radio class="choose-item" :value="'ABORT'">{{ $t('editPage.abort') }}</bk-radio>
                    </bk-radio-group>
                </bk-form-item>
                <bk-form-item>
                    <bk-input style="width: 98%" type="textarea" v-model="data.suggest" :placeholder="$t('editPage.checkSuggestTips')" class="check-suggest"></bk-input>
                </bk-form-item>
                <bk-form-item>
                    <div
                        v-for="(param, paramIndex) in data.params" :key="paramIndex"
                        class="params-item"
                        :is-error="!isMetadataVar && errors.any(`param-${paramIndex}`)">
                        <form-field class="form-field" :is-error="!isMetadataVar && errors.has(`param-${paramIndex}.key`)" :error-msg="errors.first(`param-${paramIndex}.key`)">
                            <vuex-input
                                :data-vv-scope="`param-${paramIndex}`"
                                :disabled="true"
                                :desc-tooltips="param.desc"
                                :handle-change="(name, value) => handleParamChange(name, value, paramIndex)"
                                v-validate.initial="`required|unique:${data.params.map(p => p.key).join(&quot;,&quot;)}|max: 50|${snonVarRule}`"
                                name="key"
                                :placeholder="isMetadataVar ? $t('view.key') : 'Key'"
                                :value="param.chineseName ? param.chineseName : param.key" />
                        </form-field>
                        <span :class="{ 'default-required': true ,'is-required': param.required }" />
                        <div :class="{ 'bk-form-item': true, 'required-error-item': param.required && !param.value.length && isShowReuired && !isBooleanParam(param.valueType) }">
                            <!-- 自定义变量展示 -->
                            <define-param-show :param="param" :global-params="data" :param-index="paramIndex" @handleParamChange="handleParamChange" />
                        </div>
                        <i v-if="param.required && !param.value.length && isShowReuired && !isBooleanParam(param.valueType)" v-bk-tooltips="paramRequiredTips" class="bk-icon icon-exclamation-circle-shape top-middle is-required-icon" />
                    </div>
                </bk-form-item>
            </bk-form>
        </div>
    </bk-dialog>
</template>

<script>
    import { mapActions } from 'vuex'
    import atomMixin from '../AtomPropertyPanel/atomMixin'
    import {
        isTextareaParam,
        isStringParam,
        isBooleanParam,
        isEnumParam,
        isMultipleParam
    } from '@/store/modules/atom/paramsConfig'
    import FormField from '@/components/AtomPropertyPanel/FormField'
    import VuexInput from '@/components/atomFormField/VuexInput'
    import DefineParamShow from '@/components/AtomFormComponent/DefineParam/show.vue'

    export default {
        name: 'check-atom-dialog',
        components: {
            VuexInput,
            FormField,
            DefineParamShow
        },
        mixins: [atomMixin],
        props: {
            atom: {
                type: Object,
                default: () => ({})
            },
            isShowCheckDialog: {
                type: Boolean,
                default: false
            },
            toggleCheck: {
                type: Function,
                required: true
            },
            isSupportVar: {
                type: Boolean,
                default: false
            },
            isMetadataVar: {
                type: Boolean,
                default: false
            }
        },
        data () {
            return {
                isLoading: true,
                isShowReuired: false,
                data: {
                    status: '',
                    suggest: '',
                    desc: '',
                    params: []
                },
                requireRule: {
                    required: true,
                    message: this.$t('editPage.checkResultTip'),
                    trigger: 'blur'
                },
                paramRequiredTips: {
                    showOnInit: true,
                    content: this.$t('editPage.checkParamTip'),
                    placements: ['top']
                }
            }
        },
        computed: {
            routerParams () {
                return this.$route.params
            },
            snonVarRule () {
                return !this.isSupportVar ? 'nonVarRule' : ''
            }
        },
        watch: {
            'isShowCheckDialog': function (val) {
                if (val) {
                    this.requestCheckData()
                }
            }
        },
        methods: {
            isTextareaParam,
            isStringParam,
            isBooleanParam,
            isEnumParam,
            isMultipleParam,
            ...mapActions('atom', [
                'getCheckAtomInfo',
                'handleCheckAtom'
            ]),
            cancleAtomCheck () {
                this.toggleCheck(false)
                this.$refs.checkForm.clearError()
            },
            async requestCheckData () {
                try {
                    this.isLoading = true
                    const postData = {
                        projectId: this.routerParams.projectId,
                        pipelineId: this.routerParams.pipelineId,
                        buildId: this.routerParams.buildNo,
                        elementId: this.atom.id
                    }
                    const res = await this.getCheckAtomInfo(postData)
                    this.data = Object.assign(res, { status: '' })
                } catch (err) {
                    this.$showTips({
                        theme: 'error',
                        message: err.message || err
                    })
                } finally {
                    this.isLoading = false
                }
            },
            handleAtomCheck () {
                let isCheck = true
                this.data.params.forEach(param => {
                    if (param.required && !param.value.length && !isBooleanParam(param.valueType)) {
                        isCheck = false
                        this.isShowReuired = true
                    }
                })

                this.$refs.checkForm.validate().then(
                    async () => {
                        try {
                            if (isCheck) {
                                const data = {
                                    projectId: this.routerParams.projectId,
                                    pipelineId: this.routerParams.pipelineId,
                                    buildId: this.routerParams.buildNo,
                                    elementId: this.atom.id,
                                    postData: this.data
                                }
                                const res = await this.handleCheckAtom(data)
                                if (res === true) {
                                    this.$showTips({
                                        message: this.data.status === 'ABORT' ? this.$t('editPage.abortSuc') : this.$t('editPage.agreeSuc'),
                                        theme: 'success'
                                    })
                                }
                                this.toggleCheck(false)
                            }
                        } catch (err) {
                            this.$showTips({
                                message: err.message || err,
                                theme: 'error'
                            })
                        }
                    }).catch((err) => {
                    this.$bkMessage({ message: err.content, theme: 'error' })
                })
            },
            handleParamChange (key, value, paramIndex) {
                const param = this.data.params
                if (isMultipleParam(param[paramIndex].valueType) && key === 'value') {
                    Object.assign(param[paramIndex], {
                        [key]: value.join(',')
                    })
                } else if (param) {
                    Object.assign(param[paramIndex], {
                        [key]: value
                    })
                }
                this.handleUpdateElement('params', param)
            }
        }
    }
</script>

<style lang="scss" scoped>
    .check-atom-form {
        .choose-item {
            margin-right: 30px;
        }
        .check-suggest {
            margin-top: 0px;
        }
        .params-item {
            display: flex;
            margin-bottom: 10px;
           .form-field {
               width: 286px;
               margin-right: 10px;
           }
            > .bk-form-item {
                width: 286px;
                height: 32px;
                margin-top: 0px !important;
            }
            .is-required-icon {
                color: red;
                position: relative;
                top: 10px;
                right: -6px;
            }
            .default-required {
                width: 8px;
                height: 8px;
            }
            .is-required:after {
                height: 8px;
                line-height: 1;
                content: "*";
                color: #ea3636;
                font-size: 12px;
                position: relative;
                left: -6px;
                top: 4px;
                display: inline-block;
            }
        }
    }
    .required-error-item {
        .bk-select,
        .bk-form-input,
        .bk-form-textarea {
            border: 1px solid red;
        }
    }
    
</style>
