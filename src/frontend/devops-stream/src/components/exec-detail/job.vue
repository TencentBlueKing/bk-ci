<template>
    <detail-container @close="$emit('close')"
        :title="job.name"
        :status="job.status"
    >
        <section v-if="showDebugBtn" class="web-console" :style="{ right: executeCount > 1 ? '390px' : '280px' }">
            <bk-popover placement="bottom" ref="consoleRef" ext-cls="console-menu-wrapper">
                <span>
                    Web Console
                </span>
                <ul class="console-ul-list" slot="content">
                    <li @click="startDebug('/bin/sh')"><span>Login via /bin/sh</span></li>
                    <li @click="startDebug('/bin/bash')"><span>Login via /bin/bash</span></li>
                </ul>
            </bk-popover>
        </section>
        <job-log :plugin-list="pluginList"
            :build-id="$route.params.buildId"
            :down-load-link="downLoadJobLink"
            :execute-count="executeCount"
            ref="jobLog"
        />
    </detail-container>
</template>

<script>
    import { mapState } from 'vuex'
    import jobLog from './log/jobLog'
    import detailContainer from './detailContainer'

    export default {
        components: {
            detailContainer,
            jobLog
        },

        props: {
            job: Object,
            stages: Array,
            stageIndex: Number,
            jobIndex: Number
        },

        computed: {
            ...mapState(['projectId', 'modelDetail']),
            
            pipelineId () {
                return this.$route.params.pipelineId || ''
            },

            buildId () {
                return this.$route.params.buildId || ''
            },

            hashId () {
                return this.$route.hash
            },

            showDebugBtn () {
                return this.job.dispatchType && this.job.dispatchType.buildType === 'GIT_CI' && this.modelDetail && this.modelDetail.buildNum === this.modelDetail.latestBuildNum
            },

            downLoadJobLink () {
                const fileName = encodeURI(encodeURI(`${this.stageIndex + 1}-${this.jobIndex + 1}-${this.job.name}`))
                const jobId = this.job.containerHashid
                const { pipelineId, buildId } = this.$route.params
                return `/log/api/user/logs/${this.projectId}/${pipelineId}/${buildId}/download?jobId=${jobId}&fileName=${fileName}`
            },

            pluginList () {
                const startUp = { name: 'Set up job', status: this.job.startVMStatus, id: `startVM-${this.job.id}`, executeCount: this.job.executeCount || 1 }
                return [startUp, ...this.job.elements]
            },

            executeCount () {
                const executeCountList = this.pluginList.map((plugin) => plugin.executeCount || 1)
                return Math.max(...executeCountList)
            }
        },
        methods: {
            startDebug (cmd = '/bin/sh') {
                const vmSeqId = this.job.containerId || this.getRealSeqId()
                this.startNewDocker(vmSeqId, cmd)
            },

            startNewDocker (vmSeqId, cmd) {
                let url = ''
                const tab = window.open('about:blank')
                const buildIdStr = this.buildId ? `&buildId=${this.buildId}` : ''
                url = `/webConsole?pipelineId=${this.pipelineId}&vmSeqId=${vmSeqId}${buildIdStr}&cmd=${cmd}${this.hashId}`
                tab.location = url
            },

            getRealSeqId () {
                return this.stages.slice(0, this.stageIndex).reduce((acc, stage) => {
                    acc += stage.containers.length
                    return acc
                }, 0) + this.jobIndex + 1
            }
        }
    }
</script>

<style lang="postcss">
    .web-console {
        position: absolute;
        right: 280px;
        top: 20px;
        span {
            color: #3c96ff;
            cursor: pointer;
        }
    }
    .console-menu-wrapper {
        .tippy-tooltip {
            padding: 0px;
            background: #2f363d;
            .tippy-arrow {
                border-bottom: 8px solid #2f363d;
            }
        }
    }
    .console-ul-list {
        border: 1px solid #444d56;
        border-radius: 4px;
        li {
            display: flex;
            align-items: center;
            width: 180px;
            height: 36px;
            font-size: 12px;
            cursor: pointer;
            background: #2f363d;
            &:not(:last-child) {
                border-bottom: 1px solid #444D56;
            }
            &:hover {
                background: #3a84ff;
            }
            span {
                margin-left: 25px;
            }
        }
        
    }
</style>
