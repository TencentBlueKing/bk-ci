<template>
    <svg
        :width="size"
        :height="size"
        style="fill: currentColor"
    >
        <use v-bind="{ 'xlink:href': `#color-logo-${name}` }" />
    </svg>
</template>

<script lang="ts">
    import Vue from 'vue'
    import { Component, Prop } from 'vue-property-decorator'

    @Component
    export default class SvgLogo extends Vue {
        @Prop()
        name
        @Prop({ default: 32 })
        size
    }
</script>
