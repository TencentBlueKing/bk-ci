<template>
    <bk-dialog
        v-model="isShow"
        :width="1080"
        :title="$t('environment.nodeInfo.importIMetaNode')"
        header-position="left"
        :close-icon="step === 'list'"
        :show-footer="false"
        :z-index="2000"
        @cancel="handleClose"
    >
        <!-- 列表页 -->
        <template v-if="step === 'list'">
            <!-- 提示信息 -->
            <div class="import-tips">
                <i class="bk-icon icon-info-circle-shape"></i>
                <div class="tips-text">
                    <div>{{ $t('environment.nodeInfo.importIMetaTip1') }}</div>
                    <div>{{ $t('environment.nodeInfo.importIMetaTip2') }}</div>
                    <div>{{ $t('environment.nodeInfo.importIMetaTip3') }}</div>
                </div>
            </div>


            <!-- 节点表格 -->
            <bk-table
                v-bkloading="{ isLoading: loading }"
                :data="pagedData"
                :outer-border="false"
                :header-border="false"
                :header-cell-style="{ background: '#FAFBFD', color: '#63656E', fontSize: '12px' }"
                size="small"
                row-key="deviceId"
                @select="handleSelect"
                @select-all="handleSelectAll"
                style="margin-top: 16px;"
            >
                <bk-table-column
                    type="selection"
                    width="50"
                />
                <bk-table-column
                    :label="$t('environment.nodeInfo.name')"
                    prop="name"
                    width="120"
                    show-overflow-tooltip
                />
                <bk-table-column
                    :label="$t('environment.nodeInfo.nodeId')"
                    prop="deviceId"
                    show-overflow-tooltip
                />
                <bk-table-column
                    label="IP"
                    prop="ip"
                    width="140"
                    show-overflow-tooltip
                />
                <bk-table-column
                    :label="$t('environment.nodeInfo.os')"
                    prop="os"
                    width="100"
                    show-overflow-tooltip
                />
                <bk-table-column
                    :label="$t('environment.nodeInfo.engine')"
                    prop="engine"
                    width="100"
                    show-overflow-tooltip
                />
                <bk-table-column
                    :label="$t('environment.IMetaStatus')"
                    width="130"
                >
                    <template slot-scope="props">
                        <StatusIcon :status="getStatusType(props.row.status)" />
                        <span style="margin-left: 2px;">
                            {{ $t(`environment.IMetaNodeStatus.${props.row.status}`) || props.row.status }}
                        </span>
                    </template>
                </bk-table-column>
                <bk-table-column
                    :label="$t('environment.envInfo.creator')"
                    prop="createUser"
                    width="100"
                    show-overflow-tooltip
                />
                <bk-table-column
                    :label="$t('environment.envInfo.creationTime')"
                    prop="createTime"
                    show-overflow-tooltip
                />
            </bk-table>

            <!-- 分页 -->
            <div
                class="pagination-row"
                v-if="allNodeList.length > 0"
            >
                <bk-pagination
                    :current="pagination.current"
                    :count="allNodeList.length"
                    :limit="pagination.limit"
                    :limit-list="[6, 10, 20, 50]"
                    :show-limit="false"
                    small
                    align="right"
                    @change="handlePageChange"
                />
            </div>

            <!-- 底部操作 -->
            <div class="dialog-footer">
                <div class="footer-left">
                    {{ $t('environment.nodeInfo.totalNodes', { count: allNodeList.length }) }}，
                    {{ $t('environment.selected') }} <span class="selected-count">{{ selectedNodes.length }}</span> {{ $t('environment.nodes') }}
                </div>
                <div class="footer-right">
                    <bk-button
                        theme="primary"
                        :disabled="selectedNodes.length === 0"
                        @click="handleImport"
                        :loading="importLoading"
                    >
                        {{ $t('environment.import') }}
                    </bk-button>
                    <bk-button @click="handleClose">{{ $t('environment.cancel') }}</bk-button>
                </div>
            </div>
        </template>

        <!-- 成功页 -->
        <template v-else-if="step === 'success'">
            <div class="success-page">
                <i class="bk-icon icon-check-1 success-icon"></i>
                <div class="success-title">
                    {{ $t('environment.nodeInfo.importIMetaSuccess', { count: importResultCount }) }}
                </div>
                <div class="success-tips">
                    {{ $t('environment.nodeInfo.importIMetaSuccessTip') }}
                </div>
                <bk-button
                    theme="primary"
                    @click="handleClose"
                >
                    {{ $t('environment.nodeInfo.iGotIt') }}
                </bk-button>
            </div>
        </template>
    </bk-dialog>
</template>

<script>
    import StatusIcon from '@/components/status-icon.vue'

    export default {
        components: {
            StatusIcon
        },
        data () {
            return {
                isShow: false,
                step: 'list', // 'list' | 'success'
                loading: false,
                importLoading: false,
                allNodeList: [], // 接口返回全量数据
                selectedNodes: [], // 选中的节点 nodeHashId 列表
                importResultCount: 0,
                pagination: {
                    current: 1,
                    limit: 6
                }
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            pagedData () {
                const start = (this.pagination.current - 1) * this.pagination.limit
                return this.allNodeList.slice(start, start + this.pagination.limit)
            },
            selectedCount () {
                return this.selectedNodes.length
            }
        },
        methods: {
            async open () {
                this.isShow = true
                this.step = 'list'
                this.selectedNodes = []
                this.pagination.current = 1
                await this.fetchList()
            },

            async fetchList () {
                this.loading = true
                try {
                    const res = await this.$store.dispatch('environment/getUserImateList', {
                        projectId: this.projectId
                    })
                    this.allNodeList = res || []
                } catch (err) {
                    console.error(err)
                    this.allNodeList = []
                    this.$bkMessage({
                        theme: 'error',
                        message: err.message || err || this.$t('environment.failedImport')
                    })
                } finally {
                    this.loading = false
                }
            },

            handleSelect (selection) {
                this.selectedNodes = selection.map(item => ({ deviceId: item.deviceId, name: item.name }))
            },

            handleSelectAll (selection) {
                this.selectedNodes = selection.map(item => ({ deviceId: item.deviceId, name: item.name }))
            },

            getStatusType (status) {
                const successStatus = ['RUNNING', 'NORMAL']
                if (successStatus.includes(status)) return 'success'
                return 'error'
            },

            async handleImport () {
                if (this.selectedNodes.length === 0) return
                this.importLoading = true
                try {
                    const res = await this.$store.dispatch('environment/batchImportImateNodes', {
                        projectId: this.projectId,
                        params: {
                            zoneName: 'shenzhen',
                            os: 'LINUX',
                            agentList: this.selectedNodes.map(node => ({ deviceId: node.deviceId, name: node.name }))
                        }
                    })
                    if (res) {
                        this.importResultCount = this.selectedNodes.length
                        this.step = 'success'
                        this.$emit('import-success')
                    } else {
                        throw new Error('import failed')
                    }
                } catch (err) {
                    console.error(err)
                    this.$bkMessage({
                        theme: 'error',
                        message: err.message || this.$t('environment.failedImport')
                    })
                } finally {
                    this.importLoading = false
                }
            },

            handlePageChange (val) {
                this.pagination.current = val
            },

            handleClose () {
                this.isShow = false
                this.step = 'list'
                this.selectedNodes = []
                this.pagination.current = 1
            }
        }
    }
</script>

<style lang="scss" scoped>
    .import-tips {
        display: flex;
        align-items: flex-start;
        background: #f0f6ff;
        border-radius: 2px;
        padding: 12px 16px;
        margin-bottom: 16px;
        .bk-icon {
            color: #3A84FF;
            font-size: 16px;
            margin-right: 8px;
            flex-shrink: 0;
            margin-top: 1px;
        }
        .tips-text {
            font-size: 12px;
            color: #63656E;
            line-height: 20px;
        }
    }

    .status-normal {
        color: #3FC06D;
        .status-dot {
            display: inline-block;
            width: 8px;
            height: 8px;
            border-radius: 50%;
            background: #3FC06D;
            margin-right: 4px;
        }
    }
    .status-abnormal {
        color: #EA3636;
        .status-dot {
            display: inline-block;
            width: 8px;
            height: 8px;
            border-radius: 50%;
            background: #EA3636;
            margin-right: 4px;
        }
    }

    .pagination-row {
        margin-top: 12px;
    }

    .dialog-footer {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-top: 20px;
        padding-top: 12px;
        border-top: 1px solid #DCDEE5;
        .footer-left {
            font-size: 12px;
            color: #63656E;
            .selected-count {
                color: #3A84FF;
                font-weight: 600;
            }
        }
        .footer-right {
            .bk-button {
                margin-left: 8px;
            }
        }
    }

    .success-page {
        display: flex;
        flex-direction: column;
        align-items: center;
        padding: 40px 0 20px;
        .success-icon {
            font-size: 48px;
            color: #3FC06D;
            background: #E5F6EA;
            width: 72px;
            height: 72px;
            line-height: 72px;
            text-align: center;
            border-radius: 50%;
            margin-bottom: 16px;
        }
        .success-title {
            font-size: 20px;
            color: #313238;
            margin-bottom: 8px;
        }
        .success-tips {
            font-size: 12px;
            color: #979BA5;
            margin-bottom: 24px;
            text-align: center;
            line-height: 20px;
        }
    }
</style>
