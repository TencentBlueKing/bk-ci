<template>
    <div class="pipeline-history-header">
        <pipeline-bread-crumb>
            {{ $t("pipelinesHistory") }}
        </pipeline-bread-crumb>
        <aside class="pipeline-history-right-aside">
            <bk-button
                v-perm="{
                    permissionData: {
                        projectId: $route.params.projectId,
                        resourceType: 'pipeline',
                        resourceCode: $route.params.pipelineId,
                        action: RESOURCE_ACTION.EDIT
                    }
                }"
                @click="goEdit"
            >
                {{ $t("edit") }}
            </bk-button>
            <bk-button
                theme="primary"
                v-perm="{
                    permissionData: {
                        projectId: $route.params.projectId,
                        resourceType: 'pipeline',
                        resourceCode: $route.params.pipelineId,
                        action: RESOURCE_ACTION.EXECUTE
                    }
                }"
                @click="goExecPreview"
            >
                {{ $t("exec") }}

            </bk-button>
            <more-actions />
        </aside>
    </div>
</template>

<script>
    import {
        RESOURCE_ACTION
    } from '@/utils/permission'
    import MoreActions from './MoreActions.vue'
    import PipelineBreadCrumb from './PipelineBreadCrumb.vue'

    export default {
        components: {
            PipelineBreadCrumb,
            MoreActions
        },
        computed: {
            RESOURCE_ACTION () {
                return RESOURCE_ACTION
            }
        },
        methods: {
            goEdit () {
                this.$router.push({
                    name: 'pipelinesEdit'
                })
            },
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
