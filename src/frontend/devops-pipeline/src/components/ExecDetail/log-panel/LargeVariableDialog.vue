<template>
    <bk-dialog
        :value="value"
        :title="variableKey"
        :width="960"
        :show-footer="false"
        render-directive="if"
        @value-change="onValueChange"
    >
        <div class="large-variable-dialog">
            <div
                v-if="state === 'loading'"
                class="lv-status"
                role="status"
            >
                {{ $t('loading') }}
            </div>
            <template v-else-if="state === 'ready'">
                <div class="lv-toolbar">
                    <span
                        v-if="length"
                        class="lv-size"
                    >{{ $t('logPanel.largeVariable.approxChars', [length]) }}</span>
                    <bk-button
                        text
                        theme="primary"
                        @click="copy"
                    >
                        {{ $t('logPanel.largeVariable.copy') }}
                    </bk-button>
                </div>
                <pre class="lv-content">{{ content }}</pre>
            </template>
            <div
                v-else
                class="lv-status"
            >
                <span>{{ statusText }}</span>
                <bk-button
                    v-if="state === 'error'"
                    text
                    theme="primary"
                    @click="$emit('retry')"
                >
                    {{ $t('logPanel.largeVariable.retry') }}
                </bk-button>
            </div>
        </div>
    </bk-dialog>
</template>

<script>
    import { computed, getCurrentInstance } from 'vue'
    import { copyToClipboard } from '@/utils/util'

    export default {
        name: 'LargeVariableDialog',
        props: {
            value: { type: Boolean, default: false },
            variableKey: { type: String, default: '' },
            length: { type: Number, default: 0 },
            state: { type: String, default: 'loading' },
            content: { type: String, default: '' },
            errorMessage: { type: String, default: '' }
        },
        emits: ['input', 'retry'],
        setup (props, { emit }) {
            const vm = getCurrentInstance()?.proxy
            const t = (...args) => vm.$t(...args)

            const statusText = computed(() => {
                switch (props.state) {
                    case 'sensitive':
                        return props.content || '******'
                    case 'expired':
                        return t('logPanel.largeVariable.expired')
                    case 'notFound':
                        return t('logPanel.largeVariable.notFound')
                    case 'noPermission':
                        return t('noPermission')
                    case 'error':
                        return props.errorMessage || t('logPanel.largeVariable.loadFailed')
                    default:
                        return ''
                }
            })

            function onValueChange (visible) {
                if (!visible) emit('input', false)
            }

            async function copy () {
                try {
                    await copyToClipboard(props.content)
                    vm.$bkMessage({ theme: 'success', message: t('logPanel.largeVariable.copied') })
                } catch (error) {
                    vm.$bkMessage({ theme: 'error', message: t('copyFailed') })
                }
            }

            return { statusText, onValueChange, copy }
        }
    }
</script>

<style lang="scss" scoped>
.large-variable-dialog {
    min-height: 120px;
}
.lv-toolbar {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    gap: 12px;
    margin-bottom: 8px;
}
.lv-size { color: #979ba5; }
.lv-content {
    max-height: 60vh;
    margin: 0;
    padding: 12px;
    overflow: auto;
    background: #f5f7fa;
    border-radius: 2px;
    color: #313238;
    font-family: Consolas, Menlo, Monaco, monospace;
    font-size: 12px;
    line-height: 20px;
    white-space: pre-wrap;
    word-break: break-all;
}
.lv-status {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    min-height: 120px;
    color: #63656e;
}
</style>
