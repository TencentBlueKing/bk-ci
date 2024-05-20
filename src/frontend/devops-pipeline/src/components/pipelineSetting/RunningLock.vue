<template>
    <div v-if="pipelineSetting" class="bkdevops-running-lock-setting-tab">
        <div class="pipeline-setting-title">{{$t('settings.runLock')}}</div>
        <bk-form
            :model="pipelineSetting"
            :rules="formRule"
            :label-width="300"
            form-type="vertical"
            class="new-ui-form"
        >
            <bk-form-item :is-error="errors.has('buildNumRule')" :error-msg="errors.first('buildNumRule')">
                <div class="layout-label">
                    <label class="ui-inner-label">
                        <span class="bk-label-text">{{ $t('settings.buildNumberFormat') }}</span>
                        <span @click="handleGoDocumentInfo">
                            <i class="bk-icon icon-question-circle-shape" v-bk-tooltips="$t('buildNumRuleWarn')" />
                        </span>
                    </label>
                </div>
                <vuex-input
                    name="buildNumRule"
                    :max-length="256"
                    :disabled="!editable"
                    :value="pipelineSetting.buildNumRule"
                    :placeholder="$t('buildDescInputTips')"
                    v-validate.initial="{ buildNumRule: true }"
                    :handle-change="handleBaseInfoChange"
                />
                <p class="error-tips"
                    v-if="errors.has('buildNumRule')">
                    {{ $t('settings.validatebuildNum') }}
                </p>
            </bk-form-item>
            <bk-form-item :label="$t('template.parallelSetting')">
                <bk-radio-group :value="pipelineSetting.runLockType" @change="handleLockTypeChange">
                    <div class="run-lock-radio-item">
                        <bk-radio
                            :disabled="!editable"
                            :value="runTypeMap.MULTIPLE"
                        >
                            {{$t('settings.runningOption.multiple')}}
                        </bk-radio>
                    </div>
                    <div class="run-lock-radio-item">
                        <bk-radio
                            :disabled="!editable"
                            :value="runTypeMap.GROUP"
                        >
                            {{$t('settings.runningOption.single')}}
                        </bk-radio>
                    </div>
                </bk-radio-group>
            </bk-form-item>
            <div class="single-lock-sub-form" v-if="isSingleLock">
                <bk-form-item
                    :required="isSingleLock"
                    property="concurrencyGroup"
                    desc-type="icon"
                    desc-icon="bk-icon icon-question-circle-shape"
                    :label="$t('settings.groupName')"
                    :desc="$t('settings.lockGroupDesc')"
                >
                    <bk-input
                        :placeholder="$t('settings.itemPlaceholder')"
                        :disabled="!editable"
                        :max-length="128"
                        :maxlength="128"
                        v-model="pipelineSetting.concurrencyGroup"
                    />
                </bk-form-item>

                <bk-form-item property="concurrencyCancelInProgress">
                    <bk-checkbox
                        :disabled="!editable"
                        :checked="pipelineSetting.concurrencyCancelInProgress"
                        @change="handleConCurrencyCancel"
                    >
                        {{$t('settings.stopWhenNewCome')}}
                    </bk-checkbox>
                </bk-form-item>
                <template v-if="!pipelineSetting.concurrencyCancelInProgress">
                    <bk-form-item
                        :label="$t('settings.largestNum')"
                        error-display-type="normal"
                        property="maxQueueSize"
                    >
                        <bk-input
                            type="number"
                            :disabled="!editable"
                            :placeholder="$t('settings.itemPlaceholder')"
                            v-model="pipelineSetting.maxQueueSize"
                        >
                            <template slot="append">
                                <span class="pipeline-setting-unit">{{$t('settings.item')}}</span>
                            </template>
                        </bk-input>
                    </bk-form-item>
                    <bk-form-item
                        :label="$t('settings.lagestTime')"
                        error-display-type="normal"
                        property="waitQueueTimeMinute"
                    >
                        <bk-input
                            type="number"
                            :disabled="!editable"
                            :placeholder="$t('settings.itemPlaceholder')"
                            v-model="pipelineSetting.waitQueueTimeMinute"
                        >
                            <template slot="append">
                                <span class="pipeline-setting-unit">{{$t('settings.minutes')}}</span>
                            </template>
                        </bk-input>
                    </bk-form-item>
                </template>
            </div>

            <!-- <bk-form-item :label="$t('settings.disableSetting')">
                <span @click="handleLockTypeChange(runTypeMap.LOCK)">
                    <bk-radio
                        :checked="pipelineSetting.runLockType === runTypeMap.LOCK"
                        :value="runTypeMap.LOCK"
                    >
                        {{$t('settings.runningOption.lock')}}
                    </bk-radio>
                </span>
            </bk-form-item> -->
        </bk-form>
    </div>
</template>

<script>
    import VuexInput from '@/components/atomFormField/VuexInput/index.vue'

    export default {
        name: 'bkdevops-running-lock-setting-tab',
        components: {
            VuexInput
        },
        props: {
            pipelineSetting: Object,
            editable: {
                type: Boolean,
                default: true
            },
            handleRunningLockChange: Function
        },
        computed: {
            runTypeMap () {
                return {
                    MULTIPLE: 'MULTIPLE',
                    SINGLE: 'SINGLE',
                    GROUP: 'GROUP_LOCK',
                    LOCK: 'LOCK'
                }
            },
            isSingleLock () {
                return [this.runTypeMap.GROUP, this.runTypeMap.SINGLE].includes(this.pipelineSetting?.runLockType)
            },
            formRule () {
                const requiredRule = {
                    required: this.isSingleLock,
                    message: this.$t('editPage.checkParamTip'),
                    trigger: 'blur'
                }
                return {
                    concurrencyGroup: [
                        requiredRule
                    ],
                    maxQueueSize: [
                        requiredRule,
                        {
                            validator: (val) => {
                                const intVal = parseInt(val, 10)
                                return !this.isSingleLock || (intVal <= 200 && intVal >= 0)
                            },
                            message: `${this.$t('settings.largestNum')}${this.$t('numberRange', [0, 200])}`,
                            trigger: 'blur'
                        }
                    ],
                    waitQueueTimeMinute: [
                        requiredRule,
                        {
                            validator: (val) => {
                                const intVal = parseInt(val, 10)
                                return !this.isSingleLock || (intVal <= 1440 && intVal >= 1)
                            },
                            message: `${this.$t('settings.lagestTime')}${this.$t('numberRange', [1, 1440])}`,
                            trigger: 'blur'
                        }
                    ]
                }
            }
        },
        created () {
            if (this.pipelineSetting?.runLockType === this.runTypeMap.SINGLE) {
                this.handleLockTypeChange(this.runTypeMap.GROUP)
            }
        },
        methods: {
            handleLockTypeChange (runLockType) {
                this.handleRunningLockChange({
                    runLockType,
                    concurrencyGroup: this.pipelineSetting?.concurrencyGroup || '${{ci.pipeline_id}}'
                })
            },
            handleConCurrencyCancel (val) {
                this.handleRunningLockChange({
                    concurrencyCancelInProgress: val
                })
            },
            handleBaseInfoChange (name, val) {
                this.handleRunningLockChange({
                    [name]: val
                })
            },
            handleGoDocumentInfo () {
                window.open(this.$pipelineDocs.ALIAS_BUILD_NO_DOC)
            }
        }
    }
</script>

<style lang="scss">
    .bkdevops-running-lock-setting-tab {
        .bk-form-content {
            max-width: 560px;
        }
        .layout-label {
            font-size: 12px;
            i {
                margin-left: 6px;
                color: #979BA5;
                font-size: 14px;
                cursor: pointer;
            }
        }
        .single-lock-sub-form {
            margin-bottom: 20px;
            width: 560px;
            border-radius: 2px;
            border: 1px solid #DCDEE5;
            padding: 16px;
        }
        .run-lock-radio-item {
            margin: 10px 0;
        }
        .pipeline-setting-unit {
            display: flex;
            background: #f1f4f8;
            color: #63656e;
            width: 50px;
            font-size: 12px;
            height: 100%;
            align-items: center;
            justify-content: center;
        }
    }
</style>
