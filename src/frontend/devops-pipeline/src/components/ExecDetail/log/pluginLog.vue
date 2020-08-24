<template>
    <section class="plugin-log">
        <bk-log-search :down-load-link="id === undefined ? downLoadAllLink : downLoadLink" :execute-count="executeCount" @change-execute="changeExecute" class="log-tools"></bk-log-search>
        <bk-log class="bk-log" ref="scroll" @tag-change="tagChange"></bk-log>
    </section>
</template>

<script>
    import { mapActions, mapState } from 'vuex'
    import { hashID } from '@/utils/util.js'

    export default {
        props: {
            id: {
                type: String,
                default: undefined
            },
            buildId: {
                type: String
            },
            executeCount: {
                type: Number
            }
        },

        data () {
            return {
                postData: {
                    projectId: this.$route.params.projectId,
                    pipelineId: this.$route.params.pipelineId,
                    buildId: this.buildId,
                    tag: this.id,
                    subTag: '',
                    currentExe: this.executeCount,
                    lineNo: 0
                },
                timeId: '',
                clearIds: []
            }
        },

        computed: {
            ...mapState('atom', [
                'execDetail',
                'editingElementPos'
            ]),

            downLoadLink () {
                const editingElementPos = this.editingElementPos
                const fileName = encodeURI(encodeURI(`${editingElementPos.stageIndex + 1}-${editingElementPos.containerIndex + 1}-${editingElementPos.elementIndex + 1}-${this.currentElement.name}`))
                const tag = this.currentElement.id
                return `${AJAX_URL_PIRFIX}/log/api/user/logs/${this.$route.params.projectId}/${this.$route.params.pipelineId}/${this.execDetail.id}/download?tag=${tag}&executeCount=${this.postData.currentExe}&fileName=${fileName}`
            },

            downLoadAllLink () {
                const fileName = encodeURI(encodeURI(this.execDetail.pipelineName))
                return `${AJAX_URL_PIRFIX}/log/api/user/logs/${this.$route.params.projectId}/${this.$route.params.pipelineId}/${this.execDetail.id}/download?executeCount=1&fileName=${fileName}`
            },

            currentElement () {
                const {
                    editingElementPos: { stageIndex, containerIndex, elementIndex },
                    execDetail: { model: { stages } }
                } = this
                return stages[stageIndex].containers[containerIndex].elements[elementIndex]
            }
        },

        mounted () {
            this.getLog()
        },

        beforeDestroy () {
            this.closeLog()
        },

        methods: {
            ...mapActions('atom', [
                'getInitLog',
                'getAfterLog'
            ]),

            getLog () {
                const id = hashID()
                this.getLog.id = id
                let logMethod = this.getAfterLog
                if (this.postData.lineNo <= 0) logMethod = this.getInitLog

                logMethod(this.postData).then((res) => {
                    if (this.clearIds.includes(id)) return

                    const scroll = this.$refs.scroll
                    res = res.data || {}
                    if (res.status !== 0) {
                        let errMessage
                        switch (res.status) {
                            case 1:
                                errMessage = this.$t('history.logEmpty')
                                break
                            case 2:
                                errMessage = this.$t('history.logClear')
                                break
                            case 3:
                                errMessage = this.$t('history.logClose')
                                break
                            default:
                                errMessage = this.$t('history.logErr')
                                break
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
            }
        }
    }
</script>

<style lang="scss" scoped>
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
