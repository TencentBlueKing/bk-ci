<template>
    <router-view style="width: 100%"></router-view>
</template>

<script>
    import { mapActions } from 'vuex'
    export default {
        watch: {
            '$route.params.projectId': {
                handler (val) {
                    this.fetchData(val)
                },
                immediate: true
            }
        },
        methods: {
            ...mapActions('atom', [
                'fetchContainers',
                'fetchClassify',
                'fetchStageTagList',
                'fetchCommonSetting'
            ]),
            ...mapActions('pipelines', [
                'checkViewManageAuth'
            ]),
            fetchData (projectId) {
                console.log('init', projectId)
                this.fetchContainers({
                    projectCode: projectId
                })
                this.fetchClassify()
                this.fetchStageTagList()
                this.fetchCommonSetting()
                this.checkViewManageAuth({
                    projectId
                })
            }
        }
    }
</script>

<style lang="scss">
  .sub-view-port {
      height: calc(100% - 60px);
      overflow: auto;
  }
</style>
