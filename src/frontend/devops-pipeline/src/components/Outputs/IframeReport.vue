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
                this.$refs.reportIframe.style.height = `${this.$refs.reportIframe.contentDocument.body.scrollHeight}px`
            })
        }
    }
</script>
<style lang="scss">
.exec-third-party-report {
  width: 100%;
  border: 0;
}
</style>
