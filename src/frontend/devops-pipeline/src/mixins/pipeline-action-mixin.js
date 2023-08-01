/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
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
import triggerType from '@/utils/triggerType'
import { convertMStoStringByRule, convertTime, navConfirm } from '@/utils/util'
import { mapActions, mapGetters, mapMutations } from 'vuex'

import {
    ALL_PIPELINE_VIEW_ID,
    COLLECT_VIEW_ID,
    DELETED_VIEW_ID,
    MY_PIPELINE_VIEW_ID,
    RECENT_USED_VIEW_ID,
    UNCLASSIFIED_PIPELINE_VIEW_ID
} from '@/store/constants'

import { ORDER_ENUM, PIPELINE_SORT_FILED } from '@/utils/pipelineConst'

export default {
    data () {
        return {
            pipelineMap: {},
            hasTemplatePermission: false
        }
    },
    computed: {
        ...mapGetters('pipelines', [
            'groupMap'
        ]),
        currentGroup () {
            return this.groupMap?.[this.$route.params.viewId]
        }
    },
    created () {
        this.checkHasTemplatePermission()
    },
    methods: {
        ...mapMutations('pipelines', [
            'updatePipelineActionState',
            'addCollectViewPipelineCount'
        ]),
        ...mapActions('pipelines', [
            'requestAllPipelinesListByFilter',
            'requestToggleCollect',
            'requestTemplatePermission',
            'requestRecyclePipelineList',
            'requestToggleCollect',
            'deletePipeline',
            'copyPipeline',
            'restorePipeline'
        ]),
        async checkHasTemplatePermission () {
            this.hasTemplatePermission = await this.requestTemplatePermission(this.$route.params.projectId)
            this.$nextTick(() => {
                console.log(this.hasTemplatePermission, 'this.hasTemplatePermission')
                Object.keys(this.pipelineMap).forEach(pipelineId => {
                    this.pipelineMap[pipelineId].pipelineActions = this.getPipelineActions(this.pipelineMap[pipelineId])
                })
            })
        },
        async getPipelines (query = {}) {
            try {
                const { viewId, ...restQuery } = query
                const queryParams = {
                    sortType: localStorage.getItem('pipelineSortType') ?? PIPELINE_SORT_FILED.createTime,
                    collation: localStorage.getItem('pipelineSortCollation') ?? ORDER_ENUM.descending,
                    ...this.$route.query,
                    ...restQuery
                }
                if (viewId === DELETED_VIEW_ID) {
                    return this.requestRecyclePipelineList({
                        projectId: this.$route.params.projectId,
                        ...queryParams,
                        viewId
                    })
                } else {
                    this.$router.push({
                        ...this.$route,
                        query: queryParams
                    })
                    const { page, count, records } = await this.requestAllPipelinesListByFilter({
                        showDelete: true,
                        projectId: this.$route.params.projectId,
                        ...queryParams,
                        viewId
                    })
                    const pipelineList = records.map((item, index) => Object.assign(item, {
                        latestBuildStartDate: this.getLatestBuildFromNow(item.latestBuildStartTime),
                        updateDate: convertTime(item.updateTime),
                        duration: this.calcDuration(item),
                        progress: this.calcProgress(item),
                        pipelineActions: this.getPipelineActions(item, index),
                        trigger: triggerType[item.trigger],
                        disabled: this.isDisabledPipeline(item),
                        tooltips: this.disabledTips(item),
                        historyRoute: {
                            name: 'pipelinesHistory',
                            params: {
                                projectId: item.projectId,
                                pipelineId: item.pipelineId
                            }
                        },
                        latestBuildRoute: {
                            name: 'pipelinesDetail',
                            params: {
                                type: 'executeDetail',
                                projectId: item.projectId,
                                pipelineId: item.pipelineId,
                                buildNo: item.latestBuildId
                            }
                        }
                    }))
                    this.pipelineMap = pipelineList.reduce((acc, item) => {
                        return {
                            ...acc,
                            [item.pipelineId]: item
                        }
                    }, {})
                    return {
                        page,
                        count,
                        records: pipelineList
                    }
                }
            } catch (e) {
                this.$showTips({
                    message: e.message || e,
                    theme: 'error'
                })
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
            const isDynamicGroup = this.currentGroup?.viewType === 1
            return [
                {
                    text: this.$t('addTo'),
                    handler: this.addToHandler
                },
                {
                    text: this.$t('newlist.copyAs'),
                    handler: this.copyAs
                },
                {
                    text: this.$t('newlist.saveAsTemp'),
                    disable: !this.hasTemplatePermission,
                    handler: this.saveAsTempHandler
                },
                ...(pipeline.isInstanceTemplate
                    ? [{
                        text: this.$t('newlist.jumpToTemp'),
                        handler: this.jumpToTemplate,
                        isJumpToTem: true
                    }]
                    : []),
                ...(isShowRemovedAction
                    ? [{
                        text: this.$t('removeFrom'),
                        disable: isDynamicGroup,
                        tooltips: isDynamicGroup ? this.$t('dynamicGroupRemoveDisableTips') : false,
                        handler: this.removeHandler
                    }]
                    : []),
                {
                    text: this.$t('delete'),
                    handler: this.deleteHandler
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
        deleteHandler (pipeline) {
            this.updatePipelineActionState({
                confirmType: 'delete',
                isConfirmShow: true,
                activePipelineList: [pipeline]
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

        jumpToTemplate ({ templateId }) {
            this.$router.push({
                name: 'templateEdit',
                params: {
                    templateId
                }
            })
        },
        execPipeline ({ pipelineId, disabled }) {
            if (disabled) return
            this.$router.push({
                name: 'pipelinesPreview',
                params: {
                    projectId: this.$route.params.projectId,
                    pipelineId
                }
            })
        },
        /**
         *  处理收藏和取消收藏
         */
        async togglePipelineCollect (pipelineId, isCollect = false) {
            let message = isCollect ? this.$t('collectSuc') : this.$t('uncollectSuc')
            let theme = 'success'
            try {
                const { projectId } = this.$route.params
                await this.requestToggleCollect({
                    projectId,
                    pipelineId,
                    isCollect
                })
                return true
            } catch (err) {
                message = err.message || err
                theme = 'error'
            } finally {
                this.$showTips({
                    message,
                    theme
                })
            }
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
                this.handleError(err, [{
                    actionId: this.$permissionActionMap.delete,
                    resourceId: this.$permissionResourceMap.pipeline,
                    instanceId: [{
                        id: pipelineId,
                        name: pipelineName
                    }],
                    projectId
                }])
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
                this.handleError(err, [{
                    actionId: this.$permissionActionMap.create,
                    resourceId: this.$permissionResourceMap.pipeline,
                    instanceId: [{
                        id: pipelineId,
                        name: pipelineName
                    }]
                }, {
                    actionId: this.$permissionActionMap.edit,
                    resourceId: this.$permissionResourceMap.pipeline,
                    instanceId: [{
                        id: pipelineId,
                        name: pipelineName
                    }],
                    projectId
                }])
            }
        },
        /** *
         * 恢复流水线
         */
        async restore ({ projectId, pipelineId, pipelineName }) {
            let message = this.$t('restore.restoreSuc')
            let theme = 'success'
            await navConfirm({
                content: this.$t('restorePipelineConfirm', [pipelineName])
            })
            try {
                await this.restorePipeline({
                    projectId,
                    pipelineId
                })
                return true
            } catch (err) {
                message = err.message || err
                theme = 'error'
                return false
            } finally {
                this.$showTips({
                    message,
                    theme
                })
            }
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
                        trigger: triggerType[item.trigger]
                    })
                }
            })
        },
        applyPermission ({ pipelineName, pipelineId }) {
            this.setPermissionConfig(
                this.$permissionResourceMap.pipeline,
                this.$permissionActionMap.view,
                [{
                    id: pipelineId,
                    name: pipelineName
                }],
                this.$route.params.projectId
            )
        }
    }
}
