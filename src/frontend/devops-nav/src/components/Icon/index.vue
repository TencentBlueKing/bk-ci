<template>
    <svg :width="size" :height="size" style="fill: currentColor">
        <use v-bind="{ 'xlink:href': `#${name}` }"></use>
    </svg>
</template>

<script lang="ts">
    import Vue from 'vue'
    import { Component, Prop } from 'vue-property-decorator'

    @Component
    export default class SvgIcon extends Vue {
        @Prop()
        name
        @Prop({ default: 32 })
        size
    }
</script>
