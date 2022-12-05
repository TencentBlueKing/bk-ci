<template>
    <router-view style="width: 100%"></router-view>
</template>

<script>
    import { mapActions } from 'vuex'
    export default {
        watch: {
            '$route.params.projectId' (val) {
                this.fetchData()
            }
        },
        mounted () {
            this.fetchData()
        },
        methods: {
            ...mapActions('atom', [
                'fetchContainers',
                'fetchClassify',
                'fetchStageTagList',
                'fetchCommonSetting'
            ]),
            fetchData () {
                const projectCode = this.$route.params.projectId
                this.fetchContainers({
                    projectCode
                })
                this.fetchClassify()
                this.fetchStageTagList()
                this.fetchCommonSetting()
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
