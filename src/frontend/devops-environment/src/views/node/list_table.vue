<template>
    <div
        ref="tableContainer"
        class="list-table-container"
    >
        <div
            v-if="isFlod"
            class="expand-btn"
            @click="handleExpandList"
        >
            {{ $t('environment.expandList') }}
            <bk-icon
                type="angle-double-right"
                class="angle-double-right-icon"
            />
        </div>
        <bk-table
            v-bkloading="{ isLoading: tableLoading }"
            :size="tableSize"
            class="node-table-wrapper"
            :row-class-name="tableRowClassName"
            :data="nodeList"
            :pagination="pagination"
            :default-sort="defaultSort"
            :max-height="tableMaxHeight"
            :key="`${isFlod}-${queryNodeHashId}-${isCreateResType}`"
            @row-click="handleRowClick"
            @page-change="handlePageChange"
            @page-limit-change="handlePageLimitChange"
            @sort-change="handleSortChange"
            @selection-change="handleSelectionChange"
        >
            <template v-if="isFlod">
                <bk-table-column
                    :label="$t('environment.nodeInfo.displayName')"
                    prop="displayName"
                    min-width="200"
                >
                    <template slot-scope="props">
                        <div
                            class="table-node-item node-item-id"
                            :class="{ 'is-neutral': isNeutralName(props.row) }"
                        >
                            <span
                                v-if="showDigitalTwinAvatar(props.row)"
                                class="digital-twin-avatar"
                                :style="getAvatarStyle(props.row.displayName)"
                            >
                                {{ getDisplayNameLastChar(props.row.displayName) }}
                            </span>
                            <a
                                v-perm="canShowDetail(props.row) ? {
                                    hasPermission: props.row.canView,
                                    disablePermissionApi: true,
                                    permissionData: {
                                        projectId: projectId,
                                        resourceType: currentResourceType,
                                        resourceCode: props.row.nodeHashId,
                                        action: currentResourceAction.VIEW
                                    }
                                } : {}"
                                class="node-name"
                                :class="{
                                    'pointer': canShowDetail(props.row),
                                    'useless': (!canShowDetail(props.row) || !props.row.canUse) && !isCreateResType,
                                    'unavailable': removedStatus.includes(props.row.nodeStatus),
                                    'is-neutral-name': isNeutralName(props.row)
                                }"
                                :title="props.row.displayName"
                                @click="toNodeDetail(props.row)"
                            >
                                {{ props.row.displayName || '-' }}
                            </a>
                            <bk-tag
                                v-if="isCreateResType && getNeutralLabel(props.row)"
                                class="neutral-status-tag"
                            >
                                {{ getNeutralLabel(props.row) }}
                            </bk-tag>
                        </div>
                    </template>
                </bk-table-column>
            </template>
            <template v-else>
                <bk-table-column
                    type="selection"
                    fixed="left"
                    width="40"
                ></bk-table-column>
                <bk-table-column
                    :label="$t('environment.nodeInfo.displayName')"
                    sortable="custom"
                    prop="displayName"
                    fixed="left"
                    min-width="200"
                >
                    <template slot-scope="props">
                        <div
                            class="bk-form-content node-item-content"
                            v-if="props.row.isEnableEdit"
                        >
                            <div class="edit-content">
                                <input
                                    type="text"
                                    class="bk-form-input env-name-input"
                                    maxlength="30"
                                    name="nodeName"
                                    v-validate.initial="'required'"
                                    v-model="curEditNodeDisplayName"
                                    :class="{ 'is-danger': errors.has('nodeName') }"
                                >
                                <div class="handler-btn">
                                    <span
                                        class="edit-base save"
                                        @click="saveEdit(props.row)"
                                    >{{ $t('environment.save') }}</span>
                                    <span
                                        class="edit-base cancel"
                                        @click="cancelEdit(props.row.nodeHashId)"
                                    >{{ $t('environment.cancel') }}</span>
                                </div>
                            </div>
                        </div>
                        <div
                            class="table-node-item node-item-id"
                            :class="{ 'is-neutral': isNeutralName(props.row) }"
                            v-else
                        >
                            <span
                                v-if="showDigitalTwinAvatar(props.row)"
                                class="digital-twin-avatar"
                                :style="getAvatarStyle(props.row.displayName)"
                            >
                                {{ getDisplayNameLastChar(props.row.displayName) }}
                            </span>
                            <span
                                v-perm="canShowDetail(props.row) ? {
                                    hasPermission: props.row.canView,
                                    disablePermissionApi: true,
                                    permissionData: {
                                        projectId: projectId,
                                        resourceType: currentResourceType,
                                        resourceCode: props.row.nodeHashId,
                                        action: currentResourceAction.VIEW
                                    }
                                } : {}"
                                class="node-name"
                                :class="{
                                    'pointer': canShowDetail(props.row),
                                    'useless': (!canShowDetail(props.row) || !props.row.canUse) && !isCreateResType,
                                    'is-neutral-name': isNeutralName(props.row)
                                }"
                                :title="props.row.displayName"
                                @click="toNodeDetail(props.row)"
                            >
                                {{ props.row.displayName || '-' }}
                            </span>
                            <bk-tag
                                v-if="isCreateResType && getNeutralLabel(props.row)"
                                class="neutral-status-tag"
                            >
                                {{ getNeutralLabel(props.row) }}
                            </bk-tag>
                            <span
                                v-perm="{
                                    hasPermission: props.row.canEdit,
                                    disablePermissionApi: true,
                                    permissionData: {
                                        projectId: projectId,
                                        resourceType: currentResourceType,
                                        resourceCode: props.row.nodeHashId,
                                        action: currentResourceAction.EDIT
                                    }
                                }"
                            >
                                <i
                                    class="devops-icon icon-edit"
                                    v-if="showEditIcon"
                                    @click="editNodeName(props.row)"
                                ></i>
                            </span>
                        </div>
                    </template>
                </bk-table-column>
                <bk-table-column
                    label="IP"
                    sortable="custom"
                    prop="nodeIp"
                    min-width="120"
                    show-overflow-tooltip
                >
                    <template slot-scope="props">
                        {{ props.row.ip || '-' }}
                    </template>
                </bk-table-column>
                <bk-table-column
                    v-if="allRenderColumnMap.label"
                    :label="$t('environment.tag')"
                    prop="label"
                    min-width="200"
                    show-overflow-tooltip
                >
                    <div
                        :ref="`belongsLabelBox_${props.$index}`"
                        slot-scope="props"
                        class="group-label-warpper"
                        v-if="props.row.tags"
                    >
                        <template v-if="labelGroups[props.$index].visibleLabels">
                            <span
                                class="group-tag"
                                v-for="(item, index) in labelGroups[props.$index].visibleLabels"
                                :key="index"
                                :ref="`labelName_${props.$index}`"
                            >
                                <bk-tag class="key">
                                    {{ item.tagKeyName }}: {{ item.tagValues[0].tagValueName }}
                                </bk-tag>
                            </span>
    
                            <bk-popover
                                placement="top"
                                theme="light"
                                ext-cls="group-tag-popover"
                                v-if="labelGroups[props.$index].showMore"
                            >
                                <bk-tag
                                    :ref="`labelMore_${props.$index}`"
                                >
                                    +{{ labelGroups[props.$index].showMore }}
                                </bk-tag>
                                <div slot="content">
                                    <div
                                        v-for="(item, index) in labelGroups[props.$index].hiddenLabels"
                                        class="group-tag"
                                        :key="index"
                                    >
                                        <bk-tag class="key">
                                            {{ item.tagKeyName }}: {{ item.tagValues[0].tagValueName }}
                                        </bk-tag>
                                    </div>
                                </div>
                            </bk-popover>
                        </template>
                    </div>
                    <span v-else>--</span>
                </bk-table-column>
                <bk-table-column
                    v-if="allRenderColumnMap.os"
                    sortable="custom"
                    :label="$t('environment.nodeInfo.os')"
                    min-width="120"
                    prop="osName"
                >
                    <template slot-scope="props">
                        {{ props.row.osName || '-' }}
                    </template>
                </bk-table-column>
                <bk-table-column
                    v-if="allRenderColumnMap.nodeStatus"
                    :label="agentColumnLabel"
                    sortable="custom"
                    min-width="180"
                    prop="nodeStatus"
                >
                    <template slot-scope="props">
                        <!-- 创作流：已释放/已销毁不写 Agent 列；只写正常/异常 + 版本 -->
                        <template v-if="isCreateResType">
                            <div
                                v-if="getCreateAgentStatus(props.row)"
                                class="table-node-item node-item-status"
                            >
                                <span
                                    class="node-status-icon"
                                    :class="getCreateAgentStatus(props.row) === 'normal' ? 'normal-stutus-icon' : 'abnormal-stutus-icon'"
                                ></span>
                                <span class="node-status">
                                    {{ getCreateAgentStatus(props.row) === 'normal'
                                        ? $t('environment.nodeInfo.normal')
                                        : $t('environment.nodeInfo.abnormal') }}
                                </span>
                                <span v-if="props.row.agentVersion">
                                    ({{ props.row.agentVersion }})
                                </span>
                            </div>
                            <span v-else>-</span>
                        </template>
                        <template v-else>
                            <div
                                class="table-node-item node-item-status"
                                v-if="props.row.nodeStatus === 'BUILDING_IMAGE'"
                            >
                                <span class="node-status-icon normal-stutus-icon"></span>
                                <span class="node-status">{{ $t('environment.nodeInfo.normal') }}</span>
                            </div>
                            <div class="table-node-item node-item-status">
                                <span
                                    class="node-status-icon normal-stutus-icon"
                                    v-if="successStatus.includes(props.row.nodeStatus)"
                                ></span>
                                <span
                                    class="node-status-icon abnormal-stutus-icon"
                                    v-if="failStatus.includes(props.row.nodeStatus)"
                                >
                                </span>
                                <span
                                    v-if="runningStatus.includes(props.row.nodeStatus)"
                                    class="loading-icon"
                                >
                                    <bk-loading
                                        theme="primary"
                                        mode="spin"
                                        size="mini"
                                        is-loading
                                    />
                                </span>
                                <span
                                    class="install-agent"
                                    v-if="props.row.nodeStatus === 'RUNNING'"
                                    @click="installAgent(props.row)"
                                >
                                    {{ $t(`environment.nodeStatusMap.${props.row.nodeStatus}`) }}
                                </span>
                                <span
                                    class="node-status"
                                    v-else
                                >
                                    {{ $t(`environment.nodeStatusMap.${props.row.nodeStatus}`) || props.row.nodeStatus }}
                                </span>
                                <span v-if="props.row.agentVersion">
                                    ({{ props.row.agentVersion }})
                                </span>
                            </div>
                        </template>
                    </template>
                </bk-table-column>
                <bk-table-column
                    v-if="allRenderColumnMap.nodeSource"
                    :label="$t('environment.nodeSourceType')"
                    prop="createWorkspaceSource"
                    min-width="100"
                    show-overflow-tooltip
                >
                    <template slot-scope="props">
                        {{ getNodeSourceDisplay(props.row) || '-' }}
                    </template>
                </bk-table-column>
                <bk-table-column
                    v-if="allRenderColumnMap.usage"
                    :label="$t('environment.nodeInfo.usage')"
                    sortable="custom"
                    prop="nodeType"
                    min-width="80"
                    show-overflow-tooltip
                >
                    <template slot-scope="props">
                        {{ usageMap[props.row.nodeType] || '-' }}
                    </template>
                </bk-table-column>
                <bk-table-column
                    v-if="allRenderColumnMap.createdUser"
                    :label="$t('environment.nodeInfo.importer')"
                    sortable="custom"
                    prop="createdUser"
                    min-width="120"
                    show-overflow-tooltip
                ></bk-table-column>
                <bk-table-column
                    v-if="allRenderColumnMap.lastModifyBy"
                    :label="$t('environment.lastModifier')"
                    sortable="custom"
                    prop="lastModifyUser"
                    min-width="120"
                    show-overflow-tooltip
                ></bk-table-column>
                <bk-table-column
                    v-if="allRenderColumnMap.lastModifyTime"
                    :label="$t('environment.nodeInfo.lastModifyTime')"
                    sortable="custom"
                    prop="lastModifiedTime"
                    min-width="180"
                    show-overflow-tooltip
                >
                    <template slot-scope="props">
                        {{ props.row.lastModifyTime || '-' }}
                    </template>
                </bk-table-column>
                <bk-table-column
                    v-if="allRenderColumnMap.latestBuildPipeline"
                    :label="$t('environment.nodeInfo.lastRunPipeline')"
                    min-width="180"
                    sortable="custom"
                    prop="latestBuildPipelineId"
                    show-overflow-tooltip
                >
                    <template slot-scope="props">
                        <span
                            class="pipeline-name"
                            @click="handleToPipelineDetail(props.row.latestBuildDetail)"
                        >
                            {{ props.row?.latestBuildDetail?.pipelineName }}
                        </span>
                    </template>
                </bk-table-column>
                <bk-table-column
                    v-if="allRenderColumnMap.latestBuildTime"
                    :label="$t('environment.nodeInfo.lastRunAs')"
                    prop="latestBuildTime"
                    sortable="custom"
                    min-width="180"
                    show-overflow-tooltip
                >
                    <template slot-scope="props">
                        {{ props.row.lastBuildTime || '--' }}
                    </template>
                </bk-table-column>
                <bk-table-column
                    :label="$t('environment.operation')"
                    fixed="right"
                    width="180"
                >
                    <template slot-scope="props">
                        <template v-if="props.row.canEdit">
                            <div class="table-node-item node-item-handler">
                                <!-- 流水线：异常重装；创作流：Agent 异常重装（中性状态不展示） -->
                                <span
                                    class="install-agent mr10"
                                    v-if="showReinstallAgent(props.row)"
                                    v-perm="{
                                        hasPermission: props.row.canEdit,
                                        disablePermissionApi: true,
                                        permissionData: {
                                            projectId: projectId,
                                            resourceType: currentResourceType,
                                            resourceCode: props.row.nodeHashId,
                                            action: currentResourceAction.EDIT
                                        }
                                    }"
                                    @click="installAgent(props.row)"
                                >
                                    {{ `${$t('environment.reinstallAgent')}` }}
                                </span>
                                <span
                                    v-if="['THIRDPARTY'].includes(props.row.nodeType) || (isCreateResType && !canCleanNode(props.row))"
                                    v-perm="{
                                        hasPermission: props.row.canEdit,
                                        disablePermissionApi: true,
                                        permissionData: {
                                            projectId: projectId,
                                            resourceType: currentResourceType,
                                            resourceCode: props.row.nodeHashId,
                                            action: currentResourceAction.EDIT
                                        }
                                    }"
                                    class="node-handle delete-node-text"
                                    @click.stop="handleSetTag(props.row)"
                                >
                                    {{ $t('environment.setTag') }}
                                </span>
                                <!-- 创作流：仅已释放/已销毁提供清理 -->
                                <span
                                    v-if="isCreateResType && canCleanNode(props.row)"
                                    v-perm="{
                                        hasPermission: props.row.canEdit,
                                        disablePermissionApi: true,
                                        permissionData: {
                                            projectId: projectId,
                                            resourceType: currentResourceType,
                                            resourceCode: props.row.nodeHashId,
                                            action: currentResourceAction.EDIT
                                        }
                                    }"
                                    class="node-handle delete-node-text"
                                    @click.stop="openActionConfirm(props.row, 'clean')"
                                >
                                    {{ $t('environment.clean') }}
                                </span>
                                <!-- 流水线：删除 -->
                                <span
                                    v-if="!isCreateResType && !['TSTACK'].includes(props.row.nodeType)"
                                    v-perm="{
                                        hasPermission: props.row.canDelete,
                                        disablePermissionApi: true,
                                        permissionData: {
                                            projectId: projectId,
                                            resourceType: currentResourceType,
                                            resourceCode: props.row.nodeHashId,
                                            action: currentResourceAction.DELETE
                                        }
                                    }"
                                    class="node-handle delete-node-text"
                                    @click.stop="openActionConfirm(props.row, 'delete')"
                                >
                                    {{ $t('environment.delete') }}
                                </span>
                            </div>
                        </template>
                        <template v-else>
                            <span
                                v-if="!['TSTACK'].includes(props.row.nodeType)"
                                v-bk-tooltips="getCreateNodePermissionTooltips(props.row)"
                            >
                                <bk-button
                                    theme="primary"
                                    outline
                                    :disabled="isApplyPermissionDisabled(props.row)"
                                    @click="handleApplyPermission(props.row)"
                                >
                                    {{ $t('environment.applyPermission') }}
                                </bk-button>
                            </span>
                        </template>
                    </template>
                </bk-table-column>
                <bk-table-column type="setting">
                    <bk-table-setting-content
                        :fields="tableColumn"
                        :selected="selectedTableColumn"
                        :size="tableSize"
                        @setting-change="handleSettingChange"
                    />
                </bk-table-column>
            </template>
            <template #empty>
                <EmptyTableStatus
                    :type="(searchValue.length || tagSearchValue.length || !!dateTimeRange[1]) ? 'search-empty' : 'empty'"
                    @clear="clearFilter"
                />
            </template>
        </bk-table>
        <NodeActionConfirmDialog
            :visible="actionConfirm.visible"
            :mode="actionConfirm.mode"
            :node="actionConfirm.node"
            :relations="actionConfirm.relations"
            :relations-loading="actionConfirm.relationsLoading"
            :confirm-loading="actionConfirm.confirmLoading"
            @confirm="handleActionConfirm"
            @cancel="closeActionConfirm"
        />
        <bk-sideslider
            :is-show.sync="isShowSetTagSlider"
            :show-mask="false"
            :title="$t('environment.setTag')"
            :quick-close="false"
            :width="640"
            ext-cls="set-tag-slider"
            @hidden="handleCancel"
        >
            <div slot="content">
                <div class="set-Tag-content">
                    <div
                        v-for="(item, index) in setTagForm"
                        :key="index"
                        class="form-item-row"
                    >
                        <bk-select
                            v-model="item.tagKeyId"
                            style="width: 230px;"
                            searchable
                            :name="`key_${index}`"
                            v-validate="'required'"
                            :class="{ 'is-danger': errors.has(`key_${index}`) || getDuplicateTagKeyFlags()[index] }"
                            @change="handleTagKeyChang(index)"
                        >
                            <bk-option
                                v-for="option in tagKeyIdList"
                                :key="option.tagKeyId"
                                :id="option.tagKeyId"
                                :disabled="setTagForm.some(item => item.tagKeyId === option.tagKeyId)"
                                :name="option.tagKeyName"
                            >
                            </bk-option>
                        </bk-select>
                        <span class="key-value">:</span>
                        <bk-select
                            v-model="item.tagValueId"
                            style="width: 230px;"
                            searchable
                            :name="`value_${index}`"
                            v-validate="'required'"
                            :class="{ 'is-danger': errors.has(`value_${index}`) || getDuplicateTagKeyFlags()[index] }"
                        >
                            <bk-option
                                v-for="option in getTagValueIdList(item.tagKeyId)"
                                :key="option.tagValueId"
                                :id="option.tagValueId"
                                :name="option.tagValueName"
                            >
                            </bk-option>
                        </bk-select>
                        
                        <i
                            class="devops-icon icon-plus-circle set-icon"
                            @click="addRow"
                        ></i>
                        <i
                            class="devops-icon icon-minus-circle set-icon"
                            @click="deleteRow(index)"
                        ></i>
                    </div>
                </div>
            </div>
            <div
                slot="footer"
                class="set-Tag-footer"
            >
                <bk-button
                    style="margin-left: 24px;"
                    theme="primary"
                    @click="handleSetConfirm"
                >
                    {{ $t('environment.save') }}
                </bk-button>
                <bk-button
                    theme="default"
                    @click="handleCancel"
                >
                    {{ $t('environment.cancel') }}
                </bk-button>
            </div>
        </bk-sideslider>
    </div>
</template>

<script>
    import EmptyTableStatus from '@/components/empty-table-status'
    import NodeActionConfirmDialog from './components/NodeActionConfirmDialog.vue'
    import {
        NODE_RESOURCE_ACTION,
        NODE_RESOURCE_TYPE,
        CREATIVE_STREAM_NODE_RESOURCE_ACTION,
        CREATIVE_NODE_RESOURCE_TYPE
    } from '@/utils/permission'
    import { ENV_ACTIVE_NODE_TYPE, ALLNODE, SERVICE_RESOURCE_TYPE, CREATE_WORKSPACE_TYPE } from '@/store/constants'
    import { mapActions } from 'vuex'
    import useEnvDetail from '@/hooks/useEnvDetail'
    import useResourceType from '@/hooks/useResourceType'
    import { fetchNodeRelations, invalidateNodeRelationsCache } from '@/hooks/useNodeRelations'
    const NODE_TABLE_COLUMN_CACHE = 'node_list_columns'
    const resourceTypeHelpers = useResourceType()

    export default {
        components: {
            EmptyTableStatus,
            NodeActionConfirmDialog
        },
        props: {
            nodeList: {
                type: Array,
                default: () => []
            },
            tableLoading: {
                type: Boolean,
                default: false
            },
            pagination: {
                type: Object,
                default: () => ({})
            },
            searchValue: {
                type: Array,
                default: () => []
            },
            tagSearchValue: {
                type: Array,
                default: () => []
            },
            dateTimeRange: {
                type: Array,
                default: () => []
            },
            nodeTagList: {
                type: Array,
                default: () => []
            },
            isFlod: {
                type: Boolean,
                default: false
            },
            defaultSort: {
                type: Object,
                default: () => ({})
            }
        },
        data () {
            return {
                ENV_ACTIVE_NODE_TYPE,
                ALLNODE,
                tableMaxHeight: 'auto',
                curEditNodeDisplayName: '',
                isEditNodeStatus: false,
                tableSize: localStorage.getItem('node_table_size') || 'small',
                rawSelectedTableColumn: JSON.parse(localStorage.getItem(NODE_TABLE_COLUMN_CACHE))?.columns || [
                    { id: 'displayName' },
                    { id: 'ip' },
                    { id: 'label' },
                    { id: 'os' },
                    { id: 'nodeStatus' },
                    { id: 'nodeSource' },
                    { id: 'usage' },
                    { id: 'createdUser' },
                    { id: 'lastModifyBy' },
                    { id: 'lastModifyTime' },
                    { id: 'latestBuildPipeline' },
                    { id: 'latestBuildTime' }
                ],
                isShowSetTagSlider: false,
                runningStatus: ['CREATING', 'STARTING', 'STOPPING', 'RESTARTING', 'DELETING', 'BUILDING_IMAGE'],
                successStatus: ['NORMAL', 'BUILD_IMAGE_SUCCESS'],
                failStatus: ['ABNORMAL', 'DELETED', 'LOST', 'BUILD_IMAGE_FAILED', 'UNKNOWN', 'RUNNING'],
                setTagForm: [
                    { tagKeyId: '', tagValueId: '' }
                ],
                currentNodeId: null,
                visibleLabelCountList: {},
                removedStatus: ['NOT_IN_CC', 'NOT_IN_CMDB'],
                actionConfirm: {
                    visible: false,
                    mode: 'delete',
                    node: null,
                    relations: { environments: [], references: [] },
                    relationsLoading: false,
                    confirmLoading: false
                }
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            allRenderColumnMap () {
                return this.selectedTableColumn.reduce((result, item) => {
                    result[item.id] = true
                    return result
                }, {})
            },
            createHiddenColumns () {
                return ['usage', 'createdUser', 'latestBuildPipeline']
            },
            pipelineHiddenColumns () {
                return ['nodeSource']
            },
            selectedTableColumn () {
                if (this.isCreateResType) {
                    const cols = this.rawSelectedTableColumn.filter(col => !this.createHiddenColumns.includes(col.id))
                    // 创作流默认带上节点类型列
                    if (!cols.find(c => c.id === 'nodeSource')) {
                        const statusIdx = cols.findIndex(c => c.id === 'nodeStatus')
                        cols.splice(statusIdx >= 0 ? statusIdx + 1 : cols.length, 0, { id: 'nodeSource' })
                    }
                    return cols
                }
                return this.rawSelectedTableColumn.filter(col => !this.pipelineHiddenColumns.includes(col.id))
            },
            agentColumnLabel () {
                return this.isCreateResType
                    ? this.$t('environment.agentStatusVersion')
                    : `${this.$t('environment.status')}(${this.$t('environment.version')})`
            },
            usageMap () {
                return {
                    DEVCLOUD: this.$t('environment.build'),
                    THIRDPARTY: this.$t('environment.build'),
                    CC: this.$t('environment.deploy'),
                    CMDB: this.$t('environment.deploy'),
                    UNKNOWN: this.$t('environment.deploy'),
                    OTHER: this.$t('environment.deploy')
                }
            },
            tagKeyIdList () {
                return this.nodeTagList
            },
            labelGroups () {
                const res = this.nodeList.map((item, index) => {
                    const { tags = [] } = item
                    const visibleCount = this.visibleLabelCountList[index]
                    
                    if (visibleCount >= 1) {
                        return {
                            visibleLabels: tags.slice(0, visibleCount),
                            hiddenLabels: tags.slice(visibleCount),
                            showMore: tags.length - visibleCount
                        }
                    }

                    return {
                        visibleLabels: tags,
                        hiddenLabels: [],
                        showMore: tags?.length ?? 0
                    }
                })
                return res
            },
            queryNodeHashId () {
                return this.$route.query.nodeHashId
            },
            currentResType () {
                return this.$route.params.resType
            },
            isCreateResType () {
                return this.currentResType === SERVICE_RESOURCE_TYPE.CREATE
            },
            tableColumn () {
                const columns = [
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
                        id: 'label',
                        label: this.$t('environment.tag')
                    },
                    {
                        id: 'os',
                        label: this.$t('environment.nodeInfo.os')
                    },
                    {
                        id: 'nodeStatus',
                        label: this.agentColumnLabel
                    },
                    {
                        id: 'nodeSource',
                        label: this.$t('environment.nodeSourceType')
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
                        label: this.$t('environment.lastModifier')
                    },
                    {
                        id: 'lastModifyTime',
                        label: this.$t('environment.nodeInfo.lastModifyTime')
                    },
                    {
                        id: 'latestBuildPipeline',
                        label: this.$t('environment.nodeInfo.lastRunPipeline')
                    },
                    {
                        id: 'latestBuildTime',
                        label: this.$t('environment.nodeInfo.lastRunAs')
                    }
                ]
                if (this.isCreateResType) {
                    return columns.filter(col => !this.createHiddenColumns.includes(col.id))
                }
                return columns.filter(col => !this.pipelineHiddenColumns.includes(col.id))
            },
            showEditIcon () {
                return !this.isEditNodeStatus && !this.isCreateResType
            },
            currentResourceType () {
                return this.isCreateResType ? CREATIVE_NODE_RESOURCE_TYPE : NODE_RESOURCE_TYPE
            },
            currentResourceAction () {
                return this.isCreateResType ? CREATIVE_STREAM_NODE_RESOURCE_ACTION : NODE_RESOURCE_ACTION
            }
        },
        watch: {
            nodeList: function (val) {
                if (val) {
                    setTimeout(this.calcOverPosTable, 100)
                    this.$nextTick(this.updateTableHeight)
                }
            }
        },
        mounted () {
            this.updateTableHeight()
            window.addEventListener('resize', this.updateTableHeight)
        },
        beforeDestroy () {
            window.removeEventListener('resize', this.updateTableHeight)
        },
        methods: {
            ...mapActions('environment', ['requestNodeTagList', 'requestGetCounts', 'requestNodeSourceCounts']),
            getCreateAgentStatus (row) {
                return resourceTypeHelpers.getAgentStatus(row)
            },
            canCleanNode (row) {
                return resourceTypeHelpers.canCleanNode(row)
            },
            getNeutralLabel (row) {
                const key = resourceTypeHelpers.getNeutralStatusLabelKey(row)
                return key ? this.$t(key) : ''
            },
            isNeutralName (row) {
                return this.isCreateResType && resourceTypeHelpers.isNeutralStatusNode(row)
            },
            showDigitalTwinAvatar (row) {
                return this.isCreateResType && resourceTypeHelpers.isDigitalTwin(row)
            },
            getDisplayNameLastChar (name) {
                const text = (name || '').trim()
                if (!text) return '?'
                return Array.from(text).pop()
            },
            getAvatarStyle (name) {
                const palette = ['#3A84FF', '#2DCB56', '#9B80FF', '#18C0BB', '#FF9C01', '#FF5656']
                const text = (name || '').trim()
                let hash = 0
                for (let i = 0; i < text.length; i++) {
                    hash = (hash + text.charCodeAt(i) * (i + 1)) % palette.length
                }
                return { backgroundColor: palette[hash] || palette[0] }
            },
            getNodeSourceDisplay (row) {
                return resourceTypeHelpers.getNodeSourceLabel(row, this.$t.bind(this))
            },
            showReinstallAgent (row) {
                if (this.isCreateResType) {
                    return resourceTypeHelpers.isAgentPresent(row)
                        && resourceTypeHelpers.getAgentStatus(row) === 'abnormal'
                }
                return ['THIRDPARTY'].includes(row.nodeType) && row.nodeStatus === 'ABNORMAL'
            },
            async openActionConfirm (row, mode) {
                this.actionConfirm = {
                    visible: true,
                    mode,
                    node: row,
                    relations: { environments: [], references: [] },
                    relationsLoading: true,
                    confirmLoading: false
                }
                try {
                    // 所属环境取详情 thirdPartyAgentDetail.envIds
                    let envIds = Array.isArray(row.envIds) ? row.envIds : undefined
                    if (envIds === undefined && row.nodeHashId) {
                        try {
                            const detail = await this.$store.dispatch('environment/requestNodeDetail', {
                                projectId: this.projectId,
                                nodeHashId: row.nodeHashId
                            })
                            if (Array.isArray(detail?.envIds)) {
                                envIds = detail.envIds
                            }
                        } catch (e) {
                            console.error(e)
                        }
                    }
                    const relations = await fetchNodeRelations({
                        projectId: this.projectId,
                        nodeHashId: row.nodeHashId,
                        isCreateResType: this.isCreateResType,
                        agentId: row.agentHashId || row.agentId,
                        envIds,
                        store: this.$store,
                        resType: this.currentResType
                    })
                    this.actionConfirm.relations = relations
                } catch (e) {
                    console.error(e)
                } finally {
                    this.actionConfirm.relationsLoading = false
                }
            },
            closeActionConfirm () {
                this.actionConfirm.visible = false
                this.actionConfirm.confirmLoading = false
            },
            async handleActionConfirm () {
                const row = this.actionConfirm.node
                if (!row) return
                this.actionConfirm.confirmLoading = true
                let message, theme
                try {
                    await this.$store.dispatch('environment/toDeleteNode', {
                        projectId: this.projectId,
                        params: [row.nodeHashId]
                    })
                    message = this.$t('environment.successfullyDeleted')
                    theme = 'success'
                    message && this.$bkMessage({ message, theme })
                    invalidateNodeRelationsCache(row.nodeHashId)
                    this.closeActionConfirm()
                } catch (e) {
                    this.handleError(
                        e,
                        {
                            projectId: this.projectId,
                            resourceType: this.currentResourceType,
                            resourceCode: row.nodeHashId,
                            action: this.isCreateResType
                                ? this.currentResourceAction.EDIT
                                : this.currentResourceAction.DELETE
                        }
                    )
                } finally {
                    this.actionConfirm.confirmLoading = false
                    this.$emit('refresh')
                    await this.requestGetCounts(this.projectId)
                    if (this.isCreateResType) {
                        await this.requestNodeSourceCounts(this.projectId)
                    }
                }
            },
            handleExpandList () {
                this.$emit('toggle-fold')
            },
            updateTableHeight () {
                // 用容器可用高度作为表格最大高度：行数少时表格收缩到内容高度（分页紧贴表格），
                // 行数多时表格在可用区域内滚动，避免出现行与分页之间的大段空白
                this.tableMaxHeight = this.$refs.tableContainer?.offsetHeight || 'auto'
            },
            calcOverPosTable () {
                const tagMargin = 6
                this.visibleLabelCountList = this.nodeList.reduce((acc, item, index) => {
                    if (Array.isArray(item?.tags)) {
                        const labelBoxWidth = this.$refs[`belongsLabelBox_${index}`]?.clientWidth * 2
                        const labelLength = item?.tags.length
                        const moreTag = this.$refs?.[`labelMore_${index}`]?.$el
                        const moreTagWidth = (moreTag?.clientWidth ?? 0) + tagMargin
                        const viewPortWidth = labelBoxWidth - (labelLength > 1 ? moreTagWidth : 0)
                        let sumTagWidth = 0
                        let tagVisibleCount = 0
                        this.$refs[`labelName_${index}`]?.every((label) => {
                            sumTagWidth += label?.offsetWidth + tagMargin
                            const isOverSize = sumTagWidth > viewPortWidth
                            !isOverSize && tagVisibleCount++
                            return !isOverSize
                        })

                        acc[index] = tagVisibleCount
                    }
                    return acc
                }, {})
            },
            getTagValueIdList (tagKeyId) {
                const tag = this.nodeTagList.find(tag => tag.tagKeyId === tagKeyId)
                return tag ? tag.tagValues : []
            },
            handlePageChange (page) {
                this.$emit('page-change', page)
            },
            handlePageLimitChange (limit) {
                this.$emit('page-limit-change', limit)
            },
            handleSortChange (sort) {
                this.$emit('sort-change', sort)
            },
            handleSelectionChange (list) {
                this.$emit('selected-change', list)
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
                                resourceType: this.currentResourceType,
                                resourceCode: node.nodeHashId,
                                action: this.currentResourceAction.EDIT
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
                            this.$emit('updataCurEditNodeItem', '')
                            this.curEditNodeDisplayName = ''
                            this.$emit('refresh')
                        }
                    }
                }
            },
            cancelEdit (nodeId) {
                this.isEditNodeStatus = false
                this.$emit('updataCurEditNodeItem', '')
                this.curEditNodeDisplayName = ''
                this.nodeList.forEach(val => {
                    if (val.nodeHashId === nodeId) {
                        val.isEnableEdit = false
                    }
                })
            },
            handleRowClick (node) {
                if (!this.isFlod) return
                if (this.canShowDetail(node)) {
                    this.$router.replace({
                        params: {
                            projectId: this.projectId,
                            ...this.$route.params,
                        },
                        query: {
                            ...this.$route.query,
                            nodeHashId: node.nodeHashId
                        }

                    })
                }
            },
            toNodeDetail (node) {
                if (this.isFlod) return
                if (this.canShowDetail(node)) {
                    this.$emit('show-detail', node.nodeHashId)
                }
            },
            editNodeName (node) {
                this.curEditNodeDisplayName = node.displayName
                this.isEditNodeStatus = true
                this.$emit('updataCurEditNodeItem', node.nodeHashId)
                this.nodeList.forEach(val => {
                    if (val.nodeHashId === node.nodeHashId) {
                        val.isEnableEdit = true
                    }
                })
            },
            installAgent (node) {
                this.$emit('install-agent', node)
            },
            handleToPipelineDetail (param) {
                if (!param.projectId) return
                window.open(`${window.location.origin}/console/pipeline/${param.projectId}/${param.pipelineId}/detail/${param.buildId}/executeDetail`, '_blank')
            },
            handleSetTag (row) {
                this.isShowSetTagSlider = true
                this.currentNodeId = row.nodeId

                if (row.tags && row.tags.length > 0) {
                    this.setTagForm = row.tags.flatMap(tag =>
                        tag.tagValues.map(value => ({
                            tagKeyId: tag.tagKeyId,
                            tagValueId: value.tagValueId
                        }))
                    )
                } else {
                    this.setTagForm = [{
                        tagKeyId: '',
                        tagValueId: ''
                    }]
                }
            },
            addRow () {
                this.setTagForm.push({
                    tagKeyId: '',
                    tagValueId: ''
                })
            },
            deleteRow (index) {
                const cachedValues = this.setTagForm.map(item => item.tagValueId)
                this.setTagForm.splice(index, 1)

                this.$nextTick(() => {
                    this.setTagForm.forEach((item, i) => {
                        item.tagValueId = cachedValues[i < index ? i : i + 1]
                    })
                })
            },
            hasDuplicateTagKeys () {
                const tagKeys = this.setTagForm.map(item => item.tagKeyId)
                    .filter(key => key !== "")

                return new Set(tagKeys).size !== tagKeys.length
            },
            getDuplicateTagKeyFlags () {
                const tagKeys = this.setTagForm.map(item => item.tagKeyId)
                return tagKeys.map((key, index) => {
                    if (key === "") return false
                    return tagKeys.indexOf(key) !== index
                })
            },
            async handleSetConfirm () {
                try {
                    if (this.hasDuplicateTagKeys()) {
                        this.$bkMessage({
                            message: this.$t('environment.noMultipleValuesPerNode'),
                            theme: 'error'
                        })
                        return
                    }
                    const isValid = await this.$validator.validateAll()
                    if (isValid) {
                        const params = {
                            nodeId: this.currentNodeId,
                            tags: this.setTagForm
                        }
                        const res = await this.$store.dispatch('environment/setNodeTag', {
                            projectId: this.projectId,
                            params
                        })
                        if (res) {
                            this.handleCancel()
                            this.$bkMessage({
                                message: this.$t('environment.successfullySaved'),
                                theme: 'success'
                            })
                            this.$emit('refresh')
                            await this.requestNodeTagList({
                                projectId: this.projectId,
                                createMode: this.isCreateResType
                            })
                        }
                    }
                } catch (err) {
                    this.$bkMessage({
                        message: err.message ? err.message : err,
                        theme: 'error'
                    })
                }
            },
            handleCancel () {
                this.isShowSetTagSlider = false
                this.setTagForm = [{
                    tagKeyId: '',
                    tagValueId: ''
                }]
            },
            handleTagKeyChang (index) {
                this.setTagForm[index].tagValueId = ''
            },
            handleApplyPermission (node) {
                if (this.isApplyPermissionDisabled(node)) return
                this.handleNoPermission({
                    projectId: this.projectId,
                    resourceType: this.currentResourceType,
                    resourceCode: node.nodeHashId,
                    action: this.currentResourceAction.EDIT
                })
            },
            /**
             * 创作流申请权限禁用判断：
             * - 云桌面（CLOUD_DESKTOP_NODE）：权限与普通云桌面一致，可申请
             * - 数字同事（REMOTEDEV / IEG_IMATE）及 DEVCLOUD 等：禁点 + tip 说明
             */
            isApplyPermissionDisabled (row) {
                if (!this.isCreateResType) return false
                return !resourceTypeHelpers.isCloudDesktop(row)
            },
            getCreateNodePermissionTooltips (row) {
                if (!this.isCreateResType) {
                    return { content: '', disabled: true }
                }
                // 数字同事：REMOTEDEV / IEG_IMATE 同一套鉴权说明
                if (resourceTypeHelpers.isDigitalTwin(row)) {
                    return {
                        content: this.$t('environment.createNodeIMatePermissionTip'),
                        disabled: false
                    }
                }
                // 创作流云桌面：可申请权限，不强制 tip；无权限时也可用云桌面说明
                if (resourceTypeHelpers.isCloudDesktop(row)) {
                    return {
                        content: this.$t('environment.createNodePersonalPermissionTip'),
                        disabled: true
                    }
                }
                // TEG iMate Linux
                if (row?.createWorkspaceSource === CREATE_WORKSPACE_TYPE.DEVCLOUD) {
                    return {
                        content: this.$t('environment.createNodeIMatePermissionTip'),
                        disabled: false
                    }
                }
                return { content: '', disabled: true }
            },
            clearFilter () {
                this.$emit('clear-filter')
            },
            handleSettingChange ({ fields, size }) {
                this.rawSelectedTableColumn = Object.freeze(fields)
                this.tableSize = size
                localStorage.setItem(NODE_TABLE_COLUMN_CACHE, JSON.stringify({
                    columns: fields,
                    size
                }))
            },
            canShowDetail (row) {
                return row.nodeType === 'THIRDPARTY' || this.isCreateResType
            },
            tableRowClassName ({ row }) {
                let className = 'node-item-row'
                if (row.nodeHashId === this.queryNodeHashId) {
                    className += ' is-active'
                }
                if (this.removedStatus.includes(row?.nodeStatus)) {
                    className += ' unavailable'
                }
                return className
            }
        }
    }
</script>

<style lang="scss">
  @import '@/scss/conf';
    .list-table-container {
        height: 100%;
    }
    .expand-btn {
        position: absolute;
        z-index: 100;
        height: 42px;
        line-height: 42px;
        left: 175px;
        font-size: 12px;
        color: #3A84FF;
        cursor: pointer;
        text-align: center;
        .angle-double-right-icon {
            position: relative;
            top: 3px;
            font-size: 24px !important;
        }
    }
  .node-table-wrapper {
        .bk-table-body-wrapper,
        .bk-table-pagination-wrapper {
            background-color: #fff !important;
        }
        .node-item-id {
          position: relative;
          color: $primaryColor;
          display: flex;
          align-items: center;
          flex-wrap: nowrap;
          min-width: 0;
          .digital-twin-avatar {
              flex-shrink: 0;
              width: 20px;
              height: 20px;
              margin-right: 8px;
              border-radius: 50%;
              color: #fff;
              font-size: 12px;
              line-height: 20px;
              text-align: center;
          }
          .node-name {
              line-height: 14px;
              display: inline-block;
              white-space: nowrap;
              overflow: hidden;
              text-overflow: ellipsis;
              max-width: 100%;
              &.is-neutral-name {
                  color: #C4C6CC;
              }
          }
          .neutral-status-tag {
              margin-left: 6px;
              flex-shrink: 0;
              background: #F0F1F5;
              color: #979BA5;
              border: none;
          }
          &.is-neutral {
              .digital-twin-avatar {
                  opacity: 0.45;
              }
              .neutral-status-tag {
                  color: #C4C6CC;
              }
          }
          .pointer {
              cursor: pointer;
          }
          .useless {
            color: $fontLigtherColor;
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

      td:last-child {
          cursor: pointer;
      }

      .edit-node-item {
          width: 24%;
      }

      .group-label-warpper {
          display: flex;
          width: 100%;
          flex-wrap: wrap;
          margin: 3px 0;

          .group-tag {
            margin-bottom: 4px;
          }

          .key {
            height: 100%;
            margin-bottom: 2px;
          }
       }

      .node-item-row {
        &.node-row-useless {
          border: #30D878 1px solid;
          cursor: url('../../images/cursor-lock.png'), auto;
          color: $fontLigtherColor;
          .node-count-item {
            color: $fontLigtherColor;
          }
        }
        &.is-active {
            td {
                background: #F0F5FF !important;
                .node-name {
                    color: $primaryColor !important;
                }
            }
        }
        &.unavailable {
            td {
                cursor: pointer;
            }
        }
      }

      .install-agent {
          color: $primaryColor;
          cursor: pointer;
      }

      .node-item-content {
          position: absolute;
          top: 50%;
          transform: translateY(-50%);
          display: flex;
          width: 90%;
          margin-right: 12px;
          z-index: 2;
          .edit-content {
              display: flex;
              width: 100%;
          }
          .bk-form-input {
              height: 30px;
              font-size: 12px;
              width: 100%;
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
              top: 50%;
              transform: translateY(-50%);
              color: #3A84FF;
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

          i {
            vertical-align: middle;
            margin-left: 4px;
          }
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

      .loading-icon {
          display: inline-block;
          position: relative;
          width: 12px;
          top: -12px;
          margin-right: 5px;
      }

      .abnormal-stutus-icon {
          border-color: $failColor;
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

      .pipeline-name {
          cursor: pointer;
          &:hover {
              color: $primaryColor;
          }
      }
  }

  .set-tag-slider {
    .bk-sideslider-content {
      padding: 30px 24px;
      height: calc(100vh - 150px);
    }

    .set-Tag-content {
      .key-value {
        margin: 0 16px;
      }
      .form-item-row {
        display: flex;
        align-items: center;
        margin-bottom: 10px;
      }
      .set-icon {
        cursor: pointer;
        margin-left: 10px;
        font-size: 16px;
      }
      .value-input {
        padding: 0 10px 0 8px;
        background-color: #fff;
        border: 1px solid #c4c6cc;
        border-radius: 2px;
        color: #63656e;
        height: 32px;
        width: 280px;
        outline: none;
        font-size: 12px;
        &:focus {
          border-color: #3a84ff;
        }
        &.is-danger {
          border-color: #ff5656;
          background-color: #fff4f4
        }
      }
    }
  
    .set-Tag-footer {
      box-shadow: 0 -1px 0 0 #DCDEE5;
      line-height: 47px;
      width: 100%;
    }
  }

  .info-content {
      .bk-dialog-type-header .header {
          white-space: normal !important;
      }
  }
</style>