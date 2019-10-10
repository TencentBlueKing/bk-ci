<template>
    <div class="bk-devops-nav-content-header">
        <div class="bk-devops-navcontent-header-left">
            <slot name="left" />
        </div>
        <div class="bk-devops-navcontent-header-right">
            <slot name="right" />
        </div>
    </div>
</template>

<script lang="ts">
    import Vue from 'vue'
    import { Component } from 'vue-property-decorator'

    @Component
    export default class ContentHeader extends Vue {
    }
</script>

<style lang="scss">
    .bk-devops-nav-content-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        width: 100%;
        height: 60px;
        padding: 0 20px;
        border-bottom: 1px solid #dde4eb;
        background-color: #fff;
        box-shadow: 0px 2px 5px 0px rgba(51, 60, 72, .03);
        .bk-devops-navcontent-header-left {
            font-size: 16px;
        }
    }
</style>
