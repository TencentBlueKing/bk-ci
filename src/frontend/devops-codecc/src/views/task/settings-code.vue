<template>
    <ToolConfigForm :code-message="codeMessage" scenes="manage-edit" :tools="configData" class="form-edit" />
</template>

<script>
    import { mapState } from 'vuex'
    import ToolConfigForm from '@/components/tool-config-form'
    export default {
        components: {
            ToolConfigForm
        },
        data () {
            return {
            }
        },
        computed: {
            ...mapState('task', {
                codeMessage: 'codes'
            }),
            configData () {
                const configData = []
                for (const i in this.codeMessage.toolConfigList) {
                    configData.push(this.codeMessage.toolConfigList[i].toolName)
                }
                return configData
            }
        },
        watch: {
        },
        mounted () {
        },
        created () {
            this.init()
        },
        methods: {
            async init () {
                await this.$store.dispatch('task/getCodeMessage')
            }
        }
    }
</script>

<style lang="postcss" scoped>
</style>
