<template>
    <section class="devops-empty-tips">
        <p v-if="showLock">
            <icon name="locked" size="52" />
        </p>
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
        @Prop({ default: false })
        showLock

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
            margin: 27px 0 30px 24px;
            color: #313238;
            font-size: 20px;
            font-weight: normal;
        }
        .desc {
            margin-bottom: 30px;
            color: #979BA5;
            font-size: 14px;
            font-weight: 400;
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
