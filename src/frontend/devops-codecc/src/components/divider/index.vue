<template>
    <div class="divider"></div>
</template>

<script>
    export default {
        name: 'divider',
        props: {
            title: {
                type: String,
                default: ''
            },
            dashed: {
                type: Boolean,
                default: false
            }
        },
        data () {
            return {}
        }
    }
</script>

<style lang="postcss">
    .divider {
        display: block;
        clear: both;
        width: 100%;
        /* min-width: 100%; */
        height: 1px;
        margin: 20px 0;
        background: #f0f1f5;
    }
</style>
