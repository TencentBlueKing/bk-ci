<template>
    <section class="devops-empty-tips">
        <h2 class="title">
            {{ title }}
        </h2>
        <p class="desc">
            {{ desc }}
        </p>
        <p class="btns-row">
            <slot />
        </p>
    </section>
</template>

<script lang='ts'>
    import Vue from 'vue'
    import { Component, Prop } from 'vue-property-decorator'

    @Component
    export default class EmptyTips extends Vue {
        @Prop()
        title
        @Prop()
        desc
    }
</script>

<style lang="scss">
    @import '../../assets/scss/conf';

    .devops-empty-tips {
        width: 913px;
        margin: 139px auto 0;
        text-align: center;
        .title {
            margin: 0 0 24px;
            color: #333;
            font-size: 22px;
            font-weight: normal;
        }
        .desc {
            margin-bottom: 28px;
            color: $fontColor;
            font-size: 14px;
        }
        .btns-row {
            font-size: 0;
            .bk-button {
                & + .bk-button,
                & + .empty-btns-item {
                    margin-left: 10px;
                }
            }
        }
    }
</style>
