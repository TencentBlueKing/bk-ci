<template>
    <section class="job-log">
        <bk-log-search :execute-count="executeCount" @change-execute="changeExecute" class="log-tools">
            <template v-slot:tool>
                <li class="more-button" @click="toggleShowDebugLog">{{ showDebug ? $t('hideDebugLog') : $t('showDebugLog') }}</li>
                <li class="more-button" @click="downloadLog">{{ $t('downloadLog') }}</li>
            </template>
        </bk-log-search>
        <bk-stage-log ref="multipleLog"
            class="bk-log"
            :stage="stage"
            @open-log="openLog"
            @tag-change="tagChange"
        >
            <template slot-scope="log">
                <status-icon :status="log.data.status" :is-hook="((log.data.additionalOptions || {}).elementPostInfo || false)" class="multiple-log-status"></status-icon>
                {{ log.data.name }}
                {{ stage.containers.length }}
            </template>
        </bk-stage-log>
    </section>
</template>

<script>
    import { mapActions } from 'vuex'
    import statusIcon from '../status'
    import { hashID } from '@/utils/util.js'
    import { bkLogSearch, bkStageLog } from '@blueking/log'

    export default {
        components: {
            statusIcon,
            bkLogSearch,
            bkStageLog
        },

        props: {
            buildId: {
                type: String
            },
            stage: {
                type: Object
            },
            downLoadLink: {
                type: String
            },
            executeCount: {
                type: Number
            }
        },

        data () {
            return {
                logPostData: {},
                closeIds: [],
                curExe: this.executeCount,
                showDebug: false
            }
        },

        beforeDestroy () {
            this.closeLog()
        },

        methods: {
            ...mapActions('atom', [
                'getInitLog',
                'getAfterLog'
            ]),

            toggleShowDebugLog () {
                this.showDebug = !this.showDebug
                this.clearAllLog()
                this.$refs.multipleLog.foldAllPlugin()
            },

            changeExecute (execute) {
                this.curExe = execute
                this.clearAllLog()
            },

            clearAllLog () {
                this.closeLog()
                const ref = this.$refs.multipleLog
                Object.keys(this.logPostData).forEach((id) => {
                    ref.changeExecute(id)
                })
                this.logPostData = {}
            },

            tagChange (tag, id) {
                const ref = this.$refs.multipleLog
                const postData = this.logPostData[id]
                clearTimeout(postData.timeId)
                this.closeIds.push(postData.hashId)
                ref.changeExecute(id)
                postData.lineNo = 0
                postData.subTag = tag
                this.getLog(id, postData)
            },

            closeLog () {
                Object.keys(this.logPostData).forEach(key => {
                    const postData = this.logPostData[key]
                    this.closeIds.push(postData.hashId)
                    clearTimeout(postData.timeId)
                })
            },

            openLog (plugin) {
                const id = plugin.id
                let postData = this.logPostData[id]
                if (!postData) {
                    postData = this.logPostData[id] = {
                        projectId: this.$route.params.projectId,
                        pipelineId: this.$route.params.pipelineId,
                        buildId: this.buildId,
                        tag: id,
                        currentExe: this.curExe,
                        lineNo: 0,
                        debug: this.showDebug
                    }

                    this.$nextTick(() => {
                        this.getLog(id, postData)
                    })
                }
            },

            getLog (id, postData) {
                const hashId = postData.hashId = hashID()
                let logMethod = this.getAfterLog
                if (postData.lineNo <= 0) logMethod = this.getInitLog
                const ref = this.$refs.multipleLog

                logMethod(postData).then((res) => {
                    if (this.closeIds.includes(hashId)) return

                    res = res.data || {}
                    if (res.status !== 0) {
                        const errMessage = res.message ?? this.$t('history.logErr')

                        ref.handleApiErr(errMessage, id)
                        return
                    }

                    const subTags = res.subTags
                    if (subTags && subTags.length > 0) {
                        const tags = subTags.map((tag) => ({ label: tag, value: tag }))
                        tags.unshift({ label: 'All', value: '' })
                        ref.setSubTag(tags, id)
                    }

                    const logs = res.logs || []
                    const lastLog = logs[logs.length - 1] || {}
                    const lastLogNo = lastLog.lineNo || postData.lineNo - 1 || -1
                    postData.lineNo = +lastLogNo + 1

                    if (res.finished) {
                        if (res.hasMore) {
                            ref.addLogData(logs, id)
                            postData.timeId = setTimeout(() => this.getLog(id, postData), 100)
                        } else {
                            ref.addLogData(logs, id)
                        }
                    } else {
                        ref.addLogData(logs, id)
                        postData.timeId = setTimeout(() => this.getLog(id, postData), 1000)
                    }
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                    if (ref) ref.handleApiErr(err.message, id)
                })
            },

            downloadLog () {
                const { $route, stage, curExe, buildId } = this
                const { projectId, pipelineId } = $route.params
                const query = new URLSearchParams({
                    stageId: stage.id,
                    fileName: [stage.id, stage.name].join('-'),
                    executeCount: curExe
                }).toString()

                location.href = `${API_URL_PREFIX}/log/api/user/logs/${projectId}/${pipelineId}/${buildId}/download?${query}`
            }
        }
    }
</script>

<style lang="scss" scoped>
    .job-log {
        height: calc(100% - 59px);
        .log-tools {
            position: absolute;
            right: 20px;
            top: 13px;
            display: flex;
            align-items: center;
            line-height: 30px;
            user-select: none;
            background: none;
        }
    }

    .multiple-log-status {
        width: 14px;
        height: 15px;
        margin: 0 9px;
        padding: 1px 0;
        ::v-deep svg {
            width: 14px;
            height: 14px;
        }
    }
</style>
