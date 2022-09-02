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

import { mapActions, mapState } from 'vuex'

export default {
    computed: {
        ...mapState([
            'curProject'
        ])
    },
    methods: {
        ...mapActions('pipelines', [
            'requestToggleCollect',
            'deletePipeline',
            'copyPipeline',
            'restorePipeline'
        ]),
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
                await this.deletePipeline({
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
        async restore ({ projectId, pipelineId }) {
            let message = this.$t('restore.restoreSuc')
            let theme = 'success'
            try {
                await this.restorePipeline({
                    projectId,
                    pipelineId
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
        }
    }
}
