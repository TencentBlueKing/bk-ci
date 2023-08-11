<template>
    <div class="pipeline-config-wrapper" v-bkloading="{ isLoading }">
        <header class="pipeline-config-header">
            <mode-switch v-model="pipelineMode" />
            <span
                class="text-link"
            >
                <logo name="edit-conf" size="16"></logo>
                {{$t('edit')}}
            </span>
        </header>
        <section v-if="pipeline" class="pipeline-model-content">
            <Ace
                v-if="isCodeMode"
                ref="editor"
                lang="yaml"
                height="100%"
                width="100%"
                :value="yaml"
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
    import { mapState, mapActions } from 'vuex'
    import ModeSwitch from '@/components/ModeSwitch'
    import Ace from '@/components/common/ace-editor'
    import TriggerConfig from './TriggerConfig'
    import PipelineModel from './PipelineModel'
    import NotificationConfig from './NotificationConfig'
    import BaseConfig from './BaseConfig'
    import Logo from '@/components/Logo'
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
            Ace,
            Logo
        },
        data () {
            return {
                isLoading: false,
                yaml: '',
                yamlHighlightBlockMap: {},
                yamlHighlightBlock: [],
                pipelineMode: 'codeMode' // 'uiMode'
            }
        },
        computed: {
            ...mapState('atom', [
                'pipeline'
            ]),
            pipelineType () {
                return this.$route.params.type
            },
            isCodeMode () {
                return this.pipelineMode === 'codeMode'
            },
            dynamicComponentConf () {
                switch (this.pipelineType) {
                    case 'pipeline':
                        return {
                            is: PipelineModel,
                            props: {
                                pipeline: this.pipeline
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
            }
        },
        created () {
            this.init()
        },
        methods: {
            ...mapActions('atom', [
                'getPipelineYaml',
                'requestPipeline'
            ]),
            async init () {
                try {
                    this.isLoading = true
                    const [, { yaml, ...yamlHighlightBlockMap }] = await Promise.all([
                        this.requestPipeline(this.$route.params),
                        this.getPipelineYaml(this.$route.params)
                    ])
                    this.yaml = yaml
                    this.yamlHighlightBlockMap = yamlHighlightBlockMap
                    if (this.isCodeMode) {
                        this.yamlHighlightBlock = this.yamlHighlightBlockMap[this.pipelineType]
                    }
                } catch (error) {

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
            margin-bottom: 16px;
            flex-shrink: 0;
            .text-link {
                display: flex;
                align-items: center;
                margin-left: 24px;
                font-size: 14px;
                cursor: pointer;
            }
        }
        .pipeline-model-content {
            flex: 1;
        }
    }
</style>
