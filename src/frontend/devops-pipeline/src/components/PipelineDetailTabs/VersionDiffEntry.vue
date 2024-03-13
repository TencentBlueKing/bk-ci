<template>
    <span>
        <bk-button
            :text="text"
            :outline="outline"
            :theme="theme"
            @click="initDiff"
        >
            <slot>
                {{ $t('diff') }}
            </slot>
        </bk-button>
        <bk-dialog
            render-directive="if"
            v-model="showVersionDiffDialog"
            header-position="left"
            :draggable="false"
            ext-cls="diff-version-dialog"
            width="90%"
            :title="$t('diff')"
        >
            <div class="diff-version-dialog-content" v-bkloading="{ isLoading: isLoadYaml, color: '#1d1d1d' }">
                <header class="diff-version-header">
                    <VersionSelector
                        ext-cls="dark-theme-select-trigger"
                        ext-popover-cls="dark-theme-select-menu"
                        :show-extension="false"
                        v-model="activeVersion"
                        @change="diffActiveVersion"
                    />

                    <VersionSelector
                        ext-cls="dark-theme-select-trigger"
                        ext-popover-cls="dark-theme-select-menu"
                        :show-extension="false"
                        v-model="currentVersion"
                        @change="diffCurrentVersion"

                    />

                </header>
                <div class="pipeline-yaml-diff-wrapper">
                    <yaml-diff
                        :old-yaml="activeYaml"
                        height="100%"
                        :new-yaml="currentYaml"
                    />
                </div>
            </div>

            <footer slot="footer">
                <bk-button
                    @click="showVersionDiffDialog = false"
                >
                    {{ $t('close') }}
                </bk-button>
            </footer>
        </bk-dialog>
    </span>
</template>

<script>
    import { mapActions } from 'vuex'
    import VersionSelector from '@/components/PipelineDetailTabs/VersionSelector'
    import YamlDiff from '@/components/YamlDiff'
    export default {
        components: {
            YamlDiff,
            VersionSelector
        },
        props: {
            text: {
                type: Boolean,
                default: true
            },
            outline: Boolean,
            theme: {
                type: String,
                default: 'primary'
            },
            version: {
                type: Number,
                required: true
            },
            latestVersion: {
                type: Number,
                required: true
            }
        },
        data () {
            return {
                isLoadYaml: false,
                showVersionDiffDialog: false,
                activeVersion: '',
                currentVersion: '',
                activeYaml: '',
                currentYaml: '',
                pipelineVersionList: []
            }
        },

        methods: {
            ...mapActions('atom', [
                'fetchPipelineByVersion'
            ]),
            ...mapActions('pipelines', [
                'requestPipelineVersionList'
            ]),

            async diffVersion () {
                try {
                    this.isLoadYaml = true
                    this.showVersionDiffDialog = true
                    const [activePipeline, currentPipeline] = await Promise.all([
                        this.fetchPipelineByVersion({
                            projectId: this.$route.params.projectId,
                            pipelineId: this.$route.params.pipelineId,
                            version: this.activeVersion
                        }),
                        this.fetchPipelineByVersion({
                            projectId: this.$route.params.projectId,
                            pipelineId: this.$route.params.pipelineId,
                            version: this.currentVersion
                        })
                    ])

                    if (activePipeline?.yamlSupported && currentPipeline?.yamlSupported) {
                        this.activeYaml = activePipeline.yamlPreview.yaml
                        this.currentYaml = currentPipeline.yamlPreview.yaml
                        return
                    }
                    throw new Error(activePipeline?.yamlInvalidMsg)
                } catch (error) {
                    console.log(error)
                    this.$bkMessage({
                        theme: 'error',
                        message: error.message
                    })
                } finally {
                    this.isLoadYaml = false
                }
            },
            initDiff () {
                this.activeVersion = this.version
                this.currentVersion = this.latestVersion
                this.diffVersion()
            },
            diffActiveVersion (version) {
                this.activeVersion = version
                this.diffVersion()
            },
            diffCurrentVersion (version) {
                this.currentVersion = version
                this.diffVersion()
            }
        }
    }
</script>

<style lang="scss">
    @import "@/scss/select-dark-theme.scss";
    .diff-version-dialog.bk-dialog-wrapper {
        transition: none;
        .bk-dialog {
            transition: all .3s;
            margin: 0 auto;
            top: 10%;
            .bk-dialog-content {
                height: 80vh;
                .bk-dialog-body {
                    height: calc(100% - 100px);
                    .diff-version-dialog-content {
                        display: flex;
                        flex-direction: column;
                        height: 100%;
                        .diff-version-header {
                            flex-shrink: 0;
                        }
                        .pipeline-yaml-diff-wrapper {
                            flex: 1;
                            overflow: hidden;
                        }
                    }
                }
            }
        }
    }
</style>
