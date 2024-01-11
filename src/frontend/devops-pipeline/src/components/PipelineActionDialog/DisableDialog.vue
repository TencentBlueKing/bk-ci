<template>
    <bk-dialog
        width="400"
        render-directive="if"
        v-model="value"
        @cancel="handleCancel"
    >
        <div class="disable-pipeline-dialog">
            <i class="bk-icon icon-exclamation disable-pipeline-warning-icon"></i>
            <h3>{{ $t('disablePipelineConfirmTips') }}</h3>
            <p>
                {{ $t(pacEnabled ? 'disablePacPipelineConfirmDesc' : 'disablePipelineConfirmDesc') }}
            </p>
            <pre class="disable-pac-code" v-if="pacEnabled">{{ disablePipelineYaml }}<copyIcon :text="disablePipelineYaml" /></pre>
        </div>
        <footer slot="footer">
            <bk-button :loading="disabling" theme="primary" @click="disablePipeline">
                {{ $t('disable') }}
            </bk-button>
            <bk-button @click="handleCancel">
                {{ $t(pacEnabled ? 'close' : 'cancel') }}
            </bk-button>
        </footer>
    </bk-dialog>
</template>

<script>
    import { mapActions } from 'vuex'
    import CopyIcon from '@/components/copyIcon'

    export default {
        components: {
            CopyIcon
        },
        props: {
            pipelineId: String,
            pipelineName: String,
            value: Boolean,
            pacEnabled: Boolean
        },
        data () {
            return {
                disabling: false,
                disablePipelineYaml: 'disable-pipeline: true'
            }
        },

        methods: {
            ...mapActions('pipelines', ['lockPipeline']),
            async disablePipeline () {
                try {
                    this.disabling = true
                    await this.lockPipeline({
                        projectId: this.$route.params.projectId,
                        pipelineId: this.pipelineId,
                        enable: false
                    })
                    this.$bkMessage({
                        theme: 'success',
                        message: this.$t('disableSuc', [this.pipelineName])
                    })
                    this.$nextTick(() => {
                        this.handleCancel()
                    })
                } catch (error) {
                    this.$bkMessage({
                        theme: 'error',
                        message: error.message || error
                    })
                } finally {
                    this.disabling = false
                }
            },
            handleCancel () {
                this.$emit('input', false)
            }
        }
    }
</script>

<style lang="scss">
.disable-pipeline-dialog {
    text-align: center;
    .disable-pipeline-warning-icon {
        display: inline-flex;
        width: 42px;
        height: 42px;
        background: #FFE8C3;
        color: #FF9C01;
        align-items: center;
        justify-content: center;
        border-radius: 50%;
        font-size: 26px;
    }
    .disable-pac-code {
        position: relative;
        background: #F0F1F5;
        width: 100%;
        margin: 10px 0;
        height: 36px;
        line-height: 36px;
        .icon-clipboard {
            position: absolute;
            right: 10px;
            top: 10px;
        }
    }
}
</style>
