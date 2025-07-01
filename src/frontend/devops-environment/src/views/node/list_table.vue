<template>
    <div>
        <bk-table
            v-bkloading="{ isLoading: tableLoading }"
            :size="tableSize"
            class="node-table-wrapper"
            row-class-name="node-item-row"
            :data="nodeList"
            :pagination="pagination"
            :max-height="750"
            @page-change="handlePageChange"
            @page-limit-change="handlePageLimitChange"
            @sort-change="handleSortChange"
        >
            <!-- <bk-table-column
            type="selection"
            fixed="left"
            width="40"
        ></bk-table-column> -->
            <bk-table-column
                :label="$t('environment.nodeInfo.displayName')"
                sortable="custom"
                prop="displayName"
                min-width="160"
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
                                v-validate="'required'"
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
                        v-else
                    >
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
                            :class="{ 'pointer': canShowDetail(props.row), 'useless': !canShowDetail(props.row) || !props.row.canUse }"
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
                            <i
                                class="devops-icon icon-edit"
                                v-if="!isEditNodeStatus"
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
                :label="$t('environment.标签')"
                sortable="custom"
                prop="label"
                show-overflow-tooltip
            >
                <template slot-scope="props">
                    {{ props.row.label || '-' }}
                </template>
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
                :label="`${$t('environment.status')}(${$t('environment.version')})`"
                sortable="custom"
                width="180"
                prop="nodeStatus"
            >
                <template slot-scope="props">
                    <div
                        class="table-node-item node-item-status"
                        v-if="props.row.nodeStatus === 'BUILDING_IMAGE'"
                    >
                        <span class="node-status-icon normal-stutus-icon"></span>
                        <span class="node-status">{{ $t('environment.nodeInfo.normal') }}</span>
                    </div>
                    <div class="table-node-item node-item-status">
                        <!-- 状态icon -->
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
                        <!-- 状态值 -->
                        <span
                            class="install-agent"
                            v-if="props.row.nodeStatus === 'RUNNING'"
                            @click="installAgent(props.row)"
                        >
                            {{ $t('environment.nodeStatusMap')[props.row.nodeStatus] }}
                        </span>
                        <span
                            class="node-status"
                            v-else
                        >
                            {{ $t('environment.nodeStatusMap')[props.row.nodeStatus] || props.row.nodeStatus }}
                        </span>
                        <div
                            class="install-agent"
                            v-if="['THIRDPARTY'].includes(props.row.nodeType) && props.row.nodeStatus === 'ABNORMAL'"
                            @click="installAgent(props.row)"
                        >
                            {{ `（${$t('environment.install')}Agent）` }}
                        </div>
                        <span v-if="props.row.agentVersion">
                            ({{ props.row.agentVersion }})
                        </span>
                    </div>
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
                :width="180"
                sortable="custom"
                prop="lastModifiedTime"
                min-width="80"
                show-overflow-tooltip
            >
                <template slot-scope="props">
                    {{ props.row.lastModifyTime || '-' }}
                </template>
            </bk-table-column>
            <bk-table-column
                v-if="allRenderColumnMap.latestBuildPipeline"
                :label="$t('environment.nodeInfo.lastRunPipeline')"
                :width="180"
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
                :width="180"
                :label="$t('environment.nodeInfo.lastRunAs')"
                prop="latestBuildTime"
                sortable="custom"
                min-width="80"
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
                    <template v-if="props.row.canUse">
                        <div class="table-node-item node-item-handler">
                            <span
                                v-if="!['TSTACK'].includes(props.row.nodeType)"
                                v-perm="{
                                    hasPermission: props.row.canEdit,
                                    disablePermissionApi: true,
                                    permissionData: {
                                        projectId: projectId,
                                        resourceType: NODE_RESOURCE_TYPE,
                                        resourceCode: props.row.nodeHashId,
                                        action: NODE_RESOURCE_ACTION.DELETE
                                    }
                                }"
                                class="node-handle delete-node-text"
                                @click.stop="handleSetTag(props.row)"
                            >
                                {{ $t('environment.设置标签') }}
                            </span>
                            <span
                                v-if="!['TSTACK'].includes(props.row.nodeType)"
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
                                class="node-handle delete-node-text"
                                @click.stop="confirmDelete(props.row, index)"
                            >
                                {{ $t('environment.delete') }}
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
                    @setting-change="handleSettingChange"
                />
            </bk-table-column>
            <template #empty>
                <EmptyTableStatus
                    :type="(searchValue.length || !!dateTimeRange[1]) ? 'search-empty' : 'empty'"
                    @clear="clearFilter"
                />
            </template>
        </bk-table>
        <bk-sideslider
            :is-show.sync="isShowSetTagSlider"
            :show-mask="false"
            :title="$t('environment.设置标签')"
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
                            v-model="item.selectValue"
                            style="width: 230px;"
                            searchable
                            :name="`select_${index}`"
                            v-validate="'required'"
                            :class="{ 'is-danger': errors.has(`select_${index}`) }"
                        >
                            <bk-option
                                v-for="option in selectOptions"
                                :key="option.id"
                                :id="option.id"
                                :name="option.name"
                            >
                            </bk-option>
                        </bk-select>
                        <span class="key-value">:</span>
                        <input
                            :clearable="true"
                            v-model="item.inputValue"
                            :name="`input_${index}`"
                            v-validate="'required'"
                            class="value-input"
                            :class="{ 'is-danger': errors.has(`input_${index}`) }"
                        />
                        
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
    import { NODE_RESOURCE_ACTION, NODE_RESOURCE_TYPE } from '@/utils/permission'
    import EmptyTableStatus from '@/components/empty-table-status'
    const NODE_TABLE_COLUMN_CACHE = 'node_list_columns'

    export default {
        components: {
            EmptyTableStatus
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
            dateTimeRange: {
                type: Array,
                default: () => []
            }
        },
        data () {
            return {
                NODE_RESOURCE_TYPE,
                NODE_RESOURCE_ACTION,
                curEditNodeDisplayName: '',
                isEditNodeStatus: false,
                tableSize: localStorage.getItem('node_table_size') || 'small',
                tableColumn: [
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
                        label: this.$t('environment.标签')
                    },
                    {
                        id: 'os',
                        label: this.$t('environment.nodeInfo.os')
                    },
                    {
                        id: 'nodeStatus',
                        label: this.$t('environment.status')
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
                    },
                    {
                        id: 'latestBuildPipeline',
                        label: this.$t('environment.nodeInfo.lastRunPipeline')
                    },
                    {
                        id: 'latestBuildTime',
                        label: this.$t('environment.nodeInfo.lastRunAs')
                    }
                ],
                selectedTableColumn: JSON.parse(localStorage.getItem(NODE_TABLE_COLUMN_CACHE))?.columns || [
                    { id: 'displayName' },
                    { id: 'ip' },
                    { id: 'label' },
                    { id: 'os' },
                    { id: 'nodeStatus' },
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

                selectOptions: [
                    { id: 'architecture', name: 'architecture' },
                    { id: 'design', name: 'design' },
                    { id: 'development', name: 'development' }
                ],
                // 表单行数据，每个对象包含下拉选中值和输入框值
                setTagForm: [
                    { selectValue: 'architecture', inputValue: 'X86' }
                ]
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
            usageMap () {
                return {
                    DEVCLOUD: this.$t('environment.构建'),
                    THIRDPARTY: this.$t('environment.构建'),
                    CC: this.$t('environment.部署'),
                    CMDB: this.$t('environment.部署'),
                    UNKNOWN: this.$t('environment.部署'),
                    OTHER: this.$t('environment.部署')
                }
            }
        },
        methods: {
            handlePageChange (page) {
                this.$emit('page-change', page)
            },
            handlePageLimitChange (limit) {
                this.$emit('page-limit-change', limit)
            },
            handleSortChange (sort) {
                this.$emit('sort-change', sort)
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
            toNodeDetail (node) {
                if (this.canShowDetail(node)) {
                    this.$router.push({
                        name: 'nodeDetail',
                        params: {
                            projectId: this.projectId,
                            nodeHashId: node.nodeHashId
                        }
                    })
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
                    extCls: 'info-content',
                    title: `${this.$t('environment.nodeInfo.deleteNodetips', [row.displayName])}`,
                    subTitle: this.$t('environment.nodeInfo.stopAgentProcessOnly'),
                    confirmFn: async () => {
                        let message, theme
                        try {
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
                            this.$emit('refresh')
                        }
                    }
                })
            },
            handleSetTag (row) {
                this.isShowSetTagSlider = true
            },
            addRow () {
                this.setTagForm.push({
                    selectValue: '',
                    inputValue: ''
                })
            },
            // 删除一行
            deleteRow (index) {
                this.setTagForm.splice(index, 1)
            },
            async handleSetConfirm () {
                const isValid = await this.$validator.validateAll()
                console.log('🚀 ~ handleSetConfirm:', isValid, this.setTagForm)
            },
            handleCancel () {
                this.isShowSetTagSlider = false
            },
            handleApplyPermission (node) {
                this.handleNoPermission({
                    projectId: this.projectId,
                    resourceType: NODE_RESOURCE_TYPE,
                    resourceCode: node.nodeHashId,
                    action: NODE_RESOURCE_ACTION.USE
                })
            },
            clearFilter () {
                this.$emit('clear-filter')
            },
            handleSettingChange ({ fields, size }) {
                this.selectedTableColumn = Object.freeze(fields)
                this.tableSize = size
                localStorage.setItem(NODE_TABLE_COLUMN_CACHE, JSON.stringify({
                    columns: fields,
                    size
                }))
            },
            canShowDetail (row) {
                return row.nodeType === 'THIRDPARTY'
            }
        }
    }
</script>

<style lang="scss">
  @import '@/scss/conf';

  %flex {
      display: flex;
      align-items: center;
  }

  .node-table-wrapper {
      margin-top: 20px;
      td:nth-child(1) {
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
            color: $fontLigtherColor;
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

      .node-item-row {
        &.node-row-useless {
          border: #30D878 1px solid;
          cursor: url('../../images/cursor-lock.png'), auto;
          color: $fontLigtherColor;
          .node-count-item {
            color: $fontLigtherColor;
          }
        }
      }

      .install-agent {
          color: $primaryColor;
          cursor: pointer;
      }

      .node-item-content {
          position: absolute;
          top: 6px;
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
              top: 8px;
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
