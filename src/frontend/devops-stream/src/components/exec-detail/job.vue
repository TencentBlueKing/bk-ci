<template>
    <detail-container @close="$emit('close')" :title="job.name" :status="job.status">
        <!-- <section v-if="showDebugBtn" class="web-console" :style="{ right: executeCount > 1 ? '390px' : '280px' }">
            <span onclick="startDebug">
                {{$t('pipeline.webConsole')}}
            </span>
        </section> -->
        <job-log
            :plugin-list="pluginList"
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
                const dispatchType = this.job.dispatchType || {}
                return (
                    dispatchType
                    && (['GIT_CI', 'PUBLIC_DEVCLOUD'].includes(dispatchType.buildType) || (dispatchType.buildType?.indexOf('THIRD_PARTY_') > -1 && dispatchType.dockerInfo))
                    && this.modelDetail
                    && this.modelDetail.buildNum === this.modelDetail.latestBuildNum
                )
            },

            downLoadJobLink () {
                const fileName = encodeURI(
                    encodeURI(`${this.stageIndex + 1}-${this.jobIndex + 1}-${this.job.name}`)
                )
                const jobId = this.job.containerHashId
                const { pipelineId, buildId } = this.$route.params
                return `/log/api/user/logs/${this.projectId}/${pipelineId}/${buildId}/download?jobId=${jobId}&fileName=${fileName}`
            },

            pluginList () {
                const startUp = {
                    name: 'Set up job',
                    status: this.job.startVMStatus,
                    id: `startVM-${this.job.id}`,
                    executeCount: this.job.executeCount || 1
                }
                return [startUp, ...this.job.elements]
            },
            executeCount () {
                const executeCountList = this.pluginList.map((plugin) => plugin.executeCount || 1)
                return Math.max(...executeCountList)
            }
        },
  
        methods: {
            startDebug () {
                const vmSeqId = this.job.id
                let url = ''
                const tab = window.open('about:blank')
                const dispatchType = this.job.dispatchType.buildType || 'GIT_CI'
                const buildIdStr = this.buildId ? `&buildId=${this.buildId}` : ''
                url = `/webConsole?pipelineId=${this.pipelineId}&dispatchType=${dispatchType}&vmSeqId=${vmSeqId}${buildIdStr}${this.hashId}`
                tab.location = url
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
</style>
