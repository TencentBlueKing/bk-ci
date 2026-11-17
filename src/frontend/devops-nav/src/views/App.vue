<template>
    <div
        class="devops-app"
        v-bkloading="{ isLoading: moduleLoading }"
    >
        <router-view />
    </div>
</template>

<script lang="ts">
    import Vue from 'vue'
    import { Component, Watch } from 'vue-property-decorator'
    import { Action, State } from 'vuex-class'
    
    @Component()
    export default class App extends Vue {
        @State fetchError
        @State moduleLoading

        @Action getAnnouncement
        @Action setAnnouncement
        @Action getPlatformPreData
        @Action checkConvention

        @Watch('fetchError')
        handleFetchError (e) {
            if (e.status === 503) {
                this.$router.replace('/maintaining')
            }
            this.$bkMessage({
                message: e.message || this.$t('NetworkError'),
                theme: 'error'
            })
        }

        async created () {
            this.getPlatformPreData()
            try {
                await this.checkConvention(this.$i18n.locale)
            } catch (e) {
                // status 已置为 error（fail-closed），提示用户可在页面重试
                this.$bkMessage({
                    message: (e && e.message) || this.$t('convention.loadErrorDesc'),
                    theme: 'error'
                })
            }
            const announce = await this.getAnnouncement()
            if (announce && announce.id) {
                this.setAnnouncement(announce)
            }
        }
    }
</script>
