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
        <Announcement-dialog />
    </div>
</template>

<script lang="ts">
    import Vue from 'vue'
    import { Component, Watch } from 'vue-property-decorator'
    import { State, Action } from 'vuex-class'
    import AnnouncementDialog from '../components/AnnouncementDialog/index.vue'
    
    @Component({
        components: {
            AnnouncementDialog
        }
    })
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

        getDocumentTitle (model) {
            const titlesMap = {
                '': this.$t('documentTitleHome'),
                pipeline: this.$t('documentTitlePipeline'),
                codelib: this.$t('documentTitleCodelib'),
                artifactory: this.$t('documentTitleArtifactory'),
                codecc: this.$t('documentTitleCodecc'),
                experience: this.$t('documentTitleExperience'),
                turbo: this.$t('documentTitleTurbo'),
                repo: this.$t('documentTitleRepo'),
                preci: this.$t('documentTitlePreci'),
                stream: this.$t('documentTitleStream'),
                wetest: this.$t('documentTitleWetest'),
                quality: this.$t('documentTitleQuality'),
                xinghai: this.$t('documentTitleXinghai'),
                bcs: this.$t('documentTitleBcs'),
                job: this.$t('documentTitleJob'),
                environment: this.$t('documentTitleEnvironment'),
                vs: this.$t('documentTitleVs'),
                apk: this.$t('documentTitleApk'),
                monitor: this.$t('documentTitleMonitor'),
                perm: this.$t('documentTitlePerm'),
                ticket: this.$t('documentTitleTicket'),
                store: this.$t('documentTitleStore'),
                metrics: this.$t('documentTitleMetrics')
            }
            return titlesMap[model]
        }

        async created () {
            const model = location.href.split('/')[4]
            document.title = this.getDocumentTitle(model)
            const announce = await this.getAnnouncement()
            if (announce && announce.id) {
                this.setAnnouncement(announce)
            }
        }
    }
</script>
