<template>
    <div class="pipeline-config-wrapper" v-bkloading="{ isLoading }">
        <header class="pipeline-config-header">
            <mode-switch />
            <VersionSideslider
                v-model="activePipelineVersion"
                ref="versionSideslider"
                @change="handleVersionChange"
            />
            <RollbackEntry
                v-if="canRollBack"
                :version="activePipelineVersion"
                :version-name="activePipelineVersionName"
                :draft-version-name="draftVersionName"
            >
                <i class="devops-icon icon-rollback" />
                {{$t('rollback')}}
            </RollbackEntry>
            <VersionDiffEntry
                v-if="!isCurrentVersion"
                :version="activePipelineVersion"
                :latest-version="latestVersion"
                :current-yaml="pipelineYaml"
            >
                <i class="devops-icon icon-diff" />
                {{$t('diff')}}
            </VersionDiffEntry>
        </header>
        <section class="pipeline-model-content">
            <YamlEditor
                v-if="isCodeMode"
                ref="editor"
                :value="pipelineYaml"
                :highlight-ranges="yamlHighlightBlock"
                read-only
            />

            <component
                v-else-if="dynamicComponentConf"
                v-bind="dynamicComponentConf.props"
                :is="dynamicComponentConf.is"
            />
        </section>

    </div>
</template>

<script>
    import { mapActions, mapState, mapGetters } from 'vuex'
    import ModeSwitch from '@/components/ModeSwitch'
    import YamlEditor from '@/components/YamlEditor'
    import TriggerConfig from './TriggerConfig'
    import PipelineModel from './PipelineModel'
    import NotificationConfig from './NotificationConfig'
    import BaseConfig from './BaseConfig'
    import VersionSideslider from './VersionSideslider'
    import Logo from '@/components/Logo'
    import VersionDiffEntry from './VersionDiffEntry'
    import RollbackEntry from './RollbackEntry'
    export default {
        components: {
            ModeSwitch,
            // eslint-disable-next-line vue/no-unused-components
            PipelineModel,
            // eslint-disable-next-line vue/no-unused-components
            TriggerConfig,
            // eslint-disable-next-line vue/no-unused-components
            NotificationConfig,
            // eslint-disable-next-line vue/no-unused-components
            BaseConfig,
            YamlEditor,
            Logo,
            VersionSideslider,
            VersionDiffEntry,
            RollbackEntry
        },
        data () {
            return {
                isLoading: false,
                yaml: '',
                activePipelineVersion: null,
                activePipelineVersionName: '',
                yamlHighlightBlockMap: {},
                yamlHighlightBlock: []
            }
        },
        computed: {
            ...mapState('pipelines', [
                'pipelineInfo'
            ]),
            ...mapState('atom', [
                'pipelineYaml',
                'pipeline'
            ]),
            ...mapGetters({
                pipelineWithoutTrigger: 'atom/pipelineWithoutTrigger',
                isCodeMode: 'isCodeMode'
            }),
            pipelineType () {
                return this.$route.params.type
            },
            latestVersion () {
                return this.pipelineInfo?.version
            },
            latestVersionName () {
                return this.pipelineInfo?.versionName
            },
            canRollBack () {
                return this.activePipelineVersion !== this.pipelineInfo?.version && !this.pipeline?.canDebug
            },
            isCurrentVersion () {
                return this.activePipelineVersion === this.pipelineInfo?.version
            },
            draftVersionName () {
                return this.$refs?.versionSideslider?.getDraftVersion()
            },
            dynamicComponentConf () {
                switch (this.pipelineType) {
                    case 'pipeline':
                        return {
                            is: PipelineModel,
                            props: {
                                pipeline: this.pipelineWithoutTrigger
                            }
                        }
                    case 'trigger':
                        return {
                            is: TriggerConfig
                        }
                    case 'notice':
                        return {
                            is: NotificationConfig
                        }
                    case 'setting':
                        return {
                            is: BaseConfig

                        }
                    default:
                        return null
                }
            }
        },
        watch: {
            pipelineType (type) {
                this.yamlHighlightBlock = this.yamlHighlightBlockMap[type]
            },
            isCodeMode (val) {
                if (val) {
                    this.yamlHighlightBlock = this.yamlHighlightBlockMap[this.pipelineType] ?? []
                }
            },
            latestVersion (version) {
                this.activePipelineVersion = version
                this.$nextTick(() => {
                    this.init()
                })
            },
            latestVersionName (versionName) {
                this.activePipelineVersionName = versionName
            }
        },
        created () {
            if (this.latestVersion) {
                this.activePipelineVersion = this.latestVersion
                this.$nextTick(() => {
                    this.init()
                })
            }
        },
        methods: {
            ...mapActions('atom', [
                'requestPipeline'
            ]),
            ...mapActions('pipelines', [
                'rollbackPipelineVersion'
            ]),
            async init () {
                try {
                    if (this.activePipelineVersion) {
                        this.isLoading = true
                        const yamlHighlightBlockMap = await this.requestPipeline({
                            ...this.$route.params,
                            version: this.activePipelineVersion
                        })
                        this.yamlHighlightBlockMap = yamlHighlightBlockMap
                        if (this.isCodeMode) {
                            this.yamlHighlightBlock = this.yamlHighlightBlockMap[this.pipelineType]
                        }
                    }
                } catch (error) {
                    this.$bkMessage({
                        theme: 'error',
                        message: error.message
                    })
                } finally {
                    this.isLoading = false
                }
            },
            handleVersionChange (versionId, version) {
                this.activePipelineVersionName = version.versionName
                this.$nextTick(() => {
                    this.init()
                })
            }
        }
    }
</script>

<style lang="scss">
    .pipeline-config-wrapper {
        padding: 24px;
        display: flex;
        height: 100%;
        flex-direction: column;
        overflow: hidden;
        .pipeline-config-header {
            display: flex;
            align-items: center;
            margin-bottom: 24px;
            flex-shrink: 0;
            grid-gap: 16px;
            .text-link {
                display: flex;
                align-items: center;
                font-size: 14px;
                grid-gap: 8px;

                cursor: pointer;
            }
        }
        .pipeline-model-content {
            flex: 1;
            overflow: hidden;
        }
    }
</style>
