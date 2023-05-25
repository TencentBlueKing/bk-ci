<template>
    <section class="plugin-log">
        <bk-log-search :execute-count="plugin.executeCount" @change-execute="changeExecute" class="log-tools">
            <template #tool>
                <li class="more-button" @click="toggleShowDebugLog">{{ showDebug ? $t('pipeline.hideLog') : $t('pipeline.showLog')}}</li>
                <li class="more-button" @click="downloadLog">{{$t('pipeline.downloadLog')}}</li>
            </template>
        </bk-log-search>
        <bk-log class="bk-log" ref="scroll" @tag-change="tagChange"></bk-log>
    </section>
</template>

<script>
    import { uuid } from '@/utils'
    import { pipelines } from '@/http'
    import { mapState } from 'vuex'

    export default {
        props: {
            plugin: Object,
            pluginIndex: Number,
            stageIndex: Number,
            jobIndex: Number,
            matrixIndex: Number
        },

        data () {
            return {
                postData: {},
                timeId: '',
                clearIds: [],
                showDebug: false,
                hasRetryGetLog: false
            }
        },

        computed: {
            ...mapState(['projectId']),

            pipelineId () {
                return this.$route.params.pipelineId
            },

            buildId () {
                return this.$route.params.buildId
            },

            downLoadLink () {
                const getIndex = (index) => {
                    return index !== undefined ? `-${index + 1}` : ''
                }
                const fileName = encodeURI(
                    encodeURI(
                        `${this.stageIndex + 1}${getIndex(this.jobIndex)}${getIndex(this.matrixIndex)}${getIndex(this.pluginIndex)}-${this.plugin.name}`
                    )
                )
                const tag = this.plugin.id
                const containerHashId = this.plugin.containerHashId
                const typeQuery = containerHashId ? `jobId=${containerHashId}` : `tag=${tag}`
                return `/log/api/user/logs/${this.projectId}/${this.pipelineId}/${this.buildId}/download?${typeQuery}&executeCount=${this.postData.currentExe}&fileName=${fileName}`
            }
        },

        mounted () {
            this.initPostData()
            this.getLog()
        },

        beforeDestroy () {
            this.closeLog()
        },

        methods: {
            initPostData () {
                this.postData = {
                    projectId: this.projectId,
                    pipelineId: this.pipelineId,
                    buildId: this.buildId,
                    tag: this.plugin.id,
                    jobId: this.plugin.containerHashId,
                    subTag: '',
                    currentExe: this.plugin.executeCount,
                    lineNo: 0,
                    debug: false
                }
            },

            getLog () {
                const id = uuid()
                this.getLog.id = id
                let logMethod = pipelines.getAfterLog
                if (this.postData.lineNo <= 0) logMethod = pipelines.getInitLog

                logMethod(this.postData).then((res) => {
                    if (this.clearIds.includes(id)) return

                    const scroll = this.$refs.scroll
                    if (res.status !== 0) {
                        let errMessage
                        switch (res.status) {
                            case 1:
                                errMessage = this.$t('pipeline.logEmpty')
                                break
                            case 2:
                                errMessage = this.$t('pipeline.logCleared')
                                break
                            case 3:
                                errMessage = this.$t('pipeline.logClosed')
                                break
                            default:
                                errMessage = this.$t('pipeline.logErr')
                        }
                        scroll.handleApiErr(errMessage)
                        return
                    }

                    const logs = res.logs || []
                    const lastLog = logs[logs.length - 1] || {}
                    const lastLogNo = lastLog.lineNo || this.postData.lineNo - 1 || -1
                    this.postData.lineNo = +lastLogNo + 1

                    const subTags = res.subTags
                    if (subTags && subTags.length > 0) {
                        const tags = subTags.map((tag) => ({ label: tag, value: tag }))
                        tags.unshift({ label: 'ALL', value: '' })
                        scroll.setSubTag(tags)
                    }

                    if (res.finished) {
                        if (res.hasMore) {
                            scroll.addLogData(logs)
                            this.timeId = setTimeout(this.getLog, 100)
                        } else {
                            scroll.addLogData(logs)
                            if (!this.hasRetryGetLog) {
                                this.hasRetryGetLog = true
                                this.timeId = setTimeout(this.getLog, 3000)
                            }
                        }
                    } else {
                        scroll.addLogData(logs)
                        this.timeId = setTimeout(this.getLog, 1000)
                    }
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                    if (scroll) scroll.handleApiErr(err.message)
                })
            },

            tagChange (val) {
                this.postData.subTag = val
                this.postData.lineNo = 0
                this.closeLog()
                this.getLog()
            },

            changeExecute (execute) {
                this.postData.currentExe = execute
                this.postData.lineNo = 0
                this.closeLog()
                this.getLog()
            },

            closeLog () {
                clearTimeout(this.timeId)
                this.clearIds.push(this.getLog.id)
            },

            handleApiErr (err) {
                const scroll = this.$refs.scroll
                if (scroll) scroll.handleApiErr(err)
            },

            toggleShowDebugLog () {
                this.showDebug = !this.showDebug
                this.$refs.scroll.changeExecute()
                this.postData.debug = this.showDebug
                this.postData.lineNo = 0
                this.closeLog()
                this.getLog()
            },

            async downloadLog () {
                const pluginData = {
                    projectId: this.projectId,
                    pipelineId: this.pipelineId,
                    buildId: this.buildId,
                    tag: this.plugin.id,
                    executeCount: this.plugin.executeCount
                }
                try {
                    const logStatusRes = await pipelines.getLogStatus(pluginData)
                    const logMode = logStatusRes?.logMode || ''
                    if (logMode === 'LOCAL') {
                        this.$bkMessage({ theme: 'primary', message: this.$t('history.uploadLog') })
                        return
                    }
                    const downloadLink = logMode === 'ARCHIVED' ? await pipelines.getDownloadLogFromArtifactory(pluginData) : this.downLoadLink
                    location.href = downloadLink
                } catch (error) {
                    this.$bkMessage({ theme: 'error', message: error.message || error })
                }
            }
        }
    }
</script>

<style lang="postcss" scoped>
    .plugin-log {
        display: flex;
        flex-direction: column;
        flex: 1;
    }

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
</style>
