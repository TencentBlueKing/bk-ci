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
import cookie from 'js-cookie'
import {
    navConfirm,
    HttpError
} from '@/utils/util'
import { PROCESS_API_URL_PREFIX } from '../store/constants'

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
            'pipelineSetting',
            'pipelineAuthority'
        ]),
        ...mapState('atom', [
            'pipeline',
            'executeStatus',
            'saveStatus',
            'authSettingEditing'
        ]),
        isTemplatePipeline () {
            return this.curPipeline && this.curPipeline.instanceFromTemplate
        }
    },
    methods: {
        ...mapActions('pipelines', {
            requestExecPipeline: 'requestExecPipeline',
            requestToggleCollect: 'requestToggleCollect',
            removePipeline: 'deletePipeline',
            copyPipelineAction: 'copyPipeline',
            updatePipelineSetting: 'updatePipelineSetting',
            setPipelineSetting: 'setPipelineSetting',
            requestTerminatePipeline: 'requestTerminatePipeline',
            requestRetryPipeline: 'requestRetryPipeline',
            searchPipelineList: 'searchPipelineList',
            requestPipelineDetail: 'requestPipelineDetail'
        }),
        ...mapActions('atom', [
            'setPipelineEditing',
            'setExecuteStatus',
            'setSaveStatus',
            'setAuthEditing',
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
         *  处理收藏和取消收藏
         */
        async togglePipelineCollect (pipelineId, isCollect = false) {
            try {
                const { projectId } = this.$route.params
                await this.requestToggleCollect({
                    projectId,
                    pipelineId,
                    isCollect
                })

                this.$showTips({
                    message: isCollect ? this.$t('collectSuc') : this.$t('uncollectSuc'),
                    theme: 'success'
                })
                this.updateCurPipelineByKeyValue('hasCollect', isCollect)
            } catch (err) {
                this.$showTips({
                    message: err.message || err,
                    theme: 'error'
                })
            }
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
                        id: pipelineId,
                        name: target.pipelineName
                    }],
                    projectId
                }], this.getPermUrlByRole(projectId, pipelineId, this.roleMap.executor))
            } finally {
                feConfig.buttonAllow.terminatePipeline = true
            }
        },
        /**
         *  删除流水线
         */
        async delete ({ pipelineId, pipelineName }) {
            let message, theme
            const content = `${this.$t('newlist.deletePipeline')}: ${pipelineName}`
            const { projectId } = this.$route.params
            try {
                await navConfirm({ type: 'warning', content })

                this.isLoading = true
                await this.removePipeline({
                    projectId,
                    pipelineId
                })

                this.$router.push({
                    name: 'pipelinesList'
                })

                message = this.$t('deleteSuc')
                theme = 'success'
            } catch (err) {
                this.handleError(err, [{
                    actionId: this.$permissionActionMap.delete,
                    resourceId: this.$permissionResourceMap.pipeline,
                    instanceId: [{
                        id: pipelineId,
                        name: pipelineName
                    }],
                    projectId
                }], this.getPermUrlByRole(projectId, pipelineId, this.roleMap.manager))
            } finally {
                message && this.$showTips({
                    message,
                    theme
                })
                this.isLoading = false
            }
        },
        /**
         *  复制流水线弹窗的确认回调函数
         */
        async copy (tempPipeline, pipelineId) {
            const { copyPipelineAction, pipelineList } = this
            const { projectId } = this.$route.params
            let message = ''
            let theme = ''
            const prePipeline = pipelineList.find(item => item.pipelineId === pipelineId)

            try {
                if (!tempPipeline.name) {
                    throw new Error(this.$t('subpage.nameNullTips'))
                }
                await copyPipelineAction({
                    projectId,
                    pipelineId,
                    args: {
                        ...tempPipeline,
                        group: prePipeline.group,
                        hasCollect: false
                    }
                })

                message = this.$t('copySuc')
                theme = 'success'
            } catch (err) {
                this.handleError(err, [{
                    actionId: this.$permissionActionMap.create,
                    resourceId: this.$permissionResourceMap.pipeline,
                    instanceId: [{
                        id: prePipeline.pipelineId,
                        name: prePipeline.pipelineName
                    }]
                }, {
                    actionId: this.$permissionActionMap.edit,
                    resourceId: this.$permissionResourceMap.pipeline,
                    instanceId: [{
                        id: pipelineId,
                        name: prePipeline.pipelineName
                    }],
                    projectId
                }], this.getPermUrlByRole(projectId, pipelineId, this.roleMap.manager))
            } finally {
                message && this.$showTips({
                    message,
                    theme
                })
            }
        },
        /**
         *  复制流水线弹窗的取消回调
         */
        async rename ({ name }, projectId, pipelineId) {
            let message = ''
            let theme = ''
            try {
                if (!name) {
                    throw new Error(this.$t('subpage.nameNullTips'))
                }
                await this.$ajax.post(`/${PROCESS_API_URL_PREFIX}/user/pipelines/${projectId}/${pipelineId}`, {
                    name
                })
                this.$nextTick(() => {
                    this.updateCurPipelineByKeyValue('pipelineName', name)

                    this.pipelineSetting && Object.keys(this.pipelineSetting).length && this.updatePipelineSetting({
                        container: this.pipelineSetting,
                        param: {
                            pipelineName: name
                        }
                    })
                })
                message = this.$t('updateSuc')
                theme = 'success'
            } catch (err) {
                this.handleError(err, [{
                    actionId: this.$permissionActionMap.edit,
                    resourceId: this.$permissionResourceMap.pipeline,
                    instanceId: [{
                        id: pipelineId,
                        name: this.curPipeline.pipelineName
                    }],
                    projectId
                }], this.getPermUrlByRole(projectId, pipelineId, this.roleMap.manager))
            } finally {
                message && this.$showTips({
                    message,
                    theme
                })
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
                }], this.getPermUrlByRole(projectId, pipelineId, this.roleMap.executor))
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
        savePipelineAuthority () {
            const { role, policy } = this.pipelineAuthority
            const longProjectId = this.curProject && this.curProject.projectId ? this.curProject.projectId : ''
            const { pipelineId } = this.$route.params
            const data = {
                project_id: longProjectId,
                resource_type_code: 'pipeline',
                resource_code: pipelineId,
                role: role.map(item => {
                    item.group_list = item.selected
                    return item
                }),
                policy: policy.map(item => {
                    item.group_list = item.selected
                    return item
                })
            }
            return this.$ajax.put('/backend/api/perm/service/pipeline/mgr_resource/permission/', data, { headers: { 'X-CSRFToken': cookie.get('paas_perm_csrftoken') } })
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
                }], this.getPermUrlByRole(projectId, pipelineId, this.roleMap.executor))
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
                }], this.getPermUrlByRole(this.$route.params.projectId, this.curPipeline.pipelineId, this.roleMap.executor))
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
                const responses = await Promise.all([
                    saveAction(),
                    ...(this.authSettingEditing ? [this.savePipelineAuthority()] : [])
                ])

                if (responses.some(res => res.code === 403)) {
                    throw new HttpError(403)
                }
                this.setPipelineEditing(false)
                this.setAuthEditing(false)
                this.$showTips({
                    message: this.$t('saveSuc'),
                    theme: 'success'
                })

                if (!this.isTemplatePipeline && this.pipeline.latestVersion && !isNaN(this.pipeline.latestVersion)) {
                    ++this.pipeline.latestVersion
                    this.updateCurPipelineByKeyValue('pipelineVersion', this.pipeline.latestVersion)
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
                }], this.getPermUrlByRole(projectId, pipelineId, this.roleMap.manager))
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
                }], this.getPermUrlByRole(projectId, pipelineId, this.roleMap.manager))
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
        async toApplyPermission (role) {
            const { projectId, pipelineId } = this.$route.params
            this.tencentPermission(this.getPermUrlByRole(projectId, pipelineId, role))
            // try {
            //     const { projectId } = this.$route.params
            //     const redirectUrl = await this.$ajax.post(`${AUTH_URL_PREFIX}/user/auth/permissionUrl`, [{
            //         actionId,
            //         resourceId: this.$permissionResourceMap.pipeline,
            //         instanceId: [{
            //             id: projectId,
            //             type: this.$permissionResourceTypeMap.PROJECT
            //         }, pipeline]
            //     }])
            //     console.log('redirectUrl', redirectUrl)
            //     window.open(redirectUrl, '_blank')
            //     this.$bkInfo({
            //         title: this.$t('permissionRefreshtitle'),
            //         subTitle: this.$t('permissionRefreshSubtitle'),
            //         okText: this.$t('permissionRefreshOkText'),
            //         cancelText: this.$t('close'),
            //         confirmFn: () => {
            //             location.reload()
            //         }
            //     })
            // } catch (e) {
            //     console.error(e)
            // }
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
        },
        handleError (err) {
            this.$showTips({
                message: err.message,
                theme: 'error'
            })
        }
    }
}
