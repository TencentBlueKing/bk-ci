<template>
    <div class="bk-button-group">
        <bk-button
            v-for="item in pipelineModes"
            size="small"
            :key="item.id"
            :class="item.cls"
            @click="updateMode(item.id)"
        >
            {{ item.label }}
        </bk-button>
    </div>
</template>

<script>
    import { mapState, mapActions } from 'vuex'
    export default {
        emit: ['change'],
        computed: {
            ...mapState([
                'pipelineMode'
            ]),
            pipelineModes () {
                return [
                    {
                        label: this.$t('details.codeMode'),
                        disabled: true,
                        id: 'codeMode',
                        cls: this.pipelineMode === 'codeMode' ? 'is-selected' : ''
                    },
                    {
                        label: this.$t('details.uiMode'),
                        id: 'uiMode',
                        cls: this.pipelineMode === 'uiMode' ? 'is-selected' : ''
                    }
                ]
            }
        },
        methods: {
            ...mapActions([
                'updatePipelineMode'
            ]),
            updateMode (mode) {
                this.updatePipelineMode(mode)
                this.$emit('change', mode)
            }
        }
    }
</script>
