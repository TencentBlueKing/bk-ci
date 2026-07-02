<template>
    <component
        v-bind="$props"
        :is="type"
    ></component>
</template>

<script>
    import atom from './atom'
    import service from './service'
    import devx from './DEVX'

    export default {
        components: {
            atom,
            service,
            devx
        },

        props: {
            detail: Object,
            type: String
        }
    }
</script>
