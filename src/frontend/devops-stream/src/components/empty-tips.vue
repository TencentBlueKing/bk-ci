<template>
    <section class="devops-empty-tips">
        <img :src="noDataSrc" alt="" class="no-data-pic">
        <p class="title">{{ title }}</p>
        <p class="desc">{{ desc }}</p>
        <p class="btns-row">
            <slot name="btns">
                <template v-if="btns.length">
                    <bk-button v-for="(btn, index) of btns" :disabled="btnDisabled" :theme="btn.theme" :size="btn.size" :key="index" @click="btn.handler">
                        {{ btn.text }}
                    </bk-button>
                </template>
            </slot>
        </p>
    </section>
</template>

<script>
    import noData from '@/images/box.png'
    export default {
        props: {
            imgType: {
                type: String,
                default: 'noData'
            },
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
            },
            btnDisabled: {
                type: Boolean,
                default: false
            }
        },
        data () {
            return {
                noDataSrc: '',
                noCollect: ''
            }
        },
        created () {
            this.noDataSrc = noData
        }
    }
</script>

<style lang="postcss">
    @import '@/css/conf';

    .devops-empty-tips {
        width: 913px;
        margin: 0 auto;
        text-align: center;
        .title {
            color: #333C48;
            font-size: 18px;
            line-height: 26px;
        }
        .desc {
            margin-top: 10px;
            margin-bottom: 20px;
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
        .no-data-pic {
            margin-top: 90px;
            margin-bottom: 14px;
            max-width: 320px;
            max-height: 320px;
        }
    }
</style>
