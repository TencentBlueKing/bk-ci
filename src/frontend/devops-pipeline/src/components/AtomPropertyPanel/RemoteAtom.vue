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
            src () {
                return `${AJAX_URL_PIRFIX}/artifactory/resource/bk-plugin-fe/${this.atomCode}/${this.atomVersion}/index.html?projectId=${this.$route.params.projectId}`
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
                'setPipelineEditing'
            ]),
            onLoad () {
                const { baseOS, dispatchType } = this.container
                const containerInfo = { baseOS, dispatchType }
                const currentUserInfo = this.$userInfo || {}
                const atomDisabled = this.disabled || false
                this.loading = false
                const iframe = document.getElementById('atom-iframe').contentWindow
                iframe.postMessage({ atomPropsValue: this.element.data.input, atomPropsModel: this.atomPropsModel.input, containerInfo, currentUserInfo, atomDisabled }, '*')
            },
            receiveMsgFromIframe (e) {
                // if (location.href.indexOf(e.origin) === 0) return
                if (!e.data) return
                if (e.data.atomValue) {
                    this.setPipelineEditing(true)
                    this.$nextTick(this.handleUpdateWholeAtomInput(e.data.atomValue))
                } else if (e.data.isError !== undefined) {
                    this.handleUpdateElement('isError', e.data.isError)
                } else if (e.data.iframeHeight) {
                    this.iframeHeight = parseInt(e.data.iframeHeight)
                }
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
