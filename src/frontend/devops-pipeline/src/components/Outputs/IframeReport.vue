<template>
    <iframe
        class="exec-third-party-report"
        ref="reportIframe"
        allowfullscreen
        :src="indexFileUrl"
    />
</template>

<script>
    export default {
        name: 'iframe-report',
        props: {
            indexFileUrl: {
                type: String,
                default: ''
            }
        },
        mounted () {
            this.$refs.reportIframe.addEventListener('load', () => {
                if (this.$refs.reportIframe?.contentDocument?.body?.scrollHeight) {
                    this.$refs.reportIframe.style.height = `${this.$refs.reportIframe.contentDocument.body.scrollHeight}px`
                }
            })
        }
    }
</script>
<style lang="scss">
.exec-third-party-report {
  width: 100%;
  height: 99%;
  border: 0;
}
</style>
