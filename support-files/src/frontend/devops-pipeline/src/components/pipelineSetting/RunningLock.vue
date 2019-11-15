<template>
    <div v-if="pipelineSetting" class="bkdevops-running-lock-setting-tab">
        <enum-input class="running-lock-radio" :list="runTypeList" :value="pipelineSetting.runLockType" name="runLockType" :handle-change="handleRunningLockChange"></enum-input>
        <div class="bk-form-item opera-lock" v-if="pipelineSetting.runLockType === 'SINGLE'">
            <div class="opera-lock-item">
                <label class="opera-lock-label">{{ $t('settings.largestNum') }}：</label>
                <div>
                    <vuex-input input-type="number" name="maxQueueSize" :placeholder="$t('settings.itemPlaceholder')" v-validate.initial="&quot;required|numeric|max_value:20|min_value:0&quot;" :value="pipelineSetting.maxQueueSize" :handle-change="handleRunningLockChange" />
                    <span>{{ $t('settings.item') }}</span>
                    <p v-if="errors.has('maxQueueSize')" class="is-danger">{{errors.first("maxQueueSize")}}</p>
                </div>
            </div>
            <div class="opera-lock-item">
                <label class="opera-lock-label">{{ $t('settings.lagestTime') }}：</label>
                <div>
                    <vuex-input input-type="number" name="waitQueueTimeMinute" :placeholder="$t('settings.itemPlaceholder')" v-validate.initial="'required|numeric|max_value:1440|min_value:1'" :value="pipelineSetting.waitQueueTimeMinute" :handle-change="handleRunningLockChange" />
                    <span>{{ $t('settings.minutes') }}</span>
                    <p v-if="errors.has('waitQueueTimeMinute')" class="is-danger">{{errors.first("waitQueueTimeMinute")}}</p>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
    import EnumInput from '@/components/atomFormField/EnumInput/index.vue'
    import VuexInput from '@/components/atomFormField/VuexInput/index.vue'
    export default {
        name: 'bkdevops-running-lock-setting-tab',
        components: {
            EnumInput,
            VuexInput
        },
        props: {
            pipelineSetting: Object,
            handleRunningLockChange: Function
        },
        computed: {
            runTypeList () {
                return [
                    {
                        label: this.$t('settings.runningOption.multiple'),
                        value: 'MULTIPLE'
                    },
                    {
                        label: this.$t('settings.runningOption.lock'),
                        value: 'LOCK'
                    },
                    {
                        label: this.$t('settings.runningOption.single'),
                        value: 'SINGLE'
                    }
                ]
            }
        }
    }
</script>

<style lang="scss">
    .bkdevops-running-lock-setting-tab {
        .bkdevops-radio {
            display: block;
            margin: 8px 0;
        }
        .opera-lock-item {
            display: flex;
            align-items: center;
            margin-bottom: 12px;
            font-size: 12px;
            .opera-lock-label {
                line-height: 32px;
                align-self: flex-start;
            }
            > div {
                position: relative;
                width: 360px;
                > span {
                    position: absolute;
                    top: 0;
                    right: 0;
                    line-height: 30px;
                    background: #f2f4f8;
                    padding: 0 20px;
                    border: 1px solid #c4c6cc;
                }
                > p {
                    margin-top:10px;
                    color: #ff5656;
                }
            }
        }
    }
</style>
