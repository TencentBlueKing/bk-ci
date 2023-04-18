<template>
    <div class="pipeline-history-header">
        <pipeline-bread-crumb>
            {{ $t("pipelinesHistory") }}
        </pipeline-bread-crumb>
        <aside class="pipeline-history-right-aside">
            <router-link :to="editRouteName">
                <bk-button>{{ $t("edit") }}</bk-button>
            </router-link>
            <bk-button theme="primary" @click="goExecPreview">
                {{ $t("exec") }}
            </bk-button>
            <more-actions />
        </aside>
    </div>
</template>

<script>
    import PipelineBreadCrumb from './PipelineBreadCrumb.vue'
    import MoreActions from './MoreActions.vue'

    export default {
        components: {
            PipelineBreadCrumb,
            MoreActions
        },
        computed: {
            editRouteName () {
                return { name: 'pipelinesEdit' }
            }
        },
        methods: {
            goExecPreview () {
                this.$router.push({
                    name: 'pipelinesPreview'
                })
            }
        }
    }
</script>

<style lang="scss">
.pipeline-history-header {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px 0 14px;
  .pipeline-history-right-aside {
    flex-shrink: 0;
    display: grid;
    align-items: center;
    grid-gap: 10px;
    grid-auto-flow: column;
  }
}
</style>
