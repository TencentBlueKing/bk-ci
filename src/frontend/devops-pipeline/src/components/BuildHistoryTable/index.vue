<template>
    <div
        class="build-history-table-container"
        v-bkloading="{ isLoading }"
    >
        <filter-bar
            @query="handlePageChange"
        />
        <bk-exception
            class="no-build-history-exception"
            v-if="!isLoading && !isQuerying && buildHistoryList.length === 0"
            type="empty"
        >
            <div class="no-build-history-box">
                <span>{{ $t(isDebug ? 'noDebugRecords' : 'noBuildHistory') }}</span>
                <div class="no-build-history-box-tip">
                    <div class="no-build-history-box-rows">
                        <p
                            v-for="(row, index) in emptyTips"
                            :key="index"
                        >
                            {{ $t(row) }}
                        </p>
                    </div>
                    <template v-if="!archiveFlag">
                        <bk-button
                            v-if="!isReleasePipeline && !isDebug"
                            @click="goEdit"
                            theme="primary"
                            size="large"
                            v-perm="{
                                hasPermission: canEdit,
                                disablePermissionApi: true,
                                permissionData: {
                                    projectId,
                                    resourceType: 'pipeline',
                                    resourceCode: pipelineId,
                                    action: RESOURCE_ACTION.EDIT
                                }
                            }"
                        >
                            {{ $t('goEdit') }}
                        </bk-button>
                        <span
                            v-else
                            v-bk-tooltips="tooltip"
                        >
                            <bk-button
                                :disabled="!executable"
                                @click="buildNow"
                                theme="primary"
                                size="large"
                                v-perm="{
                                    hasPermission: canExecute,
                                    disablePermissionApi: true,
                                    permissionData: {
                                        projectId,
                                        resourceType: 'pipeline',
                                        resourceCode: pipelineId,
                                        action: RESOURCE_ACTION.EXECUTE
                                    }
                                }"
                            >
                                {{ $t(isDebug ? 'debugNow' : 'buildNow') }}
                            </bk-button>
                        </span>
                    </template>
                </div>
            </div>
        </bk-exception>
        <div
            class="bkdevops-build-history-table-wrapper"
            ref="tableBox"
            :style="{ height: `${tableHeight}px` }"
            v-else
        >
            <bk-table
                class="bkdevops-build-history-table"
                :max-height="$refs?.tableBox?.offsetHeight"
                :data="buildHistoryList"
                :key="tableColumnKeys.join('-')"
                :row-class-name="handleRowStyle"
                :empty-text="$t('history.filterNullTips')"
                :pagination="pagination"
                @row-click="handleRowClick"
                @header-dragend="handleDragend"
                @page-change="handlePageChange"
                @page-limit-change="handleLimitChange"
            >
                <bk-table-column
                    :width="32"
                    :resizable="false"
                >
                    <template v-slot="{ row }">
                        <div
                            v-if="row.artifactQuality && Object.keys(row.artifactQuality).length >= 3"
                            @click.stop="toggleRowShowAll(row)"
                        >
                            <i :class="['devops-icon', 'shape-icon', row.showAll ? 'icon-down-shape' : 'icon-right-shape']" />
                        </div>
                    </template>
                </bk-table-column>
                <bk-table-column
                    v-for="col in tableColumnFields"
                    v-bind="col"
                    :prop="col.id"
                    :label="$t(col.label)"
                    :key="col.id"
                    show-overflow-tooltip
                >
                    <template
                        v-if="col.id === 'buildNum'"
                        v-slot="props"
                    >
                        <span class="build-num-status">
                            <router-link
                                :class="{ [props.row.status]: true }"
                                :to="getArchiveUrl(props.row)"
                            >
                                {{ getBuildNumHtml(props.row) }}
                            </router-link>
                            <logo
                                v-if="props.row.status === 'STAGE_SUCCESS'"
                                v-bk-tooltips="$t('details.statusMap.STAGE_SUCCESS')"
                                name="flag"
                                class="devops-icon"
                                size="12"
                                fill="#34d97b"
                            />
                            <i
                                v-else-if="
                                    props.row.status === 'QUEUE' ||
                                        props.row.status === 'RUNNING' ||
                                        !props.row.endTime
                                "
                                :class="{
                                    'devops-icon': true,
                                    'icon-hourglass hourglass-queue': props.row.status === 'QUEUE',
                                    'icon-circle-2-1 spin-icon': props.row.status === 'RUNNING' || !props.row.endTime
                                }"
                            >
                            </i>
                        </span>
                    </template>
                    <template
                        v-else-if="col.id === 'stageStatus'"
                        v-slot="props"
                    >
                        <stage-steps
                            v-if="props.row.stageStatus"
                            :steps="props.row.stageStatus"
                            :build-id="props.row.id"
                        ></stage-steps>
                        <span v-else>--</span>
                    </template>
                    <template
                        v-else-if="col.id === 'material'"
                        v-slot="props"
                    >
                        <div
                            class="build-material-cell"
                            v-for="(material, mIndex) in props.row.visibleMaterial"
                            :key="mIndex"
                        >
                            <MaterialItem
                                :material="material"
                                :show-more="false"
                                :key="material.aliasName"
                                @click.stop=""
                            />
                            <span
                                :class="['commit-times', {
                                    'commit-times-visible': material.commitTimes > 1
                                }]"
                            >
                                {{ material.commitTimes }}
                            </span>
                            <span
                                :class="['show-all-material-entry', 'history-text-link', {
                                    'show-all-material-entry-visible': props.row.material.length > 3
                                        && props.row.visibleMaterial.length === mIndex + 1
                                }]"
                                @click.stop="showMoreMaterial(props.row, mIndex)"
                            >
                                <i class="devops-icon icon-ellipsis" />
                            </span>
                        </div>
                        <span v-if="props.row.visibleMaterial.length === 0">--</span>
                    </template>
                    <template
                        v-else-if="col.id === 'artifactList'"
                        v-slot="props"
                    >
                        <template v-if="props.row.hasArtifactories">
                            <div class="artifact-list-cell">
                                <p
                                    class="artifact-entry history-text-link"
                                    @click.stop="(e) => showArtifactoriesPopup(e, props.row.index)"
                                    v-html="`${$t('history.fileUnit', [props.row.artifactList.length])}<span>（${props.row.sumSize}）</span>`"
                                />
                                <div
                                    v-if="props.row.shortUrl"
                                    @click.stop=""
                                >
                                    <bk-popover
                                        theme="light"
                                        trigger="click"
                                        placement="bottom-end"
                                    >
                                        <bk-button
                                            text
                                            theme="primary"
                                        >
                                            <i class="devops-icon icon-qrcode" />
                                        </bk-button>
                                        <div
                                            class="build-qrcode-popup"
                                            slot="content"
                                        >
                                            <span v-html="$t('scanQRCodeView')"></span>
                                            <qrcode
                                                :text="props.row.shortUrl"
                                                :size="76"
                                            >
                                                {{ props.row.shortUrl }}
                                            </qrcode>
                                        </div>
                                    </bk-popover>
                                </div>
                            </div>
                        </template>
                        <span v-else>--</span>
                    </template>
                    <template
                        v-else-if="col.id === 'appVersions'"
                        v-slot="props"
                    >
                        <template v-if="props.row.appVersions.length">
                            <div
                                class="build-app-version-list"
                                v-bk-tooltips="versionToolTipsConf"
                            >
                                <p
                                    v-for="(appVersion, index) in props.row.visibleAppVersions"
                                    :key="index"
                                >
                                    {{ appVersion }}
                                </p>
                            </div>
                            <div id="app-version-tooltip-content">
                                <p
                                    v-for="(appVersion, index) in props.row.appVersions"
                                    :key="index"
                                >
                                    {{ appVersion }}
                                </p>
                            </div>
                        </template>
                        <span v-else>--</span>
                    </template>
                    <template
                        v-else-if="col.id === 'artifactQuality'"
                        v-slot="{ row }"
                    >
                        <div
                            v-if="row.artifactQuality && Object.keys(row.artifactQuality).length"
                            class="artifact-quality"
                        >
                            <ArtifactQuality
                                :data="row.showAll ? row.artifactQuality : getSlicedData(row)"
                            />
                            <div
                                v-if="Object.keys(row.artifactQuality).length >= 3"
                                class="more-btn"
                                @click.stop="toggleRowShowAll(row)"
                            >
                                <span>
                                    {{ row.showAll ? $t('settings.fold') : $t('totalArtifactCount', [Object.keys(row.artifactQuality).length]) }}
                                    <i :class="['devops-icon', 'angle-icon', row.showAll ? 'icon-angle-up' : 'icon-angle-down']" />
                                </span>
                            </div>
                        </div>
                        <span v-else>--</span>
                    </template>
                    <template
                        v-else-if="col.id === 'startType'"
                        v-slot="props"
                    >
                        <p class="trigger-cell">
                            <logo
                                size="14"
                                :name="props.row.startType"
                            />
                            <span>{{ props.row.userId }}</span>
                        </p>
                    </template>
                    <template
                        v-else-if="col.id === 'entry'"
                        v-slot="props"
                    >
                        <p
                            class="entry-link"
                            @click.stop="showLog(props.row)"
                        >
                            {{ $t("history.completedLog") }}
                        </p>
                    </template>
                    <template
                        v-else-if="col.id === 'remark'"
                        v-slot="props"
                    >
                        <div class="remark-cell">
                            <div
                                @click.stop=""
                                v-if="activeRemarkIndex === props.row.index"
                                class="pipeline-build-remark-editor"
                            >
                                <bk-input
                                    type="textarea"
                                    ref="remarkInput"
                                    rows="3"
                                    :disabled="isChangeRemark"
                                    :maxlength="4096"
                                    class="remark-input"
                                    v-model.trim="tempRemark"
                                />
                                <i
                                    v-if="isChangeRemark"
                                    class="devops-icon icon-circle-2-1 spin-icon"
                                />
                                <template v-else>
                                    <i
                                        class="devops-icon icon-check-small"
                                        @click.stop="handleRemarkChange(props.row)"
                                    />
                                    <i
                                        class="devops-icon icon-close-small"
                                        @click.stop="resetRemark"
                                    />
                                </template>
                            </div>
                            <template v-else>
                                <span
                                    :class="{ 'remark-span': true, active: props.row.active }"
                                    v-bk-tooltips="{
                                        allowHTML: false,
                                        content: props.row.remark,
                                        maxWidth: 500,
                                        disabled: !props.row.remark, delay: [300, 0]
                                    }"
                                >
                                    {{ props.row.remark || "--" }}
                                </span>
                                <i
                                    v-if="!archiveFlag"
                                    class="devops-icon icon-edit-line remark-entry"
                                    @click.stop="activeRemarkInput(props.row)"
                                />
                                <i
                                    v-if="!archiveFlag && props.row.remark"
                                    class="bk-icon icon-copy remark-entry"
                                    @click.stop="coptRemark(props.row)"
                                />
                            </template>
                        </div>
                    </template>
                    <template
                        v-else-if="col.id === 'pipelineVersion'"
                        v-slot="props"
                    >
                        <div>
                            <span>{{ props.row.pipelineVersionName ?? '--' }}</span>
                            <logo
                                v-if="isNotLatest(props)"
                                v-bk-tooltips="$t('details.pipelineVersionDiffTips')"
                                size="12"
                                class="version-tips"
                                name="warning-circle"
                            />
                        </div>
                    </template>
                    <template
                        v-else-if="col.id === 'errorCode'"
                        v-slot="props"
                    >
                        <div
                            v-if="props.row.errorInfoList.length > 0"
                            class="error-code-cell"
                        >
                            <ul class="error-code-list">
                                <li
                                    class="error-code-item"
                                    v-for="item in props.row.errorInfoList"
                                    :key="item.taskId"
                                >
                                    <span v-if="item.title">{{ $t(item.title) }}</span>
                                    <span>
                                        ({{ item.errCode }})
                                    </span>
                                </li>
                            </ul>
                            <i
                                @click.stop="showErrorInfoPopup(props.row.index)"
                                class="devops-icon icon-list"
                            />
                        </div>
                        <span v-else>--</span>
                    </template>
                </bk-table-column>
                <template v-if="!archiveFlag">
                    <bk-table-column
                        v-if="!isDebug"
                        :label="$t('operate')"
                        fixed="right"
                        width="80"
                    >
                        <template v-slot="props">
                            <bk-button
                                v-if="retryable(props.row)"
                                text
                                theme="primary"
                                size="small"
                                @click.stop="retry(props.row.id)"
                            >
                                {{ $t(isDebug ? 'reDebug' : 'history.reBuild') }}
                            </bk-button>
                        </template>
                    </bk-table-column>
                    <bk-table-column
                        type="setting"
                        :tippy-options="{ zIndex: 3000 }"
                    >
                        <TableColumnSetting
                            ref="tableSetting"
                            :selected-column-keys="tableColumnKeys"
                            :all-table-column-map="allTableColumnMap"
                            @change="handleColumnChange"
                            @reset="handleColumnReset"
                        />
                    </bk-table-column>
                </template>
                <empty-exception
                    slot="empty"
                    type="search-empty"
                    @clear="clearFilter"
                />
            </bk-table>
        </div>

        <bk-dialog
            v-model="isShowMoreArtifactories"
            render-directive="if"
            :width="900"
            ext-cls="history-dialog"
            header-position="left"
            :title="`#${activeBuild && activeBuild.buildNum} - ${$t('history.artifactList')}`"
            @cancel="hideArtifactoriesPopup"
            :style="{ '--dialog-top-translateY': `translateY(${dialogTopOffset}px)` }"
        >
            <template slot="header">
                <div class="artifactory-popup-header">
                    <span class="header-title">{{ $t('history.artifactList') }}</span>
                    <span
                        class="pipeline-name"
                        v-bk-tooltips="{ content: pipelineName }"
                    >{{ $t('pipeline') }}: {{ pipelineName }}</span>
                    <span class="build-num">{{ $t('buildNum') }}: {{ `#${activeBuild && activeBuild.buildNum}` }}</span>
                    <bk-button
                        text
                        theme="primary"
                        class="outputs-btn"
                        @click.stop="gotoArtifactoryList"
                    >
                        <span class="go-outputs-btn">
                            <logo
                                name="tiaozhuan"
                                size="14"
                            />
                            {{ $t("goOutputs") }}
                        </span>
                    </bk-button>
                </div>
            </template>
            <!-- <p class="artifactory-popup-header">
                    <bk-button
                        text
                        theme="primary"
                        @click.stop="gotoArtifactoryList"
                    >
                        <span class="go-outputs-btn">
                            <logo
                                name="tiaozhuan"
                                size="18"
                            />
                            {{ $t("goOutputs") }}
                        </span>
                    </bk-button>
                </p> -->
            <ul
                class="build-artifact-list-ul"
                v-if="visibleIndex !== -1"
                :style="{ 'max-height': `${ulMaxHeight}px` }"
            >
                <li
                    v-for="artifactory in actifactories"
                    :key="artifactory.name"
                >
                    <p class="build-artifact-name">
                        <i :class="['devops-icon', `icon-${artifactory.icon}`]"></i>
                        <span
                            :title="artifactory.name"
                            class="artifact-name-span"
                        >
                            {{ artifactory.name }}
                        </span>
                        <span class="artifact-size">
                            ({{ artifactory.size }})
                        </span>
                    </p>
                    <div class="build-artifactory-operation">
                        <bk-button
                            v-if="artifactory.artifactoryType !== 'IMAGE'"
                            text
                            size="small"
                            theme="primary"
                            @click.stop="downloadFile(artifactory)"
                        >
                            {{ $t('download') }}
                        </bk-button>
                        <bk-button
                            v-if="artifactory.artifactoryType === 'PIPELINE'"
                            text
                            size="small"
                            theme="primary"
                            @click.stop.native="copyToCustom(artifactory)"
                        >
                            {{ $t('history.copyToCustomArtifactory') }}
                        </bk-button>
                    </div>
                </li>
            </ul>
            <footer slot="footer">
                <bk-button @click="hideArtifactoriesPopup">
                    {{ $t('close') }}
                </bk-button>
            </footer>
        </bk-dialog>
        <bk-dialog
            v-model="isShowMoreMaterial"
            render-directive="if"
            :width="640"
            header-position="left"
            :title="`#${activeBuild && activeBuild.buildNum} - ${$t('editPage.material')}`"
            @cancel="hideMoreMaterial"
        >
            <template v-if="activeBuild">
                <div
                    class="all-build-material-row"
                    v-for="material in activeBuild.material"
                    :key="material.aliasName"
                >
                    <MaterialItem
                        :is-fit-content="false"
                        :material="material"
                        :show-more="false"
                    />
                </div>
            </template>
            <footer slot="footer">
                <bk-button @click="hideMoreMaterial">
                    {{ $t('close') }}
                </bk-button>
            </footer>
        </bk-dialog>
        <bk-dialog
            v-model="showErorrInfoDialog"
            render-directive="if"
            :width="640"
            header-position="left"
            :title="`#${activeBuild && activeBuild.buildNum} - ${$t('history.errorCode')}`"
            @cancel="hideErrorInfoPopup"
        >
            <ul
                class="error-info-list"
                v-if="activeBuild"
            >
                <li
                    v-for="item in activeBuild.errorInfoList"
                    :key="item.errCode"
                >
                    <logo
                        :name="item.icon"
                        size="18"
                    />
                    <p v-bk-tooltips="{ maxWidth: 600, content: item.errorMsg }">
                        {{ $t(item.title) }} (<b>{{ item.errCode }}</b>): {{ item.errorMsg }}
                    </p>
                </li>
            </ul>
            <footer slot="footer">
                <bk-button @click="hideErrorInfoPopup">
                    {{ $t('close') }}
                </bk-button>
            </footer>
        </bk-dialog>
    </div>
</template>

<script>
    import FilterBar from '@/components/BuildHistoryTable/FilterBar'
    import TableColumnSetting from '@/components/BuildHistoryTable/TableColumnSetting'
    import MaterialItem from '@/components/ExecDetail/MaterialItem'
    import Logo from '@/components/Logo'
    import StageSteps from '@/components/StageSteps'
    import EmptyException from '@/components/common/exception'
    import qrcode from '@/components/devops/qrcode'
    import ArtifactQuality from '@/components/ExecDetail/artifactQuality'
    import {
        BUILD_HISTORY_TABLE_COLUMNS_MAP,
        BUILD_HISTORY_TABLE_DEFAULT_COLUMNS,
        errorTypeMap,
        extForFile
    } from '@/utils/pipelineConst'
    import { convertFileSize, convertMStoString, convertTime, flatSearchKey, copyToClipboard } from '@/utils/util'
    import webSocketMessage from '@/utils/webSocketMessage'
    import { mapActions, mapGetters, mapState } from 'vuex'

    import {
        RESOURCE_ACTION
    } from '@/utils/permission'
    const LS_COLUMN_KEY = 'shownColumnsKeys'
    export default {
        name: 'build-history-table',
        components: {
            Logo,
            qrcode,
            StageSteps,
            MaterialItem,
            FilterBar,
            TableColumnSetting,
            ArtifactQuality,
            EmptyException
        },
        props: {
            showLog: {
                type: Function
            },
            isDebug: Boolean
        },
        data () {
            const lsColumns = localStorage.getItem(LS_COLUMN_KEY)
            const initSortedColumns = lsColumns ? JSON.parse(lsColumns) : BUILD_HISTORY_TABLE_DEFAULT_COLUMNS
            return {
                RESOURCE_ACTION,
                isShowMoreMaterial: false,
                isShowMoreArtifactories: false,
                showErorrInfoDialog: false,
                activeIndex: 0,
                activeRemarkIndex: 0,
                tempRemark: '',
                isChangeRemark: false,
                visibleIndex: -1,
                retryingMap: {},
                buildHistories: [],
                stoping: {},
                isLoading: false,
                tableColumnKeys: initSortedColumns,
                tableHeight: null,
                dialogTopOffset: null
            }
        },
        computed: {
            ...mapGetters({
                historyPageStatus: 'pipelines/getHistoryPageStatus',
                isReleasePipeline: 'atom/isReleasePipeline',
                isCurPipelineLocked: 'atom/isCurPipelineLocked'
            }),
            ...mapState('atom', [
                'pipelineInfo',
                'activePipelineVersion'
            ]),
            allTableColumnMap () {
                return BUILD_HISTORY_TABLE_COLUMNS_MAP
            },
            tableColumnFields () {
                return this.tableColumnKeys.map(key => this.allTableColumnMap[key])
            },
            projectId () {
                return this.$route.params.projectId
            },
            pipelineId () {
                return this.$route.params.pipelineId
            },
            routePipelineVersion () {
                return this.$route.params.version ? parseInt(this.$route.params.version) : this.pipelineInfo?.releaseVersion
            },
            canEdit () {
                return this.pipelineInfo?.permissions.canEdit ?? true
            },
            canExecute () {
                return this.pipelineInfo?.permissions.canExecute ?? true
            },
            isQuerying () {
                return this.historyPageStatus?.isQuerying ?? false
            },
            canManualStartup () {
                return this.pipelineInfo?.canManualStartup ?? true
            },
            pipelineName () {
                return this.pipelineInfo?.pipelineName
            },
            executable () {
                return !this.isCurPipelineLocked && ((this.canManualStartup && this.isReleasePipeline) || this.isDebug)
            },
            emptyTips () {
                return [...(
                    !this.isReleasePipeline && !this.isDebug
                        ? [
                            'onlyDraftBuildHistoryTips',
                            'onlyDraftBuildHistoryIdTips'
                        ]
                        : this.canManualStartup
                            ? [
                                'noBuildHistoryTips'
                            ]
                            : []
                ), 'buildHistoryIdTips']
            },
            tooltip () {
                return this.executable
                    ? {
                        disabled: true
                    }
                    : {
                        content: this.$t(!this.isReleasePipeline ? 'draftPipelineExecTips' : this.isCurPipelineLocked ? 'pipelineLockTips' : 'pipelineManualDisable'),
                        delay: [300, 0]
                    }
            },
            versionToolTipsConf () {
                return {
                    delay: 500,
                    content: '#app-version-tooltip-content'
                }
            },
            pagination () {
                const { count, pageSize, page } = this.historyPageStatus
                return {
                    limit: pageSize,
                    current: page,
                    count
                }
            },
            statusIconMap () {
                return {
                    SUCCEED: 'check-circle-shape',
                    FAILED: 'close-circle-shape',
                    RUNNING: 'circle-2-1',
                    PAUSE: 'play-circle-shape',
                    SKIP: 'redo-arrow'
                }
            },
            buildHistoryList () {
                return this.buildHistories.map((item, index) => {
                    const active = index === this.activeIndex
                    const hasArtifactories
                        = Array.isArray(item.artifactList) && item.artifactList.length > 0
                    let shortUrl = ''
                    const appVersions = []
                    let sumSize = 0
                    const artifactories = hasArtifactories
                        ? item.artifactList.map((artifactory) => {
                            if (artifactory.shortUrl) {
                                shortUrl = artifactory.shortUrl
                            }
                            if (artifactory.appVersion) {
                                appVersions.push(`${artifactory.appVersion} (${artifactory.name})`)
                            }
                            sumSize += artifactory.size
                            return {
                                ...artifactory,
                                name: artifactory.name,
                                icon: extForFile(artifactory.name),
                                size: convertFileSize(artifactory.size, 'B')
                            }
                        })
                        : []
                    const stageStatus = item.stageStatus
                        ? item.stageStatus.slice(1).map((stage) => ({
                            ...stage,
                            tooltip: this.getStageTooltip(stage),
                            icon: this.statusIconMap[stage.status] || 'circle',
                            statusCls: stage.status
                        }))
                        : null
                    return {
                        ...item,
                        index,
                        active,
                        hasArtifactories,
                        shortUrl,
                        appVersions,
                        visibleAppVersions:
                            !active && Array.isArray(appVersions) && appVersions.length > 1
                                ? appVersions.slice(0, 1)
                                : appVersions,
                        startTime: item.startTime ? convertTime(item.startTime) : '--',
                        endTime: item.endTime ? convertTime(item.endTime) : '--',
                        queueTime: item.queueTime ? convertTime(item.queueTime) : '--',
                        totalTime: item.totalTime ? convertMStoString(item.totalTime) : '--',
                        executeTime: item.executeTime ? convertMStoString(item.executeTime) : '--',
                        visibleMaterial:
                            Array.isArray(item.material) ? item.material.slice(0, !active ? 1 : 3) : [],
                        sumSize: convertFileSize(sumSize, 'B'),
                        artifactories,
                        stageStatus,
                        showAll: false,
                        errorInfoList:
                            (!active && Array.isArray(item.errorInfoList) && item.errorInfoList.length > 1
                                ? item.errorInfoList.slice(0, 1)
                                : item.errorInfoList)?.map(err => {
                                    return {
                                    ...err,
                                    errCode: err?.errorCode ?? '--',
                                    ...(
                                        errorTypeMap[err.errorType] ?? {}
                                    )
                                }
                                }) ?? []
                    }
                })
            },
            actifactories () {
                return this.activeBuild?.artifactories ?? []
            },
            activeBuild () {
                const { buildHistoryList, visibleIndex } = this
                return buildHistoryList[visibleIndex] ?? null
            },
            currentBuildId () {
                return this.activeBuild.id
            },
            historyQuerys () {
                const { historyPageStatus: { query, searchKey, page, pageSize } } = this
                return {
                    query,
                    searchKey,
                    page,
                    pageSize
                }
            },
            ulMaxHeight () {
                return window.innerHeight * 0.8 - 167
            },
            dialogWidth () {
                return window.innerWidth * 0.8
            },
            archiveFlag () {
                return this.$route.query.archiveFlag
            }
        },
        watch: {
            'activePipelineVersion.version' (newVersion) {
                if ((newVersion !== this.routePipelineVersion)) {
                    this.handlePageChange(1)
                }
            },
            historyQuerys: {
                handler (val) {
                    const { query, searchKey, page, pageSize } = val
                    const queryMap = new URLSearchParams({
                        page,
                        pageSize,
                        ...query,
                        ...flatSearchKey(searchKey)
                    })
                    this.$router.push({
                        query: Object.fromEntries(queryMap.entries())
                    })
                },
                deep: true
            }
        },
        mounted () {
            webSocketMessage.installWsMessage(this.requestHistory)
            window.addEventListener('resize', this.updateTableHeight)
        },

        beforeDestroy () {
            webSocketMessage.unInstallWsMessage()
            window.removeEventListener('resize', this.updateTableHeight)
        },

        methods: {
            ...mapActions('pipelines', [
                'updateBuildRemark',
                'requestPipelinesHistory',
                'setHistoryPageStatus',
                'resetHistoryFilterCondition'
            ]),

            getSlicedData (row) {
                const keys = Object.keys(row.artifactQuality)
                const slicedKeys = keys.slice(0, 2)
                return slicedKeys.reduce((obj, key) => {
                    obj[key] = row.artifactQuality[key]
                    return obj
                }, {})
            },
            toggleRowShowAll (row) {
                row.showAll = !row.showAll
            },
            updateTableHeight () {
                this.tableHeight = this.$refs.tableBox?.offsetHeight
            },
            handleColumnChange (columns) {
                this.tableColumnKeys = columns
                this.$refs.tableSetting.$parent.instance?.hide()
                localStorage.setItem(LS_COLUMN_KEY, JSON.stringify(columns))
            },
            handleColumnReset () {
                this.tableColumnKeys = [...BUILD_HISTORY_TABLE_DEFAULT_COLUMNS]
                localStorage.setItem(LS_COLUMN_KEY, JSON.stringify(BUILD_HISTORY_TABLE_DEFAULT_COLUMNS))
                this.$refs.tableSetting.$parent.instance?.hide()
            },
            async requestHistory () {
                try {
                    const version = this.routePipelineVersion
                    if (!version) return
                    this.isLoading = true
                    this.resetRemark()
                    const {
                        projectId,
                        pipelineId
                    } = this
                    const res = await this.requestPipelinesHistory({
                        projectId,
                        pipelineId,
                        isDebug: this.isDebug,
                        archiveFlag: this.archiveFlag
                    })
                    this.setHistoryPageStatus({
                        count: res.count
                    })
                    this.buildHistories = res.records
                } catch (err) {
                    if (err.code === 403) {
                        this.hasNoPermission = true
                    } else {
                        this.$showTips({
                            message: err.message || err,
                            theme: 'error'
                        })
                        if ((err.code === 404 || err.httpStatus === 404) && this.$route.name !== 'PipelineManageList') {
                            this.$router.push({
                                name: 'PipelineManageList'
                            })
                        }
                    }
                } finally {
                    this.isLoading = false
                }
            },
            handlePageChange (page) {
                this.setHistoryPageStatus({
                    page
                })
                this.$nextTick(() => {
                    this.requestHistory()
                })
            },
            handleLimitChange (limit) {
                this.setHistoryPageStatus({
                    page: 1,
                    pageSize: limit
                })
                this.$router.push({
                    params: this.$route.params,
                    query: {
                        ...this.$route.query,
                        page: 1,
                        pageSize: limit
                    }
                })
                this.$nextTick(() => {
                    this.requestHistory()
                })
            },
            showMoreMaterial (row, mIndex) {
                if (row.material.length > 3 && row.visibleMaterial.length - 1 === mIndex) {
                    this.isShowMoreMaterial = true
                    this.visibleIndex = row.index
                }
            },
            hideMoreMaterial () {
                this.visibleIndex = -1
                this.isShowMoreMaterial = false
            },
            isNotLatest ({ $index }) {
                const length = this.buildHistoryList.length
                // table最后一条记录必不变化
                if ($index === length - 1) return false
                const current = this.buildHistoryList[$index]
                const before = this.buildHistoryList[$index + 1]
                return current.pipelineVersion !== before.pipelineVersion
            },
            getStageTooltip (stage) {
                switch (true) {
                    case !!stage.elapsed:
                        return `${stage.name}: ${convertMStoString(stage.elapsed)}`
                    case stage.status === 'PAUSE':
                        return this.$t('editPage.toCheck')
                    case stage.status === 'SKIP':
                        return this.$t('skipStageDesc')
                }
            },
            activeRemarkInput (row) {
                this.activeIndex = row.index
                this.activeRemarkIndex = row.index
                this.tempRemark = row.remark
            },
            coptRemark (row) {
                if (row.remark) {
                    copyToClipboard(row.remark)
                    this.$bkMessage({
                        theme: 'success',
                        message: this.$t('copySuc'),
                        limit: 1
                    })
                }
            },
            retryable (row) {
                return ['QUEUE', 'RUNNING'].indexOf(row.status) < 0
            },
            async handleRemarkChange (row) {
                if (this.isChangeRemark) return
                const preRemark = row.remark
                try {
                    const {
                        $route: { params },
                        tempRemark
                    } = this
                    if (tempRemark !== row.remark) {
                        this.isChangeRemark = true
                        this.$set(row, 'remark', tempRemark)
                        await this.updateBuildRemark({
                            ...params,
                            buildId: row.id,
                            remark: tempRemark
                        })
                        this.$showTips({
                            theme: 'success',
                            message: this.$t('updateSuc')
                        })
                    }
                } catch (e) {
                    console.log(e)
                    this.$showTips({
                        theme: 'error',
                        message: this.$t('updateFail')
                    })
                    this.$set(row, 'remark', preRemark)
                } finally {
                    this.resetRemark()
                }
            },
            resetRemark () {
                this.activeRemarkIndex = -1
                this.isChangeRemark = false
                this.tempRemark = ''
            },
            handleRowStyle ({ row, rowIndex }) {
                return rowIndex === this.activeIndex ? 'expand-row is-row-hover' : 'is-row-hover'
            },
            handleRowClick (row, e) {
                this.hideArtifactoriesPopup()
                if (this.activeIndex === row.index) {
                    const url = this.getArchiveUrl(row)
                    this.$router.push(url)
                } else {
                    this.activeIndex = row.index
                }
            },
            handleDragend (newWidth, oldWidth, column, event) {
                console.log(column)
                localStorage.setItem(`${column.property}Width`, newWidth)
                if (this.allTableColumnMap[column.property]?.width) {
                    this.allTableColumnMap[column.property].width = newWidth
                }
            },
            getArchiveUrl ({ id: buildNo }, type = '', codelib = '') {
                const { projectId, pipelineId } = this
                return {
                    name: 'pipelinesDetail',
                    params: {
                        projectId,
                        pipelineId,
                        buildNo,
                        ...(type ? { type } : {})
                    },
                    hash: codelib ? `#${codelib}` : '',
                    query: {
                        ...(this.archiveFlag ? { archiveFlag: this.archiveFlag } : {})
                    }
                }
            },

            gotoArtifactoryList () {
                if (this.activeBuild) {
                    const url = this.getArchiveUrl(this.activeBuild, 'outputs')
                    this.$router.push(url)
                }
            },

            goCodeRecords (row, aliasName) {
                if (row) {
                    const url = this.getArchiveUrl(row, 'codeRecords', aliasName)
                    url && this.$router.push(url)
                }
            },

            hideArtifactoriesPopup () {
                this.isShowMoreArtifactories = false
                this.visibleIndex = -1
            },
            showArtifactoriesPopup (e, index = -1) {
                this.visibleIndex = index

                const ITEM_HEIGHT = 46
                const DIALOG_EXTRA_HEIGHT = 167
                const totalListHeight = this.actifactories.length * ITEM_HEIGHT
                const listHeight = Math.min(totalListHeight, this.ulMaxHeight)
                this.dialogTopOffset = -Math.round((listHeight + DIALOG_EXTRA_HEIGHT) / 2)

                this.isShowMoreArtifactories = true
            },
            hideErrorInfoPopup () {
                this.showErorrInfoDialog = false
                this.visibleIndex = -1
            },
            showErrorInfoPopup (index = -1) {
                this.visibleIndex = index
                this.showErorrInfoDialog = true
            },
            async downloadFile ({ artifactoryType, path, name }, key = 'download') {
                try {
                    const { projectId } = this.$route.params
                    const res = await this.$store.dispatch('common/requestDownloadUrl', {
                        projectId,
                        artifactoryType,
                        path
                    })

                    this.$showTips({
                        message: `${this.$t('history.downloading')}${name}`,
                        theme: 'success'
                    })
                    window.open(res.url, '_self')
                } catch (err) {
                    const { projectId, pipelineId } = this
                    this.handleError(err, {
                        projectId,
                        resourceCode: pipelineId,
                        action: this.$permissionResourceAction.DOWNLOAD
                    })
                }
            },
            async copyToCustom (artifactory) {
                let message, theme
                try {
                    const { projectId, pipelineId } = this
                    const params = {
                        files: [artifactory.name],
                        copyAll: false
                    }
                    const res = await this.$store.dispatch('common/requestCopyArtifactory', {
                        projectId,
                        pipelineId,
                        buildNo: this.currentBuildId,
                        params
                    })
                    if (res) {
                        message = this.$t('history.copySuc', [artifactory.name])
                        theme = 'success'
                    }
                } catch (err) {
                    message = err.message ? err.message : err
                    theme = 'error'
                } finally {
                    this.$showTips({
                        message,
                        theme
                    })
                }
            },
            /**
             * 重试流水线
             */
            async retry (buildId) {
                if (this.retryingMap[buildId]) return
                let message, theme
                const { $store } = this
                try {
                    this.retryingMap[buildId] = true
                    // 请求执行构建
                    const res = await $store.dispatch('pipelines/requestRetryPipeline', {
                        ...this.$route.params,
                        buildId
                    })

                    if (res.id) {
                        message = this.$t('subpage.rebuildSuc')
                        theme = 'success'

                        this.requestHistory()
                    } else {
                        message = this.$t('subpage.rebuildFail')
                        theme = 'error'
                    }
                } catch (err) {
                    const { projectId, pipelineId } = this
                    this.handleError(err, {
                        projectId,
                        resourceCode: pipelineId,
                        action: this.$permissionResourceAction.EXECUTE
                    })
                } finally {
                    delete this.retryingMap[buildId]
                    message
                        && this.$showTips({
                            message,
                            theme
                        })
                }
            },
            getBuildNumHtml (row) {
                if (row.buildNumAlias && row.buildNumAlias.length) {
                    return row.buildNumAlias
                }
                return '#' + row.buildNum
            },
            buildNow () {
                this.$router.push({
                    name: 'executePreview',
                    query: {
                        ...(this.isDebug ? { debug: '' } : {})
                    },
                    params: {
                        ...this.$route.params,
                        version: this.pipelineInfo?.[this.isDebug ? 'version' : 'releaseVersion']
                    }
                })
            },
            goEdit () {
                this.$router.push({
                    name: 'pipelinesEdit',
                    params: {
                        ...this.$route.params,
                        version: this.pipelineInfo?.[this.isDebug ? 'version' : 'releaseVersion']
                    }
                })
            },
            clearFilter () {
                this.resetHistoryFilterCondition({ retainArchiveFlag: true })
                this.$nextTick(() => {
                    this.requestHistory()
                })
            }
        }
    }
</script>

<style lang="scss">
@import "../../scss/conf";
@import "../../scss/mixins/ellipsis";
@import "../../scss/pipelineStatus";

.entry-link {
  padding: 2px 0;
  font-size: 12px;
  cursor: pointer;
  color: #333333;
  > a {
    color: #333333;
  }
  &:hover {
    color: $primaryColor;
    > a {
      color: $primaryColor;
    }
  }
}
.build-history-table-container {
  flex: 1;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.build-list-table-empty-tips {
  width: auto;
  margin: 0;
}

.history-text-link {
  cursor: pointer;
  &:hover {
    color: $primaryColor !important;
  }
}

.no-build-history-exception {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    .no-build-history-box {
        display: grid;
        grid-gap: 12px;
        font-size: 12px;
        color: #979BA5;
        margin-top: -20px;
        .no-build-history-box-tip {
            background: #FAFBFD;
            padding: 24px;

            .no-build-history-box-rows {
                margin-bottom: 24px;
                > p {
                    text-align: left;
                    line-height: 24px;
                }
            }
        }

    }
}

.expand-row {
  height: 96px;
  cursor: pointer;
}
.all-build-material-row {
    padding: 8px 0;
    border-bottom: 2px solid #EAEBF0;
    &:first-child {
        border-top: 2px solid #EAEBF0;
    }
}
.bkdevops-build-history-table-wrapper {
    flex: 1;
    overflow: hidden;
    .bkdevops-build-history-table {
      color: #333333;
      &.bk-table-enable-row-transition .bk-table-body td {
            transition: none;
        }
      .bk-table-header-wrapper {
        height: 43px;
      }
      .bk-table-body-wrapper {
        tr:hover {
          .remark-entry {
            display: inline-block;
          }
        }
      }

      .build-num-status {
        display: flex;
        align-items: center;
        padding: 12px 0;
        .devops-icon {
          margin-left: 6px;
          display: inline-block;
        }
        .icon-retry {
          font-size: 14px;
          cursor: pointer;
          &:hover {
            color: $primaryColor;
          }
        }
      }
      .build-material-cell {
        display: flex;
        align-items: center;
        .commit-times {
            padding: 0 6px;
            margin-left: 4px;
            background: #F0F1F5;
            border-radius: 50%;
            color: #979BA5;
            text-align: center;
            @include ellipsis();
            display: none;
            &.commit-times-visible {
                display: inline-flex;
            }
        }
        .show-all-material-entry {
            padding: 0 10px;
            color: #979BA5;
            font-size: 16px;
            font-weight: bold;
            margin-top: 3px;
            opacity: 0;
            pointer-events: none;
            &-visible {
                opacity: 1;
                pointer-events: auto;
            }
        }
      }
      .shape-icon {
        color: #C4C6CC;
      }
      .angle-icon {
        margin-left: 4px;
        font-size: 10px;
      }
      .more-btn {
        min-width: 80px;
        max-width: 90px;
        margin-top: 3px;
        background: #FAFBFD;
        text-align: center;
        border: 1px solid #DCDEE5;
        border-radius: 2px;
        padding: 2px;
        font-size: 12px;
        color: #3A84FF;
      }
      .artifact-quality {
        padding: 10px 0;
      }
      .trigger-cell {
        display: flex;
        align-items: center;
        > svg {
          fill: currentColor;
        }
        > span {
          padding-left: 4px;
        }
      }
      .artifact-list-cell {
        display: flex;
        align-items: center;
        height: 100%;
        .artifact-entry {
            > b {
                color: $primaryColor;
            }
            > span {
                color: #C4C6CC;
            }
        }
      }
      .build-app-version-list {
        display: flex;
        flex-direction: column;
        > p {
          @include ellipsis();
        }
      }
      .remark-cell {
        position: relative;
        display: flex;
        align-items: center;
        .remark-span {
          cursor: pointer;
          display: -webkit-box;
          -webkit-box-orient: vertical;
          -webkit-line-clamp: 1;
          overflow: hidden;
          &.active {
            -webkit-line-clamp: 3;
          }
        }
        .remark-entry {
          display: none;
          cursor: pointer;
          vertical-align: middle;
          margin-left: 4px;
          padding: 6px;
          color: $primaryColor;

        }
      }
      .pipeline-build-remark-editor {
        display: grid;
        grid-auto-flow: column;
        grid-gap: 4px;
        width: 100%;
        grid-auto-columns: 1fr auto auto;
        margin: 10px 0;
        align-items: flex-start;
        .devops-icon {
            font-size: 24px;
            cursor: pointer;
            &.icon-check-small {
                color: $successColor;
            }
            &.spin-icon {
                font-size: 14px;

            }
        }
      }
    }
    .error-code-cell{
        display: flex;
        align-items: center;
        grid-gap: 10px;
        .icon-list {
        color: $primaryColor;
        cursor: pointer;
        }
        .error-code-list {
            flex: 1;
            .error-code-item {
            display: flex;
            width: 100%;
            align-items: center;
            > span {
                @include ellipsis();
            }
            }
        }
    }
    .version-tips {
    display: inline-block;
    vertical-align: -1px;
    color: #f6b026;
    font-size: 0;
    }
}

.build-qrcode-popup {
    text-align: center;
    color: #63656E;
    display: grid;
    grid-gap: 8px;
}
.artifactory-popup-header {
    display: flex;
    align-items: center;
    margin-bottom: 10px;
    >span {
        display: inline-block;
        font-size: 14px;
        color: #979BA5;
    }
    .header-title {
        font-size: 20px;
        color: #313238;
        line-height: 28px;
    }
    .pipeline-name {
        max-width: 300px;
        padding-left: 9px;
        margin-left: 9px;
        border-left: 1px solid #979BA5;
        @include ellipsis();
    }
    .build-num {
        padding: 0 26px;
    }
    .go-outputs-btn {
        svg {
            vertical-align: middle;
        }
        display: grid;
        align-items: center;
        grid-gap: 6px;
        grid-auto-flow: column;
        font-size: 14px;
        color: #3A84FF;
    }
}
.build-artifact-list-ul {
    border-top: 1px solid #EAEBF0;
    max-height: calc(100vh / 3);
    overflow: auto;
    > li {
        min-height: 38px;
        display: flex;
        align-items: center;
        justify-content: space-between;
        border-bottom: 1px solid #EAEBF0;
        font-size: 12px;
        padding: 8px 10px;
        line-height: 20px;
        .build-artifact-name {
            display: grid;
            grid-gap: 6px;
            grid-auto-flow: column;
            align-items: center;
            .artifact-name-span {
                @include multiline-ellipsis();
            }

        }
        .artifact-size {
            color: #C4C6CC;
        }
        .build-artifactory-operation {
            max-width: 160px;
            display: grid;
            grid-auto-flow: column;
            flex-shrink: 0;

            .bk-button-text.bk-button-small {
                padding: 0 10px !important;
            }
        }
    }
}
.error-info-list {
    > li {
        font-size: 12px;
        display: grid;
        grid-auto-flow: column;
        grid-template-columns: auto auto 1fr;
        align-items: flex-start;
        grid-gap: 6px;
        word-break: break-all;
        padding: 8px 0;
        border-bottom: 1px solid #EAEBF0;
        line-height: 22px;
        &:first-child {
            border-top: 1px solid #EAEBF0;
        }
        > p {
            display: -webkit-box;
            -webkit-box-orient: vertical;
            -webkit-line-clamp: 3;
            overflow: hidden;
        }
    }
}

.history-dialog{
    .bk-dialog {
        top: 50% !important;
        transform: var(--dialog-top-translateY) !important;
    }
}
</style>
