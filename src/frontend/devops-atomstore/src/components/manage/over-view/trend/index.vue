<template>
    <common v-bind="$props"></common>
</template>

<script>
    import common from './common'

    export default {
        components: {
            common
        },

        props: {
            detail: Object,
            type: String
        }
    }
</script>
