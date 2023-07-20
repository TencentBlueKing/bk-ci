<template>
    <div class="pipeline-preview-header">
        <pipeline-bread-crumb>
            <span class="build-num-switcher-wrapper">
                {{ $t("details.execPreview") }}
            </span>
        </pipeline-bread-crumb>
        <aside class="pipeline-preview-right-aside">
            <bk-button
                :disabled="executeStatus"
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
                {{ $t("cancel") }}
            </bk-button>
            <bk-button
                theme="primary"
                :disabled="executeStatus"
                :loading="executeStatus"
                v-perm="{
                    permissionData: {
                        projectId: $route.params.projectId,
                        resourceType: 'pipeline',
                        resourceCode: $route.params.pipelineId,
                        action: RESOURCE_ACTION.EXECUTE
                    }
                }"
                @click="handleClick"
            >
                {{ $t("exec") }}
            </bk-button>
        </aside>
    </div>
</template>

<script>
    import { bus } from '@/utils/bus'
    import {
        RESOURCE_ACTION
    } from '@/utils/permission'
    import { mapState } from 'vuex'
    import PipelineBreadCrumb from './PipelineBreadCrumb'
    export default {
        components: {
            PipelineBreadCrumb
        },
        computed: {
            ...mapState('atom', ['executeStatus', 'execDetail']),
            RESOURCE_ACTION () {
                return RESOURCE_ACTION
            }
        },
        methods: {
            handleClick () {
                bus.$emit('start-execute')
            },
            goEdit () {
                this.$router.back()
            }
        }
    }
</script>

<style lang="scss">
.pipeline-preview-header {
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px 0 14px;
  .build-num-switcher-wrapper {
    display: grid;
    grid-auto-flow: column;
    grid-gap: 6px;
  }
  .pipeline-preview-right-aside {
    display: grid;
    grid-gap: 10px;
    grid-auto-flow: column;
  }
}
</style>
