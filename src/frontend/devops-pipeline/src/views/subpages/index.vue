<template>
    <div
        class="biz-container bkdevops-history-subpage pipeline-subpages"
        v-bkloading="{ isLoading }"
    >
        <template v-if="isInfoReady">
            <div class="pipeline-subpages-header">
                <router-view
                    name="header"
                    :is-switch-pipeline="isLoading"
                ></router-view>
            </div>
            <router-view class="biz-content"></router-view>
        </template>
        <portal-target name="artifactory-popup"></portal-target>
    </div>
</template>

<script>
    import { SET_PIPELINE_INFO } from '@/store/modules/atom/constants'
    import { RESOURCE_ACTION, PROJECT_RESOURCE_ACTION } from '@/utils/permission'
    import { mapActions, mapState } from 'vuex'
    import { SET_PROJECT_PERM } from '@/store/modules/common/constants'

    export default {
        data () {
            return {
                isLoading: false
            }
        },
        computed: {
            ...mapState('atom', ['pipelineInfo']),
            isInfoReady () {
                return this.pipelineInfo && this.pipelineInfo.pipelineId === this.$route.params?.pipelineId
            }
        },
        watch: {
            '$route.params.pipelineId': {
                handler () {
                    this.$nextTick(this.fetchPipelineInfo)
                },
                immediate: true
            }
        },
        async created () {
            this.$store.dispatch('requestProjectDetail', {
                projectId: this.$route.params.projectId
            })
            try {
                const { data } = await this.$store.dispatch('common/validatePermission', {
                    projectId: this.$route.params.projectId,
                    resourceType: 'project',
                    resourceCode: this.$route.params.pipelineId,
                    actionList: [PROJECT_RESOURCE_ACTION.ARCHIVED]
                })
                this.$store.commit(`common/${SET_PROJECT_PERM}`, data[PROJECT_RESOURCE_ACTION.ARCHIVED])
            } catch (error) {
                console.log(error)
            }
        },
        beforeDestroy () {
            this.setPipeline(null)
            this.setPipelineWithoutTrigger(null)
            this.setPipelineYaml('')
            this.selectPipelineVersion(null)
            this.$store.commit('atom/resetPipelineSetting', null)
            this.$store.commit(`atom/${SET_PIPELINE_INFO}`, null)
        },
        methods: {
            ...mapActions('atom', [
                'setPipeline',
                'setPipelineYaml',
                'selectPipelineVersion',
                'setPipelineWithoutTrigger',
                'requestPipelineSummary',
                'resetAtomModalMap'
            ]),
            async fetchPipelineInfo () {
                try {
                    this.isLoading = true
                    const params = {
                        ...this.$route.params,
                        ...this.$route.query
                    }
                    await this.requestPipelineSummary(params)
                } catch (error) {
                    this.handleError(error, {
                        projectId: this.$route.params.projectId,
                        resourceCode: this.$route.params.pipelineId,
                        action: RESOURCE_ACTION.VIEW
                    })
                    if (error?.code !== 403) {
                        this.$router.replace({
                            name: 'PipelineManageList'
                        })
                    }
                    return false
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
