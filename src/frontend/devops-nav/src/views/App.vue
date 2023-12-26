<template>
    <div class="devops-app">
        <div
            v-show="moduleLoading"
            class="bk-loading"
            style="position: absolute;"
        >
            <div class="bk-loading-wrapper">
                <div class="bk-loading1">
                    <div class="point point1" /> <div class="point point2" /> <div class="point point3" /> <div class="point point4" />
                </div> <div class="bk-loading-title" />
            </div>
        </div>
        <router-view />
    </div>
</template>

<script lang="ts">
    import Vue from 'vue'
    import { Watch } from 'vue-property-decorator'
    import { State, Action } from 'vuex-class'
    
    export default class App extends Vue {
        @State('fetchError') fetchError
        @State('moduleLoading') moduleLoading

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
