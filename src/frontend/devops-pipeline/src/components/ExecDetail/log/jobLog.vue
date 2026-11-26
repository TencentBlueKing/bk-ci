<template>
    <section class="job-log">
        <bk-log-search
            ref="logSearch"
            :execute-count="executeCount"
            @change-execute="changeExecute"
            class="log-tools"
        >
            <template v-slot:tool>
                <li
                    class="more-button"
                    @click="toggleShowDebugLog"
                >
                    {{ showDebug ? $t('hideDebugLog') : $t('showDebugLog') }}
                </li>
                <li
                    class="more-button"
                    @click="downloadLog"
                >
                    {{ $t('downloadLog') }}
                </li>
            </template>
        </bk-log-search>
        <div
            v-if="emptyStatus"
            class="log-empty"
        >
            {{ $t(`logStatus.${emptyStatus}`) }}
        </div>
        <bk-multiple-log
            ref="multipleLog"
            class="bk-log"
            :log-list="pluginList"
            :enable-a-i="enableAI"
            :ai-tips="aiTips"
            @open-log="openLog"
            @tag-change="tagChange"
            @praise-ai="handlePraiseAi"
            @down-praise-ai="handleDownPraiseAi"
            @load-ai-message="handleLoadAiMessage"
            @reload-ai-message="handleReloadAiMessage"
            @cancel-praise-ai="handleCancelPraiseAI"
            @cancel-down-praise-ai="handleCancelDownPraiseAI"
        >
            <template slot-scope="log">
                <status-icon
                    :status="log.data.status"
                    :is-hook="((log.data.additionalOptions || {}).elementPostInfo || false)"
                    class="multiple-log-status"
                ></status-icon>
                {{ log.data.name }}
            </template>
        </bk-multiple-log>
    </section>
</template>

<script>
    import { mapActions } from 'vuex'
    import statusIcon from '../status'
    import { hashID } from '@/utils/util.js'
    import { bkLogSearch, bkMultipleLog } from '@blueking/log'

    export default {
        components: {
            statusIcon,
            bkLogSearch,
            bkMultipleLog
        },

        props: {
            buildId: {
                type: String
            },
            pluginList: {
                type: Array
            },
            downLoadLink: {
                type: String
            },
            executeCount: {
                type: Number
            },
            activeExecuteCount: {
                type: Number
            }
        },

        data () {
            return {
                logPostData: {},
                closeIds: [],
                curExe: this.activeExecuteCount ?? this.executeCount,
                showDebug: false,
                enableAI: false,
                aiTips: '',
                emptyStatus: ''
            }
        },

        watch: {
            executeCount (value, oldValue) {
                if (oldValue === undefined || Number(this.curExe) !== Number(oldValue)) return
                this.$refs.logSearch?.changeExecute?.(value)
            }
        },

        mounted () {
            this.checkAIStatus()
        },

        beforeDestroy () {
            this.closeLog()
        },

        methods: {
            ...mapActions('atom', [
                'getInitLog',
                'getAfterLog',
                'praiseAi',
                'cancelPraiseAi',
                'getPraiseAiInfo',
                'getLogAIMessage',
                'getAIStatus'
            ]),

            checkAIStatus () {
                this.getAIStatus().then(res => {
                    this.enableAI = res.data
                    this.aiTips = this.$t('details.aiAnalysis', [this.$pipelineDocs.AIAnalysis])
                })
            },

            handlePraiseAi ({ id, item }) {
                this.praiseAi({
                    ...this.logPostData[id],
                    score: true
                }).then(() => {
                    this.handleGetPraiseAiInfo({ id, item })
                    this.$bkMessage({ theme: 'success', message: this.$t('successPraise') })
                })
            },

            handleDownPraiseAi ({ id, item }) {
                this.praiseAi({
                    ...this.logPostData[id],
                    score: false
                }).then(() => {
                    this.handleGetPraiseAiInfo({ id, item })
                    this.$bkMessage({ theme: 'success', message: this.$t('successDownPraise') })
                })
            },

            handleCancelPraiseAI ({ id, item }) {
                this.cancelPraiseAi({
                    ...this.logPostData[id],
                    score: true
                }).then(() => {
                    this.handleGetPraiseAiInfo({ id, item })
                    this.$bkMessage({ theme: 'success', message: this.$t('successCancelPraise') })
                })
            },

            handleCancelDownPraiseAI ({ id, item }) {
                this.cancelPraiseAi({
                    ...this.logPostData[id],
                    score: false
                }).then(() => {
                    this.handleGetPraiseAiInfo({ id, item })
                    this.$bkMessage({ theme: 'success', message: this.$t('successCancelDownPraise') })
                })
            },

            handleGetPraiseAiInfo ({ id, item }) {
                const ref = this.$refs.multipleLog
                this.getPraiseAiInfo({
                    ...this.logPostData[id]
                })
                    .then((res) => {
                        item.goodUsers = res.data.goodUsers
                        item.badUsers = res.data.badUsers
                        ref.setSingleLogData(item, id)
                    })
            },

            handleLoadAiMessage ({ id, item }) {
                item.aiMessage = ''
                const ref = this.$refs.multipleLog
                this.handleGetPraiseAiInfo({ id, item })
                this.getLogAIMessage({
                    ...this.logPostData[id],
                    refresh: false,
                    callBack (val) {
                        item.aiMessage += val
                        ref.setSingleLogData(item, id)
                        ref.scrollAILogToBottom(id)
                    }
                })
            },

            handleReloadAiMessage ({ id, item }) {
                item.aiMessage = ''
                const ref = this.$refs.multipleLog
                this.getLogAIMessage({
                    ...this.logPostData[id],
                    refresh: true,
                    callBack (val) {
                        item.aiMessage += val
                        ref.setSingleLogData(item, id)
                        ref.scrollAILogToBottom(id)
                    }
                }).then(() => {
                    this.handleGetPraiseAiInfo({ id, item })
                })
            },

            toggleShowDebugLog () {
                this.showDebug = !this.showDebug
                this.clearAllLog()
                this.$refs.multipleLog.foldAllPlugin()
            },

            changeExecute (execute) {
                this.closeLog()
                this.emptyStatus = ''
                this.curExe = execute
                this.$emit('execute-change', execute)
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
                this.closeLog()
                this.emptyStatus = ''
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
                if (postData.lineNo <= 0) this.emptyStatus = ''
                let logMethod = this.getAfterLog
                if (postData.lineNo <= 0) logMethod = this.getInitLog
                const ref = this.$refs.multipleLog

                logMethod(postData).then((res) => {
                    if (this.closeIds.includes(hashId)) return

                    res = res.data || {}
                    if ([2, 3, '2', '3'].includes(res.status)) {
                        this.emptyStatus = [2, '2'].includes(res.status) ? 'clean' : 'closed'
                        return
                    }
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
                    if (this.closeIds.includes(hashId)) return
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                    if (ref) ref.handleApiErr(err.message, id)
                })
            },

            downloadLog () {
                const downloadLink = `${this.downLoadLink}&executeCount=${this.curExe}`
                location.href = downloadLink
            }
        }
    }
</script>

<style lang="scss" scoped>
    .job-log {
        height: calc(100% - 59px);
        flex: 1 1 auto;
        min-width: 0;
        .log-empty {
            position: absolute;
            top: 60px;
            right: 0;
            bottom: 0;
            left: 220px;
            z-index: 2;
            display: flex;
            align-items: center;
            justify-content: center;
            background: #2c2d34;
            color: #979ba5;
            font-size: 13px;
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
