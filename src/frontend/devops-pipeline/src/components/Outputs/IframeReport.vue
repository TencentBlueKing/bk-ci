<template>
    <iframe
        class="exec-third-party-report"
        allowfullscreen ref="iframeEle"
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
        }
    }
</script>
<style lang="scss">
    .exec-third-party-report {
        width: 100%;
        height: 100%;
        border: 0;
    }
</style>
