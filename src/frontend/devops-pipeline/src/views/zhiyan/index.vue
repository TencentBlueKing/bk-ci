<template>
    <div v-bkloading="{ isLoading: loading }">
        <component
            v-if="!loading"
            :is="zyComponent"
            @on-pipeline-delegate="onPipelineDelegate"
            @on-pipeline-update="onPipelineUpdate"
        >
            <component
                :is="bkComponent"
                ref="pipeline"
                v-bind="$attrs"
                v-on="$listeners"
            />
        </component>
    </div>
</template>

<script>
    import PipelineIndex from '../subpages/index.vue'
    import TemplateEdit from '../template/edit.vue'
    import VersionSideSlider from '@/components/VersionSideslider.vue'

    const COMPONENTS = {
        zyPipelines: PipelineIndex,
        zyTemplateEdit: TemplateEdit
    }

    const ZY_NAMES = {
        zyPipelines: 'DevopsWrap',
        zyTemplateEdit: 'TemplateEditWrap'
    }

    export default {
        name: 'ZyPipeline',
        components: {
            TemplateEdit,
            PipelineIndex,
            VersionSideSlider
        },
        provide () {
            return {
                getZyComponent: this.getZyComponent
            }
        },
        data () {
            const { protocol } = location
            const hostname = /zhiyan.([\w-_]+.)?woa.com$/.test(window.name) ? window.name : 'zhiyan.woa.com'
            const prefix = `${protocol}//${hostname}/cicd_static`

            return {
                zyComponentName: ZY_NAMES[this.$route.name] || 'DevopsWrap',
                zyComponents: {},
                jsUrl: `${prefix}/zhiyan.cicd.devops.umd.js`,
                cssUrl: `${prefix}/zhiyan.cicd.devops.min.css`,
                loading: true
            }
        },
        computed: {
            zyComponent () {
                return this.getZyComponent(ZY_NAMES[this.$route.name] || 'DevopsWrap')
            },
            bkComponent () {
                return COMPONENTS[this.$route.name] || PipelineIndex
            }
        },
        async created () {
            try {
                this.loading = true
                const components = await this.loadComponents()

                this.zyComponents = components || {}
                this.loading = false
            } catch (e) {
                this.zyComponents = {}
                this.loading = false
            }
        },
        async mounted () {
            await this.$nextTick()
        },
        beforeDestroy () {
            this.removeCss()
        },
        beforeRouteLeave (to, from, next) {
            const { name, query, params } = to || {}
            const redirectNames = [
                'pipelinesDetail',
                'pipelinesEdit',
                'pipelinesPreview'
            ]

            if (!redirectNames.includes(name)) {
                return next()
            }

            return next({
                name: name.replace(/^pipelines/, 'zyPipelines'),
                query,
                params
            })
        },
        methods: {
            onPipelineDelegate (action, ...args) {
                if (!this.$refs.pipeline) {
                    return
                }

                this.$refs.pipeline[action](...args)
            },
            onPipelineUpdate (name, value) {
                if (!this.$refs.pipeline) {
                    return
                }

                this.$refs.pipeline[name] = value
            },
            sendMessage (action, params) {
                window.top.postMessage({
                    action,
                    params
                }, '*')
            },
            getZyComponent (name) {
                const className = name === 'DevopsWrap' ? 'zy-bk-container' : ''

                return this.zyComponents[name] || {
                    template: `<div class="${className}"><slot /></div>`
                }
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
            importScript ({ url }, parent = document.body) {
                return new Promise((resolve, reject) => {
                    const s = document.createElement('script')
                    s.src = url
                    s.onload = () => resolve(s)
                    s.onerror = reject
                    parent.appendChild(s)
                })
            },
            removeCss (cssUrl = this.cssUrl) {
                document.querySelectorAll('link').forEach((item, i) => {
                    if (item.href && item.href.includes(cssUrl)) {
                        item.remove()
                    }
                })
            },
            async loadComponents (jsUrl = this.jsUrl, cssUrl = this.cssUrl, lib = 'zhiyan_cicd_devops') {
                if (!jsUrl && !cssUrl) {
                    return
                }

                try {
                    await Promise.all([
                        cssUrl && this.importStyle({ href: cssUrl }),
                        jsUrl && await this.importScript({ url: jsUrl })
                    ])

                    if (jsUrl && lib) {
                        const components = window[lib]

                        return components || {}
                    }
                } catch (e) {}
            }
        }
    }
</script>
