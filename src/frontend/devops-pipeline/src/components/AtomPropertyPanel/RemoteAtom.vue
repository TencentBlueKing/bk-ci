<template>
    <section v-bkloading="{ isLoading: loading }">
        <form-field v-if="atomPropsModel.config && atomPropsModel.config.pauseAsReview" :key="'pauseReviewers'" :desc="reviewObj.desc" :desc-link="reviewObj.descLink" :desc-link-text="reviewObj.descLinkText" :required="reviewObj.required" :label="reviewObj.label" :is-error="errors.has('pauseReviewers')" :error-msg="errors.first('pauseReviewers')">
            <component
                :is="reviewObj.component"
                :name="'pauseReviewers'"
                v-bind="reviewObj"
                :value="element['pauseReviewers']"
                :container="container"
                :element="element"
                :is-new-atom="true"
                :atom-value="element"
                v-validate.initial="Object.assign({ required: true })"
                :disabled="isPause"
                :handle-change="handleUpdateElement"
                :get-atom-key-modal="getAtomKeyModal"
                :placeholder="getPlaceholder(reviewObj, element)"></component>
        </form-field>
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
            if (this.atomPropsModel.config && this.atomPropsModel.config.pauseAsReview) {
                this.atomPropsModel.input['pauseReviewers'] = this.element.pauseReviewers || []
            }
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
                    this.setPipelineEditing(true)
                    this.$nextTick(this.handleUpdateWholeAtomInput(e.data.atomValue))
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
