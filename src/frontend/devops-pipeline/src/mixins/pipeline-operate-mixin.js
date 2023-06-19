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

import {
    HttpError,
    convertTime
} from '@/utils/util'
import { mapActions, mapGetters, mapState } from 'vuex'
import { AUTH_URL_PREFIX, PROCESS_API_URL_PREFIX } from '../store/constants'

export default {
    computed: {
        ...mapGetters({
            allPipelineList: 'pipelines/getAllPipelineList',
            pipelineList: 'pipelines/getPipelineList',
            tagGroupList: 'pipelines/getTagGroupList',
            curPipeline: 'pipelines/getCurPipeline',
            isEditing: 'atom/isEditing',
            checkPipelineInvalid: 'atom/checkPipelineInvalid'
        }),
        ...mapState([
            'curProject'
        ]),
        ...mapState('pipelines', [
            'pipelineSetting'
        ]),
        ...mapState('atom', [
            'pipeline',
            'executeStatus',
            'saveStatus'
        ]),
        isTemplatePipeline () {
            return this.curPipeline && this.curPipeline.instanceFromTemplate
        }
    },
    methods: {
        ...mapActions('pipelines', {
            requestExecPipeline: 'requestExecPipeline',
            requestToggleCollect: 'requestToggleCollect',
            updatePipelineSetting: 'updatePipelineSetting',
            requestTerminatePipeline: 'requestTerminatePipeline',
            requestRetryPipeline: 'requestRetryPipeline',
            searchPipelineList: 'searchPipelineList',
            requestPipelineDetail: 'requestPipelineDetail',
            setPipelineSetting: 'setPipelineSetting'
        }),
        ...mapActions('atom', [
            'setPipelineEditing',
            'setExecuteStatus',
            'setSaveStatus',
            'setPipeline',
            'updateContainer'
        ]),
        async fetchPipelineList (searchName) {
            try {
                const { projectId, pipelineId } = this.$route.params
                const [list, curPipeline] = await Promise.all([
                    this.searchPipelineList({
                        projectId,
                        searchName
                    }),
                    this.updateCurPipeline({
                        projectId,
                        pipelineId
                    })
                ])

                this.setBreadCrumbPipelineList(list, curPipeline)
            } catch (err) {
                console.log(err)
                this.$showTips({
                    message: err.message || err,
                    theme: 'error'
                })
            }
        },
        async setBreadCrumbPipelineList (list, pipeline) {
            if (pipeline && list.every(ele => ele.pipelineId !== pipeline.pipelineId)) {
                list = [
                    {
                        pipelineId: pipeline.pipelineId,
                        pipelineName: pipeline.pipelineName
                    },
                    ...list
                ]
            }
            this.$store.commit('pipelines/updatePipelineList', list)
        },
        async updateCurPipeline ({ projectId, pipelineId }) {
            const curPipeline = await this.requestPipelineDetail({
                projectId,
                pipelineId
            })
            this.$store.commit('pipelines/updateCurPipeline', curPipeline)
            return curPipeline
        },
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
        async executePipeline (params, goDetail = false) {
            let message, theme
            const { projectId, pipelineId } = this.$route.params
            try {
                this.setExecuteStatus(true)
                // 请求执行构建
                const res = await this.requestExecPipeline({
                    projectId,
                    params,
                    pipelineId
                })

                if (res && res.id) {
                    message = this.$t('newlist.sucToStartBuild')
                    theme = 'success'
                    this.$store.commit('pipelines/updateCurAtomPrams', null)
                    this.setExecuteStatus(false)
                    if (goDetail) {
                        this.$router.push({
                            name: 'pipelinesDetail',
                            params: {
                                projectId,
                                pipelineId,
                                type: 'executeDetail',
                                buildNo: res.id
                            }
                        })
                    }
                } else {
                    message = this.$t('newlist.failToStartBuild')
                    theme = 'error'
                }
            } catch (err) {
                this.setExecuteStatus(false)
                this.$store.commit('pipelines/updateCurAtomPrams', null)
                this.handleError(err, [{
                    actionId: this.$permissionActionMap.execute,
                    resourceId: this.$permissionResourceMap.pipeline,
                    instanceId: [{
                        id: pipelineId,
                        name: this.curPipeline.pipelineName
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
        savePipeline () {
            const { projectId, pipelineId } = this.$route.params
            const { checkPipelineInvalid, pipeline } = this
            const { inValid, message } = checkPipelineInvalid(pipeline.stages)
            if (inValid) {
                throw new Error(message)
            }
            return this.$ajax.put(`/${PROCESS_API_URL_PREFIX}/user/pipelines/${projectId}/${pipelineId}`, pipeline)
        },
        // 补全wechatGroup末尾分号
        wechatGroupCompletion (setting) {
            try {
                let successWechatGroup = setting.successSubscription.wechatGroup
                let failWechatGroup = setting.failSubscription.wechatGroup
                if (successWechatGroup && !/\;$/.test(successWechatGroup)) {
                    successWechatGroup = `${successWechatGroup};`
                }
                if (failWechatGroup && !/\;$/.test(failWechatGroup)) {
                    failWechatGroup = `${failWechatGroup};`
                }
                return {
                    ...setting,
                    successSubscription: {
                        ...setting.successSubscription,
                        wechatGroup: successWechatGroup
                    },
                    failSubscription: {
                        ...setting.failSubscription,
                        wechatGroup: failWechatGroup
                    }
                }
            } catch (e) {
                console.warn(e)
                return setting
            }
        },
        getPipelineSetting () {
            const { pipelineSetting } = this
            const { projectId } = this.$route.params
            return this.wechatGroupCompletion({
                ...pipelineSetting,
                projectId
            })
        },
        savePipelineSetting () {
            const { $route } = this
            const pipelineSetting = this.getPipelineSetting()
            const remoteUrl = $route.name === 'templateSetting' ? `/${PROCESS_API_URL_PREFIX}/user/templates/projects/${this.projectId}/templates/${this.templateId}/settings` : `/${PROCESS_API_URL_PREFIX}/user/setting/save`
            const reqMethod = $route.name === 'templateSetting' ? 'put' : 'post'
            return this.$ajax[reqMethod](remoteUrl, pipelineSetting)
        },
        saveSetting () {
            const pipelineSetting = this.getPipelineSetting()
            const { projectId, pipelineId } = this.$route.params
            return this.$ajax.post(`/${PROCESS_API_URL_PREFIX}/user/pipelines/${projectId}/${pipelineId}/saveSetting`, pipelineSetting)
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
                        name: this.curPipeline.pipelineName
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
                        id: this.curPipeline.pipelineId,
                        name: this.curPipeline.pipelineName
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
        async savePipelineAndSetting () {
            const { pipelineSetting, checkPipelineInvalid, pipeline } = this
            const { inValid, message } = checkPipelineInvalid(pipeline.stages, pipelineSetting)
            const { projectId, pipelineId } = this.$route.params
            if (inValid) {
                throw new Error(message)
            }
            // 清除流水线参数渲染过程中添加的key
            this.formatParams(pipeline)
            const finalSetting = this.wechatGroupCompletion({
                ...pipelineSetting,
                projectId: projectId
            })
            const body = {
                model: {
                    ...pipeline,
                    name: finalSetting.pipelineName,
                    desc: finalSetting.desc
                },
                setting: finalSetting
            }
            if (!pipelineId) {
                return this.importPipelineAndSetting(body)
            }

            // 请求执行构建
            return this.$ajax.post(`${PROCESS_API_URL_PREFIX}/user/pipelines/${projectId}/${pipelineId}/saveAll`, body)
        },
        importPipelineAndSetting (body) {
            const { projectId } = this.$route.params

            // 请求执行构建
            return this.$ajax.post(`${PROCESS_API_URL_PREFIX}/user/pipelines/projects/${projectId}/upload`, body)
        },
        async save () {
            const { pipelineId, projectId } = this.$route.params
            try {
                this.setSaveStatus(true)
                const saveAction = this.isTemplatePipeline ? this.saveSetting : this.savePipelineAndSetting
                const responses = await saveAction()

                if (responses.code === 403) {
                    throw new HttpError(403, responses.message)
                }
                this.setPipelineEditing(false)
                this.$showTips({
                    message: this.$t('saveSuc'),
                    theme: 'success'
                })

                if (!this.isTemplatePipeline && this.pipeline.latestVersion && !isNaN(this.pipeline.latestVersion)) {
                    ++this.pipeline.latestVersion
                    this.updateCurPipelineByKeyValue('pipelineVersion', this.pipeline.latestVersion)
                    this.updateCurPipelineByKeyValue('deploymentTime', convertTime(new Date()))
                }

                if (this.pipelineSetting && this.pipelineSetting.pipelineName !== this.curPipeline.pipelineName) {
                    this.updateCurPipelineByKeyValue('pipelineName', this.pipelineSetting.pipelineName)
                }

                return {
                    code: 0,
                    data: responses
                }
            } catch (e) {
                this.handleError(e, [{
                    actionId: this.$permissionActionMap.edit,
                    resourceId: this.$permissionResourceMap.pipeline,
                    instanceId: [{
                        id: pipelineId,
                        name: this.pipeline.name
                    }],
                    projectId
                }])
                return {
                    code: e.code,
                    message: e.message
                }
            } finally {
                this.setSaveStatus(false)
            }
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
        updateCurPipelineByKeyValue (key, value) {
            this.$store.commit('pipelines/updateCurPipelineByKeyValue', {
                key,
                value
            })
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
        },
        formatParams (pipeline) {
            const params = pipeline.stages[0].containers[0].params
            const paramList = params && params.map(param => {
                const { paramIdKey, ...temp } = param
                return temp
            })
            this.updateContainer({
                container: this.pipeline.stages[0].containers[0],
                newParam: {
                    params: paramList
                }
            })
        }
    }
}
