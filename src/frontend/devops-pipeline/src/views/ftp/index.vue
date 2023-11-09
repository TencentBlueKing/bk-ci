<template>
    <div>
        <pipeline-index v-if="!loadPreview"></pipeline-index>
        <preview v-if="loadPreview" v-show="false"></preview>
    </div>
</template>

<script>
    import { bus } from '@/utils/bus'
    import PipelineIndex from '../subpages/index.vue'
    import Preview from '../subpages/preview.vue'

    export default {
        name: 'ftpPipeline',
        components: {
            PipelineIndex,
            Preview
        },
        data () {
            const prefix = 'https://ftp.woa.com/fapi/sdk/prod'

            return {
                loadPreview: false,
                jsUrl: `${prefix}/js/bk-pipeline`,
                cssUrl: `${prefix}/css/bk-pipeline`
            }
        },
        computed: {
            ftpComponent () {
                return PipelineIndex
            }
        },
        created () {
            this.loadScripts()
        },
        beforeRouteLeave (to, from, next) {
            if (from.name === 'ftpPipelinesDetail' || from.name === 'ftpPipelinesEdit' || from.name === 'ftpPipelinesHistory') {
                if (to.name === 'pipelinesPreview') {
                    this.loadPreview = true
                    // 先加载preview页面，待500ms后执行start-execute方法
                    setTimeout(() => {
                        bus.$emit('start-execute')
                        setTimeout(() => {
                            this.loadPreview = false
                        }, 500)
                    }, 500)
                } else if (to.name === 'pipelinesDetail') {
                    this.$router.push({
                        name: 'ftpPipelinesDetail',
                        params: {
                            ...to.params
                        }
                    })
                    setTimeout(() => {
                        this.$forceUpdate()
                    }, 100)
                } else if (to.name === 'pipelinesEdit') {
                    this.$router.push({
                        name: 'ftpPipelinesEdit',
                        params: {
                            ...to.params
                        }
                    })
                    setTimeout(() => {
                        this.$forceUpdate()
                    }, 100)
                } else if (to.name === 'pipelinesHistory') {
                    this.$router.push({
                        name: 'ftpPipelinesHistory',
                        params: {
                            ...to.params
                        }
                    })
                    setTimeout(() => {
                        this.$forceUpdate()
                    }, 100)
                } else {
                    next()
                }
            }
        },
        methods: {
            loadScripts (jsUrl = this.jsUrl, cssUrl = this.cssUrl) {
                if (!jsUrl && !cssUrl) {
                    return
                }

                try {
                    Promise.all([
                        cssUrl && this.importStyle({ href: cssUrl }),
                        jsUrl && this.importJS({ url: jsUrl })
                    ])
                } catch (e) {}
            },
            importStyle ({ type, href, name }, parent = document.head) {
                return new Promise((resolve, reject) => {
                    const link = document.createElement('link')
                    type && (link.type = type)
                    link.rel = 'stylesheet'
                    link.href = href
                    name && (link.setAttribute('data-name', name))
                    link.onload = () => resolve(link)
                    link.onerror = reject
                    parent.appendChild(link)
                })
            },
            importJS ({ url }, parent = document.body) {
                return new Promise((resolve, reject) => {
                    const s = document.createElement('script')
                    s.src = url
                    s.onload = () => {
                        if (window.__ftp_content__) {
                            window.__ftp_content__(this)
                        }
                        resolve(s)
                    }
                    s.onerror = reject
                    parent.appendChild(s)
                })
            }
        }
    }
</script>
