<template>
    <svg
        :width="size"
        :height="size"
        style="fill: currentColor"
    >
        <use v-bind="{ 'xlink:href': `#stream-${name}` }" />
    </svg>
</template>

<script>
    export default {
        props: {
            name: String,
            size: {
                type: [Number, String],
                default: 32
            }
        }
    }
</script>
