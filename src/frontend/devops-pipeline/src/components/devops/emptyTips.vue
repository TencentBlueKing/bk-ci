<template>
    <section class="devops-empty-tips">
        <h2 class="title">{{ title }}</h2>
        <p class="desc">{{ desc }}</p>
        <p class="btns-row">
            <slot name="btns">
                <template v-if="Array.isArray(btns) && btns.length > 0">
                    <bk-button v-for="(btn, index) in btns" :key="index" :disabled="btn.disabled" :theme="btn.theme" :size="btn.size" :icon="btn.loading ? 'loading' : ''" @click="btn.handler">
                        {{ btn.text }}
                    </bk-button>
                </template>
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
            font-size: 18px;
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
