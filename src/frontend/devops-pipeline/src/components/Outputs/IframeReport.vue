<template>
    <section :class="['iframe-report-section', {
        'pipeline-report-full-screen': fullScreenView
    }]">
        <div class="pipeline-exec-report-header">
            <span class="pipeline-exec-report-header-name">
                <i :class="`devops-icon icon-${reportIcon}`" />
                {{ reportName }}
            </span>
            <bk-button
                text
                theme="primary"
                @click="toggleFullScreen"
            >
                <span class="full-screen-toggler">
                    <i :class="`bk-icon icon-${fullScreenView ? 'un-' : ''}full-screen`"></i>
                    {{ $t(fullScreenView ? 'exitFullscreen' : 'fullScreenView') }}
                </span>
            </bk-button>
        </div>
        <div class="pipeline-exec-report-content">
            <iframe
                class="exec-third-party-report"
                ref="reportIframe"
                allowfullscreen
                :src="indexFileUrl"
            />
        </div>
    </section>
</template>

<script>
    export default {
        name: 'iframe-report',
        props: {
            reportIcon: {
                type: String,
                default: 'order'
            },
            reportName: {
                type: String,
                default: ''
            },
            indexFileUrl: {
                type: String,
                default: ''
            }
        },
        data () {
            return {
                fullScreenView: false
            }
        },
        mounted () {
            this.$refs.reportIframe.addEventListener('load', () => {
                if (this.$refs.reportIframe?.contentDocument?.body?.scrollHeight) {
                    this.$refs.reportIframe.style.height = `${this.$refs.reportIframe.contentDocument.body.scrollHeight}px`
                }
            })
        },
        methods: {
            toggleFullScreen () {
                this.fullScreenView = !this.fullScreenView
            }
        }
    }
</script>
<style lang="scss">

    .iframe-report-section {
        display: flex;
        flex-direction: column;
        flex: 1;
        &.pipeline-report-full-screen {
            position: fixed;
            width:100vw;
            height: 100vh;
            left:0;
            top: 0;
            z-index: 999;
        }
        .pipeline-exec-report-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            height: 48px;
            padding: 0 24px;
            background: #FAFBFD;
            flex-shrink: 0;
            .full-screen-toggler {
                display: grid;
                align-items: center;
                grid-gap: 6px;
                grid-auto-flow: column;
            }
        }
        .pipeline-exec-report-content {
            flex: 1;
            background: white;
            .exec-third-party-report {
                width: 100%;
                height: 100%;
                flex: 1;
                border: 0;
            }
        }
    }
</style>
