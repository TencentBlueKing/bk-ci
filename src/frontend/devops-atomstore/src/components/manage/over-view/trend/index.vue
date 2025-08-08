<template>
    <component
        v-bind="$props"
        :is="type"
    ></component>
</template>

<script>
    import atom from './atom'

    export default {
        components: {
            atom
        },

        props: {
            detail: Object,
            type: String
        }
    }
</script>
