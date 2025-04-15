<template>
    <div
        class="biz-container pipeline-subpages instance-manage-wrapper"
        v-bkloading="{
            isLoading: loading.isLoading,
            title: loading.title
        }"
    >
        <div
            class="sub-view-port"
            v-if="showContent && showInstanceList"
        >
            <section class="info-header">
                <div class="instance-handle-row">
                    <bk-button
                        class="batch-update"
                        :disabled="!selectItemList.length"
                        @click="handleBatch()"
                    >
                        <span>{{ $t('template.batchUpdate') }}</span>
                    </bk-button>
                    <bk-button
                        v-if="isEnabledPermission"
                        theme="primary"
                        @click="createInstance()"
                        v-perm="{
                            hasPermission: hasCreatePermission,
                            disablePermissionApi: true,
                            permissionData: {
                                projectId: projectId,
                                resourceType: 'pipeline',
                                resourceCode: projectId,
                                action: RESOURCE_ACTION.CREATE
                            }
                        }"
                    >
                        {{ $t('template.addInstance') }}
                    </bk-button>
                    <bk-button
                        v-else
                        theme="primary"
                        @click="createInstance()"
                    >
                        <span>{{ $t('template.addInstance') }}</span>
                    </bk-button>
                </div>
                <bk-input
                    :placeholder="$t('search')"
                    ext-cls="instance-handle-row-right"
                    :clearable="true"
                    right-icon="icon-search"
                    v-model="searchKey"
                    @enter="query"
                    @clear="query"
                >
                </bk-input>
            </section>
            <section class="instance-table">
                <bk-table
                    :data="instanceList"
                    size="small"
                    ref="instanceList"
                    :pagination="pagination"
                    @page-change="handlePageChange"
                    @page-limit-change="pageLimitChange"
                    @select="selectItem"
                    @select-all="selectItem"
                >
                    <bk-table-column
                        type="selection"
                        width="60"
                        align="center"
                        :selectable="isUpdating"
                    ></bk-table-column>
                    <bk-table-column
                        :label="$t('pipelineName')"
                        prop="pipelineName"
                    >
                        <template slot-scope="props">
                            <span
                                class="pipeline-name"
                                :title="props.row.pipelineName"
                                @click="toPipelineHistory(props.row.pipelineId)"
                            >{{ props.row.pipelineName }}</span>
                        </template>
                    </bk-table-column>
                    <bk-table-column
                        :label="$t('version')"
                        prop="versionName"
                    ></bk-table-column>
                    <bk-table-column
                        :label="$t('template.newestVersion')"
                        :formatter="currentVersionFormatter"
                    >
                        <template>
                            <span>{{ currentVersionName }}</span>
                        </template>
                    </bk-table-column>
                    <bk-table-column
                        :label="$t('status')"
                        :render-header="statusHeader"
                        prop="status"
                    >
                        <template slot-scope="props">
                            <div
                                class="status-card"
                                @click="handleStatusClick(props.row)"
                                :class="statusMap[props.row.status] && statusMap[props.row.status].className"
                            >
                                {{ statusMap[props.row.status] && statusMap[props.row.status].label }}
                            </div>
                        </template>
                    </bk-table-column>
                    <bk-table-column
                        :label="$t('lastUpdateTime')"
                        prop="updateTime"
                    >
                        <template slot-scope="props">
                            <span>{{ localConvertTime(props.row.updateTime) }}</span>
                        </template>
                    </bk-table-column>
                    <bk-table-column
                        :label="$t('operate')"
                        width="250"
                    >
                        <template slot-scope="props">
                            <bk-button
                                v-if="isEnabledPermission"
                                class="mr10"
                                theme="primary"
                                text
                                @click="updateInstance(props.row)"
                                v-perm="{
                                    hasPermission: props.row.hasPermission,
                                    disablePermissionApi: true,
                                    permissionData: {
                                        projectId: projectId,
                                        resourceType: 'pipeline',
                                        resourceCode: props.row.pipelineId,
                                        action: RESOURCE_ACTION.EDIT
                                    }
                                }"
                            >
                                {{ $t('edit') }}
                            </bk-button>
                            <bk-button
                                v-else
                                theme="primary"
                                text
                                :disabled="!props.row.hasPermission"
                                @click="updateInstance(props.row)"
                            >
                                {{ $t('edit') }}
                            </bk-button>
                            <bk-button
                                v-if="isEnabledPermission"
                                class="mr10"
                                theme="primary"
                                text
                                @click="copyAsTemplateInstance(props.row)"
                                v-perm="{
                                    hasPermission: props.row.hasPermission,
                                    disablePermissionApi: true,
                                    permissionData: {
                                        projectId: projectId,
                                        resourceType: 'pipeline',
                                        resourceCode: props.row.pipelineId,
                                        action: RESOURCE_ACTION.EDIT
                                    }
                                }"
                            >
                                {{ $t('copy') }}
                            </bk-button>
                            <bk-button
                                v-else
                                class="mr10"
                                theme="primary"
                                text
                                @click="copyAsTemplateInstance(props.row)"
                                :disabled="!props.row.hasPermission"
                            >
                                {{ $t('copy') }}
                            </bk-button>
                            <bk-button
                                theme="primary"
                                text
                                @click="toCompared(props.row)"
                            >
                                {{ $t('template.diff') }}
                            </bk-button>
                        </template>
                    </bk-table-column>
                </bk-table>
            </section>
        </div>
        <empty-tips
            v-if="showContent && !showInstanceList"
            :title="emptyTipsConfig.title"
            :desc="emptyTipsConfig.desc"
            :btns="emptyTipsConfig.btns"
        >
        </empty-tips>
        <instance-compared
            :show-compared-instance="showComparedInstance"
            :loading="dialogLoading"
            :instance-version="instanceVersion"
            :cur-version="currentVersion"
            :version-list="versionList"
            :cur-params-list="curParamsList"
            :target-params-list="targetParamsList"
            :cur-stages="curStages"
            :target-stages="targetStages"
            @cancel="cancelHandler"
            :selected-version="selectedVersion"
        />
        <instance-message
            :show-instance-message="showFailedMessageDialog"
            :show-title="false"
            :fail-list="activeFailInstances"
            :fail-message="activeFailMessages"
            @cancel="hideFailedMessageDialog"
        />
    </div>
</template>

<script>
    import emptyTips from '@/components/pipelineList/imgEmptyTips'
import instanceCompared from '@/components/Template/instance-compared.vue'
import instanceMessage from '@/components/Template/instance-message.vue'
import {
RESOURCE_ACTION,
TEMPLATE_RESOURCE_ACTION
} from '@/utils/permission'
import { convertTime } from '@/utils/util'

    export default {
        components: {
            emptyTips,
            instanceCompared,
            instanceMessage
        },
        props: {
            isEnabledPermission: Boolean
        },
        data () {
            return {
                showContent: false,
                showComparedInstance: false,
                dialogLoading: false,
                isInit: false,
                searchable: false,
                searchKey: '',
                instanceVersion: '',
                currentVersion: '',
                currentVersionId: '',
                currentVersionName: '',
                curComparedPipeline: '',
                selectItemList: [],
                instanceList: [],
                versionList: [],
                curParamsList: [],
                targetParamsList: [],
                curStages: [],
                curTplParamsList: [],
                targetStages: [],
                targetTplParamsList: [],
                showFailedMessageDialog: false,
                activeFailMessages: [],
                activeFailInstances: [],
                loading: {
                    isLoading: false,
                    title: ''
                },
                pagination: {
                    current: 1,
                    count: 0,
                    limit: 10,
                    limitList: [10, 50, 100]
                },
                emptyTipsConfig: {
                    title: this.$t('template.instanceEmptyTitle'),
                    desc: this.$t('template.instanceEmptyDesc'),
                    btns: [
                        {
                            theme: 'primary',
                            size: 'normal',
                            handler: () => this.createInstance(),
                            text: this.$t('template.addInstance')
                        }
                    ]
                },
                statusMap: {
                    UPDATED: {
                        label: this.$t('template.noNeedToUpdate'),
                        className: ''
                    },
                    FAILED: {
                        label: this.$t('template.updateFailed'),
                        className: 'update-failed cursor-pointer'
                    },
                    PENDING_UPDATE: {
                        label: this.$t('template.needToUpdate'),
                        className: 'need-update'
                    },
                    UPDATING: {
                        label: this.$t('template.updating'),
                        className: 'updating'
                    }
                },
                hasCreatePermission: false
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            pipelineId () {
                return this.$route.params.pipelineId
            },
            templateId () {
                return this.$route.params.templateId
            },
            showInstanceList () {
                return this.showContent && (this.instanceList.length || this.searchable)
            },
            TEMPLATE_RESOURCE_ACTION () {
                return TEMPLATE_RESOURCE_ACTION
            },
            RESOURCE_ACTION () {
                return RESOURCE_ACTION
            }
        },
        watch: {
            instanceVersion (newVal) {
                if (newVal && !this.isInit) {
                    this.requestVersionCompare(newVal)
                }
            }
        },
        async mounted () {
            // 初始化url中携带的分页信息
            this.$route.query.limit > 0 && (this.pagination.limit = parseInt(this.$route.query.limit))
            this.$route.query.page > 0 && (this.pagination.current = parseInt(this.$route.query.page))

            this.renderData()
        },
        methods: {
            statusHeader () {
                const h = this.$createElement
                return h('div', [
                    h('span',
                      this.$t('status')
                    ),
                    h('i', {
                        on: {
                            click: this.renderData
                        },
                        style: {
                            color: '#3a84ff',
                            cursor: 'pointer',
                            margin: '5px'
                        },
                        attrs: {
                            class: 'bk-icon icon-refresh'
                        }
                    })
                ])
            },
            async renderData () {
                await this.requestInstanceList(this.pagination.current, this.pagination.limit)
            },
            async requestInstanceList (page, pageSize) {
                const { $store, loading, searchKey } = this

                loading.isLoading = true

                try {
                    const params = { searchKey, page, pageSize }
                    const res = await $store.dispatch('pipelines/requestInstanceList', {
                        projectId: this.projectId,
                        templateId: this.templateId,
                        params
                    })
                    // this.currentVersionId = res.latestVersion.version
                    // this.versionList = res.versions
                    // this.currentVersionName = res.latestVersion.versionName
                    this.instanceList = res.records
                    this.pagination.count = res.count
                    this.hasCreatePermission = res.hasCreateTemplateInstancePerm
                } catch (err) {
                    this.$showTips({
                        message: err.message || err,
                        theme: 'error'
                    })
                } finally {
                    this.loading.isLoading = false
                    this.showContent = true
                }
            },
            localConvertTime (timestamp) {
                return convertTime(timestamp)
            },
            currentVersionFormatter () {
                return this.currentVersionName
            },
            createInstance (pipeline, type) {
                const route = {
                    name: 'createInstance',
                    params: {
                        projectId: this.projectId,
                        pipelineId: this.pipelineId,
                        curVersionId: this.currentVersionId
                    }
                }
                if (pipeline) {
                    route.hash = type === 'single' ? `#${pipeline}` : `#${pipeline.join('&')}`
                    route.query = type === 'single' ? {} : { page: this.pagination.current, limit: this.pagination.limit }
                }
                this.$router.push(route)
            },
            selectItem (items) {
                this.selectItemList = items.filter(i => i.hasPermission)
                return items.filter(i => i.hasPermission)
            },
            async handlePageChange (page) {
                if (page !== this.pagination.current) {
                    this.pagination.current = page
                    await this.requestInstanceList(this.pagination.current, this.pagination.limit)
                }
            },
            async pageLimitChange (limit) {
                if (limit !== this.pagination.limit) {
                    this.pagination.current = 1
                    this.pagination.limit = limit
                    await this.requestInstanceList(this.pagination.current, this.pagination.limit)
                }
            },
            async query () {
                this.searchable = true
                this.pagination.current = 1
                await this.requestInstanceList(this.pagination.current, this.pagination.limit)
            },
            updateInstance (row) {
                if (row.hasPermission) {
                    const pipeline = row.pipelineId
                    this.createInstance(pipeline, 'single')
                }
            },
            handleBatch () {
                const targetList = []
                if (this.selectItemList && this.selectItemList.length) {
                    this.selectItemList.forEach(val => {
                        targetList.push(val.pipelineId)
                    })
                    this.createInstance(targetList, 'batch')
                } else {
                    this.$showTips({
                        message: this.$t('template.batchErrTips'),
                        theme: 'error'
                    })
                }
            },
            toCompared (row) {
                this.showComparedInstance = true
                this.isInit = true
                this.currentVersion = row.versionName
                this.curComparedPipeline = row.pipelineId
                this.requestVersionCompare(this.currentVersionId)
            },
            async requestVersionCompare (versionId) {
                this.dialogLoading = true

                try {
                    const res = await this.$store.dispatch('pipelines/requestVersionCompare', {
                        projectId: this.projectId,
                        templateId: this.templateId,
                        versionId: versionId,
                        pipelineId: this.curComparedPipeline
                    })

                    this.versionList = res.versions
                    const curVersion = this.versionList.find(val => {
                        return val.version === parseInt(versionId)
                    })
                    this.instanceVersion = curVersion?.version

                    const curData = res.origin
                    const targetData = res.target

                    this.curStages = curData.model.stages
                    this.targetStages = targetData.model.stages

                    this.curParamsList.splice(0, this.curParamsList.length)
                    this.targetParamsList.splice(0, this.targetParamsList.length)

                    curData.params && curData.params.forEach((item) => {
                        const temp = {
                            key: item.id,
                            value: item.defaultValue
                        }
                        this.curParamsList.push(temp)
                    })

                    targetData.params && targetData.params.forEach((item) => {
                        const temp = {
                            key: item.id,
                            value: item.defaultValue
                        }
                        this.targetParamsList.push(temp)
                    })
                } catch (err) {
                    this.$showTips({
                        message: err.message || err,
                        theme: 'error'
                    })
                } finally {
                    this.dialogLoading = false
                }
            },
            isUpdating (row) {
                return row.status !== 'UPDATING' && row.hasPermission
            },
            cancelHandler () {
                this.showComparedInstance = false
            },
            selectedVersion (data) {
                this.isInit = false
                this.instanceVersion = data
            },
            toPipelineHistory (pipelineId) {
                const url = `${WEB_URL_PREFIX}/pipeline/${this.projectId}/${pipelineId}/history`
                window.open(url, '_blank')
            },
            copyAsTemplateInstance (row) {
                const route = {
                    name: 'createInstance',
                    params: {
                        curVersionId: this.currentVersionId,
                        pipelineName: (row.pipelineName + '_copy').substring(0, 128)
                    },
                    query: {
                        pipelineId: row.pipelineId
                    }
                }
                this.$router.push(route)
            },
            handleStatusClick (row) {
                if (row.status === 'FAILED') {
                    this.showFailedMessageDialog = true
                    this.activeFailInstances = [
                        row.pipelineName
                    ]
                    this.activeFailMessages = {
                        [row.pipelineName]: row.instanceErrorInfo
                    }
                }
            },
            hideFailedMessageDialog () {
                this.showFailedMessageDialog = false
            }
        }
    }
</script>

<style lang="scss">
    @import './../../scss/conf';

    .pipeline-subpages {
        min-height: 100%;
    }
    .instance-manage-wrapper {
        flex-direction: column;
        .instance-header {
            .bk-button-normal {
                margin-top: -6px;
                padding: 0 10px;
                font-size: 12px;
            }
        }
        .sub-view-port {
            padding: 20px;
            height: calc(100% - 60px);
            overflow: auto;
        }
        .info-header {
            display: flex;
            justify-content: space-between;
        }
        .bk-form-control {
            display: inline-table;
            width: 200px;
        }
        .instance-handle-row {
            display: flex;
            margin-bottom: 20px;
            button {
                margin-right: 4px;
                padding: 0 11px;
                font-size: 12px;
            }
        }
        .instance-handle-row-right {
            .right-icon {
                top: 16px;
            }
        }
        .instance-table {
            .pipeline-name {
                color: $primaryColor;
                cursor: pointer;
            }
            .disabled-checkbox {
                position: absolute;
                left: -60px;
                top:  0;
            }
            .status-card {
                max-width: 120px;
                font-size: 12px;
                text-align: center;
                color: #fff;
                background-color: #39E084;
            }
            .need-update {
                background-color: #F6B026;
            }
            .update-failed {
                background-color: $dangerColor;
            }
            .updating {
                background-color: $primaryColor;
            }
            .update-btn,
            .compared-btn {
                display: inline-block;
                margin-right: 20px;
                cursor: pointer;
            }
        }
        .batch-update {
            padding: 0 11px;
            font-size: 12px;
        }
    }
</style>
