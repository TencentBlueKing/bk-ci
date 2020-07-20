<template>
    <div class="devops-accordion">
        <slot />
    </div>
</template>

<script lang="ts">
    import Vue from 'vue'
    import Component from 'vue-class-component'
    @Component
    export default class Accordion extends Vue {
    
    }
</script>

<style lang="scss">
    @import '../../assets/scss/conf';
    .devops-accordion {
        border: 1px solid $borderWeightColor;
        border-radius: 2px;
        overflow: hidden;
        margin-bottom: 20px;
        background: white;
        -webkit-box-shadow: 0px 3px 6px rgba(0, 0, 0, 0.03);
        box-shadow: 0px 3px 6px rgba(0, 0, 0, 0.03);

        &:first-child {
            border-top: 0;
        }
    }
</style>
