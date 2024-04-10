<template>
    <svg :width="size" :height="size">
        <use v-bind="{ 'xlink:href': `#${name}` }"></use>
    </svg>
</template>

<script>
    export default {
        name: 'svg-logo',
        props: {
            name: String,
            size: {
                type: [String, Number],
                default: 24
            }
        }
    }
</script>
