<template>
    <component
        v-bind="$props"
        :is="type"
    ></component>
</template>

<script>
    import atom from './atom'
    import service from './service'
    import DEVX from './DEVX'

    export default {
        components: {
            atom,
            service,
            DEVX
        },

        props: {
            detail: Object,
            type: String
        }
    }
</script>
