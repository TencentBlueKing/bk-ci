<template>
    <bk-input readonly :value="appName" disabled />
</template>

<script>
    import mixins from '../mixins'
    export default {
        name: 'app-id',
        mixins: [mixins],
        props: {
            appIdKey: {
                type: String,
                default: 'ccAppId'
            },
            appIdName: {
                type: String,
                default: ''
            }
        },
        computed: {
            curProject () {
                return this.$store.state.curProject
            },
            appId () {
                return this.curProject ? this.curProject[this.appIdKey] : ''
            },
            hasAppId () {
                return !!this.appId
            },
            appName () {
                return this.curProject && this.curProject[this.appIdName] ? this.curProject[this.appIdName] : this.appId
            }
        },
        mounted () {
            this.hasAppId && this.handleChange(this.name, this.appId)
        }
    }
</script>
