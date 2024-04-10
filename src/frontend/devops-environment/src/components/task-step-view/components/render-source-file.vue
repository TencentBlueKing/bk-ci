<template>
    <div class="render-server-file">
        <bk-collapse
            v-if="isShowServerFile"
            v-model="activeResult"
            class="host-detail">
            <collapse-item
                v-if="isShowServerFile"
                :active="activeResult"
                name="server">
                <span class="collapse-title">
                    {{ $t('environment.已选择') }}
                    <span class="number strong">{{ serverFileList.length }}</span>
                    {{ $t('environment.个服务器文件') }}
                </span>
                <template #content>
                    <table>
                        <thead>
                            <th style="width: 40%;">
                                {{ $t('environment.文件路径') }}
                            </th>
                            <th style="width: 15%;">
                                {{ $t('environment.服务器列表') }}
                            </th>
                            <th>{{ $t('environment.Agent 状态') }}</th>
                            <th style="width: 20%;">
                                {{ $t('environment.服务器账号') }}
                            </th>
                        </thead>
                        <tbody>
                            <tr
                                v-for="(row, index) in serverFileList"
                                :key="index">
                                <td>
                                    <render-file-path :data="row.fileList" />
                                </td>
                                <td>
                                    <render-file-server type="server" :data="row" />
                                </td>
                                <td>
                                    <render-file-server type="agent" :data="row" />
                                </td>
                                <!-- <td>
                                    <server-host-agent
                                        :host-list="row.host.hostNodeInfo.hostList"
                                        :title="$t('environment.服务器文件-服务器列表')" />
                                </td> -->
                                <td>{{ row.account.name }}</td>
                            </tr>
                        </tbody>
                    </table>
                </template>
            </collapse-item>
        </bk-collapse>
    </div>
</template>
<script>
    import CollapseItem from './collapse-item/'
    import RenderFilePath from './render-file-path'
    import RenderFileServer from './render-file-server'
    // 文件类型
    const FILE_TYPE_SERVER = 1
    // const FILE_TYPE_LOCAL = 2
    // const FILE_TYPE_SOURCE = 3
    export default {
        components: {
            CollapseItem,
            RenderFilePath,
            RenderFileServer
        },
        props: {
            data: {
                type: Array,
                default: () => []
            },
            account: {
                type: Array,
                default: () => []
            }
        },
        data () {
            return {
                activeResult: [],
                serverFileList: []
            }
        },
        computed: {
            isShowServerFile () {
                return this.serverFileList.length > 0
            }
        },
        watch: {
            data: {
                handler (val) {
                    val.forEach((fileItem) => {
                        if (fileItem.fileType === FILE_TYPE_SERVER) {
                            this.serverFileList.push(fileItem)
                        }
                    })
                    if (this.serverFileList.length > 0) {
                        this.activeResult = ['server']
                    }
                },
                immediate: true
            }
        }
    }
</script>
<style lang='scss'>
.render-server-file {
    flex: 1;

    .bk-collapse-item-header {
        display: flex;
        align-items: center;
        padding-left: 23px;

        .collapse-title {
            padding-left: 23px;
        }
        .strong {
            color: #3a84ff;
            font-weight: 700;
        }
    }

    table {
        width: 100%;
        line-height: 20px;
        background: #fff;

        tr:nth-child(n+2) {
            td {
                border-top: 1px solid #dcdee5;
            }
        }

        th,
        td {
            height: 42px;
            padding-top: 5px;
            padding-bottom: 5px;
            padding-left: 16px;
            font-size: 12px;
            text-align: left;

            &:first-child {
                padding-left: 60px;
            }
        }

        th {
            font-weight: normal;
            color: #313238;
            border-bottom: 1px solid #dcdee5;
        }

        td {
            color: #63656e;

            .file-path {
                display: inline-block;
                max-width: 100%;
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;
            }
        }
    }

    .source-file-alias {
        cursor: pointer;
        &:hover {
            color: #3a84ff !important;
            .source-file-icon {
                display: inline;
            }
        }
    }

    .source-file-icon {
        display: none;
    }

    .bk-table-empty-block {
        display: none;
    }

    .source-file-tips-box {
        max-width: 300px;
        max-height: 280px;
        min-width: 60px;
        overflow-y: auto;
        .row {
            word-break: break-all;
        }

        .dot {
            display: inline-block;
            width: 6px;
            height: 6px;
            background: currentcolor;
            border-radius: 50%;
        }
    }
}
</style>
