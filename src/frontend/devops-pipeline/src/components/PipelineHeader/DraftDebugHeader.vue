<template>
    <div class="pipeline-draft-debug-header">
        <pipeline-bread-crumb>
            {{$t('调试纪录')}}
        </pipeline-bread-crumb>
    </div>
</template>

<script>
    import PipelineBreadCrumb from './PipelineBreadCrumb.vue'

    export default {
        components: {
            PipelineBreadCrumb
        }
    }
</script>

<style lang="scss">
.pipeline-draft-debug-header {
    width: 100%;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 24px 0 14px;

}
</style>
