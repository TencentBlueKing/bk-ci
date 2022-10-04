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

import { mapActions, mapMutations } from 'vuex'
import moment from 'moment'
import { statusAlias } from '@/utils/pipelineStatus'
import triggerType from '@/utils/triggerType'
import { navConfirm, convertMStoStringByRule } from '@/utils/util'
import { bus } from '@/utils/bus'

import {
    ALL_PIPELINE_VIEW_ID,
    COLLECT_VIEW_ID,
    MY_PIPELINE_VIEW_ID,
    DELETED_VIEW_ID,
    UNCLASSIFIED_PIPELINE_VIEW_ID
} from '@/store/constants'

export default {
    data () {
        return {
            pipelineMap: {}
        }
    },
    computed: {
        isDeleteView () {
            console.log(this.$route.params.viewId, DELETED_VIEW_ID)
            return this.$route.params.viewId === DELETED_VIEW_ID
        }
    },
    created () {
        moment.locale(this.$i18n.locale)
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
            'deletePipeline',
            'requestTemplatePermission',
            'requestRecyclePipelineList',
            'requestToggleCollect',
            'deleteHandler',
            'copyPipeline',
            'restorePipeline',
            'requestTemplatePermission'
        ]),
        async checkHasTemplatePermission () {
            this.hasTemplatePermission = await this.requestTemplatePermission(this.$route.params.projectId)
        },
        async getPipelines (query = {}) {
            try {
                const { viewId, ...restQuery } = query
                if (viewId === DELETED_VIEW_ID) {
                    return await this.requestRecyclePipelineList({
                        projectId: this.$route.params.projectId,
                        ...query
                    })
                } else {
                    this.$router.push({
                        ...this.$route,
                        query: {
                            ...this.$route.query,
                            ...restQuery
                        }
                    })
                    const { page, count, records } = await this.requestAllPipelinesListByFilter({
                        projectId: this.$route.params.projectId,
                        ...query
                    })
                    const pipelineList = records.map((item, index) => Object.assign(item, {
                        latestBuildStartDate: this.getLatestBuildFromNow(item.latestBuildStartTime),
                        duration: this.calcDuration(item),
                        progress: this.calcProgress(item),
                        pipelineActions: this.getPipelineActions(item, index),
                        trigger: triggerType[item.trigger]
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
            return latestBuildStartTime ? moment(latestBuildStartTime).fromNow() : '--'
        },
        calcDuration ({ latestBuildEndTime, latestBuildStartTime }) {
            const duration = convertMStoStringByRule(latestBuildEndTime - latestBuildStartTime)
            return `${this.$t('history.tableMap.totalTime')}${duration}`
        },
        calcProgress ({ latestBuildStatus, lastBuildFinishCount, lastBuildTotalCount, currentTimestamp, latestBuildStartTime }) {
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
                UNCLASSIFIED_PIPELINE_VIEW_ID
            ].includes(this.$route.params.viewId)

            return [
                {
                    text: (pipeline.hasCollect ? this.$t('uncollect') : this.$t('collect')),
                    handler: this.collectHandler
                },
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
                this.pipelineMap[pipeline.pipelineId].pipelineActions = this.getPipelineActions(this.pipelineMap[pipeline.pipelineId])
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
                isCopyDialogShow: true,
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
                confirmType: '',
                isConfirmShow: false,
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
        execPipeline ({ pipelineId }) {
            this.$router.push({
                name: 'pipelinesPreview',
                params: {
                    projectId: this.$route.params.projectId,
                    pipelineId
                }
            })
        },
        /**
             *  跳转执行历史
             */
        goHistory (pipelineId) {
            this.$router.push({
                name: 'pipelinesHistory',
                params: {
                    projectId: this.$route.params.projectId,
                    pipelineId: pipelineId
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
                await this.deleteHandler({
                    projectId,
                    pipelineId
                })
                this.$showTips({
                    message: this.$t('deleteSuc'),
                    theme: 'success'
                })
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
            const {
                statusMap
            } = this
            const knownErrorList = JSON.parse(localStorage.getItem('pipelineKnowError')) || {}
            Object.keys(data).forEach(pipelineId => {
                const item = data[pipelineId]
                if (item) {
                    const status = statusMap[item.latestBuildStatus]
                    let feConfig = {
                        isRunning: false,
                        status: status || 'not_built'
                    }
                    // 单独修改当前任务是否在执行的状态, 拼接右下角按钮
                    switch (feConfig.status) {
                        case 'error': {
                            const isKnowErrorPipeline = !!knownErrorList[`${this.projectId}_${pipelineId}_${item.latestBuildId}`]
                            feConfig = {
                                ...feConfig,
                                customBtns: isKnowErrorPipeline
                                    ? []
                                    : [{
                                        icon: 'check-1',
                                        text: this.$t('newlist.known'),
                                        handler: 'error-noticed'
                                    }],
                                isRunning: !isKnowErrorPipeline,
                                status: isKnowErrorPipeline ? 'known_error' : 'error'
                            }
                            break
                        }
                        case 'cancel': {
                            const isKnowCancelPipeline = !!knownErrorList[`${this.projectId}_${pipelineId}_${item.latestBuildId}`]
                            feConfig = {
                                ...feConfig,
                                customBtns: isKnowCancelPipeline
                                    ? []
                                    : [{
                                        icon: 'check-1',
                                        text: this.$t('newlist.known'),
                                        handler: 'error-noticed'
                                    }],
                                isRunning: !isKnowCancelPipeline,
                                status: isKnowCancelPipeline ? 'known_cancel' : 'cancel'
                            }
                            break
                        }
                        case 'running':
                            feConfig = {
                                ...feConfig,
                                isRunning: true,
                                customBtns: [{
                                    text: this.$t('terminate'),
                                    handler: 'terminate-pipeline'
                                }]
                            }
                            break
                        case 'warning':
                            feConfig = {
                                ...feConfig,
                                customBtns: [
                                    {
                                        text: this.$t('resume'),
                                        handler: 'resume-pipeline'
                                    },
                                    {
                                        text: this.$t('terminate'),
                                        handler: 'terminate-pipeline'
                                    }
                                ],
                                isRunning: true
                            }
                            break
                        default:
                            feConfig.isRunning = false
                    }

                    feConfig = {
                        ...feConfig,
                        footer: [
                            {
                                upperText: item.taskCount,
                                lowerText: this.$t('newlist.totalAtomNums'),
                                handler: this.goEditPipeline
                            },
                            {
                                upperText: item.buildCount,
                                lowerText: this.$t('newlist.execTimes'),
                                handler: this.goHistory
                            }
                        ],
                        runningInfo: {
                            time: convertMStoStringByRule(status === 'error' ? (item.latestBuildEndTime - item.latestBuildStartTime) : (item.currentTimestamp - item.latestBuildStartTime)),
                            percentage: this.calcPercentage(item),
                            log: item.latestBuildTaskName,
                            buildCount: item.runningBuildCount || 0
                        },
                        projectId: this.projectId,
                        pipelineId,
                        buildId: item.latestBuildId || 0
                    }
                    if (!(this.pipelineFeConfMap[pipelineId] && this.pipelineFeConfMap[pipelineId].extMenu && this.pipelineFeConfMap[pipelineId].extMenu.length)) {
                        feConfig.extMenu = [
                            {
                                text: this.$t('edit'),
                                handler: this.goEditPipeline
                            },
                            {
                                text: (item.hasCollect ? this.$t('uncollect') : this.$t('collect')),
                                handler: this.togglePipelineCollect
                            },
                            {
                                text: this.$t('newlist.copyAs'),
                                handler: this.copyPipeline
                            },
                            {
                                text: this.$t('newlist.saveAsTemp'),
                                disable: !this.hasTemplatePermission,
                                handler: this.copyAsTemplate
                            },
                            {
                                text: this.$t('delete'),
                                handler: this.deletePipeline
                            }
                        ]
                    }

                    if (this.pipelineFeConfMap[pipelineId] && !this.pipelineFeConfMap[pipelineId].extMenu.length && this.pipelineFeConfMap[pipelineId].isInstanceTemplate) {
                        feConfig.extMenu.splice((feConfig.extMenu.length - 1), 0, {
                            text: this.$t('newlist.jumpToTemp'),
                            handler: this.jumpToTemplate,
                            isJumpToTem: true
                        })
                    }

                    this.pipelineFeConfMap[pipelineId] = {
                        ...(this.pipelineFeConfMap[pipelineId] || {}),
                        ...feConfig
                    }
                }
            })
        },
        applyPermission ({ pipelineName, pipelineId }) {
            bus.$emit(
                'set-permission',
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
