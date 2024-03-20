<template>
    <bk-dialog v-model="nodeSelectConf.isShow"
        :width="'900'"
        :ext-cls="'node-select-wrapper'"
        :close-icon="false">
        <div>
            <div class="node-list-header">
                <div class="title">{{ $t('environment.nodeInfo.selectNodeTip') }}
                    <span class="selected-node-prompt">
                        {{ $t('environment.nodeInfo.total') }}<span class="node-count"> {{ pagination.count }} </span>{{ $t('environment.nodes') }}
                    </span>
                    <span class="selected-node-prompt">
                        {{ $t('environment.selected') }}<span class="node-count"> {{ selectedNodeList.length }} </span>{{ $t('environment.nodes') }}
                    </span>
                    <bk-popover placement="right">
                        <i class="devops-icon icon-info-circle"></i>
                        <template slot="content">
                            <p style="max-width: 300px; text-align: left; white-space: normal;word-break: break-all;font-weight: 400;">{{ $t('environment.cmdbNodeDesc') }}</p>
                        </template>
                    </bk-popover>
                </div>
                <div class="search-input-row">
                    <bk-select v-model="operator" class="operator-select" :clearable="false" @change="changeOperator">
                        <bk-option v-for="(option, index) in operatorList"
                            :key="index"
                            :id="option.id"
                            :name="option.name">
                        </bk-option>
                    </bk-select>
                    <div class="biz-search-input">
                        <div class="biz-ip-searcher-wrapper">
                            <div class="biz-searcher" @click="focusSearch" ref="bizSearcher">
                                <ul class="search-key" ref="searchKey">
                                    <li class="key-node" v-for="(entry, index) in searchKeyList" :key="index">
                                        <span>{{ entry }}</span>
                                        <i class="devops-icon icon-close" @click="deleteKey(index)"></i>
                                    </li>
                                    <li class="input-item">
                                        <input type="text" class="search-input" ref="searchInput"
                                            v-model="inputValue"
                                            :style="inputStyle"
                                            @blur="handleBlur"
                                            @paste="paste"
                                            @keyup="keyupHandler">
                                    </li>
                                </ul>
                            </div>
                            <div class="actions">
                                <i class="devops-icon icon-close" @click="deleteAllKey" v-if="searchKeyList.length"></i>
                                <i class="devops-icon icon-search" @click="searchNode"></i>
                            </div>
                            <div class="ip-searcher-footer" v-if="isSearchFooter">
                                <p>{{ $t('environment.nodeInfo.searchNodePlaceholder') }}</p>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            <div class="node-table"
                v-bkloading="{
                    isLoading: loading.isLoading,
                    title: loading.title
                }"
            >
                <bk-table
                    ref="nodeListTable"
                    :data="rowList"
                    height="100%"
                    size="small"
                    ext-cls="node-list-table"
                    :empty-text="$t('environment.nodeEmptyOpertaor')"
                    :pagination="pagination"
                    @page-change="handlePageChange"
                    @page-limit-change="pageLimitChange"
                    @select="toggleNodeSelect"
                    @select-all="toggleAllSelect"
                >
                    <bk-table-column type="selection" width="60" align="center" :selectable="isImported" show-overflow-tooltip></bk-table-column>
                    <bk-table-column label="IP" prop="ip" width="150" show-overflow-tooltip></bk-table-column>
                    <bk-table-column :label="$t('environment.nodeInfo.hostName')" prop="name" show-overflow-tooltip></bk-table-column>
                    <bk-table-column :label="$t('environment.operator')" width="150" prop="operator" show-overflow-tooltip></bk-table-column>
                    <bk-table-column :label="$t('environment.bkOperator')" width="150" prop="bakOperator" show-overflow-tooltip></bk-table-column>
                    <bk-table-column :label="$t('environment.status')" prop="nodeStatus" show-overflow-tooltip>
                        <template slot-scope="{ row }">
                            <span>
                                <StatusIcon v-if="successStatus.includes(row.nodeStatus)" status="success" />
                                <StatusIcon v-else-if="failStatus.includes(row.nodeStatus)" status="error" />
                                <StatusIcon v-else-if="['NOT_INSTALLED'].includes(row.nodeStatus)" status="normal" />
                                {{ ['NOT_IN_CC', 'NOT_IN_CMDB'].includes(row.nodeStatus) ? '' : $t('environment.nodeStatusMap')[row.nodeStatus] }}
                            </span>
                        </template>
                    </bk-table-column>
                </bk-table>
            </div>
        </div>
        <div slot="footer">
            <div class="footer-handler">
                <bk-button theme="primary" @click="confirmFn" :disabled="!selectedNodeList.length || loading.isLoading">{{ importText }}</bk-button>
                <bk-button theme="default" @click="cancelFn" :disabled="loading.isLoading">{{ $t('environment.cancel') }}</bk-button>
            </div>
        </div>
    </bk-dialog>
</template>

<script>
    import { mapGetters } from 'vuex'
    import StatusIcon from '@/components/status-icon.vue'

    export default {
        components: {
            StatusIcon
        },
        props: {
            nodeSelectConf: Object,
            curUserInfo: Object,
            changeCreatedUser: Function,
            query: Function,
            searchInfo: {
                type: Object,
                default: {
                    data: [],
                    onChange: () => {}
                }
            }
        },
        data () {
            return {
                isSearchFooter: false,
                inputValue: '',
                operator: 'operator',
                importText: this.$t('environment.import'),
                searchKeyList: [],
                rowList: [],
                operatorList: [
                    { id: 'operator', name: this.$t('environment.mainOperator') },
                    { id: 'bakOperator', name: this.$t('environment.bkOperator') }
                ],
                loading: {
                    isLoading: false,
                    title: ''
                },
                pagination: {
                    current: 1,
                    count: 0,
                    limit: 8,
                    limitList: [8, 20, 50, 100]
                },
                selectedNodeList: [],
                successStatus: ['NORMAL', 'BUILD_IMAGE_SUCCESS'],
                failStatus: ['ABNORMAL', 'DELETED', 'LOST', 'BUILD_IMAGE_FAILED', 'UNKNOWN']
            }
        },
        computed: {
            ...mapGetters('environment', [
                'getNodeTypeMap',
                'getNodeStatusMap'
            ]),
            projectId () {
                return this.$route.params.projectId
            },
            // 动态计算输入长度
            inputStyle () {
                const tag = this.inputValue
                const charLen = this.getCharLength(tag) + 1
                return { width: charLen * 8 + 'px' }
            }
        },
        watch: {
            async 'nodeSelectConf.isShow' (val) {
                if (!val) {
                    this.rowList = []
                    this.selectedNodeList = []
                    this.inputValue = ''
                    this.operator = 'operator'
                    this.importText = this.$t('environment.import')
                    this.searchKeyList.splice(0, this.searchKeyList.length)
                } else {
                    this.pagination.current = 1
                    await this.getDate()
                }
            },
            operator (val) {
                this.selectedNodeList = []
            }
        },
        methods: {
            async getDate () {
                this.loading.isLoading = true

                try {
                    const params = {
                        bakOperator: this.operator === 'bakOperator',
                        ipList: this.searchKeyList,
                        page: this.pagination.current,
                        pageSize: this.pagination.limit,
                        projectId: this.projectId
                    }
                    const res = await this.$store.dispatch('environment/requestCmdbNode', { params })
                    this.rowList = res.records || []
                    
                    // 回填已经导入的节点
                    this.$nextTick(() => {
                        this.rowList.forEach(i => {
                            if (i.importStatus) {
                                this.$refs.nodeListTable.toggleRowSelection(i, true)
                            }
                        })
                    })
                    this.pagination.count = res.count
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                } finally {
                    this.loading.isLoading = false
                }
            },
            // 获取字符长度，汉字两个字节
            getCharLength (str) {
                const len = str.length
                let bitLen = 0
                for (let i = 0; i < len; i++) {
                    if ((str.charCodeAt(i) & 0xff00) !== 0) {
                        bitLen++
                    }
                    bitLen++
                }
                return bitLen
            },
            changeOperator () {
                this.pagination.current = 1
                this.getDate()
            },
            handlePageChange (page) {
                this.pagination.current = page
                this.getDate()
            },
            pageLimitChange (pageSize) {
                this.pagination.limit = pageSize
                this.pagination.current = 1
                this.getDate()
            },
            handleFocus () {
                this.isSearchFooter = !this.isSearchFooter
            },
            handleBlur () {
                this.isSearchFooter = false
            },
            focusSearch () {
                if (!this.isSearchFooter) {
                    this.isSearchFooter = true
                }
                this.$refs.searchInput.focus()
            },
            deleteAllKey () {
                this.searchKeyList.splice(0, this.searchKeyList.length)
                this.inputValue = ''
            },
            paste (event) {
                const value = event.clipboardData.getData('text')
                let valParams = value.replace(/[\r\n]/g, ' ')
                valParams = valParams.replace(/\t/g, ' ')
                let target = valParams.split(' ')
                target = target.filter(item => {
                    return item
                })
                if (target.length) {
                    this.searchKeyList.push(...target)
                    this.inputValue = ''
                    setTimeout(() => {
                        this.inputValue = ''
                        this.$nextTick(() => {
                            this.$refs.bizSearcher.scrollTo(this.$refs.bizSearcher.offsetWidth + this.$refs.bizSearcher.scrollWidth, 0)
                        })
                    }, 10)
                }
            },
            selectedKey (val) {
                if (val && /\s*\S+/.test(val)) {
                    const target = val.replace(/(^\s*)|(\s*$)/g, '')
                    this.searchKeyList.push(target)
                    this.inputValue = ''

                    this.$nextTick(() => {
                        this.$refs.bizSearcher.scrollTo(this.$refs.bizSearcher.offsetWidth + this.$refs.bizSearcher.scrollWidth, 0)
                    })
                }
            },
            searchNode () {
                this.selectedKey(this.inputValue)
                this.pagination.current = 1
                this.isSearchFooter = false
                this.getDate()
            },
            keyupHandler (event) {
                switch (event.code) {
                    case 'Space':
                        this.selectedKey(this.inputValue)
                        break
                    case 'Enter':
                    case 'NumpadEnter':
                        this.searchNode()
                        console.log(123)
                        break
                    case 'Backspace':
                        if (!this.inputValue) {
                            this.searchKeyList.pop()
                            this.searchNode()
                        }
                        break
                    default:
                        break
                }
            },
            deleteKey (index) {
                this.searchKeyList.splice(index, 1)
                this.pagination.current = 1
                this.getDate()
            },
            toggleNodeSelect (selection) {
                this.selectedNodeList = selection.filter(i => !i.importStatus)
            },
            toggleAllSelect (selection) {
                this.selectedNodeList = selection.filter(i => !i.importStatus)
            },
            async confirmFn () {
                const selectNodeId = []
                this.selectedNodeList.map(node => selectNodeId.push(node.ip))
                let theme, message, agentAbnormalNodesCount, agentNotInstallNodesCount

                this.loading.isLoading = true
                this.importText = `${this.$t('environment.nodeInfo.importing')}...`
                try {
                    const res = await this.$store.dispatch('environment/importCmdbNode', {
                        projectId: this.projectId,
                        params: selectNodeId
                    })
                    agentAbnormalNodesCount = res.agentAbnormalNodesCount
                    agentNotInstallNodesCount = res.agentNotInstallNodesCount
                    theme = 'success'
                } catch (e) {
                    theme = 'error'
                    message = e.message || e
                } finally {
                    this.loading.isLoading = false
                    this.$emit('confirm-fn', {
                        theme,
                        message,
                        agentAbnormalNodesCount,
                        agentNotInstallNodesCount
                    })
                    this.importText = this.$t('environment.import')
                }
            },
            cancelFn () {
                this.$emit('cancel-fn')
            },
            /**
             * 当前行是否可以勾选
             */
            isImported (row) {
                return !row.importStatus
            }
        }
    }
</script>

<style lang="scss">
    @import './../../../scss/conf';

    %flex {
        display: flex;
        align-items: center;
    }

    .node-select-wrapper {
        .bk-dialog-body {
            padding: 3px 24px 1px;
        }
        .bk-dialog-tool {
            display: none;
        }

        .bk-dialog-body {
            padding-left: 0;
            padding-right: 0;
        }

        .node-list-header {
            padding: 20px;
            height: 58px;
            display: flex;
            justify-content: space-between;

            .title {
                line-height: 16px;
                color: $fontWeightColor;

                .icon-info-circle {
                    position: relative;
                    top: 2px;
                    margin-left: 4px;
                }
            }

            .selected-node-prompt {
                margin-left: 6px;
                font-size: 12px;
            }

            .node-count {
                color: $failColor;
            }

            .search-input-row {
                position: absolute;
                top: 10px;
                right: 20px;
            }

            .operator-select {
                float: left;
                width: 120px;
                margin-top: 5px;
                margin-right: 8px;
            }

            .search-tool-row {
                position: relative;
                top: -8px;
            }

            .biz-search-input {
                position: relative;
                display: inline-block;
                width: 320px;
            }

            .biz-ip-searcher-wrapper {
                position: relative;
                width: 100%;
                border: 1px solid #dde4eb;
                background-color: #fff;
                border-radius: 2px;

                .search-key {
                    height: 100%;
                    line-height: 1;

                    li {
                        display: inline-block;
                        margin: -1px 3px;
                        cursor: pointer;
                        position: relative;
                        padding: 2px;
                        border-radius: 2px;
                        // height: 27px;
                        line-height: 1;

                        input {
                            width: 20px;
                            padding: 0;
                            border: 0;
                            -webkit-box-shadow: border-box;
                            box-shadow: border-box;
                            outline: none;
                            max-width: 150px;
                            height: 36px;
                            margin: -3px 3px;
                            margin-left: 0;
                        }
                    }

                    .key-node {
                        background: #ebf4ff;

                        span {
                            display: inline-block;
                            background-color: #ebf4ff;
                            color: #7b7d8a;
                            font-size: 12px;
                            border: none;
                            vertical-align: middle;
                            -webkit-box-sizing: border-box;
                            box-sizing: border-box;
                            overflow: hidden;
                            border-radius: 2px;
                            padding: 0 9px;
                            min-height: 21px;
                            line-height: 22px;
                            word-break: break-all;
                        }

                        i {
                            position: relative;
                            top: 2px;
                            right: 6px;
                        }
                    }
                }

                .biz-searcher::-webkit-scrollbar {
                    display: none;
                }

                .actions {
                    position: absolute;
                    right: 10px;
                    top: 2px;
                    color: #c4ced8;
                    z-index: 10;
                    height: 36px;
                    line-height: 36px;

                    .devops-icon {
                        cursor: pointer;
                    }

                    .icon-close {
                        margin-right: 4px;
                        font-size: 14px;
                    }
                }
            }

            .biz-searcher {
                width: 80%;
                height: 36px;
                border-radius: 2px;
                font-size: 12px;
                position: relative;
                z-index: 1;
                background: #fff;
                cursor: pointer;
                white-space: nowrap;
                margin: 0 5px;
                overflow: hidden;
                overflow-x: scroll;
            }

            .ip-searcher-footer {
                padding-left: 8px;
                position: relative;
                left: -1px;
                top: 2px;
                width: 320px;
                height: 36px;
                line-height: 36px;
                border: 1px solid #dde4eb;
                border-top: none;
                background-color: #fff;
                color: #c3cdd7;
                font-size: 12px;
                z-index: 66;

                p {
                    text-align: left;
                }
            }
        }

        .node-table {
            height: 450px;
            margin: 0;
            border: none;
        }

        .no-data-row {
            padding-top: 40px;
            text-align: center;
            border-top: 1px solid $borderWeightColor;
        }

        .table-node-body {
            height: 425px;
            overflow: auto;
        }

        .table-node-head,
        .table-node-row {
            padding: 0 20px;
            @extend %flex;
            height: 42px;
            border-top: 1px solid $borderWeightColor;
            color: #333C48;
            font-size: 12px;
        }

        .table-node-row {
            color: $fontWeightColor;
            font-weight: normal;
            cursor: pointer;
        }

        .node-item-name {
            flex: 5;
        }

        .node-item-ip,
        .node-item-status,
        .node-item-type,
        .node-item-agstatus,
        .node-item-operator {
            flex: 2;
        }

        .node-item-type {
            flex: 4;
            width: 200px;
        }
        .node-item-agstatus {
            flex: 1;
            min-width: 82px;
        }

        .prompt-operator,
        .edit-operator {
            padding-right: 10px;
            color: #ffbf00;

            .devops-icon {
                margin-right: 6px;
            }
        }

        .edit-operator {
            cursor: pointer;
        }

        .over-content {
            padding-left: 6px;
        }

        .bk-form-checkbox {
            margin-right: 12px;
        }

        .node-bkOperator {
            max-width: 130px;
            text-align: left;
            overflow: hidden;
            text-overflow: ellipsis;
        }

        .checkbox-all {
            padding-top: 0;

            &:before {
                content: '';
                display: block;
                width: 8px;
                height: 8px;
                position: relative;
                top: 14px;
                left: 5px;
                background-color: $lineColor;
            }
        }

        .all-checked {
            &:before {
                display: none;
            }
        }

        .normal-status-node {
            color: #30D878;
        }

        .abnormal-status-node {
            color: $failColor;
        }

        .refresh-status-node {
            color: $primaryColor;
        }

        .footer-handler {
            // text-align: right;

            .bk-button {
                height: 32px;
                line-height: 32px;
            }
        }
    }
    .node-list-table {
        &::before {
            background-color: white !important;
        }
        .bk-table-body-wrapper {
            overflow-y: auto;
        }
        .bk-page-selection-count {
            display: none;
        }
    }
</style>
