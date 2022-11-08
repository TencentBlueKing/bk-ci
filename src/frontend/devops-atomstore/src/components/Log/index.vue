<template>
    <section class="pipeline-logs">
        <!-- <div class='options-log'>
            <button class='bk-button bk-button-small showtime-log'
                :class="{'bk-primary': showTime}"
                @click.stop="showTime = !showTime">显示时间</button>
            <a class='bk-button bk-button-small export-log' v-if="showExport" download :href="downloadUrl">导出日志</a>
        </div> -->
        <div ref="logContainer" :buildNo="buildNo"></div>
    </section>
</template>

<script>
    import '@/components/Log/log.min.css'
    export default {
        name: 'pipeline-log',
        props: {
            buildNo: {
                type: String,
                required: true
            },
            buildNum: {
                type: Number,
                required: true
            },
            buildTag: {
                type: String,
                default: ''
            },
            showExport: {
                type: Boolean,
                default: false
            },
            logUrl: {
                type: String,
                required: true
            }
        },
        data () {
            return {
                showTime: false
                // sodaLog: require()
            }
        },
        watch: {
            buildNo (newVal, oldVal) {
                newVal && this.renderLog(newVal)
            },
            buildTag (newVal, oldVal) {
                this.renderLog(this.buildNo)
            },
            showTime (newVal, oldVal) {
                this.renderLog(this.buildNo)
            }
        },
        beforeDestroy () {
            this._destroyLog()
        },
        async mounted () {
            if (!window.SodaLog) {
                await import(
                    /* webpackChunkName: 'log' */
                    '@/components/Log/log.min.js'
                )
            }
            this.SodaLog = window.SodaLog
            this.buildNo && this.renderLog(this.buildNo)
        },
        methods: {
            renderLog (buildNo) {
                this.SodaLog.render(this.$refs.logContainer, `${window.API_URL_PREFIX}/${this.logUrl}`, buildNo, this.showTime, this.buildTag)
            },
            _destroyLog () {
                this.SodaLog.unMount(this.$refs.logContainer)
            }
        }
    }
</script>

<style lang="scss">
    @import '@/assets/scss/conf.scss';

    .pipeline-logs {
        height: 100%;
        position: relative;
        .options-log {
            top: -44px;
            position: absolute;
            font-size: 14px;
            color: $primaryColor;
            right: 20px;
            height: 32px;
        }
        > div {
            height: 100%;
        }
    }
</style>
