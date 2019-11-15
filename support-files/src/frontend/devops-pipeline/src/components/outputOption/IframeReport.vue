<template>
    <div class="report-iframe-content">
        <div :style="{ height: &quot;100%&quot; }">
            <iframe id="iframe-box" allowfullscreen ref="iframeEle" :src="indexFileUrl"></iframe>
        </div>
    </div>
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
