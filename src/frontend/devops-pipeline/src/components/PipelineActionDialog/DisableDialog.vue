<template>
    <bk-dialog
        width="400"
        :loading="disabling"
        render-directive="if"
        v-model="isShow"
        theme="warning"
        :title="$t('disablePipelineConfirmTips')"
        @confirm="disablePipeline"
        @cancel="handleCancel"
    >
        <div v-if="pacEnable">
            <p>
                {{ $t('disablePacPipelineConfirmDesc') }}
            </p>
            <pre class="disable-pac-code">
                {{ $t('codeConfig') }}
            </pre>
        </div>
        <p v-else>{{ $t('disablePipelineConfirmDesc') }}</p>
        <footer slot="footer">
            <bk-button theme="primary">
                {{ $t('disable') }}
            </bk-button>
            <bk-button>
                {{ $t(pacEnable ? 'close' : 'cancel') }}
            </bk-button>
        </footer>
    </bk-dialog>
</template>

<script>
    export default {
        props: {
            isShow: Boolean,
            pacEnable: Boolean
        },
        data () {
            return {
                disabling: false
            }
        },
        methods: {
            disablePipeline () {
                console.log('disablePipeline')
            },
            handleCancel () {
                this.$emit('update:isShow', false)
            }
        }
    }
</script>

<style lang="scss">
    .disable-pac-code {
        background: #F0F1F5;
        width: 100%;
    }
</style>
