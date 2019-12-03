<template>
    <div class="bk-form bk-form-vertical">
        <iframe
            v-if="src"
            id="atom-iframe"
            ref="iframeEle"
            allowfullscreen
            :src="src"
            @load="onLoad"
        />
    </div>
</template>

<script>
    import atomMixin from './atomMixin'
    import validMixins from '../validMixins'
    export default {
        name: 'pushimage-to-thiedrepo',
        mixins: [atomMixin, validMixins],
        data () {
            return {
                newModel: {},
                src: 'http://dev.nav.oa.com:8001'
            }
        },
        mounted () {
            window.addEventListener('message', function (e) {
                if (location.href.indexOf(e.origin) === 0) return
                console.log(e, e.data, 'top')
            })
        },
        methods: {
            onLoad () {
                console.log(55)
                const iframe = document.getElementById('atom-iframe').contentWindow
                iframe.postMessage({ atomValue: this.element.data.input }, '*')
                iframe.postMessage({ atomModel: this.atomPropsModel.input }, '*')
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
