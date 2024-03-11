<template>
    <div v-bkloading="{ isLoading }" class="biz-container bkdevops-history-subpage pipeline-subpages">
        <div class="pipeline-subpages-header">
            <router-view name="header" :update-pipeline="init"></router-view>
        </div>
        <router-view class="biz-content"></router-view>
        <portal-target name="artifactory-popup"></portal-target>
    </div>
</template>

<script>
    import { mapActions, mapState } from 'vuex'
    import { SET_PIPELINE_INFO } from '@/store/modules/atom/constants'

    export default {
        data () {
            return {
                isLoading: false
            }
        },
        computed: {
            ...mapState('atom', [
                'activePipelineVersion',
                'editfromImport',
                'pipelineInfo'
            ]),
            pipelineId () {
                return this.pipelineInfo?.pipelineId
            }
        },
        watch: {
            pipelineId: {
                handler (id) {
                    if (id) {
                        if (this.activePipelineVersion?.version === this.pipelineInfo?.releaseVersion) {
                            this.init()
                        } else {
                            this.selectPipelineVersion({
                                version: this.pipelineInfo?.releaseVersion
                            })
                        }
                    }
                },
                immediate: true
            }
        },
        created () {
            this.$store.dispatch('requestProjectDetail', {
                projectId: this.$route.params.projectId
            })
        },
        beforeDestroy () {
            this.setPipeline(null)
            this.setPipelineWithoutTrigger(null)
            this.setPipelineYaml('')
            this.selectPipelineVersion(null)
            this.$store.commit('atom/resetPipelineSetting', null)
            this.$store.commit(`atom/${SET_PIPELINE_INFO}`, null)
            this.$store.commit('pipelines/updatePipelineList', [])
        },
        methods: {
            ...mapActions('atom', [
                'requestPipeline',
                'setPipeline',
                'setPipelineYaml',
                'selectPipelineVersion',
                'setPipelineWithoutTrigger'
            ]),
            async init () {
                try {
                    const version = this.activePipelineVersion?.version ?? this.pipelineInfo?.releaseVersion
                    if (version) {
                        this.isLoading = true
                        await this.requestPipeline({
                            ...this.$route.params,
                            version
                        })
                    }
                } catch (error) {
                    this.$bkMessage({
                        theme: 'error',
                        message: error.message
                    })
                } finally {
                    this.isLoading = false
                }
            }
        }
    }
</script>

<style lang="scss">
@import "../../scss/conf";
.bkdevops-history-subpage {
  min-height: 100%;
  flex-direction: column;
  background: #F5F7FA;
  .pipeline-subpages-header {
    display: flex;
    align-items: center;
    height: 48px;
    background-color: white;
    width: 100%;
    box-shadow: 0 2px 5px 0 rgba(51, 60, 72, 0.03);
    border-bottom: 1px solid #DCDEE5;
  }
}

.bkdevops-pipeline-tab-card {
  // display: flex;
  // overflow: hidden;
  // flex-direction: column;
  min-height: 100%;
  border: 0;
  background-color: transparent;
  &-setting {
    font-size: 18px;
    display: flex;
    align-items: center;
    height: 100%;

    .devops-icon {
      color: $fontLighterColor;
      padding-left: 16px;
      cursor: pointer;
      &:hover,
      &.active {
        color: $primaryColor;
      }
    }
  }
  .bk-tab-header {
    background: transparent;
    background-image: none !important;
    .bk-tab-label-wrapper .bk-tab-label-list .bk-tab-label-item {
      min-width: auto;
      padding: 0;
      margin-right: 30px;
      text-align: left;
      font-weight: bold;
      &.active {
        color: $primaryColor;
        background: transparent;
      }
    }
  }
  .bk-tab-section {
    width: 100%;
    min-height: calc(100% - 60px);
    padding: 0;
    margin-top: 10px;
    flex: 1;
    // overflow: hidden;
    .bk-tab-content {
      height: 100%;
      display: flex;
      flex-direction: column;
    }
  }
}
</style>
