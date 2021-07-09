<template>
    <bk-dialog
        v-model="isShow"
        ext-cls="check-atom-form"
        :close-icon="false"
        :width="600"
        :position="{ top: '100' }"
        :auto-close="false"
        @confirm="startReview"
        @cancel="handleCancel">
        <div v-bkloading="{ isLoading }">
            <bk-form form-type="vertical" :label-width="200" ref="checkForm">
                <bk-form-item :label="$t('details.checkDesc')">
                    <div style="white-space: pre-wrap;word-break:break-all;">{{$t('details.checkDescInfo', [time])}}</div>
                </bk-form-item>
                <bk-form-item :label="$t('stageReviewInputDesc')">
                    <div style="white-space: pre-wrap;word-break:break-all;">{{ ((reviewInfo || {}).stageControlOption || {}).reviewDesc || $t('none') }}</div>
                </bk-form-item>
                <bk-form-item :label="$t('stageReviewParams')">
                    <section v-for="(param, paramIndex) in reviewParams" :key="paramIndex" class="params-item">
                        <form-field class="form-field" :is-error="errors.has(`param-${paramIndex}.key`)" :error-msg="errors.first(`param-${paramIndex}.key`)">
                            <vuex-input
                                :data-vv-scope="`param-${paramIndex}`"
                                :disabled="true"
                                :desc-tooltips="param.desc"
                                :handle-change="(name, value) => handleVariableChange(param, name, value)"
                                v-validate.initial="`required|unique:${reviewParams.map(p => p.key).join(&quot;,&quot;)}|max: 50|${snonVarRule}`"
                                name="key"
                                placeholder="Key"
                                :value="param.chineseName ? param.chineseName : param.key" />
                        </form-field>
                        <span :class="{ 'default-required': true ,'is-required': param.required }" />
                        <div :class="{ 'bk-form-item': true, 'required-error-item': param.required && !param.value.length && isShowReuired && !isBooleanParam(param.valueType) }">
                            <!-- 自定义变量展示 -->
                            <define-param-show :param="param" :global-params="reviewParams" :param-index="paramIndex" @handleParamChange="handleVariableChange(param, ...arguments)" />
                        </div>
                        <i v-if="param.required && !param.value.length && isShowReuired && !isBooleanParam(param.valueType)"
                            v-bk-tooltips="paramRequiredTips"
                            class="bk-icon icon-exclamation-circle-shape top-middle is-required-icon"
                        />
                    </section>
                    <span v-if="reviewParams.length <= 0">{{ $t('none') }}</span>
                </bk-form-item>
                <bk-form-item :label="$t('stageReviewInputDesc')">
                    <div style="white-space: pre-wrap;word-break:break-all;">{{ ((reviewInfo || {}).stageControlOption || {}).reviewDesc || $t('none') }}</div>
                </bk-form-item>
                <bk-form-item label="审核流">
                    <stage-review-flow :show-review-opt="true" :review-groups="reviewGroups" disabled v-if="reviewGroups.length"></stage-review-flow>
                </bk-form-item>

                <bk-form-item :label="$t('editPage.checkResult')">
                    <bk-radio-group v-model="isCancel">
                        <bk-radio class="choose-item" :value="false">{{ $t('details.agree') }}</bk-radio>
                        <bk-radio class="choose-item" :value="true">{{ $t('details.abort') }}</bk-radio>
                    </bk-radio-group>
                </bk-form-item>
            </bk-form>
        </div>
    </bk-dialog>
</template>

<script>
    import { mapActions, mapState } from 'vuex'
    import { convertTime } from '@/utils/util'
    import StageReviewFlow from '@/components/StagePropertyPanel/StageReviewFlow'
    import DefineParamShow from '@/components/AtomFormComponent/DefineParam/show'
    import FormField from '@/components/AtomPropertyPanel/FormField'
    import VuexInput from '@/components/atomFormField/VuexInput'
    import {
        isBooleanParam
    } from '@/store/modules/atom/paramsConfig'

    export default {
        name: 'review-dialog',
        components: {
            StageReviewFlow,
            DefineParamShow,
            FormField,
            VuexInput
        },
        props: {
            isShow: {
                type: Boolean,
                default: false
            }
        },

        data () {
            return {
                isLoading: false,
                isCancel: false,
                params: [],
                isShowReuired: false,
                paramRequiredTips: {
                    showOnInit: true,
                    content: this.$t('editPage.checkParamTip'),
                    placements: ['top']
                }
            }
        },
        computed: {
            ...mapState('atom', [
                'reviewInfo'
            ]),
            routerParams () {
                return this.$route.params
            },
            time () {
                try {
                    const hour2Ms = 60 * 60 * 1000
                    return convertTime(this.reviewInfo.startEpoch + this.reviewInfo.stageControlOption.timeout * hour2Ms)
                } catch (e) {
                    return 'unknow'
                }
            },
            reviewParams () {
                return ((this.reviewInfo || {}).stageControlOption || {}).reviewParams || []
            },
            reviewGroups () {
                return ((this.reviewInfo || {}).stageControlOption || {}).reviewGroups || []
            }
        },
        watch: {
            reviewParams (val) {
                val && this.handleChangeParams('reviewParams', val)
            }
        },
        methods: {
            ...mapActions('atom', [
                'toggleReviewDialog',
                'triggerStage'
            ]),
            isBooleanParam,
            startReview () {
                let isCheck = true
                this.reviewParams.forEach(param => {
                    if (param.required && !param.value.length && !isBooleanParam(param.valueType)) {
                        isCheck = false
                        this.isShowReuired = true
                    }
                })

                this.$refs.checkForm.validate().then(() => {
                    if (isCheck) {
                        this.triggerStage({
                            ...this.$route.params,
                            stageId: this.reviewInfo.id,
                            cancel: this.isCancel,
                            reviewParams: this.params
                        })

                        this.$nextTick(() => {
                            this.handleCancel()
                        })
                    }
                }).catch((err) => {
                    this.$bkMessage({ message: err.content, theme: 'error' })
                })
            },

            handleCancel () {
                this.$refs.checkForm.clearError()
                this.toggleReviewDialog({
                    isShow: false,
                    reviewInfo: null
                })
            },

            handleVariableChange (item, name, value) {
                item[name] = value
            },

            handleChangeParams (name, value) {
                this.params = value
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
            margin-top: 10px;
        }
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
</style>
