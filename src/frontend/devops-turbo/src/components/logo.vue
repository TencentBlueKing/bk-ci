<template>
    <svg
        :width="size"
        :height="size"
        style="fill: currentColor"
    >
        <use v-bind="{ 'xlink:href': `#${prefix}${name}` }" />
    </svg>
</template>

<script>
    export default {
        props: {
            name: String,
            size: {
                type: Number,
                default: 12.5
            },
            prefix: {
                type: String,
                default: 'icon-'
            }
        }
    }
</script>
