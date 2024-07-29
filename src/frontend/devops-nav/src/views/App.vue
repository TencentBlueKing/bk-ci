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
            const announce = await this.getAnnouncement()
            if (announce && announce.id) {
                this.setAnnouncement(announce)
            }
        }
    }
</script>
