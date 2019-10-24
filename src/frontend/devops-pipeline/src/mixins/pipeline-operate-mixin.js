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
import {
    navConfirm
} from '@/utils/util'
import { PROCESS_API_URL_PREFIX } from '../store/constants'

export default {
    computed: {
        ...mapGetters({
            pipelineList: 'pipelines/getPipelineList',
            tagGroupList: 'pipelines/getTagGroupList',
            curPipeline: 'pipelines/getCurPipeline',
            isEditing: 'atom/isEditing',
            checkPipelineInvalid: 'atom/checkPipelineInvalid'
        }),
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
        projectId () {
            return this.$route.params.projectId
        },
        pipelineId () {
            return this.$route.params.pipelineId
        },
        longProjectId () {
            return this.$store.state.curProject.project_id || ''
        },
        isTemplatePipeline () {
            return this.curPipeline && this.curPipeline.instanceFromTemplate
        }
    },
    methods: {
        ...mapActions('pipelines', {
            requestPipelinesList: 'requestPipelinesList',
            requestExecPipeline: 'requestExecPipeline',
            requestToggleCollect: 'requestToggleCollect',
            removePipeline: 'deletePipeline',
            copyPipelineAction: 'copyPipeline',
            updatePipelineSetting: 'updatePipelineSetting'
        }),
        ...mapActions('atom', [
            'setPipelineEditing',
            'setExecuteStatus',
            'setSaveStatus',
            'setAuthEditing',
            'updateContainer'
        ]),
        async fetchPipelineList () {
            try {
                const { pipelineId, requestPipelinesList, projectId } = this
                const res = await requestPipelinesList({
                    projectId,
                    tag: 'pipelines',
                    page: 1,
                    pageSize: -1
                })
                this.$store.commit('pipelines/updatePipelineList', res.records)
                this.$nextTick(() => {
                    // 选中下拉列表中的项
                    this.updateCurPipelineId(pipelineId)
                })
            } catch (err) {
                this.$showTips({
                    message: err.message || err,
                    theme: 'error'
                })
            }
        },
        /**
         *  处理收藏和取消收藏
         */
        async togglePipelineCollect (pipelineId, isCollect = false) {
            try {
                await this.requestToggleCollect({
                    projectId: this.projectId,
                    pipelineId,
                    isCollect
                })

                this.$showTips({
                    message: isCollect ? this.$t('collectSuc') : this.$t('uncollectSuc'),
                    theme: 'success'
                })
                this.fetchPipelineList()
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
            const { $store, projectId } = this
            const target = this.pipelineList.find(item => item.pipelineId === pipelineId)
            const { feConfig } = target

            if (!feConfig.buttonAllow.terminatePipeline) return

            feConfig.buttonAllow.terminatePipeline = false

            try {
                await $store.dispatch('pipelines/requestTerminatePipeline', {
                    projectId,
                    pipelineId,
                    buildId: feConfig.buildId || target.latestBuildId
                })
                this.updatePipelineValueById(pipelineId, {
                    isRunning: false,
                    status: 'known_error'
                })
            } catch (err) {
                if (err.code === 403) { // 没有权限终止
                    this.setPermissionConfig(`${this.$t('pipeline')}：${target.pipelineName}`, this.$t('exec'), target.pipelineId)
                } else {
                    this.$showTips({
                        message: err.message || err,
                        theme: 'error'
                    })
                }
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

            try {
                await navConfirm({ title: this.$t('subpage.confirmDelete'), content })

                await this.removePipeline({
                    projectId: this.projectId,
                    pipelineId
                })

                this.$router.push({
                    name: 'pipelinesList'
                })

                message = this.$t('deleteSuc')
                theme = 'success'
            } catch (err) {
                if (err.code === 403) { // 没有权限删除
                    this.setPermissionConfig(`${this.$t('pipeline')}：${pipelineName}`, this.$t('delete'), this.projectId, pipelineId)
                } else {
                    message = err.message || err
                    theme = 'error'
                }
            } finally {
                message && this.$showTips({
                    message,
                    theme
                })
            }
        },
        /**
         *  复制流水线弹窗的确认回调函数
         */
        async copy (tempPipeline, pipelineId) {
            const { projectId, copyPipelineAction, pipelineList } = this
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

                this.$nextTick(() => {
                    this.fetchPipelineList()
                })
            } catch (err) {
                if (err.code === 403) { // 没有权限复制
                    this.setPermissionConfig(`${this.$t('pipeline')}：${prePipeline.pipelineName}`, this.$t('edit'), projectId, prePipeline.pipelineId)
                } else {
                    message = err.message || err
                    theme = 'error'
                }
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
                    this.fetchPipelineList()
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
                if (err.code === 403) { // 没有权限复制
                    this.setPermissionConfig(`${this.$t('pipeline')}：${this.curPipeline.pipelineName}`, this.$t('edit'), projectId, this.curPipeline.pipelineId)
                } else {
                    message = err.message || err
                    theme = 'error'
                }
            } finally {
                message && this.$showTips({
                    message,
                    theme
                })
            }
        },
        async executePipeline (params, goDetail = false) {
            let message, theme
            const { projectId, pipelineId, requestExecPipeline, setExecuteStatus } = this

            try {
                setExecuteStatus(true)
                // 请求执行构建
                const res = await requestExecPipeline({
                    projectId,
                    params,
                    pipelineId
                })

                if (res.id) {
                    message = this.$t('newlist.sucToStartBuild')
                    theme = 'success'
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
                if (err.code === 403) { // 没有权限执行
                    this.setPermissionConfig(`${this.$t('pipeline')}：${this.curPipeline.pipelineName}`, this.$t('exec'), this.projectId, pipelineId)
                } else {
                    message = err.message || err
                    theme = 'error'
                }
            } finally {
                setExecuteStatus(false)
                message && this.$showTips({
                    message,
                    theme
                })
            }
        },
        savePipeline () {
            const { checkPipelineInvalid, projectId, pipelineId, pipeline } = this
            const { inValid, message } = checkPipelineInvalid(pipeline.stages)
            if (inValid) {
                throw new Error(message)
            }
            return this.$ajax.put(`/${PROCESS_API_URL_PREFIX}/user/pipelines/${projectId}/${pipelineId}`, pipeline)
        },
        getPipelineSetting () {
            const { pipelineSetting, projectId } = this
            return {
                ...pipelineSetting,
                projectId
            }
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
            const { projectId, pipelineId } = this
            return this.$ajax.post(`/${PROCESS_API_URL_PREFIX}/user/pipelines/${projectId}/${pipelineId}/saveSetting`, pipelineSetting)
        },
        async retry (buildId, goDetail = false) {
            let message, theme
            try {
                // 请求执行构建
                const res = await this.$store.dispatch('pipelines/requestRetryPipeline', {
                    ...this.$route.params,
                    buildId
                })

                if (res.id) {
                    message = this.$t('subpage.retrySuc')
                    theme = 'success'
                    if (goDetail) {
                        this.$router.replace({
                            name: 'pipelinesDetail',
                            params: {
                                projectId: this.projectId,
                                pipelineId: this.pipelineId,
                                buildNo: res.id
                            }
                        })
                    }
                    this.$emit('update-table')
                } else {
                    message = this.$t('subpage.retryFail')
                    theme = 'error'
                }
            } catch (err) {
                if (err.code === 403) { // 没有权限执行
                    this.setPermissionConfig(`${this.$t('pipeline')}：${this.curPipeline.pipelineName}`, this.$t('exec'), this.$route.params.projectId, this.$route.params.pipelineId)
                    return
                } else {
                    message = err.message || err
                    theme = 'error'
                }
            } finally {
                message && this.$showTips({
                    message,
                    theme
                })
            }
        },
        async savePipelineAndSetting () {
            const { pipelineSetting, checkPipelineInvalid, $route, pipeline } = this
            const { inValid, message } = checkPipelineInvalid(pipeline.stages)
            if (inValid) {
                throw new Error(message)
            }
            // 清除流水线参数渲染过程中添加的key
            this.formatParams(pipeline)
            const finalSetting = {
                ...pipelineSetting,
                projectId: $route.params.projectId
            }
            // 请求执行构建
            return this.$ajax.post(`/${PROCESS_API_URL_PREFIX}/user/pipelines/${$route.params.projectId}/${$route.params.pipelineId}/saveAll`, {
                model: {
                    ...pipeline,
                    name: finalSetting.pipelineName,
                    desc: finalSetting.desc
                },
                setting: finalSetting
            })
        },
        async save () {
            try {
                this.setSaveStatus(true)
                const saveAction = this.isTemplatePipeline ? this.saveSetting : this.savePipelineAndSetting
                const responses = await Promise.all([
                    saveAction()
                ])

                if (responses.some(res => res.code === 403)) {
                    this.setPermissionConfig(`${this.$t('pipeline')}：${this.pipeline.name}`, this.$t('edit'), this.$route.params.projectId, this.$route.params.pipelineId)
                    return false
                }
                this.setPipelineEditing(false)
                this.setAuthEditing(false)
                this.$showTips({
                    message: this.$t('saveSuc'),
                    theme: 'success'
                })
                this.fetchPipelineList()
                return true
            } catch (e) {
                if (e.code === 403) { // 没有权限编辑
                    this.setPermissionConfig(`${this.$t('pipeline')}：${this.pipeline.name}`, this.$t('edit'), this.$route.params.projectId, this.$route.params.pipelineId)
                } else {
                    this.$showTips({
                        message: e.message,
                        theme: 'error'
                    })
                }
                return false
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
                if (e.code === 403) { // 没有权限编辑
                    this.setPermissionConfig(`${this.$t('pipeline')}：${this.pipeline.name}`, this.$t('edit'), this.$route.params.projectId, this.$route.params.pipelineId)
                } else {
                    this.$showTips({
                        message: e.message,
                        theme: 'error'
                    })
                }
            }
        },
        /**
         * 设置权限弹窗的参数
         */
        setPermissionConfig (resource, option, projectId, pipelineId) {
            this.$showAskPermissionDialog({
                noPermissionList: [{
                    resource,
                    option
                }],
                applyPermissionUrl: `${PERM_URL_PIRFIX}/backend/api/perm/apply/subsystem/?client_id=pipeline&project_code=${projectId}&service_code=pipeline&${option === this.$t('exec') ? 'role_executor' : 'role_manager'}=pipeline:${pipelineId}`
            })
        },
        updateCurPipelineId (pipelineId) {
            for (let i = 0; i < this.pipelineList.length; i++) {
                const item = this.pipelineList[i]
                if (item.pipelineId === pipelineId) {
                    this.$store.commit('pipelines/updateCurPipeline', item)
                    return
                }
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
        goToApplyPerm (role = 'role_viewer') {
            const url = `${PERM_URL_PIRFIX}/backend/api/perm/apply/subsystem/?client_id=pipeline&project_code=${this.$route.params.projectId}&service_code=pipeline&${role}=pipeline:${this.$route.params.pipelineId}`
            window.open(url, '_blank')
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
