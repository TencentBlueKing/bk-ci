<template>
    <bk-popover style="font-size: 0;" theme="light" trigger="click">
        <img class="pipeline-exec-badge" :src="badgeImageUrl" />
        <section class="badge-link-content" slot="content">
            <span @click="copy('markdownLink')" class="pointer">
                {{ $t('copyBadgeMarkdownLink') }}
                <i class="bk-icon icon-info-circle" />
            </span>
            <span @click="copy('picLink')" class="pointer">
                {{ $t('copyBadgePicLink') }}
            </span>
        </section>
    </bk-popover>
</template>

<script>
    export default {
        props: {
            projectId: {
                type: String,
                required: true
            },
            pipelineId: {
                type: String,
                required: true
            }
        },
        computed: {
            badgeImageUrl () {
                const { projectId, pipelineId } = this
                if (!projectId || !pipelineId) {
                    return ''
                }
                return `${BADGE_URL_PREFIX}/process/api/external/pipelines/projects/${projectId}/${pipelineId}/badge?X-DEVOPS-PROJECT-ID=${projectId}`
            }
        },
        methods: {
            copy (ref) {
                try {
                    window.navigator.clipboard.writeText(this.badgeImageUrl)
                    this.$bkMessage({
                        theme: 'success',
                        message: this.$t('copySuc')
                    })
                } catch (error) {
                    console.log(error)
                }
            }
        }
    }
</script>

<style lang="scss">
    .pipeline-exec-badge {
        cursor: pointer;
    }
    .badge-link-content {
        display: grid;
        grid-gap: 6px;
        grid-auto-rows: 32px;
        width: 168px;
        align-items: stretch;
        > span {
            line-height: 32px;
            color: #63656E;
            &:hover {
                background: #F5F7FA;
            }
        }
    }
</style>
