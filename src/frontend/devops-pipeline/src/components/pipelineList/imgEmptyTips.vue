<template>
    <section class="devops-empty-tips">
        <img v-if="imgType !== 'noCollect'" :src="noDataSrc" alt="" class="no-data-pic">
        <img v-if="imgType === 'noCollect'" :src="noCollect" alt="" class="no-collect-pic">
        <p class="title">{{ title }}</p>
        <p class="desc">{{ desc }}</p>
        <p class="btns-row">
            <slot name="btns">
                <template v-if="btns.length">
                    <bk-button
                        v-perm="permissionData ? permParams : {}"
                        v-bind="btnParams"
                        v-for="(btn, index) of btns"
                        :theme="btn.theme"
                        :size="btn.size"
                        :key="index"
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
            },
            hasPermission: {
                type: Boolean
            },
            disablePermissionApi: {
                type: Boolean
            },
            permissionData: {
                type: Object
            }
        },
        data () {
            return {
                noDataSrc: '',
                noCollect: '',
                permParams: {}
            }
        },
        computed: {
            btnParams () {
                return !this.permissionData
                    ? {
                        disabled: this.btnDisabled
                    }
                    : {}
            }
        },
        created () {
            this.noDataSrc = noData
            this.noCollect = require(`../../images/${this.$i18n.locale}-no-collect.png`)
            
            if (this.permissionData) this.permParams.permissionData = this.permissionData
            if (this.hasPermission !== 'undefined') this.permParams.hasPermission = this.hasPermission
            if (this.disablePermissionApi !== 'undefined') this.permParams.disablePermissionApi = this.disablePermissionApi
        }
    }
</script>

<style lang="scss">
    @import './../../scss/conf';

    .devops-empty-tips {
        text-align: center;
        .title {
            color: #333C48;
            font-size: 18px;
            line-height: 26px;
            // font-weight: bold;
        }
        .desc {
            margin-top: 10px;
            margin-bottom: 20px;
            color: $fontWeightColor;
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
        .no-collect-pic {
            margin-top: 40px;
            margin-bottom: 24px;
            width: 320px;
            height: 320px;
        }
    }
</style>
