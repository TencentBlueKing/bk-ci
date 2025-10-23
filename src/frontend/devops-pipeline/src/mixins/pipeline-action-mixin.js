/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

import { statusAlias } from '@/utils/pipelineStatus'
import { convertMStoStringByRule, convertMStoString, convertTime, isShallowEqual, navConfirm } from '@/utils/util'
import { mapActions, mapGetters, mapMutations, mapState } from 'vuex'

import {
    ALL_PIPELINE_VIEW_ID,
    COLLECT_VIEW_ID,
    DELETED_VIEW_ID,
    ARCHIVE_VIEW_ID,
    MY_PIPELINE_VIEW_ID,
    RECENT_USED_VIEW_ID,
    UNCLASSIFIED_PIPELINE_VIEW_ID
} from '@/store/constants'
import {
    PROJECT_RESOURCE_ACTION,
    RESOURCE_ACTION,
    TEMPLATE_RESOURCE_ACTION,
    handleProjectNoPermission
} from '@/utils/permission'

import { ORDER_ENUM, PIPELINE_SORT_FILED, VERSION_STATUS_ENUM, pipelineTabIdMap } from '@/utils/pipelineConst'

export default {
    data () {
        return {
            pipelineMap: {}
        }
    },
    computed: {
        ...mapGetters('pipelines', [
            'groupMap'
        ]),
        ...mapState('pipelines', [
            'isManage'
        ]),
        currentGroup () {
            return this.groupMap?.[this.$route.params.viewId]
        },
        statusIconMap () {
            return {
                SUCCEED: 'check-circle-shape',
                FAILED: 'close-circle-shape',
                RUNNING: 'circle-2-1',
                PAUSE: 'play-circle-shape',
                SKIP: 'redo-arrow'
            }
        }
    },
    methods: {
        ...mapMutations('pipelines', [
            'updatePipelineActionState',
            'addCollectViewPipelineCount'
        ]),
        ...mapActions('pipelines', [
            'requestAllPipelinesListByFilter',
            'requestRecyclePipelineList',
            'requestArchivePipelineList',
            'requestToggleCollect',
            'deletePipeline',
            'copyPipeline',
            'restorePipeline',
            'deleteMigrateArchive',
            'lockPipeline'
        ]),
        async getPipelines (query = {}) {
            try {
                const { viewId, ...restQuery } = query
                const queryParams = {
                    sortType: localStorage.getItem('pipelineSortType') ?? PIPELINE_SORT_FILED.createTime,
                    collation: localStorage.getItem('pipelineSortCollation') ?? ORDER_ENUM.descending,
                    ...this.$route.query,
                    ...restQuery
                }
                const otherViews = viewId !== DELETED_VIEW_ID && viewId !== ARCHIVE_VIEW_ID

                const requestParams = {
                    projectId: this.$route.params.projectId,
                    ...queryParams,
                    viewId,
                    ...(otherViews ? { showDelete: true } : {})
                }

                if (viewId === DELETED_VIEW_ID) {
                    return this.requestRecyclePipelineList(requestParams)
                }

                if (otherViews) {
                    if (!isShallowEqual(queryParams, this.$route.query)) {
                        this.$router.replace({ query: queryParams })
                    }
                }
                const apiRequest = viewId === ARCHIVE_VIEW_ID
                    ? this.requestArchivePipelineList
                    : this.requestAllPipelinesListByFilter
                const { page, count, records } = await apiRequest(requestParams)
                const pipelineList = records.map((item, index) => {
                    const isArchive = viewId === ARCHIVE_VIEW_ID
                    const archiveQuery = isArchive ? { archiveFlag: true } : {}
                    const isDraft = item.latestVersionStatus === VERSION_STATUS_ENUM.COMMITTING

                    const archiveObj = {
                        ...item,
                        latestBuildStartDate: this.getLatestBuildFromNow(item.latestBuildStartTime),
                        updater: item.lastModifyUser,
                        updateDate: convertTime(item.updateTime),
                        duration: this.calcDuration(item),
                        latestBuildUserId: item.lastModifyUser,
                        onlyDraftVersion: isDraft,
                        historyRoute: {
                            name: isDraft ? 'pipelinesEdit' : 'pipelinesHistory',
                            params: {
                                projectId: item.projectId,
                                pipelineId: item.pipelineId,
                                type: item.onlyDraftVersion ? pipelineTabIdMap.pipeline : 'history'
                            },
                            query: archiveQuery
                        },
                        latestBuildRoute: {
                            name: 'pipelinesDetail',
                            params: {
                                type: 'executeDetail',
                                projectId: item.projectId,
                                pipelineId: item.pipelineId,
                                buildNo: item.latestBuildId
                            },
                            query: archiveQuery
                        }
                    }

                    if (otherViews) {
                        return Object.assign(archiveObj, {
                            latestBuildUserId: item.latestBuildUserId,
                            progress: this.calcProgress(item),
                            pipelineActions: this.getPipelineActions(item, index),
                            disabled: this.isDisabledPipeline(item),
                            tooltips: this.disabledTips(item),
                            latestBuildStageStatus: this.getLatestBuildStageStatus(item),
                            released: item.latestVersionStatus === VERSION_STATUS_ENUM.RELEASED,
                            onlyBranchVersion: item.latestVersionStatus === VERSION_STATUS_ENUM.BRANCH
                        })
                    } else {
                        return archiveObj
                    }
                })

                if (otherViews) {
                    this.pipelineMap = pipelineList.reduce((acc, item) => {
                        acc[item.pipelineId] = item
                        return acc
                    }, {})
                }
                return {
                    page,
                    count,
                    records: pipelineList
                }
            } catch (e) {
                if (e.code === 403) {
                    handleProjectNoPermission({
                        projectId: this.$route.params.projectId,
                        resourceCode: this.$route.params.projectId,
                        action: PROJECT_RESOURCE_ACTION.MANAGE
                    })
                } else {
                    this.$showTips({
                        message: e.message || e,
                        theme: 'error'
                    })
                }
                return false
            }
        },
        getLatestBuildFromNow (latestBuildStartTime) {
            return latestBuildStartTime ? convertTime(latestBuildStartTime) : '--'
        },
        calcDuration ({ latestBuildEndTime, latestBuildStartTime, latestBuildNum }) {
            if (latestBuildNum) {
                const duration = convertMStoStringByRule(latestBuildEndTime - latestBuildStartTime)
                return this.$t('history.tableMap.totalTime', [duration])
            }
            return '--'
        },
        isDisabledPipeline (pipeline) {
            return pipeline.lock || !pipeline.canManualStartup
        },
        disabledTips (pipeline) {
            if (!this.isDisabledPipeline(pipeline)) return { disabled: true }
            return this.$t(pipeline.lock ? 'pipelineLockTips' : 'pipelineManualDisable')
        },
        calcProgress ({ latestBuildStatus, lastBuildFinishCount = 0, lastBuildTotalCount = 1, currentTimestamp, latestBuildStartTime }) {
            if (latestBuildStatus === statusAlias.RUNNING) {
                return `${this.$t('execedTimes')}${convertMStoStringByRule(currentTimestamp - latestBuildStartTime)}(${Math.floor((lastBuildFinishCount / lastBuildTotalCount) * 100)}%)`
            }
            return ''
        },
        getPipelineActions (pipeline) {
            const isShowRemovedAction = ![
                ALL_PIPELINE_VIEW_ID,
                COLLECT_VIEW_ID,
                MY_PIPELINE_VIEW_ID,
                UNCLASSIFIED_PIPELINE_VIEW_ID,
                RECENT_USED_VIEW_ID
            ].includes(this.$route.params.viewId)
            const removedActionAuthMap = this.currentGroup?.projected
                ? {
                    hasPermission: this.isManage,
                    disablePermissionApi: true,
                    permissionData: {
                        projectId: pipeline.projectId,
                        resourceType: 'project',
                        resourceCode: pipeline.projectId,
                        action: PROJECT_RESOURCE_ACTION.MANAGE
                    }
                }
                : {}
            const isDynamicGroup = this.currentGroup?.viewType === 1
            const isBuilding = pipeline.runningBuildCount > 0
            
            const isDraft = pipeline.latestVersionStatus === VERSION_STATUS_ENUM.COMMITTING
            let archiveTooltip
            if (isBuilding) {
                archiveTooltip = this.$t('archive.unableToFile')
            } else if (isDraft) {
                archiveTooltip = this.$t('archive.onlyDraftVersion')
            } else if (pipeline.archivingFlag) {
                archiveTooltip = this.$t('archive.archiving')
            } else {
                archiveTooltip = false
            }

            return [
                {
                    text: this.$t(pipeline.lock ? 'enable' : 'disable'),
                    handler: this.lockPipelineHandler,
                    hasPermission: pipeline.permissions.canEdit,
                    disablePermissionApi: true,
                    permissionData: {
                        projectId: pipeline.projectId,
                        resourceType: 'pipeline',
                        resourceCode: pipeline.pipelineId,
                        action: RESOURCE_ACTION.EDIT
                    }
                },
                {
                    text: this.$t('addTo'),
                    handler: this.addToHandler
                },
                ...(pipeline.templateId
                    ? [{
                        text: this.$t('copyAsTemplateInstance'),
                        handler: () => this.copyAsTemplateInstance(pipeline),
                        hasPermission: pipeline.permissions.canManage,
                        disablePermissionApi: true,
                        permissionData: {
                            projectId: pipeline.projectId,
                            resourceType: 'project',
                            resourceCode: pipeline.projectId,
                            action: RESOURCE_ACTION.CREATE
                        }
                    }]
                    : []),
                {
                    text: this.$t('newlist.copyAs'),
                    handler: this.copyAs,
                    hasPermission: pipeline.permissions.canEdit,
                    disablePermissionApi: true,
                    permissionData: {
                        projectId: pipeline.projectId,
                        resourceType: 'pipeline',
                        resourceCode: pipeline.pipelineId,
                        action: RESOURCE_ACTION.EDIT
                    }
                },
                {
                    text: this.$t('newlist.saveAsTemp'),
                    handler: this.saveAsTempHandler,
                    hasPermission: this.isManage,
                    disablePermissionApi: true,
                    permissionData: {
                        projectId: pipeline.projectId,
                        resourceType: 'project',
                        resourceCode: pipeline.projectId,
                        action: TEMPLATE_RESOURCE_ACTION.CREATE
                    }
                },
                ...(pipeline.instanceFromTemplate
                    ? [{
                        text: this.$t('newlist.jumpToTemp'),
                        handler: this.jumpToTemplate
                    }]
                    : []),
                ...(isShowRemovedAction
                    ? [{
                        text: this.$t('removeFrom'),
                        disable: isDynamicGroup,
                        tooltips: isDynamicGroup ? this.$t('dynamicGroupRemoveDisableTips') : false,
                        handler: this.removeHandler,
                        ...removedActionAuthMap
                    }]
                    : []),
                {
                    text: this.$t('archive.archive'),
                    disable: isBuilding || isDraft || pipeline.archivingFlag,
                    tooltips: archiveTooltip,
                    handler: this.archiveHandler,
                    hasPermission: pipeline.permissions.canArchive,
                    disablePermissionApi: true,
                    permissionData: {
                        projectId: pipeline.projectId,
                        resourceType: 'pipeline',
                        resourceCode: pipeline.pipelineId,
                        action: RESOURCE_ACTION.ARCHIVED
                    }
                },
                {
                    text: this.$t('delete'),
                    handler: this.deleteHandler,
                    hasPermission: pipeline.permissions.canDelete,
                    disablePermissionApi: true,
                    permissionData: {
                        projectId: pipeline.projectId,
                        resourceType: 'pipeline',
                        resourceCode: pipeline.pipelineId,
                        action: RESOURCE_ACTION.DELETE
                    }
                }
            ]
        },
        async collectHandler (pipeline) {
            const isCollect = !pipeline.hasCollect
            try {
                await this.requestToggleCollect({
                    ...pipeline,
                    isCollect
                })

                pipeline.hasCollect = !pipeline.hasCollect
                this.pipelineMap[pipeline.pipelineId].hasCollect = isCollect
                this.addCollectViewPipelineCount(isCollect ? 1 : -1)

                this.$showTips({
                    message: isCollect ? this.$t('collectSuc') : this.$t('uncollectSuc'),
                    theme: 'success'
                })
            } catch (err) {
                this.$showTips({
                    message: err.message || err,
                    theme: 'error'
                })
            }
        },
        lockPipelineHandler (pipeline) {
            this.updatePipelineActionState({
                isDisableDialogShow: true,
                activePipeline: pipeline
            })
        },
        addToHandler (pipeline) {
            this.updatePipelineActionState({
                addToDialogShow: true,
                activePipeline: pipeline
            })
        },
        copyAs (pipeline) {
            this.updatePipelineActionState({
                isCopyDialogShow: true,
                activePipeline: pipeline
            })
        },
        saveAsTempHandler (pipeline) {
            this.updatePipelineActionState({
                isSaveAsTemplateShow: true,
                activePipeline: pipeline
            })
        },
        removeHandler (pipeline) {
            this.updatePipelineActionState({
                confirmType: 'remove',
                isConfirmShow: true,
                activePipelineList: [pipeline]
            })
        },
        archiveHandler (pipeline) {
            this.updatePipelineActionState({
                isArchiveDialogShow: true,
                activePipelineList: [pipeline]
            })
        },
        deleteHandler (pipeline) {
            this.updatePipelineActionState({
                confirmType: 'delete',
                isConfirmShow: true,
                activePipelineList: [pipeline]
            })
        },
        closeDisableDialog () {
            this.updatePipelineActionState({
                isDisableDialogShow: false,
                activePipeline: null
            })
        },
        closeCopyDialog () {
            this.updatePipelineActionState({
                isCopyDialogShow: false,
                activePipeline: null
            })
        },
        closeAddToDialog () {
            this.updatePipelineActionState({
                addToDialogShow: false,
                activePipeline: null
            })
        },
        closeRemoveConfirmDialog () {
            this.updatePipelineActionState({
                isConfirmShow: false,
                confirmType: '',
                activePipelineList: []
            })
        },
        closeSaveAsDialog () {
            this.updatePipelineActionState({
                isSaveAsTemplateShow: false,
                activePipeline: null
            })
        },
        closeArchiveDialog () {
            this.updatePipelineActionState({
                isArchiveDialogShow: false,
                activePipelineList: []
            })
        },

        openDeleteArchivedDialog (pipeline) {
            this.updatePipelineActionState({
                isShowDeleteArchivedDialog: true,
                activePipelineList: [pipeline]
            })
        },

        closeDeleteArchiveDialog () {
            this.updatePipelineActionState({
                isShowDeleteArchivedDialog: false,
                activePipelineList: []
            })
        },

        jumpToTemplate ({ templateId }) {
            this.$router.push({
                name: 'templateEdit',
                params: {
                    templateId
                }
            })
        },
        execPipeline ({ projectId, pipelineId, disabled, released, onlyBranchVersion, pipelineVersion }) {
            if (disabled || !(released || onlyBranchVersion)) return
            this.$router.push({
                name: 'executePreview',
                params: {
                    projectId,
                    pipelineId,
                    version: pipelineVersion
                }
            })
        },
        /**
         *  删除流水线
         */
        async delete ({ pipelineId, pipelineName, projectId }) {
            try {
                await this.deletePipeline({
                    projectId,
                    pipelineId
                })
                this.$showTips({
                    message: this.$t('deleteSuc'),
                    theme: 'success'
                })
                return true
            } catch (err) {
                this.handleError(
                    err,
                    {
                        projectId: projectId,
                        resourceCode: pipelineId,
                        action: this.$permissionResourceAction.DELETE
                    }
                )
            }
        },
        /**
         *  复制流水线弹窗的确认回调函数
         */
        async copy (tempPipeline, { projectId, pipelineId, group, pipelineName }) {
            try {
                if (!tempPipeline.name) {
                    throw new Error(this.$t('subpage.nameNullTips'))
                }
                await this.copyPipeline({
                    projectId,
                    pipelineId,
                    args: {
                        ...tempPipeline,
                        group,
                        hasCollect: false
                    }
                })

                this.$showTips({
                    message: this.$t('copySuc'),
                    theme: 'success'
                })
                return true
            } catch (err) {
                this.handleError(
                    err,
                    {
                        projectId: projectId,
                        resourceCode: projectId,
                        action: this.$permissionResourceAction.CREATE
                    }
                )
            }
        },
        /** *
         * 恢复流水线
         */
        async restore ({ projectId, pipelineId, pipelineName }) {
            const res = await navConfirm({
                content: this.$t('restorePipelineConfirm', [pipelineName])
            })
            if (!res) return
            try {
                await this.restorePipeline({
                    projectId,
                    pipelineId
                })
                this.$showTips({
                    message: this.$t('restore.restoreSuc'),
                    theme: 'success'
                })
                return true
            } catch (err) {
                if (err.code === 403) {
                    handleProjectNoPermission({
                        projectId: projectId,
                        resourceCode: projectId,
                        action: PROJECT_RESOURCE_ACTION.MANAGE
                    })
                } else {
                    this.$showTips({
                        message: err.message || err,
                        theme: 'error'
                    })
                }
                return false
            }
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
        getLatestBuildStageStatus (item) {
            return item.latestBuildStageStatus ? item.latestBuildStageStatus.slice(1).map((stage) => ({
                ...stage,
                tooltip: this.getStageTooltip(stage),
                icon: this.statusIconMap[stage.status] || 'circle',
                statusCls: stage.status
            })) : null
        },
        updatePipelineStatus (data, isFirst = false) {
            Object.keys(data).forEach(pipelineId => {
                const item = data[pipelineId]
                if (this.pipelineMap[pipelineId]) {
                    // 单独修改当前任务是否在执行的状态, 拼接右下角按钮
                    Object.assign(this.pipelineMap[pipelineId], {
                        ...item,
                        latestBuildStartDate: this.getLatestBuildFromNow(item.latestBuildStartTime),
                        duration: this.calcDuration(item),
                        progress: this.calcProgress(item),
                        latestBuildStageStatus: this.getLatestBuildStageStatus(item)
                    })
                }
            })
        },
        copyAsTemplateInstance (pipeline) {
            const pipelineName = (pipeline.pipelineName + '_copy').substring(0, 128)
            const { templateId, pipelineId, projectId, version } = pipeline
            window.top.location.href = `${location.origin}/console/pipeline/${projectId}/template/${templateId}/createInstance/${version}/${pipelineName}?pipelineId=${pipelineId}`
        }
    }
}
