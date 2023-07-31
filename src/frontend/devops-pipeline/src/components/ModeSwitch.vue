<template>
    <div class="bk-button-group">
        <bk-button
            v-for="item in pipelineModes"
            :key="item.id"
            :class="item.cls"
            @click="updateMode(item.id)"
        >
            {{ item.label }}
        </bk-button>
    </div>
</template>

<script>
    export default {
        props: {
            value: {
                type: String,
                default: 'uiMode'
            }
        },
        computed: {
            pipelineModes () {
                return [
                    {
                        label: this.$t('details.codeMode'),
                        disabled: true,
                        id: 'codeMode',
                        cls: this.value === 'codeMode' ? 'is-selected' : ''
                    },
                    {
                        label: this.$t('details.uiMode'),
                        id: 'uiMode',
                        cls: this.value === 'uiMode' ? 'is-selected' : ''
                    }
                ]
            }
        },
        methods: {
            updateMode (mode) {
                this.$emit('input', mode)
            }
        }
    }
</script>
