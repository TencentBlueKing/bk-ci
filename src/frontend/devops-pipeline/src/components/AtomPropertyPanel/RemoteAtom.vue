<template>
    <section v-bkloading="{ isLoading: loading }">
        <div class="bk-form bk-form-vertical">
            <iframe
                v-if="src"
                id="atom-iframe"
                ref="iframeEle"
                allowfullscreen
                :height="iframeHeight"
                :src="src"
                @load="onLoad"
            />
        </div>
        <atom-output :element="element" :atom-props-model="atomPropsModel" :set-parent-validate="() => {}"></atom-output>
    </section>
</template>

<script>
    import { mapActions } from 'vuex'
    import atomMixin from './atomMixin'
    import validMixins from '../validMixins'
    import AtomOutput from './AtomOutput'
    export default {
        name: 'remote-atom',
        components: {
            AtomOutput
        },
        mixins: [atomMixin, validMixins],
        props: {
            atom: Object
        },
        data () {
            return {
                newModel: {},
                loading: true,
                iframeHeight: '300px'
            }
        },
        computed: {
            atomVersion () {
                return this.atom.version || '1.0.0'
            },
            atomCode () {
                return this.atom.atomCode
            },
            pipelineId () {
                return this.$route.params.pipelineId || ''
            },
            src () {
                return `${location.origin}/bk-plugin-fe/${this.atomCode}/${this.atomVersion}/index.html?projectId=${this.$route.params.projectId}&pipelineId=${this.pipelineId}`
            }
        },
        mounted () {
            window.addEventListener('message', this.receiveMsgFromIframe)
        },
        destroyed () {
            setTimeout(() => {
                window.removeEventListener('message', this.receiveMsgFromIframe)
            }, 1000)
        },
        methods: {
            ...mapActions('atom', [
                'setPipelineEditing',
                'getAtomEnvConfig'
            ]),
            async onLoad () {
                const { baseOS, dispatchType } = this.container
                const containerInfo = { baseOS, dispatchType }
                const currentUserInfo = this.$userInfo || {}
                const atomDisabled = this.disabled || false
                const envConf = await this.getEnvConf()
                this.loading = false
                const iframe = document.getElementById('atom-iframe').contentWindow
                iframe.postMessage({
                    atomPropsValue: this.element.data.input,
                    atomPropsModel: this.atomPropsModel.input,
                    atomHashId: this.element.id,
                    containerInfo,
                    currentUserInfo,
                    envConf,
                    atomDisabled,
                    hostInfo: {
                        ...this.$route.params
                    }
                }, '*')
            },
            receiveMsgFromIframe (e) {
                // if (location.href.indexOf(e.origin) === 0) return
                if (!e.data) return
                if (e.data.atomValue) {
                    console.log(e.data, this.element?.id, 'dataFromIframeAtom')
                    // 如果不含有elementId(旧版本的自定义插件)， 或者elementId与当前id相同，则更新
                    if (!e.data.elementId || (e.data.elementId && e.data.elementId === this.element?.id)) {
                        this.setPipelineEditing(true)
                        this.handleUpdateWholeAtomInput(e.data.atomValue)
                    } else {
                        console.log(`nowId: ${this.element.id}, 'fromId: ${e.data.elementId}`)
                    }
                } else if (e.data.isError !== undefined) {
                    this.handleUpdateElement('isError', e.data.isError)
                } else if (e.data.iframeHeight) {
                    this.iframeHeight = parseInt(e.data.iframeHeight)
                }
            },
            async getEnvConf () {
                let env = {}
                try {
                    const atomEnvs = await this.getAtomEnvConfig(this.atomCode) || []
                    atomEnvs.forEach(function (item) {
                        console.log(item, item.fieldName, item.fieldValue)
                        Object.assign(env, { [item.fieldName]: item.fieldValue })
                    })
                } catch (err) {
                    env = {}
                }
                return env
            }
        }
    }
</script>

<style type="scss">
    #atom-iframe {
        width: 100%;
        min-height: 100%;
        border: 0;
    }
</style>
