<template>
    <bk-input readonly :value="appName" disabled />
</template>

<script>
    import mixins from '../mixins'
    export default {
        name: 'cc-app-id',
        mixins: [mixins],
        computed: {
            curProject () {
                return this.$store.state.curProject
            },
            appId () {
                return this.curProject ? this.curProject.cc_app_id : ''
            },
            hasAppId () {
                return !!this.appId
            },
            appName () {
                return this.curProject ? this.curProject.cc_app_name : ''
            }
        },
        mounted () {
            this.hasAppId && this.handleChange(this.name, this.appId)
        }
    }
</script>
