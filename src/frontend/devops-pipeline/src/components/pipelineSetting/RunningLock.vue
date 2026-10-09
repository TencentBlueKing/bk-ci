<template>
    <div
        v-if="pipelineSetting"
        class="bkdevops-running-lock-setting-tab"
    >
        <div class="pipeline-setting-title">{{ $t('settings.runLock') }}</div>
        <bk-form
            :model="pipelineSetting"
            :rules="formRule"
            :label-width="300"
            form-type="vertical"
            class="new-ui-form"
        >
            <bk-form-item
                :is-error="errors.has('buildNumRule')"
                :error-msg="errors.first('buildNumRule')"
            >
                <constraint-wraper
                    :classify="CLASSIFY_ENUM.SETTING"
                    field="buildNumRule"
                >
                    <template v-slot:constraint-title>
                        <div class="layout-label">
                            <label class="ui-inner-label">
                                <span class="bk-label-text">{{ $t('settings.buildNumberFormat') }}</span>
                                <span @click="handleGoDocumentInfo">
                                    <i
                                        class="bk-icon icon-question-circle-shape"
                                        v-bk-tooltips="$t('buildNumRuleWarn')"
                                    />
                                </span>
                            </label>
                        </div>
                    </template>
                    <template v-slot:constraint-area="{ props: { isOverride } }">
                        <vuex-input
                            name="buildNumRule"
                            :max-length="256"
                            :disabled="!(editable || isOverride)"
                            :value="pipelineSetting.buildNumRule"
                            :placeholder="$t('buildDescInputTips')"
                            v-validate.initial="{ buildNumRule: true }"
                            :handle-change="handleBaseInfoChange"
                        />
                        <p
                            class="error-tips"
                            v-if="errors.has('buildNumRule')"
                        >
                            {{ $t('settings.validatebuildNum') }}
                        </p>
                    </template>
                </constraint-wraper>
            </bk-form-item>
            <bk-form-item
                ext-cls="variable-invalid"
            >
                <constraint-wraper
                    :classify="CLASSIFY_ENUM.SETTING"
                    field="failIfVariableInvalid"
                    :label="$t('settings.whenVariableExceedsLength')"
                >
                    <template v-slot:constraint-area="{ props: { isOverride } }">
                        <bk-radio-group
                            v-model="proxyFailIfVariableInvalid"
                            @change="val => handleBaseInfoChange('failIfVariableInvalid', val)"
                        >
                            <bk-radio
                                :value="false"
                                :disabled="!(editable || isOverride)"
                            >
                                {{ $t('settings.clearTheValue') }}
                            </bk-radio>
                            <bk-radio
                                :value="true"
                                class="ml20"
                                :disabled="!(editable || isOverride)"
                            >
                                {{ $t('settings.errorAndHalt') }}
                            </bk-radio>
                        </bk-radio-group>
                    </template>
                </constraint-wraper>
            </bk-form-item>
            <bk-form-item>
                <constraint-wraper
                    :classify="CLASSIFY_ENUM.SETTING"
                    field="parallelSetting"
                    :label="$t('template.parallelSetting')"
                >
                    <template v-slot:constraint-area="{ props: { isOverride } }">
                        <bk-radio-group
                            :value="pipelineSetting.runLockType"
                            :disabled="!(editable || isOverride)"
                            @change="handleLockTypeChange"
                        >
                            <div class="run-lock-radio-item">
                                <bk-radio
                                    :disabled="!(editable || isOverride)"
                                    :value="runTypeMap.MULTIPLE"
                                >
                                    {{ $t('settings.runningOption.multiple') }}
                                </bk-radio>
                            </div>
                        </bk-radio-group>
                        <div
                            v-if="isMultipleLock"
                            class="single-lock-sub-form"
                            :key="pipelineSetting.runLockType"
                        >
                            <bk-form-item
                                :label="$t('settings.concurrentMaxConcurrency')"
                                error-display-type="normal"
                                property="maxConRunningQueueSize"
                            >
                                <bk-input
                                    type="number"
                                    :disabled="!(editable || isOverride)"
                                    :placeholder="$t('settings.maxConcurrencyPlaceholder')"
                                    v-model="pipelineSetting.maxConRunningQueueSize"
                                    @change="val => handleBaseInfoChange('maxConRunningQueueSize', val ? Number(val) : null)"
                                />
                            </bk-form-item>

                            <bk-form-item
                                :required="isMultipleLock"
                                :label="$t('settings.concurrentTimeout')"
                                error-display-type="normal"
                                property="waitQueueTimeMinute"
                            >
                                <bk-input
                                    type="number"
                                    :disabled="!(editable || isOverride)"
                                    :placeholder="$t('settings.itemPlaceholder')"
                                    v-model="pipelineSetting.waitQueueTimeMinute"
                                    @change="val => handleBaseInfoChange('waitQueueTimeMinute', val ? Number(val) : null)"
                                >
                                    <template slot="append">
                                        <span class="pipeline-setting-unit">{{ $t('settings.minutes') }}</span>
                                    </template>
                                </bk-input>
                            </bk-form-item>
                        </div>
                        <bk-radio-group
                            :value="pipelineSetting.runLockType"
                            @change="handleLockTypeChange"
                        >
                            <div class="run-lock-radio-item">
                                <bk-radio
                                    :disabled="!(editable || isOverride)"
                                    :value="runTypeMap.GROUP"
                                >
                                    {{ $t('settings.runningOption.single') }}
                                </bk-radio>
                            </div>
                        </bk-radio-group>
                        <div
                            v-if="isSingleLock"
                            class="single-lock-sub-form"
                        >
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
                                    :disabled="!(editable || isOverride)"
                                    :max-length="128"
                                    :maxlength="128"
                                    v-model="pipelineSetting.concurrencyGroup"
                                    @change="val => handleBaseInfoChange('concurrencyGroup', val)"
                                />
                            </bk-form-item>

                            <bk-form-item :label="$t('settings.arrivalPolicy')">
                                <bk-radio-group
                                    :value="arrivalPolicy"
                                    @change="handleArrivalPolicyChange"
                                >
                                    <div
                                        v-for="item in arrivalPolicies"
                                        :key="item.id"
                                        class="run-lock-radio-item"
                                    >
                                        <bk-radio
                                            :disabled="!(editable || isOverride)"
                                            :value="item.id"
                                        >
                                            <span
                                                :class="{ 'arrival-policy-tip': item.tip }"
                                                v-bk-tooltips="item.tip ? { content: item.tip, placements: ['top'] } : { disabled: true }"
                                            >{{ item.label }}</span>
                                        </bk-radio>
                                        <p class="arrival-policy-desc">{{ item.desc }}</p>
                                    </div>
                                </bk-radio-group>
                            </bk-form-item>
                            <bk-form-item
                                v-if="showSubGroup"
                                :label="$t('settings.subGroup')"
                                :desc="$t('settings.subGroupDesc')"
                                desc-type="icon"
                                desc-icon="bk-icon icon-question-circle-shape"
                                property="concurrencySubGroup"
                                error-display-type="normal"
                            >
                                <bk-input
                                    :placeholder="$t('settings.subGroupPlaceholder')"
                                    :disabled="!(editable || isOverride)"
                                    :max-length="128"
                                    :maxlength="128"
                                    :value="pipelineSetting.concurrencySubGroup"
                                    @change="val => handleBaseInfoChange('concurrencySubGroup', val)"
                                />
                            </bk-form-item>
                            <template v-if="showQueueFields">
                                <bk-form-item
                                    :label="$t('settings.largestNum')"
                                    error-display-type="normal"
                                    property="maxQueueSize"
                                >
                                    <bk-input
                                        type="number"
                                        :disabled="!(editable || isOverride)"
                                        :placeholder="$t('settings.itemPlaceholder')"
                                        v-model="pipelineSetting.maxQueueSize"
                                        @change="val => handleBaseInfoChange('maxQueueSize', val)"
                                    >
                                        <template slot="append">
                                            <span class="pipeline-setting-unit">{{ $t('settings.item') }}</span>
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
                                        :disabled="!(editable || isOverride)"
                                        :placeholder="$t('settings.itemPlaceholder')"
                                        v-model="pipelineSetting.waitQueueTimeMinute"
                                        @change="val => handleBaseInfoChange('waitQueueTimeMinute', val)"
                                    >
                                        <template slot="append">
                                            <span class="pipeline-setting-unit">{{ $t('settings.minutes') }}</span>
                                        </template>
                                    </bk-input>
                                </bk-form-item>
                            </template>
                        </div>
                    </template>
                </constraint-wraper>
            </bk-form-item>

            <bk-form-item>
                <constraint-wraper
                    :classify="CLASSIFY_ENUM.SETTING"
                    field="buildCancelPolicy"
                    :label="$t('settings.buildCancelPolicyLabel')"
                >
                    <template v-slot:constraint-area="{ props: { isOverride } }">
                        <bk-radio-group
                            :value="pipelineSetting.buildCancelPolicy"
                            @change="val => handleBaseInfoChange('buildCancelPolicy', val)"
                        >
                            <div
                                v-for="(value, key) in BUILD_CANCEL_POLICY"
                                :key="key"
                                class="run-lock-radio-item"
                            >
                                <bk-radio
                                    :disabled="!(editable || isOverride)"
                                    :value="value"
                                >
                                    {{ $t(`settings.buildCancelPolicyOptions.${value}`) }}
                                </bk-radio>
                            </div>
                        </bk-radio-group>
                    </template>
                </constraint-wraper>
            </bk-form-item>

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
    import ConstraintWraper from '@/components/ConstraintWraper.vue'
    import { CLASSIFY_ENUM } from '@/hook/useTemplateConstraint'
    import { BUILD_CANCEL_POLICY } from '@/store/constants'
    import Vue from 'vue'

    export default {
        name: 'bkdevops-running-lock-setting-tab',
        components: {
            VuexInput,
            ConstraintWraper
        },
        props: {
            pipelineSetting: Object,
            editable: {
                type: Boolean,
                default: true
            },
            handleRunningLockChange: Function
        },
        data () {
            return {
                arrivalPolicy: 'QUEUE'
            }
        },
        computed: {
            CLASSIFY_ENUM () {
                return CLASSIFY_ENUM
            },
            BUILD_CANCEL_POLICY () {
                return BUILD_CANCEL_POLICY
            },
            proxyFailIfVariableInvalid: {
                get () {
                    return this.pipelineSetting.failIfVariableInvalid ?? false
                },
                set (val) {
                    Vue.set(this.pipelineSetting, 'failIfVariableInvalid', val)
                }
            },
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
            isMultipleLock () {
                return [this.runTypeMap.MULTIPLE].includes(this.pipelineSetting?.runLockType)
            },
            showSubGroup () {
                return this.arrivalPolicy === 'CANCEL_BATCH' || this.arrivalPolicy === 'KEEP_BATCH'
            },
            showQueueFields () {
                return this.arrivalPolicy !== 'CANCEL_GROUP'
            },
            arrivalPolicies () {
                const batchTip = this.$t('settings.arrivalBatchTip')
                return [
                    {
                        id: 'CANCEL_GROUP',
                        label: this.$t('settings.arrivalCancelGroup'),
                        desc: this.$t('settings.arrivalCancelGroupDesc')
                    },
                    {
                        id: 'QUEUE',
                        label: this.$t('settings.arrivalQueue'),
                        desc: this.$t('settings.arrivalQueueDesc')
                    },
                    {
                        id: 'CANCEL_BATCH',
                        label: this.$t('settings.arrivalCancelBatch'),
                        desc: this.$t('settings.arrivalCancelBatchDesc'),
                        tip: batchTip
                    },
                    {
                        id: 'KEEP_BATCH',
                        label: this.$t('settings.arrivalKeepBatch'),
                        desc: this.$t('settings.arrivalKeepBatchDesc'),
                        tip: batchTip
                    }
                ]
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
                    concurrencySubGroup: [
                        {
                            validator: (val) => {
                                if (!this.showSubGroup) return true
                                return !!(val && String(val).trim())
                            },
                            message: this.$t('settings.subGroupRequired'),
                            trigger: 'blur'
                        }
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
                                if (this.isSingleLock || this.isMultipleLock) {
                                    return intVal <= 1440 && intVal >= 1
                                }
                                return true
                            },
                            message: `${this.isSingleLock
                                ? this.$t('settings.lagestTime')
                                : this.$t('settings.concurrentTimeout')
                            }${this.$t('numberRange', [1, 1440])}`,
                            trigger: 'blur'
                        }
                    ],
                    maxConRunningQueueSize: [
                        requiredRule,
                        {
                            validator: (val) => {
                                if (!val && val !== 0) return true
                                return /^(?:[1-9]|[1-9][0-9]|1[0-9]{2}|200)$/.test(val)
                            },
                            message: this.$t('settings.maxConRunningQueueSizeTips'),
                            trigger: 'blur'
                        }
                    ]
                }
            }
        },
        watch: {
            pipelineSetting: {
                immediate: true,
                handler (setting, oldSetting) {
                    if (!setting) return
                    if (!oldSetting || setting.pipelineId !== oldSetting.pipelineId) {
                        this.arrivalPolicy = this.deriveArrivalPolicy(setting)
                    }
                }
            }
        },
        created () {
            if (this.pipelineSetting?.runLockType === this.runTypeMap.SINGLE) {
                this.handleLockTypeChange(this.runTypeMap.GROUP)
            }
        },
        methods: {
            deriveArrivalPolicy (setting) {
                const subGroup = (setting?.concurrencySubGroup || '').trim()
                if (subGroup) {
                    return setting.concurrencyCancelInProgress ? 'CANCEL_BATCH' : 'KEEP_BATCH'
                }
                return setting?.concurrencyCancelInProgress ? 'CANCEL_GROUP' : 'QUEUE'
            },
            handleArrivalPolicyChange (policy) {
                this.arrivalPolicy = policy
                const cancel = policy === 'CANCEL_GROUP' || policy === 'CANCEL_BATCH'
                const keepSubGroup = policy === 'CANCEL_BATCH' || policy === 'KEEP_BATCH'
                this.handleRunningLockChange({
                    concurrencyCancelInProgress: cancel,
                    concurrencySubGroup: keepSubGroup ? (this.pipelineSetting?.concurrencySubGroup || '') : ''
                })
            },

            handleLockTypeChange (runLockType) {
                this.handleRunningLockChange({
                    runLockType,
                    concurrencyGroup: this.pipelineSetting?.concurrencyGroup || '${{ci.pipeline_id}}'
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
        .variable-invalid {
            color: #63656E;
            font-size: 12px;
            font-weight: 500;

            .variable-radio .bk-form-radio {
                display: inline-block !important;
            }
        }
        .single-lock-sub-form {
            margin-bottom: 20px;
            border-radius: 2px;
            border: 1px solid #DCDEE5;
            padding: 16px;
        }
        .run-lock-radio-item {
            margin: 10px 0;
        }
        .arrival-policy-tip {
            border-bottom: 1px dashed #979ba5;
            cursor: help;
        }
        .arrival-policy-desc {
            margin: 4px 0 0 22px;
            color: #979ba5;
            font-size: 12px;
            line-height: 18px;
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
