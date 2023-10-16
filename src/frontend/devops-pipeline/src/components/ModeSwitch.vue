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
                'pipelineMode',
                'modeList'
            ]),
            pipelineModes () {
                return this.modeList.map(mode => ({
                    label: this.$t(`details.${mode}`),
                    disabled: true,
                    id: mode,
                    cls: this.pipelineMode === mode ? 'is-selected' : ''
                }))
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
