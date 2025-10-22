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

import { mapActions, mapGetters, mapState } from 'vuex'
import { PROCESS_API_URL_PREFIX } from '../store/constants'

export default {
    computed: {
        ...mapGetters({
            tagGroupList: 'pipelines/getTagGroupList',
            isEditing: 'atom/isEditing'
        }),
        ...mapState([
            'curProject'
        ]),
        ...mapState('pipelines', [
            'executeStatus'
        ]),
        ...mapState('atom', [
            'pipeline',
            'pipelineInfo',
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
        ...mapActions('atom', [
            'setPipeline',
            'updateContainer'
        ]),
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
                this.handleError(
                    e,
                    {
                        projectId,
                        resourceCode: pipelineId,
                        action: this.$permissionResourceAction.EDIT
                    }
                )
            }
        },
        changeProject () {
            this.$toggleProjectMenu(true)
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
