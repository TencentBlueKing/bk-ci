<template>
    <bk-dialog
        v-model="isShowCheckDialog"
        ext-cls="check-atom-form"
        :close-icon="false"
        :width="600"
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
                    <bk-input type="textarea" v-model="data.suggest" :placeholder="$t('editPage.checkSuggestTips')" class="check-suggest"></bk-input>
                </bk-form-item>
                <bk-form-item :label="$t('editPage.customVar')" v-if="data.status === 'PROCESS' && data.params && data.params.length">
                    <key-value-normal :value="data.params" :edit-value-only="true"></key-value-normal>
                </bk-form-item>
                
                <!-- <bk-form-item
                    :label="$t('editPage.customVar')"
                    v-if="data.status === 'PROCESS' && data.params && data.params.length">
                    <selector
                        :popover-min-width="250"
                        v-if="isSelectorParam(param.valueType)"
                        :handle-change="(name, value) => handleUpdateParam(name, value, index)"
                        :list="transformOpt(param.options)"
                        :multi-select="isMultipleParam(param.valueType)"
                        name="value"
                        :data-vv-scope="`param-${param.key}`"
                        :placeholder="$t('editPage.defaultValueTips')"
                        :disabled="disabled"
                        :key="param.valueType"
                        :value="getSelectorDefaultVal(param)">
                    </selector>
                    <enum-input
                        v-if="isBooleanParam(param.valueType)"
                        name="value"
                        :list="boolList"
                        :disabled="disabled"
                        :data-vv-scope="`param-${param.key}`"
                        :handle-change="(name, value) => handleUpdateParam(name, value, index)"
                        :value="param.value">
                    </enum-input>
                    <vuex-input
                        v-if="isStringParam(param.valueType)"
                        :disabled="disabled"
                        :handle-change="(name, value) => handleUpdateParam(name, value, index)"
                        name="value"
                        :click-unfold="true"
                        :data-vv-scope="`param-${param.key}`"
                        :placeholder="$t('editPage.defaultValueTips')" :value="param.value" />
                    <vuex-textarea
                        v-if="isTextareaParam(param.valueType)"
                        :click-unfold="true"
                        :disabled="disabled"
                        :handle-change="(name, value) => handleUpdateParam(name, value, index)"
                        name="value"
                        :data-vv-scope="`param-${param.key}`"
                        :placeholder="$t('editPage.defaultValueTips')"
                        :value="param.value" />
                </bk-form-item> -->
            </bk-form>
        </div>
    </bk-dialog>
</template>

<script>
    import { mapActions } from 'vuex'
    import KeyValueNormal from '@/components/atomFormField/KeyValueNormal'
    export default {
        name: 'check-atom-dialog',
        components: {
            KeyValueNormal
        },
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
            }
        },

        data () {
            return {
                isLoading: true,
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
                }
            }
        },
        computed: {
            routerParams () {
                return this.$route.params
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
                    console.log(res, 'check-atom-dialog-res')
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
                this.$refs.checkForm.validate().then(async () => {
                    try {
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
                    } catch (err) {
                        this.$showTips({
                            message: err.message || err,
                            theme: 'error'
                        })
                    }
                }).catch((err) => {
                    this.$bkMessage({ message: err.content, theme: 'error' })
                })
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
</style>
