<template>
    <section class="devops-empty-tips">
        <h2 class="title">{{ title }}</h2>
        <p class="desc">{{ desc }}</p>
        <p class="btns-row">
            <slot name="btns">
                <button class="bk-button"
                    v-for="(btn, index) of btns"
                    :key="index"
                    :class="[`bk-${btn.type}`, `bk-button-${btn.size}`]"
                    @click="btn.handler">
                    {{ btn.text }}
                </button>
            </slot>
        </p>
    </section>
</template>

<script>
    export default {
        props: {
            title: {
                type: String,
                default: ''
            },
            desc: {
                type: String,
                default: ''
            },
            btns: {
                type: Array,
                default () {
                    return []
                }
            }
        }
    }
</script>

<style lang="scss">
    @import './../../scss/conf';

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
                & + .bk-button {
                    margin-left: 10px;
                }
            }
        }
    }
</style>
