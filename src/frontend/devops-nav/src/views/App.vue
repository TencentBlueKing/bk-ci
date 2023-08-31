<template>
    <div :class="AppClass">
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
    import { mapDocumnetTitle } from '@/utils/constants'
    
    export default class App extends Vue {
        @State('fetchError') fetchError
        @State('moduleLoading') moduleLoading

        @Action getAnnouncement
        @Action setAnnouncement
        serviceName: String = ''

        @Watch('fetchError')

        get AppClass (): string {
            return `devops-app ${this.serviceName}-model`
        }

        handleFetchError (e) {
            if (e.status === 503) {
                this.$router.replace('/maintaining')
            }
            this.$bkMessage({
                message: e.message || this.$t('NetworkError'),
                theme: 'error'
            })
        }

        getDocumentTitle (model) {
            return this.$t(mapDocumnetTitle(model)) as string
        }

        async created () {
            this.serviceName = location.href.split('/')[4]
            document.title = this.getDocumentTitle(this.serviceName)
            const announce = await this.getAnnouncement()
            if (announce && announce.id) {
                this.setAnnouncement(announce)
            }
        }
    }
</script>
