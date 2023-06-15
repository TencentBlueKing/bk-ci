<template>
    <div class="biz-container bkdevops-history-subpage pipeline-subpages">
        <div class="pipeline-subpages-header">
            <router-view name="header"></router-view>
        </div>
        <router-view class="biz-content" v-bkloading="{ isLoading }"></router-view>
        <portal-target name="artifactory-popup"></portal-target>
    </div>
</template>

<script>
    export default {
        created () {
            this.$store.dispatch('requestProjectDetail', {
                projectId: this.$route.params.projectId
            })
        },
        beforeDestroy () {
            this.$store.commit('pipelines/updateCurPipeline', {})
            this.$store.commit('pipelines/updatePipelineList', [])
        }
    }
</script>

<style lang="scss">
@import "../../scss/conf";
.bkdevops-history-subpage {
  min-height: 100%;
  flex-direction: column;
  .bk-exception {
    position: absolute;
  }
  .pipeline-subpages-header {
    display: flex;
    align-items: center;
    height: 48px;
    background-color: white;
    width: 100%;
    box-shadow: 0 2px 5px 0 rgba(51, 60, 72, 0.03);
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
