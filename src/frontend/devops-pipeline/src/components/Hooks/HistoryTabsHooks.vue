<template>
    <div class="build-history-ext-tab-content" v-bkloading="{ isLoading: !loaded }">
        <iframe :src="hookIframeUrl" ref="extensionIframe" @load="handleIframeLoad">
        </iframe>
    </div>
</template>

<script>
    export default {
        name: 'history-tabs-hooks',
        props: {
            tabData: {
                type: Object,
                default: () => ({})
            },
            hookIframeUrl: String
        },
        data () {
            return {
                loaded: false
            }
        },
        watch: {
            tabData (newData) {
                this.syncData(newData)
            }
        },
        methods: {
            handleIframeLoad () {
                this.loaded = true
                this.syncData(this.tabData)
            },
            syncData (data) {
                try {
                    // @ts-ignore
                    this.$refs.extensionIframe.contentWindow.postMessage({
                        action: 'syncCustomData',
                        params: JSON.stringify(data)
                    }, '*')
                } catch (e) {
                    console.warn('can not find extensionIframe')
                }
            }
        }
    }
</script>

<style lang='scss'>
    @import '../../scss/mixins/ellipsis';
    .build-history-ext-tab-content {
        height: 100%;
        > iframe {
            width: 100%;
            height: 100%;
            border: 0;
        }
    }
</style>
