<template>
    <bk-table
        ext-cls="pipeline-list-table"
        v-bkloading="{ isLoading }"
        ref="pipelineTable"
        row-key="pipelineId"
        :max-height="maxHeight"
        row-auto-height
        :data="pipelineList"
        :size="tableSize"
        :pagination="pagination"
        @page-change="handlePageChange"
        @page-limit-change="handlePageLimitChange"
        :row-class-name="setRowCls"
        @sort-change="handleSort"
        :default-sort="sortField"
        @selection-change="handleSelectChange"
        @header-dragend="handelHeaderDragend"
        v-on="$listeners"
        :key="viewId"
    >
        <PipelineListEmpty
            slot="empty"
            :is-patch="isPatchView"
        ></PipelineListEmpty>
        <div
            v-if="selectionLength > 0"
            slot="prepend"
            class="selected-all-indicator"
        >
            <span v-html="$t('selectedCount', [selectionLength])"></span>
            <bk-button
                theme="primary"
                text
                @click="clearSelection"
            >
                {{ $t('clearSelection') }}
            </bk-button>
        </div>
        <bk-table-column
            v-if="isPatchView"
            type="selection"
            width="60"
            fixed="left"
            :selectable="checkSelecteable"
        ></bk-table-column>
        <bk-table-column
            v-if="!isPatchView && !isDeleteView"
            width="30"
            fixed="left"
        >
            <template slot-scope="{ row }">
                <bk-button
                    text
                    :class="{
                        'icon-star-btn': true,
                        'is-collect': row.hasCollect
                    }"
                    :theme="row.hasCollect ? 'warning' : ''"
                    @click="collectHandler(row)"
                >
                    <i
                        :class="{
                            'devops-icon': true,
                            'icon-star': !row.hasCollect,
                            'icon-star-shape': row.hasCollect
                        }"
                    />
                </bk-button>
            </template>
        </bk-table-column>
        <bk-table-column
            v-if="allRenderColumnMap.pipelineName"
            :width="tableWidthMap.pipelineName"
            min-width="250"
            fixed="left"
            sortable="custom"
            :label="$t('pipelineName')"
            prop="pipelineName"
        >
            <template slot-scope="props">
                <!-- hack disabled event -->
                <div
                    class="pipeline-name-wrapper"
                    :key="props.row.pipelineName"
                >
                    <div
                        class="pipeline-name"
                        v-bk-overflow-tips
                    >
                        <span
                            v-if="props.row.permissions && !props.row.permissions.canView"
                            class="pointer"
                            @click="applyPermission(props.row)"
                        >
                            {{ props.row.pipelineName }}
                        </span>
                        <router-link
                            v-else-if="!props.row.delete && !isDeleteView && props.row.historyRoute"
                            class="pipeline-cell-link"
                            :disabled="props.row.permissions && !props.row.permissions.canView"
                            :to="props.row.historyRoute"
                        >
                            {{ props.row.pipelineName }}
                        </router-link>
                        <span v-else>{{ props.row.pipelineName }}</span>
                    </div>
                    <logo
                        v-if="props.row.templateId"
                        class="ml5 template-mode-icon"
                        name="template-mode"
                        size="12"
                        v-bk-tooltips.right="$t('pipelineConstraintModeTips')"
                    />
                    <bk-tag
                        v-if="props.row.onlyDraftVersion"
                        theme="success"
                        class="draft-tag"
                    >
                        {{ $t('draft') }}
                    </bk-tag>
                    <bk-tag
                        v-else-if="props.row.onlyBranchVersion"
                        theme="warning"
                        class="draft-tag"
                    >
                        {{ $t('history.branch') }}
                    </bk-tag>
                </div>
            </template>
        </bk-table-column>
        <bk-table-column
            v-if="allRenderColumnMap.ownGroupName && (isAllPipelineView || isPatchView || isDeleteView)"
            :width="tableWidthMap.viewNames"
            min-width="300"
            :label="$t('ownGroupName')"
            prop="viewNames"
        >
            <div
                :ref="`belongsGroupBox_${props.$index}`"
                class="pipeline-group-box-cell"
                slot-scope="props"
            >
                <template v-if="pipelineGroups[props.$index].visibleGroups">
                    <bk-tag
                        ext-cls="pipeline-group-name-tag"
                        :ref="`groupName_${props.$index}`"
                        v-for="(viewName, index) in pipelineGroups[props.$index].visibleGroups"
                        :key="index"
                        v-bk-overflow-tips="{ delay: [500, 0], interactive: false }"
                        @click="goGroup(viewName)"
                    >
                        {{ viewName }}
                    </bk-tag>
                    <bk-popover
                        theme="light"
                        placement="bottom-end"
                        max-width="250"
                        v-if="pipelineGroups[props.$index].showMore"
                    >
                        <bk-tag
                            :ref="`groupNameMore_${props.$index}`"
                        >
                            +{{ pipelineGroups[props.$index].showMore }}
                        </bk-tag>
                        <div slot="content">
                            <bk-tag
                                v-for="hiddenGroup in pipelineGroups[props.$index].hiddenGroups"
                                ext-cls="pipeline-group-name-tag pipeline-group-more-tag"
                                :key="hiddenGroup"
                                v-bk-overflow-tips="{ delay: [500, 0], interactive: false }"
                                @click="goGroup(hiddenGroup)"
                            >
                                {{ hiddenGroup }}
                            </bk-tag>
                        </div>
                    </bk-popover>
                </template>
            </div>
        </bk-table-column>
        <bk-table-column
            v-if="allRenderColumnMap.label"
            :label="$t('label')"
            :width="tableWidthMap.groupLabel"
            min-width="200"
            prop="groupLabel"
        >
            <div
                :ref="`belongsLabelBox_${props.$index}`"
                slot-scope="props"
                class="group-label-warpper"
            >
                <template v-if="labelGroups[props.$index].visibleLabels">
                    <span
                        class="group-tag"
                        v-for="(item, index) in labelGroups[props.$index].visibleLabels"
                        :key="index"
                        :ref="`labelName_${props.$index}`"
                    >
                        <span class="key">
                            {{ item.groupName }}
                        </span>
                        <span
                            class="value"
                            v-bk-overflow-tips
                        >
                            {{ item.labelName.join(',') }}
                        </span>
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
                                v-bk-overflow-tips
                            >
                                <span class="key">
                                    {{ item.groupName }}
                                </span>
                                <span
                                    class="value"
                                    v-bk-overflow-tips
                                >
                                    {{ item.labelName.join(',') }}
                                </span>
                            </div>
                        </div>
                    </bk-popover>
                </template>
            </div>
        </bk-table-column>
        <template v-if="isPatchView">
            <bk-table-column
                :width="tableWidthMap.latestBuildNum"
                :label="$t('latestExec')"
                prop="latestBuildNum"
            >
                <span slot-scope="props">{{ props.row.latestBuildNum ? `#${props.row.latestBuildNum}` : '--' }}</span>
            </bk-table-column>
            <bk-table-column
                :width="tableWidthMap.latestBuildStartDate"
                sortable="custom"
                :label="$t('lastExecTime')"
                prop="latestBuildStartDate"
            />
            <bk-table-column
                :width="tableWidthMap.createTime"
                sortable="custom"
                :label="$t('createTime')"
                prop="createTime"
                :formatter="formatTime"
            />
            <bk-table-column
                :width="tableWidthMap.creator"
                :label="$t('creator')"
                prop="creator"
            />
        </template>
        <template v-else-if="isDeleteView">
            <bk-table-column
                :width="tableWidthMap.createTime"
                key="createTime"
                :label="$t('createTime')"
                sortable="custom"
                prop="createTime"
                sort
                :formatter="formatTime"
            />
            <bk-table-column
                :width="tableWidthMap.deleteTime"
                key="updateTime"
                :label="$t('restore.deleteTime')"
                sortable="custom"
                prop="updateTime"
                :formatter="formatTime"
            />
            <bk-table-column
                :width="tableWidthMap.lastModifyUser"
                key="lastModifyUser"
                :label="$t('restore.deleter')"
                prop="lastModifyUser"
            ></bk-table-column>
        </template>
        <template v-else>
            <bk-table-column
                v-if="allRenderColumnMap.latestExec"
                :width="tableWidthMap.latestExec"
                min-width="180"
                :label="$t('latestExec')"
                prop="latestExec"
            >
                <span
                    v-if="props.row.delete"
                    slot-scope="props"
                >
                    {{ $t('deleteAlready') }}
                </span>
                <div
                    v-else
                    slot-scope="props"
                    class="pipeline-latest-exec-cell"
                >
                    <pipeline-status-icon :status="props.row.latestBuildStatus" />
                    <div class="pipeline-exec-msg">
                        <template v-if="props.row.latestBuildNum">
                            <span
                                class="pipeline-cell-link pipeline-exec-msg-title"
                                :disabled="props.row.permissions && !props.row.permissions.canView"
                                v-perm="{
                                    hasPermission: props.row.permissions && props.row.permissions.canView,
                                    disablePermissionApi: true,
                                    permissionData: {
                                        projectId,
                                        resourceType: 'pipeline',
                                        resourceCode: props.row.pipelineId,
                                        action: RESOURCE_ACTION.VIEW
                                    }
                                }"
                                :event="props.row.permissions && props.row.permissions.canView ? 'click' : ''"
                                @click="$router.push(props.row.latestBuildRoute)"
                            >
                                <b>#{{ props.row.latestBuildNum }}</b>
                                |
                                <span>{{ props.row.lastBuildMsg }}</span>
                            </span>
                            <p class="pipeline-exec-msg-desc">
                                <span class="desc">
                                    <logo
                                        :name="props.row.startType"
                                        size="16"
                                    />
                                    <span>{{ props.row.latestBuildUserId }}</span>
                                </span>
                                <span
                                    v-if="props.row.webhookAliasName"
                                    class="desc"
                                >
                                    <logo
                                        name="branch"
                                        size="16"
                                    />
                                    <span>{{ props.row.webhookAliasName }}</span>
                                </span>
                                <span
                                    v-if="props.row.webhookMessage"
                                    class="desc"
                                >
                                    <span>{{ props.row.webhookMessage }}</span>
                                </span>
                            </p>
                        </template>
                        <p
                            v-else
                            class="desc"
                        >
                            {{ $t('unexecute') }}
                        </p>
                    </div>
                </div>
            </bk-table-column>
            <bk-table-column
                v-if="allRenderColumnMap.lastExecTime"
                :width="tableWidthMap.latestBuildStartDate"
                sortable="custom"
                :label="$t('lastExecTime')"
                prop="latestBuildStartDate"
            >
                <div
                    class="latest-build-multiple-row"
                    v-if="!props.row.delete"
                    slot-scope="props"
                >
                    <p>{{ props.row.latestBuildStartDate }}</p>
                    <p
                        v-if="props.row.progress"
                        class="primary"
                    >
                        {{ props.row.progress }}
                    </p>
                    <p
                        v-else
                        class="desc"
                    >
                        {{ props.row.duration }}
                    </p>
                </div>
            </bk-table-column>
            <bk-table-column
                v-if="allRenderColumnMap.lastModify"
                :width="tableWidthMap.updateTime"
                :label="$t('lastModify')"
                sortable="custom"
                prop="updateTime"
                sort
            >
                <div
                    class="latest-build-multiple-row"
                    v-if="!props.row.delete"
                    slot-scope="props"
                >
                    <p>{{ props.row.updater }}</p>
                    <p class="desc">{{ props.row.updateDate }}</p>
                </div>
            </bk-table-column>
            <bk-table-column
                v-if="allRenderColumnMap.creator"
                :width="tableWidthMap.creator"
                :label="$t('creator')"
                prop="creator"
            />
            <bk-table-column
                v-if="allRenderColumnMap.createTime"
                :width="tableWidthMap.createTime"
                :label="$t('created')"
                sortable="custom"
                prop="createTime"
            >
                <template slot-scope="props">
                    {{ prettyDateTimeFormat(props.row.createTime) }}
                </template>
            </bk-table-column>
        </template>
        <bk-table-column
            v-if="!isPatchView"
            :width="tableWidthMap.pipelineId"
            fixed="right"
            :label="$t('operate')"
            prop="pipelineId"
        >
            <div
                class="pipeline-operation-cell"
                slot-scope="props"
            >
                <bk-button
                    v-if="isDeleteView"
                    text
                    theme="primary"
                    v-perm="{
                        hasPermission: props.row.permissions && props.row.permissions.canManage,
                        disablePermissionApi: true,
                        permissionData: {
                            projectId: projectId,
                            resourceType: 'project',
                            resourceCode: projectId,
                            action: PROJECT_RESOURCE_ACTION.MANAGE
                        }
                    }"
                    @click="handleRestore(props.row)"
                >
                    {{ $t('restore.restore') }}
                </bk-button>
                <bk-button
                    v-else-if="props.row.delete && !isRecentView"
                    text
                    theme="primary"
                    :disabled="!isManage"
                    v-perm="{
                        hasPermission: props.row.permissions && props.row.permissions.canManage,
                        disablePermissionApi: true,
                        permissionData: {
                            projectId: projectId,
                            resourceType: 'project',
                            resourceCode: projectId,
                            action: PROJECT_RESOURCE_ACTION.MANAGE
                        }
                    }"
                    @click="removeHandler(props.row)"
                >
                    {{ $t('removeFromGroup') }}
                </bk-button>
                <bk-button
                    v-else-if="props.row.permissions && !props.row.permissions.canView && !props.row.delete"
                    outline
                    theme="primary"
                    @click="applyPermission(props.row)"
                >
                    {{ $t('apply') }}
                </bk-button>
                <template
                    v-else-if="props.row.hasPermission && !props.row.delete"
                >
                    <span v-if="!(props.row.released || props.row.onlyBranchVersion)">
                        <bk-button
                            text
                            class="exec-pipeline-btn"
                            @click="goEdit(props.row)"
                            v-perm="{
                                hasPermission: props.row.permissions && props.row.permissions.canEdit,
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
                    </span>
                    <span
                        v-else
                        v-bk-tooltips="props.row.tooltips"
                    >
                        <bk-button
                            text
                            theme="primary"
                            class="exec-pipeline-btn"
                            :disabled="props.row.disabled"
                            v-perm="{
                                hasPermission: props.row.permissions && props.row.permissions.canExecute,
                                disablePermissionApi: true,
                                permissionData: {
                                    projectId: projectId,
                                    resourceType: 'pipeline',
                                    resourceCode: props.row.pipelineId,
                                    action: RESOURCE_ACTION.EXECUTE
                                }
                            }"
                            @click="execPipeline(props.row)"
                        >
                            {{ props.row.lock ? $t('disabled') : props.row.canManualStartup ? $t('exec') : $t('nonManual') }}
                        </bk-button>
                    </span>
                    <ext-menu
                        :data="props.row"
                        :config="props.row.pipelineActions"
                    ></ext-menu>
                </template>
            </div>
        </bk-table-column>
        <bk-table-column
            v-if="!isPatchView && !isDeleteView"
            type="setting"
        >
            <bk-table-setting-content
                :fields="tableColumn"
                :selected="selectedTableColumn"
                :size="tableSize"
                @setting-change="handleSettingChange"
            />
        </bk-table-column>
    </bk-table>
</template>

<script>
    import Logo from '@/components/Logo'
    import PipelineStatusIcon from '@/components/PipelineStatusIcon'
    import PipelineListEmpty from '@/components/pipelineList/PipelineListEmpty'
    import ExtMenu from '@/components/pipelineList/extMenu'
    import pipelineActionMixin from '@/mixins/pipeline-action-mixin'
    import {
        ALL_PIPELINE_VIEW_ID,
        CACHE_PIPELINE_TABLE_WIDTH_MAP,
        DELETED_VIEW_ID,
        PIPELINE_TABLE_COLUMN_CACHE,
        PIPELINE_TABLE_LIMIT_CACHE,
        RECENT_USED_VIEW_ID
    } from '@/store/constants'
    import {
        PROJECT_RESOURCE_ACTION,
        RESOURCE_ACTION,
        handlePipelineNoPermission
    } from '@/utils/permission'
    import { ORDER_ENUM, PIPELINE_SORT_FILED } from '@/utils/pipelineConst'
    import { convertTime, isShallowEqual, prettyDateTimeFormat } from '@/utils/util'
    import { mapGetters, mapState } from 'vuex'

    export default {
        components: {
            Logo,
            ExtMenu,
            PipelineStatusIcon,
            PipelineListEmpty
        },
        mixins: [pipelineActionMixin],
        props: {
            isPatchView: Boolean,
            maxHeight: {
                type: [Number, String],
                default: 'auto'
            },
            filterParams: {
                type: Object,
                default: () => ({})
            }
        },
        data () {
            return {
                isLoading: false,
                pipelineList: [],
                selectionLength: 0,
                pagination: {
                    current: parseInt(this.$route.query.page ?? 1),
                    limit: 50,
                    count: 0
                },
                visibleTagCountList: {},
                RESOURCE_ACTION,
                PROJECT_RESOURCE_ACTION,
                tableWidthMap: {},
                tableSize: 'medium',
                tableColumn: [],
                selectedTableColumn: [],
                showCollectIndex: -1,
                visibleLabelCountList: {}
            }
        },
        computed: {
            ...mapGetters('pipelines', [
                'groupNamesMap'
            ]),
            ...mapState('pipelines', [
                'isManage'
            ]),
            isAllPipelineView () {
                return this.$route.params.viewId === ALL_PIPELINE_VIEW_ID
            },
            isDeleteView () {
                return this.$route.params.viewId === DELETED_VIEW_ID
            },
            isRecentView () {
                return this.$route.params.viewId === RECENT_USED_VIEW_ID
            },
            pipelineGroups () {
                const res = this.pipelineList.map((pipeline, index) => {
                    const { viewNames } = pipeline
                    const visibleCount = this.visibleTagCountList[index]

                    if (visibleCount >= 1) {
                        return {
                            visibleGroups: viewNames.slice(0, visibleCount),
                            hiddenGroups: viewNames.slice(visibleCount),
                            showMore: viewNames.length - visibleCount
                        }
                    }

                    return {
                        visibleGroups: viewNames,
                        hiddenGroups: [],
                        showMore: viewNames?.length ?? 0
                    }
                })
                return res
            },
            labelGroups () {
                const res = this.pipelineList.map((pipeline, index) => {
                    const { groupLabel = [] } = pipeline
                    const visibleCount = this.visibleLabelCountList[index]
                    if (visibleCount >= 1) {
                        return {
                            visibleLabels: groupLabel.slice(0, visibleCount),
                            hiddenLabels: groupLabel.slice(visibleCount),
                            showMore: groupLabel.length - visibleCount
                        }
                    }

                    return {
                        visibleLabels: groupLabel,
                        hiddenLabels: [],
                        showMore: groupLabel?.length ?? 0
                    }
                })
                return res
            },
            sortField () {
                const { sortType, collation } = this.$route.query
                const prop = sortType ?? localStorage.getItem('pipelineSortType') ?? PIPELINE_SORT_FILED.createTime
                const order = collation ?? localStorage.getItem('pipelineSortCollation') ?? ORDER_ENUM.descending
                return {
                    prop: this.getkeyByValue(PIPELINE_SORT_FILED, prop),
                    order: this.getkeyByValue(ORDER_ENUM, order)
                }
            },
            projectId () {
                return this.$route.params.projectId
            },
            allRenderColumnMap () {
                return this.selectedTableColumn.reduce((result, item) => {
                    result[item.id] = true
                    return result
                }, {})
            }
        },

        watch: {
            '$route.params.viewId': function (viewId) {
                this.requestList({
                    viewId,
                    page: 1
                })
            },
            sortField: function (newSortField, oldSortField) {
                if (!isShallowEqual(newSortField, oldSortField)) {
                    this.$refs?.pipelineTable?.clearSort?.()
                    this.$refs?.pipelineTable?.sort?.(newSortField.prop, newSortField.order)
                    this.$nextTick(this.requestList)
                }
            },
            filterParams: function (filterMap, oldFilterMap) {
                if (!isShallowEqual(filterMap, oldFilterMap) && Object.keys(filterMap).length > 0) {
                    this.requestList({
                        ...filterMap,
                        page: 1
                    })
                }
            },
            isAllPipelineView (val) {
                this.setTableColumn(val)
            },
            isPatchView (val) {
                this.setTableColumn(val)
            },
            isDeleteView (val) {
                this.setTableColumn(val)
            }
        },
        mounted () {
            const { pageSize } = this.$route.query
            const tableLimit = JSON.parse(localStorage.getItem(PIPELINE_TABLE_LIMIT_CACHE)) || 50
            pageSize ? this.pagination.limit = parseInt(pageSize) : this.pagination.limit = tableLimit
            this.tableColumn = [
                {
                    id: 'pipelineName',
                    label: this.$t('pipelineName'),
                    disabled: true
                },
                {
                    id: 'ownGroupName',
                    label: this.$t('ownGroupName')
                },
                {
                    id: 'label',
                    label: this.$t('label')
                },
                {
                    id: 'latestExec',
                    label: this.$t('latestExec')
                },
                {
                    id: 'lastExecTime',
                    label: this.$t('lastExecTime')
                },
                {
                    id: 'lastModify',
                    label: this.$t('lastModify')
                },
                {
                    id: 'creator',
                    label: this.$t('creator')
                },
                {
                    id: 'createTime',
                    label: this.$t('created')
                },
                {
                    id: 'operate',
                    label: this.$t('operate'),
                    disabled: true
                }
            ]
            const columnsCache = JSON.parse(localStorage.getItem(PIPELINE_TABLE_COLUMN_CACHE))
            if (columnsCache) {
                this.selectedTableColumn = columnsCache.columns
                this.tableSize = columnsCache.size
            } else {
                this.selectedTableColumn = [
                    { id: 'pipelineName' },
                    { id: 'ownGroupName' },
                    { id: 'latestExec' },
                    { id: 'lastExecTime' },
                    { id: 'lastModify' },
                    { id: 'creator' },
                    { id: 'createTime' },
                    { id: 'operate' }
                ]
            }

            if (!(this.isAllPipelineView)) {
                this.tableColumn.splice(1, 1)
            }
            this.tableWidthMap = JSON.parse(localStorage.getItem(CACHE_PIPELINE_TABLE_WIDTH_MAP)) || {
                pipelineName: 192,
                viewNames: 192,
                latestBuildNum: 150,
                latestBuildStartDate: 154,
                createTime: 154,
                deleteTime: 154,
                creator: 154,
                updateTime: 154,
                lastModifyUser: '',
                latestExec: 484,
                pipelineId: 120,
                groupLabel: 200
            }
            this.requestList()
        },
        methods: {
            prettyDateTimeFormat,
            getkeyByValue (obj, value) {
                return Object.keys(obj).find(key => obj[key] === value)
            },
            setRowCls ({ row }) {
                const clsObj = {
                    'has-delete': row.delete,
                    'no-permission': !row.hasPermission
                }
                return Object.keys(clsObj).filter(key => clsObj[key]).join(' ')
            },
            checkSelecteable (row) {
                return !row.delete
            },
            clearSelection () {
                this.$refs.pipelineTable?.clearSelection?.()
            },
            handleSelectChange (selection, ...args) {
                this.selectionLength = selection.length
                this.$emit('selection-change', selection, ...args)
                this.$nextTick(() => {
                    this.$refs?.pipelineTable?.doLayout?.()
                })
            },
            goGroup (groupName) {
                const group = this.groupNamesMap[groupName]
                if (group) {
                    this.$router.push({
                        params: {
                            viewId: group?.id
                        }
                    })
                }
            },
            handlePageLimitChange (limit) {
                localStorage.setItem(PIPELINE_TABLE_LIMIT_CACHE, JSON.stringify(limit))
                this.pagination.limit = limit
                this.$router.replace({
                    query: {
                        ...this.$route.query,
                        pageSize: limit
                    }
                })
                this.$nextTick(this.requestList)
            },
            handlePageChange (page) {
                this.pagination.current = page
                this.$router.replace({
                    query: {
                        ...this.$route.query,
                        page
                    }
                })
                this.$nextTick(this.requestList)
            },
            handleSort ({ prop, order }) {
                if (isShallowEqual(this.sortField, { prop, order })) return
                const sortType = PIPELINE_SORT_FILED[prop]
                if (sortType) {
                    const collation = prop ? ORDER_ENUM[order] : ORDER_ENUM.descending
                    localStorage.setItem('pipelineSortType', sortType)
                    localStorage.setItem('pipelineSortCollation', collation)
                    this.$router.replace({
                        query: {
                            ...this.$route.query,
                            sortType,
                            collation
                        }
                    })
                }
            },
            async requestList (query = {}) {
                this.isLoading = true

                try {
                    this.pipelineList = []
                    const { count, page, records } = await this.getPipelines({
                        page: String(this.pagination.current),
                        pageSize: String(this.pagination.limit),
                        viewId: this.$route.params.viewId,
                        ...this.filterParams,
                        ...query
                    })
                    Object.assign(this.pagination, {
                        count,
                        current: page
                    })
                    this.pipelineList = records
                    if (this.isAllPipelineView || this.isPatchView || this.isDeleteView) {
                        this.visibleTagCountList = {}
                        setTimeout(this.calcOverPosGroup, 100)
                    }
                    this.visibleLabelCountList = {}
                    setTimeout(this.calcOverPosTable, 100)
                } catch (e) {
                    console.error(e)
                } finally {
                    this.isLoading = false
                }
            },
            refresh () {
                this.requestList()
            },
            formatTime (row, cell, value) {
                return convertTime(value)
            },
            async handleRestore (...args) {
                const res = await this.restore(...args)
                if (res) {
                    this.refresh()
                }
            },
            calcOverPosGroup () {
                const tagMargin = 6

                this.visibleTagCountList = this.pipelineList.reduce((acc, pipeline, index) => {
                    if (Array.isArray(pipeline.viewNames)) {
                        const groupNameBoxWidth = this.$refs[`belongsGroupBox_${index}`]?.clientWidth * 2
                        const groupNameLength = pipeline.viewNames.length
                        const moreTag = this.$refs?.[`groupNameMore_${index}`]?.$el
                        const moreTagWidth = (moreTag?.clientWidth ?? 0) + tagMargin
                        const viewPortWidth = groupNameBoxWidth - (groupNameLength > 1 ? moreTagWidth : 0)
                        let sumTagWidth = 0
                        let tagVisbleCount = 0

                        this.$refs[`groupName_${index}`]?.every((groupName) => {
                            sumTagWidth += groupName.$el.offsetWidth + tagMargin
                            const isOverSize = sumTagWidth > viewPortWidth
                            !isOverSize && tagVisbleCount++
                            return !isOverSize
                        })

                        acc[index] = tagVisbleCount
                    }
                    return acc
                }, {})
            },
            calcOverPosTable () {
                const tagMargin = 6

                this.visibleLabelCountList = this.pipelineList.reduce((acc, pipeline, index) => {
                    if (Array.isArray(pipeline?.groupLabel)) {
                        const labelBoxWidth = this.$refs[`belongsLabelBox_${index}`]?.clientWidth * 2
                        const labelLength = pipeline?.groupLabel.length
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
            applyPermission (row) {
                handlePipelineNoPermission({
                    projectId: this.$route.params.projectId,
                    resourceCode: row.pipelineId,
                    action: RESOURCE_ACTION.VIEW
                })
            },
            handelHeaderDragend (newWidth, oldWidth, column) {
                this.tableWidthMap[column.property] = newWidth
                // this.tableWidthMap.pipelineName -= 1
                localStorage.setItem(CACHE_PIPELINE_TABLE_WIDTH_MAP, JSON.stringify(this.tableWidthMap))
            },
            setTableColumn (val) {
                if (val) {
                    this.tableColumn.splice(1, 0, {
                        id: 'ownGroupName',
                        label: this.$t('ownGroupName')
                    })
                } else {
                    this.tableColumn.splice(1, 1)
                }
            },
            handleSettingChange ({ fields, size }) {
                this.selectedTableColumn = fields
                this.tableSize = size
                localStorage.setItem(PIPELINE_TABLE_COLUMN_CACHE, JSON.stringify({
                    columns: fields,
                    size
                }))
            },
            goEdit (pipeline) {
                this.$router.push({
                    name: 'pipelinesEdit',
                    params: {
                        projectId: pipeline.projectId,
                        pipelineId: pipeline.pipelineId
                    }
                })
            }
        }
    }

</script>

<style lang="scss">
    @import '@/scss/conf.scss';
    @import '@/scss/mixins/ellipsis';
    .primary {
        color: $primaryColor;
    }
    .desc {
        color: #979BA5;
    }
    tr.no-permission {
        background-color: #FAFBFD;
    }
    tr.has-delete {
        color: #C4C6CC;
    }
    .selected-all-indicator {
        display: flex;
        align-items: center;
        justify-content: center;
        background: #EAEBF0;
        grid-gap: 10px;
        height: 32px;
        grid-gap: 10px;
    }
    .latest-build-multiple-row {
        display: flex;
        flex-direction: column;
    }
    .pipeline-list-table {
        &.bk-table-enable-row-transition .bk-table-body td {
            transition: none;
        }
        td {
            position: inherit;

        }
        .bk-table-body-wrapper {
            td {
                .bk-table-setting-content {
                    display: none;
                }
            }
        }
        .pipeline-name-wrapper {
            width: 100%;
            display: inline-flex;
            align-items: center;
            white-space: nowrap;
            overflow: hidden;
        }
        .pipeline-name {
            overflow: hidden;
            text-overflow: ellipsis;
        }
        .template-mode-icon {
            flex-shrink: 0;
            position: relative;
            top: 2px;
        }
        .exec-pipeline-btn {
            width: 55px;
            text-align: left;
            overflow: hidden;
        }
        .icon-star-btn {
            position: relative;
            font-size: 14px !important;
            z-index: 999;
            display: none;
        }
        .is-collect {
            display: block
        }
        .bk-table-row.hover-row {
            .icon-star-btn {
                display: block
            }
        }

        .pipeline-latest-exec-cell {
            display: flex;
            align-items: center;
            .pipeline-exec-status-icon {
                display: inline-flex;
                font-size: 22px;
                margin-right: 10px;

            }
            .pipeline-exec-msg {
                display: flex;
                flex-direction: column;
                font-size: 12px;
                line-height: 20px;
                margin-left: 12px;
                overflow: hidden;
                .desc {
                    color: #979BA5;
                }
                .pipeline-exec-msg-title {
                    @include ellipsis();
                    flex: 1;
                    cursor: pointer;
                    > span {
                        color: #63656e;
                        &:hover {
                            color: $primaryColor;
                        }
                    }
                }
                .pipeline-exec-msg-desc {
                    display: grid;
                    column-gap: 16px;
                    grid-template-columns: auto auto auto;
                    > span {
                        display: flex;
                        align-items: center;
                        overflow: hidden;
                        > span {
                            display: flex;
                            @include ellipsis();
                            min-width: 0;
                            margin-left: 6px;
                        }
                    }
                }
            }
        }
        .pipeline-operation-cell {
            display: flex;
            align-items: center;
            text-wrap: nowrap;
            .more-action-menu {
                font-size: 0;
                cursor: pointer;
                .more-action-menu-trigger {
                    font-size: 18px;
                    padding: 0 6px;
                }
            }
        }
        .group-label-warpper {
            display: flex;
            width: 100%;
            flex-wrap: wrap;
            margin-top: 3px;
        }
        .group-tag {
            display: inline-flex;
            margin-right: 5px;
            margin-bottom: 5px;
            max-width: 95%;
            .key {
                flex-shrink: 0;
                display: inline-block;
                padding: 4px 8px;
                border: 1px solid #dfe0e6;
                background-color: #f0f1f5;
            }
            .value {
                display: inline-block;
                padding: 4px 8px;
                border: 1px solid #dfe0e6;
                background-color: #fff;
                border-left: none;
                -webkit-box-sizing: border-box;
                box-sizing: border-box;
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;
                max-width: 100%;
            }

        }
    }
    .hidden-count {
        display: inline-block;
        background-color: #f0f1f5;
        padding: 4px 8px;
    }
    .group-tag-popover {
        .group-tag {
            max-width: 300px;
            width: min-content;
            display: flex;
            margin-right: 5px;
            margin-bottom: 5px;
            &:last-child {
                margin-bottom: 0px;
            }
        }
        .key {
            flex-shrink: 0;
            display: inline-block;
            padding: 4px 8px;
            border: 1px solid #dfe0e6;
            background-color: #f0f1f5;
        }
        .value {
            display: inline-block;
            padding: 4px 8px;
            border: 1px solid #dfe0e6;
            background-color: #fff;
            border-left: none;
            -webkit-box-sizing: border-box;
            box-sizing: border-box;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
            max-width: 100%;
        }
    }
</style>
