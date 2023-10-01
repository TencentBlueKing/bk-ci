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

import { mapActions, mapGetters, mapState } from 'vuex'
import { AUTH_URL_PREFIX, PROCESS_API_URL_PREFIX } from '../store/constants'

export default {
    computed: {
        ...mapGetters({
            allPipelineList: 'pipelines/getAllPipelineList',
            pipelineList: 'pipelines/getPipelineList',
            tagGroupList: 'pipelines/getTagGroupList',
            isEditing: 'atom/isEditing',
            checkPipelineInvalid: 'atom/checkPipelineInvalid'
        }),
        ...mapState([
            'curProject'
        ]),
        ...mapState('pipelines', [
            'pipelineInfo',
            'executeStatus'
        ]),
        ...mapState('atom', [
            'pipeline',
            'pipelineSetting'
        ]),
        isTemplatePipeline () {
            return this.pipelineInfo?.instanceFromTemplate ?? false
        },
        pipelineVersion () {
            return this.pipelineInfo?.version ?? ''
        }
    },
    methods: {
        ...mapActions('pipelines', [
            'requestToggleCollect',
            'requestTerminatePipeline',
            'requestRetryPipeline'
        ]),
        ...mapActions('atom', [
            'setPipelineEditing',
            'setPipeline',
            'updateContainer'
        ]),
        /**
             *  终止任务
             */
        async terminatePipeline (pipelineId) {
            const { projectId } = this.$route.params
            const target = this.pipelineList.find(item => item.pipelineId === pipelineId)
            const { feConfig } = target

            if (!feConfig.buttonAllow.terminatePipeline) return

            feConfig.buttonAllow.terminatePipeline = false

            try {
                await this.requestTerminatePipeline({
                    projectId,
                    pipelineId,
                    buildId: feConfig.buildId || target.latestBuildId
                })
                this.updatePipelineValueById(pipelineId, {
                    isRunning: false,
                    status: 'known_error'
                })
            } catch (err) {
                this.handleError(err, [{
                    actionId: this.$permissionActionMap.execute,
                    resourceId: this.$permissionResourceMap.pipeline,
                    instanceId: [{
                        id: target.pipelineId,
                        name: target.pipelineName
                    }],
                    projectId
                }])
            } finally {
                feConfig.buttonAllow.terminatePipeline = true
            }
        },
        async retry (buildId, goDetail = false) {
            let message, theme
            const { projectId, pipelineId } = this.$route.params
            try {
                // 请求执行构建
                const res = await this.requestRetryPipeline({
                    ...this.$route.params,
                    buildId
                })

                if (res && res.id) {
                    message = this.$t('subpage.rebuildSuc')
                    theme = 'success'
                    if (goDetail) {
                        this.$router.replace({
                            name: 'pipelinesDetail',
                            params: {
                                projectId,
                                pipelineId,
                                type: 'executeDetail',
                                buildNo: res.id
                            }
                        })
                    }
                    this.$emit('update-table')
                } else {
                    message = this.$t('subpage.rebuildFail')
                    theme = 'error'
                }
            } catch (err) {
                this.handleError(err, [{
                    actionId: this.$permissionActionMap.execute,
                    resourceId: this.$permissionResourceMap.pipeline,
                    instanceId: [{
                        id: pipelineId,
                        name: this.pipelineInfo?.pipelineName ?? '--'
                    }],
                    projectId
                }])
            } finally {
                message && this.$showTips({
                    message,
                    theme
                })
            }
        },
        /**
         *  终止流水线
         */
        async stopExecute (buildId) {
            let message, theme

            try {
                const res = await this.requestTerminatePipeline({
                    ...this.$route.params,
                    buildId
                })

                if (res) {
                    message = this.$t('subpage.stopSuc')
                    theme = 'success'
                } else {
                    message = this.$t('subpage.stopFail')
                    theme = 'error'
                }
            } catch (err) {
                this.handleError(err, [{
                    actionId: this.$permissionActionMap.execute,
                    resourceId: this.$permissionResourceMap.pipeline,
                    instanceId: [{
                        id: this.pipelineInfo.pipelineId,
                        name: this.pipelineInfo?.pipelineName ?? '--'
                    }],
                    projectId: this.$route.params.projectId
                }])
            } finally {
                message && this.$showTips({
                    message,
                    theme
                })
            }
        },
        importPipelineAndSetting (body) {
            const { projectId } = this.$route.params

            // 请求执行构建
            return this.$ajax.post(`${PROCESS_API_URL_PREFIX}/user/pipelines/projects/${projectId}/upload`, body)
        },

        async saveAsPipelineTemplate (projectId, pipelineId, templateName, isCopySetting = false) {
            try {
                if (!templateName) {
                    throw new Error(this.$t('newlist.tempNameNullTips'))
                }
                await this.$ajax.post(`${PROCESS_API_URL_PREFIX}/user/templates/projects/${projectId}/templates/saveAsTemplate`, {
                    pipelineId,
                    templateName,
                    isCopySetting
                })
                this.$showTips({
                    message: this.$t('newlist.saveAsTempSuc'),
                    theme: 'success'
                })
            } catch (e) {
                this.handleError(e, [{
                    actionId: this.$permissionActionMap.edit,
                    resourceId: this.$permissionResourceMap.pipeline,
                    instanceId: [{
                        id: pipelineId,
                        name: this.pipeline ? this.pipeline.name : ''
                    }],
                    projectId
                }])
            }
        },
        changeProject () {
            this.$toggleProjectMenu(true)
        },

        async toApplyPermission (actionId, pipeline) {
            try {
                const { projectId } = this.$route.params
                const redirectUrl = await this.$ajax.post(`${AUTH_URL_PREFIX}/user/auth/permissionUrl`, [{
                    actionId,
                    resourceId: this.$permissionResourceMap.pipeline,
                    instanceId: [{
                        id: projectId,
                        type: this.$permissionResourceTypeMap.PROJECT
                    }, {
                        type: this.$permissionResourceTypeMap.PIPELINE_DEFAULT,
                        ...pipeline
                    }]
                }])
                window.open(redirectUrl, '_blank')
                this.$bkInfo({
                    title: this.$t('permissionRefreshtitle'),
                    subTitle: this.$t('permissionRefreshSubtitle'),
                    okText: this.$t('permissionRefreshOkText'),
                    cancelText: this.$t('close'),
                    confirmFn: () => {
                        location.reload()
                    }
                })
            } catch (e) {
                console.error(e)
            }
        }
    }
}
