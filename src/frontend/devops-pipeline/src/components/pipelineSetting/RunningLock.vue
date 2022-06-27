<template>
    <bk-form
        v-if="pipelineSetting"
        class="bkdevops-running-lock-setting-tab"
        :model="pipelineSetting"
        :rules="formRule"
        form-type="vertical"
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
                :label="$t('group.groupName')"
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
                    :required="isSingleLock && !pipelineSetting.concurrencyCancelInProgress"
                    :label="$t('settings.largestNum')"
                    property="maxQueueSize"
                >
                    <bk-input
                        type="number"
                        :placeholder="$t('settings.itemPlaceholder')"
                        v-model="pipelineSetting.maxQueueSize"
                    />
                </bk-form-item>
                <bk-form-item
                    :required="isSingleLock && !pipelineSetting.concurrencyCancelInProgress"
                    :label="$t('settings.lagestTime')"
                    property="waitQueueTimeMinute"
                >
                    <bk-input
                        type="number"
                        :placeholder="$t('settings.itemPlaceholder')"
                        v-model="pipelineSetting.waitQueueTimeMinute"
                    />
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
                    GROUP: 'GROUP_LOCK',
                    LOCK: 'LOCK'
                }
            },
            isSingleLock () {
                return this.pipelineSetting.runLockType === this.runTypeMap.GROUP
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
                                return !this.isSingleLock || (intVal <= 20 && intVal >= 0)
                            },
                            message: `${this.$t('settings.largestNum')}${this.$t('numberRange', [0, 20])}`,
                            trigger: 'change'
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
                            trigger: 'change'
                        }
                    ]
                }
            }
        },
        methods: {
            handleLockTypeChange (runLockType) {
                this.handleRunningLockChange({
                    runLockType
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
        .single-lock-sub-form {
            margin-left: 20px
        }
        .run-lock-radio-item {
            margin: 10px 0;
        }
    }
</style>
