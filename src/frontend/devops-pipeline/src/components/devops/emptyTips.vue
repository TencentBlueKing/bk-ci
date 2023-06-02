<template>
    <section class="devops-empty-tips">
        <p v-if="showLock">
            <logo name="locked" size="52" />
        </p>
        <h2 class="title">{{ title }}</h2>
        <p class="desc">{{ desc }}</p>
        <p class="btns-row">
            <slot name="btns">
                <template v-if="Array.isArray(btns) && btns.length > 0">
                    <bk-button
                        v-for="(btn, index) in btns"
                        v-perm="btn.isCheckPermission ? {
                            permissionData: btn.permissionData
                        } : {}"
                        v-bind="btn.isCheckPermission ? {} : { disabled: btn.disabled }"
                        :key="index"
                        :theme="btn.theme"
                        :size="btn.size"
                        :icon="btn.loading ? 'loading' : ''"
                        @click="btn.handler"
                    >
                        {{ btn.text }}
                    </bk-button>
                </template>
            </slot>
        </p>
    </section>
</template>

<script>
    import Logo from '@/components/Logo'
    export default {
        components: {
            Logo
        },
        props: {
            showLock: Boolean,
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
            margin: 27px 0 30px 0;
            color: #333;
            font-size: 20px;
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
