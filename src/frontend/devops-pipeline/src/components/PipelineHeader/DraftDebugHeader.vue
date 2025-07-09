<template>
    <div
        v-if="!isLoading"
        class="pipeline-draft-debug-header"
    >
        <pipeline-bread-crumb :pipeline-name="pipelineName">
            <span>
                {{ $t('draftExecRecords') }}
            </span>
        </pipeline-bread-crumb>
    </div>
    <i
        v-else
        class="devops-icon icon-circle-2-1 spin-icon"
        style="margin-left: 20px;"
    />
</template>

<script>
    import { mapActions, mapState } from 'vuex'
    import PipelineBreadCrumb from './PipelineBreadCrumb.vue'
    export default {
        components: {
            PipelineBreadCrumb
        },
        data () {
            return {
                pipelineName: '--',
                isLoading: false
            }
        },
        computed: {
            ...mapState('atom', ['pipelineInfo'])
        },
        created () {
            this.init()
        },
        methods: {
            ...mapActions('atom', [
                'fetchPipelineByVersion'
            ]),
            async init () {
                try {
                    this.isLoading = true
                    const res = await this.fetchPipelineByVersion({
                        ...this.$route.params,
                        version: this.pipelineInfo?.version,
                        archiveFlag: this.$route.query?.archiveFlag
                    })
                    this.pipelineName = res?.modelAndSetting?.model?.name ?? '--'
                } catch (error) {
                    console.error(error)
                } finally {
                    this.isLoading = false
                }
            }
        }
    }
</script>

<style lang="scss">
@import '~@/scss/conf';
.pipeline-draft-debug-header {
    width: 100%;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 24px 0 14px;
    font-size: 14px;
    .pipeline-draft-version {
        display: flex;
        align-items: center;

        .bk-icon {
            margin: 0 8px 0 0;
            color: $primaryColor;
        }
    }
}
</style>
