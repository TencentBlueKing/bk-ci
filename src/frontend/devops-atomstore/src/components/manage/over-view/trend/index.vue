<template>
    <component v-bind="$props" :is="type"></component>
</template>

<script>
    import atom from './atom'
    import service from './service'

    export default {
        components: {
            atom,
            service
        },

        props: {
            detail: Object,
            type: String
        }
    }
</script>
