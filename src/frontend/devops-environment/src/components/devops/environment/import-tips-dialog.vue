<template>
    <bk-dialog
        v-model="isShow"
        ext-cls="import-tips"
        render-directive="if"
        width="450"
        header-position="center"
        :show-footer="false"
        :z-index="2000">
        <header slot="header">
            <i class="bk-icon import-status-icon" :class="iconClass"></i>
        </header>
        <div class="tips-header">
            {{ status === 'success' ? $t('environment.successfullyImported') : $t('environment.failedImport') }}
        </div>
        <div class="tips-content">
            <template v-if="status === 'success'">
                <template v-if="!agentAbnormalNodesCount">
                    <div>{{ $t('environment.nextCan') }}</div>
                    <div>
                        1.{{ $t('environment.use') }}
                        <a class="handle-btn" :href="`${hostName}/console/store/atomStore/detail/atom/JobScriptExecutionA`" target="__blank">{{ $t('environment.jobScriptExecution') }}</a>
                        {{ $t('environment.executionJobScript') }}
                    </div>
                    <div>
                        2.{{ $t('environment.use') }}
                        <a class="handle-btn" :href="`${hostName}/console/store/atomStore/detail/atom/JobPushFile`" target="__blank">{{ $t('environment.jobPushFile') }}</a>
                        {{ $t('environment.executionJobPushFile') }}
                    </div>
                </template>
                <template v-else>
                    <span>{{ $t('environment.有') }}</span>
                    <span v-if="agentNotInstallNodesCount">{{ $t('environment.GSEAgentNotInstall', [agentNotInstallNodesCount]) }}</span>
                    <span v-if="agentAbnormalNodesCount">{{ $t('environment.GSEAgentAbnormal', [agentAbnormalNodesCount]) }}</span>
                    <span>{{ $t('environment.importSuccessTips1') }}</span>
                    <a class="handle-btn" :href="`${hostName}/console/store/atomStore/detail/atom/JobScriptExecutionA`" target="__blank">{{ $t('environment.jobScriptExecution') }}</a>
                    <span>{{ $t('environment.and') }}</span>
                    <a class="handle-btn" :href="`${hostName}/console/store/atomStore/detail/atom/JobPushFile`" target="__blank">{{ $t('environment.jobPushFile') }}</a>
                    <span>{{ $t('environment.importSuccessTips2') }}</span>
                </template>
            </template>
            <template v-else>
                {{ message }}
            </template>
        </div>
    </bk-dialog>
</template>
<script>
    export default {
        props: {
            status: String,
            message: String,
            agentAbnormalNodesCount: Number,
            agentNotInstallNodesCount: Number
        },
        data () {
            return {
                isShow: false
            }
        },
        computed: {
            hostName () {
                return window.location.origin
            },
            iconClass () {
                return this.status === 'success' ? 'icon-check-1 success' : 'icon-close error'
            }
        }
    }
</script>
<style lang="scss">
    .import-tips {
        .tips-header {
            display: inline-block;
            width: 100%;
            font-size: 20px;
            color: #313238;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
            line-height: 1.5;
            text-align: center;
        }
        .tips-content {
            text-align: left !important;
            padding: 16px;
            margin-top: 15px;
            background-color: #f6f6fa;
            font-size: 12px;
        }
        .handle-btn {
            color: #3c96ff;
        }
    }

    .import-status-icon {
        width: 42px;
        height: 42px;
        line-height: 42px;
        font-size: 36px;
        border-radius: 50%;
        &.success {
            background-color: #e5f6ea;
            color: #3fc06d;
        }
        &.error {
            background-color: #fdd;
            color: #ea3636;
        }
    }
</style>
