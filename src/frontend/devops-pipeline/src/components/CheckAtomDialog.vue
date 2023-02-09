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
                    <mavon-editor
                        class="markdown-desc"
                        :editable="false"
                        default-open="preview"
                        :subfield="false"
                        :toolbars-flag="false"
                        :external-link="false"
                        :box-shadow="false"
                        preview-background="#fff"
                        v-model="data.desc"
                    >
                    </mavon-editor>
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
                    <check-params
                        :params="data.params"
                        :is-metadata-var="isMetadataVar"
                        :is-support-var="isSupportVar"
                        @handleParamChange="handleParamChange"
                    />
                </bk-form-item>
            </bk-form>
        </div>
    </bk-dialog>
</template>

<script>
    import { mapActions } from 'vuex'
    import atomMixin from '@/components/AtomPropertyPanel/atomMixin'
    import {
        isTextareaParam,
        isStringParam,
        isBooleanParam,
        isEnumParam,
        isMultipleParam
    } from '@/store/modules/atom/paramsConfig'
    import CheckParams from '@/components/CheckParams.vue'

    export default {
        name: 'check-atom-dialog',
        components: {
            CheckParams
        },
        mixins: [atomMixin],
        props: {
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
            }
        },
        watch: {
            isShowCheckDialog: function (val) {
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
                        elementId: this.element.id
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
                                    elementId: this.element.id,
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
    }
    .markdown-desc {
        min-height: 50px !important;
        max-height: 120px !important;
    }
    ::v-deep .bk-label-text {
        font-weight: bold;
    }
</style>
