<template>
    <div v-if="pipelineSetting" class="bkdevops-running-lock-setting-tab">
        <enum-input class="running-lock-radio" :list="runTypeList" :value="pipelineSetting.runLockType" name="runLockType" :handle-change="handleRunningLockChange"></enum-input>
        <div class="bk-form-item opera-lock" v-if="pipelineSetting.runLockType === 'SINGLE'">
            <div class="opera-lock-item">
                <label class="opera-lock-label">最大排队数量：</label>
                <div>
                    <vuex-input input-type="number" name="maxQueueSize" placeholder="请输入" v-validate.initial="&quot;required|numeric|max_value:20|min_value:0&quot;" :value="pipelineSetting.maxQueueSize" :handle-change="handleRunningLockChange" />
                    <span>个</span>
                    <p v-if="errors.has('maxQueueSize')" class="is-danger">{{errors.first("maxQueueSize")}}</p>
                </div>
            </div>
            <div class="opera-lock-item">
                <label class="opera-lock-label">最大排队时长：</label>
                <div>
                    <vuex-input input-type="number" name="waitQueueTimeMinute" placeholder="请输入" v-validate.initial="&quot;required|numeric|max_value:1440|min_value:60&quot;" :value="pipelineSetting.waitQueueTimeMinute" :handle-change="handleRunningLockChange" />
                    <span>分钟</span>
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
                        label: '可同时运行多个构建任务',
                        value: 'MULTIPLE'
                    },
                    {
                        label: '同一时间最多只能运行一个构建任务',
                        value: 'SINGLE'
                    },
                    {
                        label: '锁定流水线，任何触发方式都无法运行',
                        value: 'LOCK'
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
