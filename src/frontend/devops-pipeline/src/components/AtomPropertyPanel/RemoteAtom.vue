<template>
    <section v-bkloading="{ isLoading: loading }">
        <div class="bk-form bk-form-vertical">
            <iframe
                v-if="src"
                id="atom-iframe"
                ref="iframeEle"
                allowfullscreen
                height="300px"
                :src="src"
                @load="onLoad"
            />
        </div>
        <atom-output :element="element" :atom-props-model="atomPropsModel"></atom-output>
    </section>
</template>

<script>
    import atomMixin from './atomMixin'
    import validMixins from '../validMixins'
    import AtomOutput from './AtomOutput'
    export default {
        name: 'remote-atom',
        components: {
            AtomOutput
        },
        mixins: [atomMixin, validMixins],
        data () {
            return {
                newModel: {},
                loading: true,
                src: 'http://dev.nav.oa.com:8001?projectId=t1'
            }
        },
        mounted () {
            window.addEventListener('message', this.receiveMsgFromIframe)
        },
        methods: {
            onLoad () {
                this.loading = false
                const iframe = document.getElementById('atom-iframe').contentWindow
                iframe.postMessage({ atomPropsValue: this.element.data.input, atomPropsModel: this.atomPropsModel.input }, '*')
            },
            receiveMsgFromIframe (e) {
                if (location.href.indexOf(e.origin) === 0) return
                console.log(e.data, 'top')
                if (!e.data) return
                if (e.data.atomValue) {
                    // Vue.set(this.element.data, 'input', e.data.atomValue)
                    this.handleUpdateWholeAtomInput(e.data.atomValue)
                } else if (e.data.isError) {
                    console.log(e.data)
                } else if (e.data.iframeHeight) {
                    console.log(e.data)
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
