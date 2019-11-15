<template>
    <div class="inner-header clearfix">
        <div class="inner-header-left">
            <slot name="left"></slot>
        </div>
        <div class="inner-header-right">
            <slot name="right"></slot>
        </div>
    </div>
</template>

<style lang="scss">
    @import './../../scss/conf';

    .inner-header {
        width: 100%;
        height: 60px;
        line-height: 60px;
        padding: 0 20px;
        border-bottom: 1px solid $borderWeightColor;
        box-shadow: 0 2px 5px rgba(0, 0, 0, .03);
        background-color: #fff;
        &-left {
            position: relative;
            float: left;
            width: 50%;
            height: 100%;
        }
        &-right {
            position: relative;
            float: right;
            width: 50%;
            height: 100%;
            text-align: right;
        }
    }
</style>
