<template>
    <div class="pipeline-config-wrapper" v-bk-loading="{ isLoading }">
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
            <Ace v-if="pipelineMode === 'codeMode'" height="100%" width="100%" />
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
                pipelineMode: 'uiMode'
            }
        },
        computed: {
            ...mapState('atom', [
                'pipeline'
            ]),
            pipelineType () {
                return this.$route.params.type
            },
            dynamicComponentConf () {
                switch (this.pipelineType) {
                    case 'pipelineModel':
                        return {
                            is: PipelineModel,
                            props: {
                                pipeline: this.pipeline
                            }

                        }
                    case 'triggerConf':
                        return {
                            is: TriggerConfig
                        }
                    case 'notification':
                        return {
                            is: NotificationConfig
                        }
                    case 'baseInfo':
                        return {
                            is: BaseConfig

                        }
                    default:
                        return null
                }
            }
        },
        created () {
            this.init()
        },
        methods: {
            ...mapActions('atom', [
                'requestPipeline'
            ]),
            async init () {
                try {
                    this.isLoading = true
                    const res = await this.requestPipeline(this.$route.params)
                    console.log(123, res)
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
