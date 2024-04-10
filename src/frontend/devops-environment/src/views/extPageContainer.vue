<template>
    <div class="ext-page-container-wrapper">
        <content-header class="env-header">
            <div slot="left">{{ servideName }}</div>
        </content-header>

        <section class="sub-view-port">
            <template v-if="service">
                <iframe class="environment-view-port-iframe" ref="extensionIframe" :src="iframeUrl" @load="handlePageLoad"></iframe>
            </template>
        </section>
    </div>
</template>

<script>
    import { mapState } from 'vuex'
    export default {
        props: {
            extensionMap: {
                type: Object
            }
        },
        computed: {
            ...mapState('environment', [
                'extensions'
            ]),
            serviceCode () {
                return this.$route.params.serviceCode
            },
            service () {
                return this.extensions.find(ext => ext.serviceCode === this.serviceCode)
            },
            servideName () {
                console.log(this.service, this.extensions)
                return this.service && this.service.serviceName ? this.service.serviceName : ''
            },
            iframeUrl () {
                return this.service ? this.getResUrl(this.service.props.entryResUrl, this.service.baseUrl) : ''
            }
        },
        methods: {
            isAbsoluteURL (url = '') {
                return /^https?:\/\//i.test(url)
            },
            urlJoin (...args) {
                return args.filter(arg => arg).join('/').replace(/([^:]\/)\/+/g, '$1')
            },
            getResUrl (url = 'index.html', baseURL) {
                return this.isAbsoluteURL(url) ? url : this.urlJoin(baseURL, 'static', url)
            },
            handlePageLoad () {
                this.syncData({
                    ...this.$route.params
                })
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
    @import './../scss/conf';

    .ext-page-container-wrapper {
        height: 100%;
        overflow: hidden;
        .env-header {
            display: flex;
            justify-content: space-between;
            padding: 18px 20px;
            width: 100%;
            height: 60px;
            border-bottom: 1px solid $borderWeightColor;
            background-color: #fff;
            box-shadow:0px 2px 5px 0px rgba(51,60,72,0.03);
        }
        .environment-view-port-iframe {
          width: 100%;
          height: 100%;
          border: none;
        }
    }
</style>
