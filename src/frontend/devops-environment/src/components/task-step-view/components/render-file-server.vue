
<template>
    <div :class="isServer ? 'execute-history-file-server' : 'execute-history-agent-status'">
        <div
            class="server-agent-text"
            @click="handlerView"
        >
            <template v-if="isServer">
                <span class="strong">{{ hostList.length }}</span>{{ $t('environment.台主机') }}
            </template>
            <template v-else>
                <div class="agent-text">
                    <div v-if="normalHostList.length">
                        {{ $t('environment.正常') }}:
                        <span class="success number">{{ normalHostList.length }}</span>
                    </div>
                    <div v-if="abnormalHostList.length">
                        {{ $t('environment.异常') }}:
                        <span class="error number">{{ abnormalHostList.length }}</span>
                    </div>
                </div>
            </template>
        </div>
        <bk-dialog
            v-model="isShowDetail"
            class="execute-history-step-view-server-detail-dialog"
            :ok-text="$t('environment.关闭')"
            :width="1020">
            <template #header>
                <div class="title">
                    <span>{{ $t('environment.服务器文件-服务器列表') }}</span>
                    <i
                        class="dialog-close-btn bk-icon icon-close"
                        @click="handlerClose" />
                </div>
            </template>
            <div class="content-wraper">
                <bk-table
                    :data="hostList"
                    :outer-border="false"
                    :header-border="false"
                    :max-height="500"
                >
                    <bk-table-column :label="$t('environment.主机ID')" prop="bkHostId"></bk-table-column>
                    <bk-table-column label="Agent ID" prop="bkAgentId"></bk-table-column>
                    <bk-table-column label="IPv4" prop="ip">
                        <template slot-scope="{ row }">
                            {{ row.ip || '--' }}
                        </template>
                    </bk-table-column>
                    <bk-table-column label="IPv6" prop="ipv6">
                        <template slot-scope="{ row }">
                            {{ row.ipv6 || '--' }}
                        </template>
                    </bk-table-column>
                    <bk-table-column :label="$t('environment.管控区域')" prop="bkCloudName"></bk-table-column>
                    <bk-table-column :label="$t('environment.Agent 状态')" prop="alive">
                        <template slot-scope="{ row }">
                            <StatusIcon :status="row.alive ? 'success' : 'error'" />
                            {{ row.alive ? $t('environment.正常') : $t('environment.异常') }}
                        </template>
                    </bk-table-column>
                </bk-table>
            </div>
            <template #footer>
                <bk-button @click="handlerClose">
                    {{ $t('关闭') }}
                </bk-button>
            </template>
        </bk-dialog>
    </div>
</template>
<script>
    import StatusIcon from '@/components/status-icon.vue'
    export default {
        components: {
            StatusIcon
        },
        props: {
            data: {
                type: Object,
                required: true
            },
            type: {
                type: String,
                required: true
            }
        },
        data () {
            return {
                isShowDetail: false,
                hostNodeInfo: {}
            }
        },
        computed: {
            isServer () {
                return this.type === 'server'
            },
            hostList () {
                return this.data.server.hostList || []
            },
            normalHostList () {
                return this.hostList.filter(i => i.alive)
            },
            abnormalHostList () {
                return this.hostList.filter(i => !i.alive)
            }

        },
        methods: {
            handlerView () {
                this.hostNodeInfo = this.data.server.hostList
                this.isShowDetail = true
            },
            handlerClose () {
                this.isShowDetail = false
            }
        }
    }
</script>
<style lang='scss'>
.execute-history-file-server {
    min-height: 30px;
    padding: 5px;
    margin-left: -6px;
    cursor: pointer;

    &:hover {
        background: #f0f1f5;
    }

    .server-agent-text {
        .strong {
            color: #3a84ff;
            font-weight: 700;
            padding-right: 5px;
        }
        .sep-location {
            &::before {
                content: "";
            }
        }
    }
}
.execute-history-agent-status {
    min-height: 30px;
    padding: 5px;
    margin-left: -6px;
    cursor: pointer;
    .agent-text {
        .success {
            color: #3fc06d;
        }
        .error {
            color: #ea3636;
        }
        .number {
            font-weight: 700;
            padding: 0 4px;
        }
    }
}

.execute-history-step-view-server-detail-dialog {
    z-index: 3000;
    .ip-selector-view-host{
        margin-top: 0 !important;
    }

    .bk-dialog-tool {
        display: none;
    }

    .bk-dialog-header,
    .bk-dialog-footer {
        position: relative;
        z-index: 99999;
        background: #fff;
    }

    .bk-dialog-header {
        padding: 0;
    }

    .bk-dialog-wrapper .bk-dialog-header .bk-dialog-header-inner {
        font-size: 20px;
        color: #000;
        text-align: left;
    }

    .bk-dialog-wrapper .bk-dialog-body {
        padding: 0;

        .server-panel {
            height: 100%;

            &.show-detail {
                overflow: hidden;
            }

            .host-detail.show {
                padding-left: 20%;
            }
        }
    }

    .content-wraper {
        height: 500px;
        margin-top: -1px;
    }

    button[name="cancel"] {
        display: none;
    }

    .title {
        position: relative;
        height: 68px;
        padding-top: 0;
        padding-bottom: 0;
        padding-left: 25px;
        font-size: 20px;
        line-height: 68px;
        color: #000;
        text-align: left;
        border-bottom: 1px solid #dcdee5;
    }

    .dialog-close-btn {
        position: absolute;
        top: 5px;
        right: 5px;
        z-index: 1;
        width: 26px;
        height: 26px;
        font-size: 22px;
        font-weight: 700;
        line-height: 26px;
        color: #979ba5;
        text-align: center;
        cursor: pointer;
        border-radius: 50%;

        &:hover {
            background-color: #f0f1f5;
        }
    }
}
</style>
