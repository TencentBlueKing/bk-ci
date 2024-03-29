<template>
    <div class="node-list-wrapper">
        <content-header class="env-header">
            <div slot="left">
                <span>{{ $t('environment.node') }}</span>
                <span v-if="isEnableDashboard" class="dashboard-entry ml5">
                    <i class="devops-icon icon-tiaozhuan jump-icon"></i>
                    <a :href="jumpDashboardUrl" target="_blank">{{ $t('environment.查看构建机监控') }}</a>
                </span>
            </div>
            <div slot="right">
                <template v-if="isExtendTx">
                    <bk-button
                        v-perm="{
                            permissionData: {
                                projectId: projectId,
                                resourceType: NODE_RESOURCE_TYPE,
                                resourceCode: projectId,
                                action: NODE_RESOURCE_ACTION.CREATE
                            }
                        }"
                        theme="primary" @click="toImportNode('cmdb')" key="idcTestMachine">{{ $t('environment.nodeInfo.idcTestMachine') }}</bk-button>
                    <bk-button
                        v-perm="{
                            permissionData: {
                                projectId: projectId,
                                resourceType: NODE_RESOURCE_TYPE,
                                resourceCode: projectId,
                                action: NODE_RESOURCE_ACTION.CREATE
                            }
                        }"
                        theme="primary" @click="toImportNode('construct')" key="thirdPartyBuildMachine">{{ $t('environment.thirdPartyBuildMachine') }}</bk-button>
                </template>
                <bk-button
                    v-else
                    v-perm="{
                        permissionData: {
                            projectId: projectId,
                            resourceType: NODE_RESOURCE_TYPE,
                            resourceCode: projectId,
                            action: NODE_RESOURCE_ACTION.CREATE
                        }
                    }"
                    theme="primary"
                    @click="toImportNode('construct')">
                    {{ $t('environment.nodeInfo.importNode') }}
                </bk-button>
            </div>
        </content-header>
        <section class="sub-view-port" v-bkloading="{
            isLoading: loading.isLoading,
            title: loading.title
        }">
            <template>
                <SearchSelect
                    class="search-input"
                    v-model="searchValue"
                    :placeholder="$t('environment.nodeSearchTips')"
                    :data="filterData"
                    :show-condition="false"
                    clearable
                ></SearchSelect>
                <bk-table
                    v-bkloading="{ isLoading: tableLoading }"
                    :size="tableSize"
                    class="node-table-wrapper"
                    row-class-name="node-item-row"
                    :data="nodeList"
                    :pagination="pagination"
                    @page-change="handlePageChange"
                    @page-limit-change="handlePageLimitChange"
                >
                    <bk-table-column :label="$t('environment.nodeInfo.displayName')" prop="displayName" :show-overflow-tooltip="!isEditNodeStatus">
                        <template slot-scope="props">
                            <div class="bk-form-content node-item-content" v-if="props.row.isEnableEdit">
                                <div class="edit-content">
                                    <input type="text" class="bk-form-input env-name-input"
                                        maxlength="30"
                                        name="nodeName"
                                        v-validate="'required'"
                                        v-model="curEditNodeDisplayName"
                                        :class="{ 'is-danger': errors.has('nodeName') }">
                                    <div class="handler-btn">
                                        <span class="edit-base save" @click="saveEdit(props.row)">{{ $t('environment.save') }}</span>
                                        <span class="edit-base cancel" @click="cancelEdit(props.row.nodeHashId)">{{ $t('environment.cancel') }}</span>
                                    </div>
                                </div>
                            </div>
                            <div class="table-node-item node-item-id" v-else>
                                <span
                                    v-perm="canShowDetail(props.row) ? {
                                        hasPermission: props.row.canView,
                                        disablePermissionApi: true,
                                        permissionData: {
                                            projectId: projectId,
                                            resourceType: NODE_RESOURCE_TYPE,
                                            resourceCode: props.row.nodeHashId,
                                            action: NODE_RESOURCE_ACTION.VIEW
                                        }
                                    } : {}"
                                    class="node-name"
                                    :class="{
                                        'pointer': canShowDetail(props.row),
                                        'useless': !canShowDetail(props.row) || !props.row.canUse,
                                        'unavailable': removedStatus.includes(props.row.nodeStatus)
                                    }"
                                    :title="props.row.displayName"
                                    @click="toNodeDetail(props.row)"
                                >
                                    {{ props.row.displayName || '-' }}
                                </span>
                                <span
                                    v-perm="{
                                        hasPermission: props.row.canEdit,
                                        disablePermissionApi: true,
                                        permissionData: {
                                            projectId: projectId,
                                            resourceType: NODE_RESOURCE_TYPE,
                                            resourceCode: props.row.nodeHashId,
                                            action: NODE_RESOURCE_ACTION.EDIT
                                        }
                                    }"
                                >
                                    <i class="devops-icon icon-edit" v-if="!isEditNodeStatus" @click="editNodeName(props.row)"></i>
                                </span>
                            </div>
                        </template>
                    </bk-table-column>
                    <bk-table-column label="IP" prop="ip" min-width="80" show-overflow-tooltip>
                        <template slot-scope="props">
                            {{ props.row.ip || '-' }}
                        </template>
                    </bk-table-column>
                    <bk-table-column v-if="allRenderColumnMap.os" :label="$t('environment.nodeInfo.os')" prop="osName" show-overflow-tooltip>
                        <template slot-scope="props">
                            {{ props.row.osName || '-' }}
                        </template>
                    </bk-table-column>
                    <bk-table-column v-if="allRenderColumnMap.nodeStatus" :label="`${$t('environment.status')}(${$t('environment.version')})`" prop="nodeStatus" min-width="150" show-overflow-tooltip>
                        <template slot-scope="props">
                            <div class="table-node-item node-item-status">
                                <!-- 责任人已变更 -->
                                <template
                                    v-if="((props.row.nodeType === 'CC' && props.row.createdUser !== props.row.operator && props.row.createdUser !== props.row.bakOperator)
                                        || (props.row.nodeType === 'CMDB' && props.row.createdUser !== props.row.operator && props.row.bakOperator.split(';').indexOf(props.row.createdUser) === -1))"
                                >
                                    <span class="prompt-operator">
                                        <i class="devops-icon icon-exclamation-circle"></i>
                                        {{ $t('environment.nodeInfo.prohibited') }}
                                    </span>
                                </template>
                                <!-- 已从 CMDB 、蓝鲸CC 移除 -->
                                <template v-else-if="removedStatus.includes(props.row.nodeStatus) && deploymentNodes.includes(props.row.nodeType)">
                                    <i class="bk-icon node-removed-icon icon-close error"></i>
                                    <span class="node-removed-message">
                                        {{ removedMessage[props.row.nodeStatus] }}
                                    </span>
                                </template>
                                <template v-else>
                                    <!-- 状态icon -->
                                    <StatusIcon v-if="successStatus.includes(props.row.nodeStatus)" status="success" />
                                    <StatusIcon v-else-if="failStatus.includes(props.row.nodeStatus)" status="error" />
                                    <StatusIcon v-else-if="['NOT_INSTALLED'].includes(props.row.nodeStatus)" status="normal" />
                                
                                    <div v-else-if="runningStatus.includes(props.row.nodeStatus)"
                                        class="bk-spin-loading bk-spin-loading-mini bk-spin-loading-primary loading-icon"
                                    >
                                        <div class="rotate rotate1"></div>
                                        <div class="rotate rotate2"></div>
                                        <div class="rotate rotate3"></div>
                                        <div class="rotate rotate4"></div>
                                        <div class="rotate rotate5"></div>
                                        <div class="rotate rotate6"></div>
                                        <div class="rotate rotate7"></div>
                                        <div class="rotate rotate8"></div>
                                    </div>
                                    <!-- 状态值 -->
                                    <span class="node-status">
                                        {{ $t('environment.nodeStatusMap')[props.row.nodeStatus] }}
                                        <span v-if="props.row.agentVersion">
                                            ({{ props.row.agentVersion }})
                                        </span>
                                        <span
                                            v-if="props.row.nodeStatus === 'RUNNING'"
                                            v-bk-tooltips="$t('environment.查看日志')"
                                            class="log-icon-box"
                                            @click="handleShowLogDetail(props.row)">
                                            <Icon
                                                class="log-icon"
                                                name="log"
                                                size="16"
                                            />
                                        </span>
                                    </span>
                                </template>
                            </div>
                        </template>
                    </bk-table-column>
                    <bk-table-column v-if="allRenderColumnMap.usage" :label="$t('environment.nodeInfo.usage')" prop="usage" min-width="80" show-overflow-tooltip>
                        <template slot-scope="props">
                            {{ usageMap[props.row.nodeType] || '-' }}
                        </template>
                    </bk-table-column>
                    <bk-table-column v-if="allRenderColumnMap.createdUser" :label="$t('environment.nodeInfo.importer')" prop="createdUser" min-width="80" show-overflow-tooltip></bk-table-column>
                    <bk-table-column v-if="allRenderColumnMap.lastModifyBy" :label="$t('environment.nodeInfo.lastModifyBy')" prop="lastModifyUser" min-width="80" show-overflow-tooltip></bk-table-column>
                    <bk-table-column v-if="allRenderColumnMap.lastModifyTime" :label="$t('environment.nodeInfo.lastModifyTime')" prop="lastModifyTime" min-width="80" show-overflow-tooltip>
                        <template slot-scope="props">
                            {{ props.row.lastModifyTime || '-' }}
                        </template>
                    </bk-table-column>
                    <bk-table-column :label="$t('environment.operation')" width="180">
                        <template slot-scope="props">
                            <template v-if="props.row.canUse">
                                <!-- 用途为部署的节点-操作按钮 -->
                                <div class="table-node-item">
                                    <template v-if="deploymentNodes.includes(props.row.nodeType)">
                                        <span
                                            v-bk-tooltips="{
                                                content: $t('environment.主机负责人已变更，请联系主机负责人重新授权使用'),
                                                disabled: userInfo.username === props.row.operator || userInfo.username === props.row.bakOperator
                                            }"
                                        >
                                            <bk-button
                                                v-if="((props.row.nodeType === 'CC' && props.row.createdUser !== props.row.operator && props.row.createdUser !== props.row.bakOperator)
                                                    || (props.row.nodeType === 'CMDB' && props.row.createdUser !== props.row.operator && props.row.bakOperator.split(';').indexOf(props.row.createdUser) === -1))"
                                                class="mr5"
                                                :disabled="!(userInfo.username === props.row.operator || userInfo.username === props.row.bakOperator)"
                                                text
                                                @click="changeCreatedUser(props.row)"
                                            >
                                                {{ $t('environment.重新授权') }}
                                            </bk-button>
                                        </span>
                                        <!-- CC中不存在 - 重新导入 -->
                                        <span
                                            v-bk-tooltips="{
                                                content: $t('environment.你不是主机负责人，请联系主机负责人重新导入使用'),
                                                disabled: userInfo.username === props.row.operator || userInfo.username === props.row.bakOperator
                                            }"
                                        >
                                            <bk-button
                                                v-if="['NOT_IN_CC'].includes(props.row.nodeStatus)"
                                                class="mr5"
                                                text
                                                v-perm="{
                                                    hasPermission: props.row.canEdit,
                                                    disablePermissionApi: true,
                                                    permissionData: {
                                                        projectId: projectId,
                                                        resourceType: NODE_RESOURCE_TYPE,
                                                        resourceCode: props.row.nodeHashId,
                                                        action: NODE_RESOURCE_ACTION.EDIT
                                                    }
                                                }"
                                                :disabled="!(userInfo.username === props.row.operator || userInfo.username === props.row.bakOperator)"
                                                @click="handleReImport(props.row)"
                                            >
                                                {{ $t('environment.reImport') }}
                                            </bk-button>
                                        </span>
                                        <!-- 重装Agent -->
                                        <bk-button
                                            v-if="props.row.nodeStatus === 'ABNORMAL' || (props.row.nodeStatus === 'RUNNING' && props.row.agentStatus === 1)"
                                            v-perm="{
                                                hasPermission: props.row.canEdit,
                                                disablePermissionApi: true,
                                                permissionData: {
                                                    projectId: projectId,
                                                    resourceType: NODE_RESOURCE_TYPE,
                                                    resourceCode: props.row.nodeHashId,
                                                    action: NODE_RESOURCE_ACTION.EDIT
                                                }
                                            }"
                                            :disable="props.row.nodeStatus === 'RUNNING'"
                                            text
                                            class="mr5"
                                            @click="installAgent(props.row)"
                                        >
                                            {{ $t('environment.reinstallAgent') }}
                                        </bk-button>
                                        <!-- 未安装Agent -->
                                        <bk-button
                                            v-if="props.row.nodeStatus === 'NOT_INSTALLED' || (props.row.nodeStatus === 'RUNNING' && props.row.agentStatus === 0)"
                                            v-perm="{
                                                hasPermission: props.row.canEdit,
                                                disablePermissionApi: true,
                                                permissionData: {
                                                    projectId: projectId,
                                                    resourceType: NODE_RESOURCE_TYPE,
                                                    resourceCode: props.row.nodeHashId,
                                                    action: NODE_RESOURCE_ACTION.EDIT
                                                }
                                            }"
                                            :disable="props.row.nodeStatus === 'RUNNING'"
                                            text
                                            class="mr5"
                                            @click="installAgent(props.row)"
                                        >
                                            {{ $t('environment.installAgent') }}
                                        </bk-button>
                                    </template>
                                    <!-- 用途为构建的节点-操作按钮 -->
                                    <template v-else>
                                        <!-- Agent异常 - 重装Agent -->
                                        <bk-button
                                            v-if="props.row.nodeStatus === 'ABNORMAL'"
                                            v-perm="{
                                                hasPermission: props.row.canEdit,
                                                disablePermissionApi: true,
                                                permissionData: {
                                                    projectId: projectId,
                                                    resourceType: NODE_RESOURCE_TYPE,
                                                    resourceCode: props.row.nodeHashId,
                                                    action: NODE_RESOURCE_ACTION.EDIT
                                                }
                                            }"
                                            text
                                            class="mr5"
                                            @click="installAgent(props.row)"
                                        >
                                            {{ $t('environment.reinstallAgent') }}
                                        </bk-button>
                                    </template>
                                    <bk-button
                                        v-if="!['TSTACK'].includes(props.row.nodeType)"
                                        text
                                        v-perm="{
                                            hasPermission: props.row.canDelete,
                                            disablePermissionApi: true,
                                            permissionData: {
                                                projectId: projectId,
                                                resourceType: NODE_RESOURCE_TYPE,
                                                resourceCode: props.row.nodeHashId,
                                                action: NODE_RESOURCE_ACTION.DELETE
                                            }
                                        }"
                                        :disable="props.row.nodeStatus === 'RUNNING'"
                                        class="mr5"
                                        @click.stop="confirmDelete(props.row, index)"
                                    >
                                        {{ $t('environment.delete') }}
                                    </bk-button>
                                    <span id="moreHandler" class="node-handle more-handle"
                                        v-if="props.row.canUse && props.row.nodeType === 'DEVCLOUD'">
                                        <bk-popover
                                            placement="bottom-start"
                                            size="samll"
                                            theme="light">
                                            <span>{{ $t('environment.more') }}</span>
                                            <div slot="content" class="devcloud-menu-list">
                                                <dropdown-list :is-show="showTooltip" @handleNode="handleNode" :node="props.row"></dropdown-list>
                                            </div>
                                        </bk-popover>
                                    </span>
                                </div>
                            </template>
                            <template v-else>
                                <bk-button
                                    v-if="!['TSTACK'].includes(props.row.nodeType)"
                                    theme="primary"
                                    outline
                                    @click="handleApplyPermission(props.row)"
                                >
                                    {{ $t('environment.applyPermission') }}
                                </bk-button>
                            </template>
                        </template>
                    </bk-table-column>
                    <bk-table-column type="setting">
                        <bk-table-setting-content
                            :fields="tableColumn"
                            :selected="selectedTableColumn"
                            :size="tableSize"
                            @setting-change="handleSettingChange" />
                    </bk-table-column>
                    <template #empty>
                        <EmptyTableStatus :type="searchValue.length ? 'search-empty' : 'empty'" @clear="clearFilter" />
                    </template>
                </bk-table>
            </template>
        </section>

        <!-- 导入CMDB -->
        <config-manage-node
            :node-select-conf="cmdbNodeSelectConf"
            @confirm-fn="confirmCmdbFn"
            @cancel-fn="cancelCmdbFn"
        ></config-manage-node>

        <!-- 导入第三方构建机 -->
        <third-construct :construct-tool-conf="constructToolConf"
            :construct-import-form="constructImportForm"
            :connect-node-detail="connectNodeDetail"
            :gateway-list="gatewayList"
            :loading="dialogLoading"
            :requet-construct-node="requetConstructNode"
            :has-permission="hasPermission"
            :empty-tips-config="emptyTipsConfig"
            :confirm-fn="confirmFn"
            :cancel-fn="cancelFn"
            :is-agent="isAgent"
            :node-ip="nodeIp"
        ></third-construct>

        <make-mirror-dialog
            :current-node="createImageNode"
            :make-mirror-conf="makeMirrorConf"
            @cancelMakeMirror="makeMirrorConf.isShow = false"
            @submitMakeMirror="requestList"
        ></make-mirror-dialog>

        <!-- 导入成功、失败提示弹框 -->
        <import-tips-dialog
            ref="importTipsDialog"
            :status="importStatus"
            :message="importMessage"
            :agent-abnormal-nodes-count="agentAbnormalNodesCount"
            :agent-not-install-nodes-count="agentNotInstallNodesCount"
        />

        <!-- 重装/安装Agent -->
        <installAgent
            ref="installAgent"
            :task-id.sync="taskId"
            :inner-ip="installAgentIp"
            :host-id="installHostId"
            :os-type="installOsType"
            @install="handleInstallEnd"
        />
    </div>
</template>

<script>
    import configManageNode from '@/components/devops/environment/config-manage-node'
    import dropdownList from '@/components/devops/environment/dropdown-list'
    import importTipsDialog from '@/components/devops/environment/import-tips-dialog'
    import installAgent from '@/components/devops/environment/install-agent'
    import makeMirrorDialog from '@/components/devops/environment/make-mirror-dialog'
    import thirdConstruct from '@/components/devops/environment/third-construct-dialog'
    import EmptyTableStatus from '@/components/empty-table-status.vue'
    import StatusIcon from '@/components/status-icon.vue'
    import { NODE_RESOURCE_ACTION, NODE_RESOURCE_TYPE } from '@/utils/permission'
    import { getQueryString } from '@/utils/util'
    import SearchSelect from '@blueking/search-select'
    import webSocketMessage from '../utils/webSocketMessage.js'
    // import emptyNode from './empty_node'
    import '@blueking/search-select/dist/styles/index.css'
    const NODE_TABLE_COLUMN_CACHE = 'node_list_columns'

    export default {
        components: {
            // emptyNode,
            thirdConstruct,
            configManageNode,
            dropdownList,
            makeMirrorDialog,
            importTipsDialog,
            StatusIcon,
            installAgent,
            EmptyTableStatus,
            SearchSelect
        },
        data () {
            return {
                NODE_RESOURCE_TYPE,
                NODE_RESOURCE_ACTION,
                curEditNodeItem: '',
                curEditNodeDisplayName: '',
                createImageNode: '',
                nodeIp: '',
                isAgent: false,
                isMultipleBtn: false,
                isEditNodeStatus: false,
                hasPermission: true, // 构建机权限
                showTooltip: false,
                curNodeDialog: '', // 当前弹窗节点
                lastCliCKNode: {},
                nodeList: [], // 节点列表
                allNodeList: [],
                gatewayList: [], // 网关列表
                runningStatus: ['CREATING', 'RUNNING', 'STARTING', 'STOPPING', 'RESTARTING', 'DELETING', 'BUILDING_IMAGE'],
                successStatus: ['NORMAL', 'BUILD_IMAGE_SUCCESS'],
                failStatus: ['ABNORMAL', 'DELETED', 'LOST', 'BUILD_IMAGE_FAILED', 'UNKNOWN'],
                removedStatus: ['NOT_IN_CC', 'NOT_IN_CMDB'],
                removedMessage: {
                    NOT_IN_CMDB: this.$t('environment.节点已从CMDB移除，不可使用'),
                    NOT_IN_CC: this.$t('environment.节点已从蓝鲸CC移除，不可使用')
                },
                tableLoading: false,
                // 页面loading
                loading: {
                    isLoading: false,
                    title: this.$t('environment.loadingTitle')
                },
                // 弹窗loading
                dialogLoading: {
                    isLoading: false,
                    title: ''
                },
                emptyInfo: {
                    title: this.$t('environment.nodeInfo.emptyNode'),
                    desc: this.$t('environment.nodeInfo.emptyNodeTips')
                },
                // 构建机弹窗配置
                constructToolConf: {
                    isShow: false,
                    hasHeader: false,
                    quickClose: false,
                    importText: this.$t('environment.import')
                },
                // 构建机内容
                constructImportForm: {
                    model: 'Linux',
                    location: '',
                    link: ''
                },
                // 构建机信息
                connectNodeDetail: {
                    isConnectNode: false,
                    hostname: '',
                    status: 'UN_IMPORT',
                    os: 'macOS 10.13.4'
                },
                // CMDB弹窗配置
                cmdbNodeSelectConf: {
                    isShow: false,
                    quickClose: false,
                    hasHeader: false,
                    unselected: true,
                    importText: '导入'
                },
                makeMirrorConf: {
                    isShow: false
                },
                // 权限配置
                emptyTipsConfig: {
                    title: this.$t('environment.noPermission'),
                    desc: this.$t('environment.nodeInfo.noCreateNodePermissionTips'),
                    btns: [
                        {
                            type: 'primary',
                            size: 'normal',
                            handler: this.changeProject,
                            text: this.$t('environment.switchProject')
                        },
                        {
                            type: 'success',
                            size: 'normal',
                            handler: this.goToApplyPerm,
                            text: this.$t('environment.applyPermission')
                        }
                    ]
                },
                isEnableDashboard: false,
                bizId: 0,
                selectedTableColumn: [],
                tableSize: 'small',
                searchValue: [],
                importStatus: 'success',
                importMessage: '',
                agentAbnormalNodesCount: 0,
                agentNotInstallNodesCount: 0,
                pagination: {
                    current: 1,
                    count: 0,
                    limit: 10
                },
                requestParams: {},
                buildNodes: ['DEVCLOUD', 'THIRDPARTY'], // Build 构建用途的节点 - 第三方构建机类型
                deploymentNodes: ['CC', 'CMDB', 'UNKNOWN', 'OTHER'], // deployment 部署用途的节点
                installAgentIp: '',
                installHostId: 0,
                installOsType: '',
                isDeleteIng: false,
                taskId: 0 // 查询安装日志 -> 安装Agent任务Id
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            userInfo () {
                return window.userInfo
            },
            allRenderColumnMap () {
                return this.selectedTableColumn.reduce((result, item) => {
                    result[item.id] = true
                    return result
                }, {})
            },
            filterData () {
                const data = [
                    {
                        name: 'IP',
                        id: 'nodeIp',
                        default: true
                    },
                    {
                        name: this.$t('environment.alias'),
                        id: 'displayName'
                    },
                    {
                        name: this.$t('environment.nodeInfo.importer'),
                        id: 'createdUser'
                    },
                    {
                        name: this.$t('environment.lastModifier'),
                        id: 'lastModifiedUser'
                    }
                ]
                return data.filter(data => {
                    return !this.searchValue.find(val => val.id === data.id)
                })
            },
            usageMap () {
                return {
                    DEVCLOUD: this.$t('environment.构建'),
                    THIRDPARTY: this.$t('environment.构建'),
                    CC: this.$t('environment.部署'),
                    CMDB: this.$t('environment.部署'),
                    UNKNOWN: this.$t('environment.部署'),
                    OTHER: this.$t('environment.部署')
                }
            },
            jumpDashboardUrl () {
                return `https://bkm.woa.com/?bizId=${this.bizId}#/grafana/d/bT8qy3NVa`
            }
        },
        watch: {
            projectId: async function (val) {
                this.$router.push({ name: 'envList' })
            },
            // 构建机型变化
            'constructImportForm.model' (val) {
                if (val && !this.isAgent) {
                    this.constructImportForm.link = ''
                    this.constructImportForm.location = ''
                    this.requestGateway()
                }
            },
            'constructImportForm.location' (val) {
                if (val && !this.isAgent) {
                    this.requestDevCommand()
                }
            },
            searchValue (val) {
                if (val.length) {
                    val.forEach(i => {
                        if (i.values) {
                            this.requestParams[i.id] = i.values[0].name
                        } else {
                            this.requestParams[i.id] = i.name
                        }
                    })
                    this.pagination.current = 1
                } else {
                    this.requestParams = {}
                }
                this.requestList(this.requestParams)
            }
        },
        created () {
            this.tableColumn = [
                {
                    id: 'displayName',
                    label: this.$t('environment.nodeInfo.displayName'),
                    disabled: true
                },
                {
                    id: 'ip',
                    label: 'IP',
                    disabled: true
                },
                {
                    id: 'os',
                    label: this.$t('environment.nodeInfo.os')
                },
                {
                    id: 'nodeStatus',
                    label: `${this.$t('environment.status')}(${this.$t('environment.version')})`
                },
                {
                    id: 'usage',
                    label: this.$t('environment.nodeInfo.usage')
                },
                {
                    id: 'createdUser',
                    label: this.$t('environment.nodeInfo.importer')
                },
                {
                    id: 'lastModifyBy',
                    label: this.$t('environment.nodeInfo.lastModifyBy')
                },
                {
                    id: 'lastModifyTime',
                    label: this.$t('environment.nodeInfo.lastModifyTime')
                }

            ]
            const columnsCache = JSON.parse(localStorage.getItem(NODE_TABLE_COLUMN_CACHE))
            if (columnsCache) {
                this.selectedTableColumn = Object.freeze(columnsCache.columns)
                this.tableSize = columnsCache.size
            } else {
                this.selectedTableColumn = Object.freeze([
                    { id: 'displayName' },
                    { id: 'ip' },
                    { id: 'os' },
                    { id: 'nodeStatus' },
                    { id: 'usage' },
                    { id: 'createdUser' },
                    { id: 'lastModifyBy' },
                    { id: 'lastModifyTime' }
                ])
            }

            const urlParams = getQueryString('type')
            if (urlParams) {
                this.constructImportForm.model = urlParams
                this.toImportNode('construct')
            }

            webSocketMessage.installWsMessage(this.requestList)
            this.$once('hook:beforeDestroy', webSocketMessage.unInstallWsMessage)
        },
        async mounted () {
            await this.init()
            await this.getEnableDashboard()
        },
        methods: {
            async init () {
                const {
                    loading
                } = this

                loading.isLoading = true
                loading.title = this.$t('environment.loadingTitle')

                try {
                    this.requestList()
                } catch (err) {
                    this.$bkMessage({
                        message: err.message ? err.message : err,
                        theme: 'error'
                    })
                } finally {
                    setTimeout(() => {
                        this.loading.isLoading = false
                    }, 1000)
                }
            },
            /**
             * 节点列表
             */
            async requestList (params = {}) {
                try {
                    this.tableLoading = true
                    const res = await this.$store.dispatch('environment/requestNodeList', {
                        projectId: this.projectId,
                        params: {
                            ...params,
                            page: this.pagination.current,
                            pageSize: this.pagination.limit
                        }
                    })
                    this.nodeList.splice(0, this.nodeList.length)
                    this.pagination.count = res.count

                    res.records.forEach(item => {
                        item.isEnableEdit = item.nodeHashId === this.curEditNodeItem
                        this.nodeList.push(item)
                    })
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                } finally {
                    this.tableLoading = false
                }
            },
            async getEnableDashboard () {
                try {
                    const res = await this.$store.dispatch('environment/checkEnableDashboard', {
                        projectId: this.projectId
                    })
                    if (res) {
                        this.isEnableDashboard = res.result
                        this.bizId = res.bizId
                    }
                } catch (e) {
                    console.err(e)
                }
            },
            changeProject () {
                this.$toggleProjectMenu(true)
            },
            goToApplyPerm () {
                this.handleNoPermission({
                    projectId: this.projectId,
                    resourceType: NODE_RESOURCE_TYPE,
                    resourceCode: this.projectId,
                    action: NODE_RESOURCE_ACTION.CREATE
                })
            },
            toNodeDetail (node) {
                if (this.canShowDetail(node)) {
                    this.$router.push({
                        name: 'nodeDetail',
                        params: {
                            nodeHashId: node.nodeHashId,
                            enableDashboard: this.enableDashboard
                        }
                    })
                }
            },
            handleApplyPermission (node) {
                this.handleNoPermission({
                    projectId: this.projectId,
                    resourceType: NODE_RESOURCE_TYPE,
                    resourceCode: node.nodeHashId,
                    action: NODE_RESOURCE_ACTION.USE
                })
            },
            /**
             * 删除节点
             */
            async confirmDelete (row, index) {
                const params = []
                const id = row.nodeHashId

                params.push(id)

                this.$bkInfo({
                    theme: 'warning',
                    type: 'warning',
                    title: this.$t('environment.delete'),
                    confirmLoading: true,
                    subTitle: `${this.$t('environment.nodeInfo.deleteNodetips', [row.displayName])}`,
                    confirmFn: async () => {
                        let message, theme
                        try {
                            if (this.isDeleteIng) return
                            this.isDeleteIng = true
                            await this.$store.dispatch('environment/toDeleteNode', {
                                projectId: this.projectId,
                                params
                            })

                            message = this.$t('environment.successfullyDeleted')
                            theme = 'success'

                            message && this.$bkMessage({
                                message,
                                theme
                            })
                        } catch (e) {
                            this.handleError(
                                e,
                                {
                                    projectId: this.projectId,
                                    resourceType: NODE_RESOURCE_TYPE,
                                    resourceCode: row.nodeHashId,
                                    action: NODE_RESOURCE_ACTION.DELETE
                                }
                            )
                        } finally {
                            this.isDeleteIng = false
                            this.requestList()
                        }
                    }
                })
            },
            /**
             * 构建机信息
             */
            async changeCreatedUser (row) {
                this.$bkInfo({
                    title: this.$t('environment.重新授权'),
                    subTitle: this.$t('environment.确认授权节点X在流水线中进行远程脚本执行或构件分发吗', [row.displayName]),
                    confirmFn: async () => {
                        let message, theme

                        try {
                            await this.$store.dispatch('environment/changeCreatedUser', {
                                projectId: this.projectId,
                                nodeHashId: row.nodeHashId
                            })

                            message = this.$t('environment.successfullyModified')
                            theme = 'success'
                        } catch (err) {
                            message = err.message ? err.message : err
                            theme = 'error'
                        } finally {
                            this.$bkMessage({
                                message,
                                theme
                            })
                            this.requestList()
                        }
                    }
                })
            },
            /**
             * 构建机信息
             */
            async requetConstructNode () {
                this.dialogLoading.isLoading = true

                try {
                    const res = await this.$store.dispatch('environment/requetConstructNode', {
                        projectId: this.projectId,
                        agentId: this.constructImportForm.agentId
                    })

                    this.connectNodeDetail = Object.assign({}, res)
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                } finally {
                    this.dialogLoading.isLoading = false
                }
            },
            /**
             * 是否启动了构建机
             */
            async switchConstruct (node) {
                let message, theme
                this.dialogLoading.isLoading = true

                try {
                    const res = await this.$store.dispatch('environment/hasConstructPermission', {
                        projectId: this.projectId
                    })

                    if (res) {
                        this.constructToolConf.isShow = true
                        if (node) {
                            const gateway = node.nodeType === 'DEVCLOUD' ? 'shenzhen' : node.gateway
                            this.constructImportForm.model = node.nodeType === 'DEVCLOUD' ? 'LINUX' : node.osName.toUpperCase()
                            this.requestGateway(gateway, node)
                        } else {
                            this.constructImportForm.model = 'LINUX'
                            this.requestGateway()
                        }
                    } else {
                        message = this.$t('environment.nodeInfo.grayscalePublicBeta')
                        theme = 'warning'

                        this.$bkMessage({
                            message,
                            theme
                        })
                    }

                    this.dialogLoading.isLoading = false
                } catch (err) {
                    message = err.message ? err.message : err
                    theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                }
            },
            /**
             * 获取网关列表
             */
            async requestGateway (gateway, node) {
                try {
                    const res = await this.$store.dispatch('environment/requestGateway', {
                        projectId: this.projectId,
                        model: this.constructImportForm.model
                    })

                    this.gatewayList.splice(0, this.gatewayList.length)
                    res.forEach(item => {
                        this.gatewayList.push(item)
                    })

                    if (this.gatewayList.length && gateway && gateway === 'shenzhen') {
                        this.constructImportForm.location = 'shenzhen'
                    } else if (this.gatewayList.length && gateway && gateway !== 'shenzhen') {
                        const isTarget = this.gatewayList.find(item => item.showName === gateway)
                        this.constructImportForm.location = isTarget && isTarget.zoneName
                    }

                    if (node && this.buildNodes.includes(node.nodeType)) { // 如果是第三方构建机类型则获取构建机详情以获得安装命令或下载链接
                        this.getVmBuildDetail(node.nodeHashId)
                    } else {
                        this.requestDevCommand()
                    }
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    if (err.httpStatus === 403) {
                        this.hasPermission = false
                    } else {
                        this.$bkMessage({
                            message,
                            theme
                        })
                    }
                }
            },
            /**
             * 生成链接
             */
            async requestDevCommand () {
                if (!this.constructImportForm.location && this.gatewayList.length) return

                this.dialogLoading.isLoading = true

                try {
                    const res = await this.$store.dispatch('environment/requestDevCommand', {
                        projectId: this.projectId,
                        model: this.constructImportForm.model,
                        zoneName: this.constructImportForm.location || undefined
                    })

                    this.constructImportForm.link = res.link
                    this.constructImportForm.agentId = res.agentId
                    this.requetConstructNode()
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    if (err.httpStatus === 403) {
                        this.hasPermission = false
                    } else {
                        this.$bkMessage({
                            message,
                            theme
                        })
                    }
                } finally {
                    this.dialogLoading.isLoading = false
                }
            },
            async getVmBuildDetail (nodeHashId) {
                try {
                    const res = await this.$store.dispatch('environment/requestNodeDetail', {
                        projectId: this.projectId,
                        nodeHashId
                    })
                    if (res.os === 'WINDOWS' && res.agentUrl) {
                        this.constructImportForm.link = res.agentUrl
                        this.constructImportForm.agentId = res.agentId
                        this.requetConstructNode()
                    } else if (['MACOS', 'LINUX'].includes(res.os) && res.agentScript) {
                        this.constructImportForm.link = res.agentScript
                        this.constructImportForm.agentId = res.agentId
                        this.requetConstructNode()
                    } else {
                        this.requestDevCommand()
                    }
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                }
            },
            installAgent (node) {
                if (this.buildNodes.includes(node.nodeType)) {
                    this.nodeIp = node.ip
                    this.isAgent = true
                    this.constructToolConf.importText = this.$t('environment.confirm')
                    this.switchConstruct(node)
                } else if (this.deploymentNodes.includes(node.nodeType)) {
                    this.installAgentIp = node.ip
                    this.installHostId = node.bkHostId
                    this.installOsType = node.osType
                    this.$refs.installAgent.isShow = true
                }
            },

            handleShowLogDetail (node) {
                this.installAgentIp = node.ip
                this.installHostId = node.bkHostId
                this.installOsType = node.osType
                this.taskId = node.taskId
                this.$refs.installAgent.isShow = true
            },

            async toImportNode (type) {
                if (type === 'cmdb') {
                    this.cmdbNodeSelectConf.isShow = true
                } else {
                    this.switchConstruct()
                }
            },
            /**
             * 构建机导入节点
             */
            async confirmFn () {
                if (!this.dialogLoading.isLoading) {
                    this.dialogLoading.isLoading = true
                    this.constructToolConf.importText = this.constructToolConf.importText === this.$t('environment.confirm') ? `${this.$t('environment.nodeInfo.submitting')}...` : `${this.$t('environment.nodeInfo.importing')}...`

                    let message, theme

                    try {
                        await this.$store.dispatch('environment/importConstructNode', {
                            projectId: this.projectId,
                            agentId: this.constructImportForm.agentId
                        })

                        message = this.constructToolConf.importText === `${this.$t('environment.submitting')}...` ? this.$t('environment.successfullySubmited') : this.$t('environment.successfullyImported')
                        theme = 'success'
                        this.$bkMessage({
                            message,
                            theme
                        })
                        this.constructToolConf.isShow = false
                    } catch (e) {
                        this.handleError(
                            e,
                            {
                                projectId: this.projectId,
                                resourceType: NODE_RESOURCE_TYPE,
                                resourceCode: this.projectId,
                                action: NODE_RESOURCE_ACTION.CREATE
                            }
                        )
                    } finally {
                        this.dialogLoading.isLoading = false
                        this.dialogLoading.isShow = false
                        this.constructToolConf.importText = this.$t('environment.import')
                        this.requestList()
                    }
                }
            },
            cancelFn () {
                if (!this.dialogLoading.isShow) {
                    this.isAgent = false
                    this.constructToolConf.isShow = false
                    this.dialogLoading.isShow = false
                    this.constructImportForm.link = ''
                    this.constructImportForm.location = ''
                    this.constructToolConf.importText = this.$t('environment.import')
                }
            },
            editNodeName (node) {
                this.curEditNodeDisplayName = node.displayName
                this.isEditNodeStatus = true
                this.curEditNodeItem = node.nodeHashId
                this.nodeList.forEach(val => {
                    if (val.nodeHashId === node.nodeHashId) {
                        val.isEnableEdit = true
                    }
                })
            },
            async saveEdit (node) {
                const valid = await this.$validator.validate()
                const displayName = this.curEditNodeDisplayName.trim()
                if (valid) {
                    let message, theme
                    const params = {
                        displayName
                    }

                    try {
                        await this.$store.dispatch('environment/updateDisplayName', {
                            projectId: this.projectId,
                            nodeHashId: node.nodeHashId,
                            params
                        })

                        message = this.$t('environment.successfullyModified')
                        theme = 'success'
                    } catch (e) {
                        this.handleError(
                            e,
                            {
                                projectId: this.projectId,
                                resourceType: NODE_RESOURCE_TYPE,
                                resourceCode: node.nodeHashId,
                                action: NODE_RESOURCE_ACTION.EDIT
                            }
                        )
                    } finally {
                        if (theme === 'success') {
                            message && this.$bkMessage({
                                message,
                                theme
                            })
                            this.nodeList.forEach(val => {
                                if (val.nodeHashId === node.nodeHashId) {
                                    val.isEnableEdit = false
                                    val.displayName = this.curEditNodeDisplayName
                                }
                            })
                            this.isEditNodeStatus = false
                            this.curEditNodeItem = ''
                            this.curEditNodeDisplayName = ''
                            this.requestList()
                        }
                    }
                }
            },
            cancelEdit (nodeId) {
                this.isEditNodeStatus = false
                this.curEditNodeItem = ''
                this.curEditNodeDisplayName = ''
                this.nodeList.forEach(val => {
                    if (val.nodeHashId === nodeId) {
                        val.isEnableEdit = false
                    }
                })
            },
            async destoryNode (node) {
                const h = this.$createElement
                const content = h('p', {
                    style: {
                        textAlign: 'center'
                    }
                }, `${this.$t('environment.nodeInfo.destoryNode')}？`)

                this.$bkInfo({
                    theme: 'warning',
                    type: 'warning',
                    subHeader: content,
                    confirmFn: async () => {
                        let message, theme
                        try {
                            await this.$store.dispatch('environment/toDestoryNode', {
                                projectId: this.projectId,
                                nodeHashId: node.nodeHashId
                            })

                            message = this.$t('environment.successfullySubmited')
                            theme = 'success'
                        } catch (err) {
                            message = err.message ? err.message : err
                            theme = 'error'
                        } finally {
                            this.$bkMessage({
                                message,
                                theme
                            })
                            this.requestList()
                        }
                    }
                })
            },
            makeImage (node) {
                this.createImageNode = node
                // this.showCreateImage = true
                this.makeMirrorConf.isShow = true
            },
            handleNode (name, canUse, node) {
                if (canUse) {
                    switch (name) {
                        case 'destory':
                            this.destoryNode(node)
                            break
                        case 'makeImage':
                            this.makeImage(node)
                            break
                        default:
                            break
                    }
                }
            },
            canShowDetail (row) {
                return row.nodeType === 'THIRDPARTY' || (row.nodeType === 'DEVCLOUD' && row.nodeStatus === 'NORMAL')
            },
            confirmCmdbFn ({ theme, message, agentAbnormalNodesCount, agentNotInstallNodesCount }) {
                this.importStatus = theme
                this.importMessage = message
                this.agentAbnormalNodesCount = agentAbnormalNodesCount
                this.agentNotInstallNodesCount = agentNotInstallNodesCount
                this.$refs.importTipsDialog.isShow = true
                this.cmdbNodeSelectConf.isShow = false
                this.requestList()
            },
            cancelCmdbFn () {
                this.cmdbNodeSelectConf.isShow = false
            },
            handleSettingChange ({ fields, size }) {
                this.selectedTableColumn = Object.freeze(fields)
                this.tableSize = size
                localStorage.setItem(NODE_TABLE_COLUMN_CACHE, JSON.stringify({
                    columns: fields,
                    size
                }))
            },
            handlePageChange (page) {
                this.pagination.current = page
                this.requestList(this.requestParams)
            },
            
            handlePageLimitChange (limit) {
                this.pagination.current = 1
                this.pagination.limit = limit
                this.requestList(this.requestParams)
            },
            handleReImport (row) {
                const params = []
                params.push({
                    nodeIp: row.ip,
                    nodeId: row.nodeId
                })
                const confirmFn = async () => {
                    let theme, message, agentAbnormalNodesCount, agentNotInstallNodesCount
                    try {
                        const res = await this.$store.dispatch('environment/reImportCmdbNode', {
                            projectId: this.projectId,
                            params
                        })
                        agentAbnormalNodesCount = res.agentAbnormalNodesCount
                        agentNotInstallNodesCount = res.agentNotInstallNodesCount
                        theme = 'success'
                        await this.confirmCmdbFn({ theme, message, agentAbnormalNodesCount, agentNotInstallNodesCount })
                    } catch (e) {
                        theme = 'error'
                        message = e.message || e
                    }
                }
                this.$bkInfo({
                    title: this.$t('environment.确认重新导入节点吗？', [row.ip]),
                    okText: this.$t('environment.confirm'),
                    cancelText: this.$t('environment.cancel'),
                    confirmFn
                })
            },
            clearFilter () {
                this.searchValue = []
            },
            handleInstallEnd () {
                this.requestList(this.requestParams)
            }
        }
    }
</script>

<style lang="scss">
@import './../scss/conf';

    %flex {
        display: flex;
        align-items: center;
    }

    .node-list-wrapper {
        min-width: 1126px;
        height: 100%;
        overflow: hidden;

        .prompt-operator,
        .edit-operator {
            padding-right: 10px;
            color: #ffbf00;

            .devops-icon {
                margin-right: 4px;
            }
        }
        .dashboard-entry {
            color: #3c96ff;
            cursor: pointer;
            a {
                font-size: 14px;
                color: #3c96ff;
            }
        }
        .jump-icon {
            font-size: 18px;
            position: relative;
            top: 2px;
        }

        .edit-operator {
            cursor: pointer;
        }

        .over-handler {
            max-width: 100px;
            min-width: 90px;
        }

        .node-reset,
        .node-handle,
        .install-btn {
            color: $primaryColor;
            cursor: pointer;

            .icon-angle-down {
                display: inline-block;
                margin-left: 4px;
                transition: all ease 0.2s;

                &.icon-flip {
                    transform: rotate(180deg);
                }
            }
        }

        .more-handle {
            position: relative;
        }

        .install-btn {
            margin-left: 4px;
        }

        .node-reset {
            margin-right: 10px;
        }

        .node-status-icon {
            display: inline-block;
            margin-left: 2px;
            width: 10px;
            height: 10px;
            border: 2px solid #30D878;
            border-radius: 50%;
            -webkit-border-radius: 50%;
        }

        .abnormal-stutus-icon {
            border-color: $failColor;
        }

        .normal-stutus-icon {
            border-color: #979ba5;
        }

        .delete-node-text {
            position: relative;
            padding-right: 9px;
        }

        .normal-status-node {
            color: #30D878;
        }

        .abnormal-status-node {
            color: $failColor;
        }

        .node-item-content {
            position: absolute;
            top: 7px;
            display: flex;
            width: 90%;
            min-width: 280px;
            margin-right: 12px;
            z-index: 2;
            .edit-content {
                display: flex;
                width: 100%;
            }
            .bk-form-input {
                height: 30px;
                font-size: 12px;
                min-width: 280px;
                padding-right: 74px;
            }
            .error-tips {
                font-size: 12px;
            }
            .handler-btn {
                display: flex;
                align-items: center;
                margin-left: 10px;
                position: absolute;
                right: 11px;
                top: 7px;
                .edit-base {
                    cursor: pointer;
                }
                .save {
                    margin-right: 8px;
                }
            }
            .is-danger {
                border-color: #ff5656;
                background-color: #fff4f4;
            }
        }

        .node-item-id {
            display: flex;
        }

        .node-table-wrapper {
            margin-top: 20px;
            .bk-table-body-wrapper,
            .bk-table-pagination-wrapper {
                background-color: #fff !important;
            }
            td:first-child {
                position: relative;
                color: $primaryColor;
                .node-name {
                    line-height: 14px;
                    display: inline-block;
                    white-space: nowrap;
                    overflow: hidden;
                    text-overflow: ellipsis;
                }
                .pointer {
                    cursor: pointer;
                }
                .useless {
                  color: $fontLighterColor;
                }
                .unavailable {
                    text-decoration: line-through;
                }
                .icon-edit {
                    position: relative;
                    left: 4px;
                    color: $fontColor;
                    cursor: pointer;
                    display: none;
                }
                &:hover {
                    .icon-edit {
                        display: inline-block;
                    }
                }
            }

            .th-handler,
            td:last-child {
                padding-right: 30px;
            }

            .edit-node-item {
                width: 24%;
            }

            .node-item-row {
              &.node-row-useless {
                cursor: url('../images/cursor-lock.png'), auto;
                color: $fontLighterColor;
                .node-count-item {
                  color: $fontLighterColor;
                }
              }
            }
            .loading-icon {
                display: inline-flex;
                position: relative;
                top: -2px;
                margin-right: 5px;
            }
            .log-icon-box {
                position: relative;
                top: 3px;
            }
            .log-icon {
                cursor: pointer;
                margin-left: 4px;
            }

            .node-removed-icon {
                width: 20px;
                height: 20px;
                line-height: 20px;
                font-size: 16px;
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
            .node-removed-message {
                padding-left: 2px;
                color: #ea3636;
            }
        }
    }

    .search-input {
        width: 500px;
        background: #fff;
        flex: 1;
        ::placeholder {
            color: #c4c6cc;
        }
    }
</style>
