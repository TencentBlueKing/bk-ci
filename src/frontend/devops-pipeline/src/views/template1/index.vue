<template>
    <div
        class="biz-container pipeline-subpages"
        v-bkloading="{ isLoading }"
    >
        <template v-if="!isLoading">
            <router-view
                v-if="hasViewPermission && isInfoReady"
                class="biz-content"
                :is-enabled-permission="isEnabledPermission"
            >
            </router-view>
            <empty-tips
                v-else
                :title="$t('template.accessDeny.title')"
                :desc="$t('template.accessDeny.desc')"
                show-lock
            >
                <bk-button
                    theme="primary"
                    @click="handleApply"
                >
                    {{ $t('template.accessDeny.apply') }}
                </bk-button>
            </empty-tips>
        </template>
    </div>
</template>

<script>
    import emptyTips from '@/components/template/empty-tips'
    import { SET_PIPELINE_INFO } from '@/store/modules/atom/constants'
    import { handleTemplateNoPermission, TEMPLATE_RESOURCE_ACTION } from '@/utils/permission'
    import { mapActions, mapState } from 'vuex'

    export default {
        components: {
            emptyTips
        },

        data () {
            return {
                isLoading: true,
                isEnabledPermission: false,
                hasViewPermission: true
            }
        },
        computed: {
            ...mapState('atom', ['pipelineInfo']),
            isInfoReady () {
                return this.pipelineInfo?.id === this.$route.params?.templateId
            }
        },
        created () {
            console.log(this.$route.params)
            this.init()
        },
        mounted () {
            this.$updateTabTitle?.()
        },
        beforeDestroy () {
            this.selectPipelineVersion(null)
            this.setPipeline(null)
            this.setPipelineWithoutTrigger(null)
            this.resetAtomModalMap()
            this.$store.commit('atom/resetPipelineSetting', null)
            this.$store.commit(`atom/${SET_PIPELINE_INFO}`, null)
        },

        methods: {
            ...mapActions({
                enableTemplatePermissionManage: 'pipelines/enableTemplatePermissionManage',
                getTemplateHasViewPermission: 'pipelines/getTemplateHasViewPermission',
                requestProjectDetail: 'requestProjectDetail'
            }),
            ...mapActions('atom', [
                'requestTemplateSummary',
                'selectPipelineVersion',
                'setPipeline',
                'setPipelineWithoutTrigger',
                'resetAtomModalMap'
            ]),
            handleApply () {
                const { projectId, templateId } = this.$route.params
                handleTemplateNoPermission({
                    projectId,
                    resourceCode: templateId,
                    action: TEMPLATE_RESOURCE_ACTION.VIEW
                })
            },
            async init () {
                try {
                    this.isLoading = true
                    const { projectId, templateId } = this.$route.params
                    const [enablePermRes, viewPermRes] = await Promise.all([
                        this.enableTemplatePermissionManage(projectId),
                        this.getTemplateHasViewPermission({ projectId, templateId }),
                        this.requestTemplateSummary({
                            projectId,
                            templateId
                        }),
                        this.requestProjectDetail({ projectId })
                    ])
                    this.isEnabledPermission = enablePermRes.data
                    this.hasViewPermission = viewPermRes.data
                    if (!this.hasViewPermission) {
                        await this.handleApply()
                    }
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
    .pipeline-subpages {
        min-height: 100%;
        .biz-content {
            width: 100%;
            height: 100%;
        }
    }
</style>
