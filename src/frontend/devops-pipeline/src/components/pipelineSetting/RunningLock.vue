<template>
    <bk-form
        v-if="pipelineSetting"
        class="bkdevops-running-lock-setting-tab"
        :model="pipelineSetting"
        :rules="formRule"
        form-type="vertical"
        :label-width="300"
    >
        <bk-form-item :label="$t('settings.parallelSetting')">
            <bk-radio-group :value="pipelineSetting.runLockType" @change="handleLockTypeChange">
                <div class="run-lock-radio-item">
                    <bk-radio
                        :value="runTypeMap.MULTIPLE"
                    >
                        {{$t('settings.runningOption.multiple')}}
                    </bk-radio>
                </div>
                <div class="run-lock-radio-item">
                    <bk-radio
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
                desc-icon="bk-icon icon-info-circle"
                :label="$t('settings.groupName')"
                :desc="$t('settings.lockGroupDesc')"
            >
                <bk-input
                    :placeholder="$t('settings.itemPlaceholder')"
                    v-model="pipelineSetting.concurrencyGroup"
                />
            </bk-form-item>

            <bk-form-item property="concurrencyCancelInProgress">
                <bk-checkbox
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
                        :placeholder="$t('settings.itemPlaceholder')"
                        :min="0"
                        :max="200"
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
        <bk-form-item :label="$t('settings.disableSetting')">
            <span @click="handleLockTypeChange(runTypeMap.LOCK)">
                <bk-radio
                    :checked="pipelineSetting.runLockType === runTypeMap.LOCK"
                    :value="runTypeMap.LOCK"
                >
                    {{$t('settings.runningOption.lock')}}
                </bk-radio>
            </span>
        </bk-form-item>
    </bk-form>

</template>

<script>

    export default {
        name: 'bkdevops-running-lock-setting-tab',
        props: {
            pipelineSetting: Object,
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
                return [this.runTypeMap.GROUP, this.runTypeMap.SINGLE].includes(this.pipelineSetting.runLockType)
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
            // TODO: hack old data ugly!!!!!!
            if (this.pipelineSetting.runLockType === this.runTypeMap.SINGLE) {
                this.handleLockTypeChange(this.runTypeMap.GROUP)
            }
        },
        methods: {
            handleLockTypeChange (runLockType) {
                this.handleRunningLockChange({
                    runLockType,
                    concurrencyGroup: this.pipelineSetting.concurrencyGroup || '${{ci.pipeline_id}}'
                })
            },
            handleConCurrencyCancel (val) {
                this.handleRunningLockChange({
                    concurrencyCancelInProgress: val
                })
            }
        }
    }
</script>

<style lang="scss">
    .bkdevops-running-lock-setting-tab {
        .bk-label {
            font-weight: 900;
        }
        .single-lock-sub-form {
            margin: 0 0 10px 20px;
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
