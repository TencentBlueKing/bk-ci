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
                <!-- <bk-form-item :label="$t('editPage.customVar')" v-if="data.status === 'PROCESS' && data.params && data.params.length">
                    <define-param :value="data.params" :disabled="true" :params-list="paramsList" />
                </bk-form-item> -->
                <bk-form-item>
                    <ul>
                        <template v-if="data.params.length">
                            <li class="param-item" v-for="(param, paramIndex) in data.params" :key="paramIndex" :is-error="!isMetadataVar && errors.any(`param-${paramIndex}`)">
                                <form-field class="form-field" :is-error="!isMetadataVar && errors.has(`param-${paramIndex}.key`)" :error-msg="errors.first(`param-${paramIndex}.key`)">
                                    <vuex-input
                                        :data-vv-scope="`param-${paramIndex}`"
                                        :disabled="true"
                                        :handle-change="(name, value) => handleParamChange(name, value, paramIndex)"
                                        v-validate.initial="`required|unique:${data.params.map(p => p.key).join(&quot;,&quot;)}|max: 50|${snonVarRule}`"
                                        name="key"
                                        :placeholder="isMetadataVar ? $t('view.key') : 'Key'"
                                        :value="param.key" />
                                </form-field>
                                <div class="bk-form-item">
                                    <selector
                                        :popover-min-width="250"
                                        v-if="isSelectorParam(param.valueType)"
                                        :handle-change="(name, value) => handleParamChange(name, value, paramIndex)"
                                        :list="transformOpt(param.options)"
                                        :multi-select="isMultipleParam(param.valueType)"
                                        name="value"
                                        :data-vv-scope="`param-${param.key}`"
                                        :placeholder="$t('editPage.defaultValueTips')"
                                        :disabled="disabled && !editValueOnly"
                                        :key="param.valueType"
                                        :value="getSelectorDefaultVal(param)">
                                    </selector>
                                    <enum-input
                                        v-if="isBooleanParam(param.valueType)"
                                        name="value"
                                        :list="boolList"
                                        :disabled="disabled && !editValueOnly"
                                        :data-vv-scope="`param-${param.key}`"
                                        :handle-change="(name, value) => handleParamChange(name, value, paramIndex)"
                                        :value="param.value">
                                    </enum-input>
                                    <vuex-input
                                        v-if="isStringParam(param.valueType)"
                                        :disabled="disabled && !editValueOnly"
                                        :handle-change="(name, value) => handleParamChange(name, value, paramIndex)"
                                        name="value"
                                        :click-unfold="true"
                                        :data-vv-scope="`param-${param.key}`"
                                        :placeholder="$t('editPage.defaultValueTips')" :value="param.value" />
                                    <vuex-textarea
                                        v-if="isTextareaParam(param.valueType)"
                                        :click-unfold="true"
                                        :disabled="disabled && !editValueOnly"
                                        :handle-change="(name, value) => handleParamChange(name, value, paramIndex)"
                                        name="value"
                                        :data-vv-scope="`param-${param.key}`"
                                        :placeholder="$t('editPage.defaultValueTips')"
                                        :value="param.value" />
                                </div>
                            </li>
                        </template>
                    </ul>
                </bk-form-item>
            </bk-form>
            <!-- <bk-form-item :label="$t('editPage.customVar')" v-if="data.status === 'PROCESS' && data.params && data.params.length">
                    <key-value-normal :value="data.params" :edit-value-only="true"></key-value-normal>
                </bk-form-item> -->
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
        isMultipleParam,
        CHECK_PARAM_LIST } from '@/store/modules/atom/paramsConfig'
    import EnumInput from '@/components/atomFormField/EnumInput'
    import VuexInput from '@/components/atomFormField/VuexInput'
    import VuexTextarea from '@/components/atomFormField/VuexTextarea'
    import Selector from '@/components/atomFormField/Selector'

    const BOOLEAN = [
        {
            value: true,
            label: true
        },
        {
            value: false,
            label: false
        }
    ]

    export default {
        name: 'check-atom-dialog',
        components: {
            // DefineParam,
            EnumInput,
            VuexInput,
            VuexTextarea,
            Selector
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
            },
            // 只允许修改值，不允许增减项和修改key
            editValueOnly: {
                type: Boolean,
                default: false
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
            },
            snonVarRule () {
                return !this.isSupportVar ? 'nonVarRule' : ''
            },
            paramsList () {
                return CHECK_PARAM_LIST.map(item => {
                    return {
                        id: item.id,
                        name: this.$t(`storeMap.${item.name}`)
                    }
                })
            },
            boolList () {
                return BOOLEAN
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
                    console.log('requestCheckData---data====', this.data)
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
            },
            getSelectorDefaultVal ({ valueType, value = '' }) {
                if (isMultipleParam(valueType)) {
                    return value && typeof value === 'string' ? value.split(',') : []
                }

                return value
            },
            transformOpt (opts) {
                const uniqueMap = {}
                opts = opts.filter(opt => opt.key.length)
                return Array.isArray(opts) ? opts.filter(opt => {
                    if (!uniqueMap[opt.key]) {
                        uniqueMap[opt.key] = 1
                        return true
                    }
                    return false
                }).map(opt => ({ id: opt.key, name: opt.value })) : []
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
            },
            isSelectorParam (type) {
                return isMultipleParam(type) || isEnumParam(type)
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
        .param-item {
           .form-field {
               width: 50%;
               margin-right: 10px;
           }
            > .bk-form-item {
                width: 50%;
                height: 32px;
                margin-top: 0px !important;
            }
        }
    }
</style>
