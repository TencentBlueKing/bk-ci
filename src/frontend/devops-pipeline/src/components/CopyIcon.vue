<template>
    <i
        class="bk-icon icon-clipboard pointer-events-auto"
        :style="style"
        @click="copyTxt"
    />
</template>
<script>
    import { copyToClipboard } from '@/utils/util'
    import { bkMessage } from 'bk-magic-vue'
    import { defineComponent } from 'vue'
    import { useI18n } from 'vue-i18n-bridge'

    export default defineComponent({
        props: {
            value: {
                type: String,
                default: ''
            }
        },

        setup (props) {
            const { t } = useI18n()
            const style = {
                cursor: 'pointer'
            }

            async function copyTxt () {
                try {
                    await copyToClipboard(props.value)
                    bkMessage({ theme: 'success', message: t('copySuc') })
                } catch (error) {
                    bkMessage({ theme: 'error', message: error.message })
                }
            }

            return {
                style,
                copyTxt
            }
        }
    })
</script>
