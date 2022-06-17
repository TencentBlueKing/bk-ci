<template>
    <div v-bkloading="{ isLoading: loading }">
        <component
            v-if="!loading"
            :is="zyComponent"
            @on-pipeline-delegate="onPipelineDelegate"
            @on-pipeline-update="onPipelineUpdate"
        >
            <template #versionSelect="">
                <VersionSideSlider />
            </template>
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
<style lang="scss">
.zy-bk-container {
    position: relative;

    .inner-header {
        height: 52px;
        line-height: 32px;
        padding: 8px 24px 8px 14px;
        border-bottom: none;
        box-shadow: none;

        .history-bread-crumb {
            .bread-crumb-name {
                font-size: 14px;
                color: rgba(0, 10, 41, 0.4);
            }

            .bread-crumb-comp {
                flex: 1;
            }

            .bread-crumb-item {
                .devops-icon.icon-angle-right:before {
                    content: '/';
                    color: rgba(0, 10, 41, 0.4);
                }
            }
        }

        .inner-header-right {
            margin-right: -10px;
        }

        .more-operation-entry {
            padding-top: 0;

            &:before {
                top: 0;
            }

            .entry-btn {
                z-index: 1;
            }

            .more-operation-dropmenu {
                top: 38px;
                right: 3px;

                & > ul:nth-child(1) li:nth-child(2) {
                    display: none;
                }
            }
        }

    }

    .biz-content {
        padding: 8px 16px;
    }

    .bkdevops-pipeline-edit-wrapper {
        .bk-tab-header,
        .bk-tab-section
        {
            padding: 0;
        }
    }

    .pipeline-execute-preview .execute-preview-content {
        padding: 0;
    }

    .pipeline-execute-preview {
        .execute-detail-option .scroll-wraper {
            padding: 0;
        }
    }
}
</style>
