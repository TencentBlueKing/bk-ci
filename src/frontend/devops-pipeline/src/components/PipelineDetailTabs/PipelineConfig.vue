<template>
    <div class="pipeline-config-wrapper" v-bkloading="{ isLoading }">
        <header class="pipeline-config-header">
            <mode-switch />
            <VersionSideslider
                v-model="activePipelineVersion"
                @change="init"
            />
            <template v-if="!isCurrentVersion">
                <RollbackEntry
                    :version="activePipelineVersion"
                >
                    <i class="devops-icon icon-rollback" />
                    {{$t('rollback')}}
                </RollbackEntry>
                <VersionDiffEntry
                    :version="activePipelineVersion"
                    :release-version="releaseVersion"
                    :current-yaml="pipelineYaml"
                >
                    <i class="devops-icon icon-diff" />
                    {{$t('diff')}}
                </VersionDiffEntry>
            </template>

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
                yamlHighlightBlockMap: {},
                yamlHighlightBlock: []
            }
        },
        computed: {
            ...mapState('pipelines', [
                'pipelineInfo'
            ]),
            ...mapState('atom', [
                'pipeline',
                'pipelineYaml'
            ]),
            ...mapGetters({
                pipelineWithoutTrigger: 'atom/pipelineWithoutTrigger',
                isCodeMode: 'isCodeMode'
            }),
            pipelineType () {
                return this.$route.params.type
            },
            releaseVersion () {
                return this.pipelineInfo?.version
            },
            isCurrentVersion () {
                return this.activePipelineVersion === this.releaseVersion
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
            releaseVersion (version) {
                console.log(123, version, 'pipelineInfo.version')
                this.activePipelineVersion = version
                this.init()
            }
        },
        created () {
            if (this.releaseVersion) {
                this.activePipelineVersion = this.releaseVersion
                this.init()
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
                    console.log('11111', this.activePipelineVersion)
                    if (this.activePipelineVersion) {
                        this.isLoading = true
                        await this.requestPipeline({
                            ...this.$route.params,
                            version: this.activePipelineVersion
                        })
                        // this.yamlHighlightBlockMap = yamlHighlightBlockMap
                        // if (this.isCodeMode) {
                        //     this.yamlHighlightBlock = this.yamlHighlightBlockMap[this.pipelineType]
                        // }
                    }
                } catch (error) {
                    this.$bkMessage({
                        theme: 'error',
                        message: error.message
                    })
                } finally {
                    this.isLoading = false
                }
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
        overflow: auto;
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
        }
    }
</style>
