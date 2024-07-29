<template>
    <bk-dialog
        width="400"
        render-directive="if"
        v-model="value"
        footer-position="center"
        @cancel="handleCancel"
    >
        <div class="disable-pipeline-dialog">
            <i
                :class="['bk-icon disable-pipeline-warning-icon', {
                    'icon-exclamation': !lock,
                    'icon-check-1': lock
                }]"
            ></i>
            <h3>{{ $t(lock ? 'enablePipelineConfirmTips' : 'disablePipelineConfirmTips') }}</h3>
            <p v-if="!lock">
                {{ $t(pacEnabled ? 'disablePacPipelineConfirmDesc' : 'disablePipelineConfirmDesc') }}
            </p>
            <p v-else>
                {{ $t(pacEnabled ? 'enablePacPipelineConfirmDesc' : 'enablePipelineConfirmDesc') }}
            </p>
            <pre
                class="disable-pac-code"
                v-if="pacEnabled"
            >{{ disablePipelineYaml }}<copy-icon :value="disablePipelineYaml" /></pre>
        </div>
        <footer slot="footer">
            <bk-button
                v-if="!pacEnabled"
                :loading="disabling"
                theme="primary"
                @click="disablePipeline"
            >
                {{ $t(lock ? 'enable' : 'disable') }}
            </bk-button>
            <bk-button @click="handleCancel">
                {{ $t(pacEnabled ? 'close' : 'cancel') }}
            </bk-button>
        </footer>
    </bk-dialog>
</template>

<script>
    import CopyIcon from '@/components/copyIcon'
    import { mapActions } from 'vuex'

    export default {
        components: {
            CopyIcon
        },
        props: {
            pipelineId: String,
            pipelineName: String,
            value: Boolean,
            pacEnabled: Boolean,
            lock: Boolean
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
                        enable: this.lock
                    })
                    this.$bkMessage({
                        theme: 'success',
                        message: this.$t(this.lock ? 'enableSuc' : 'disableSuc', [this.pipelineName]),
                        limit: 1
                    })
                    this.$nextTick(() => {
                        this.handleCancel()
                        this.$emit('done', this.lock)
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
                this.$emit('close')
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
        &.icon-check-1 {
            background: #e5f6ea;
            color: #3fc06d;
        }
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
