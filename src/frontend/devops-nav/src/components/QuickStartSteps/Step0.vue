<template>
    <div>
        <header class="setting-title">
            <span class="setting-title-main-text">选择配置选项</span>
            <span class="devops-desc setting-title-desc">你可以在蓝盾DevOps平台上选择一个Demo代码库快速配置自己的交付流水线</span>
        </header>
        <div class="setting-option selected disabled">
            <label class="bk-form-checkbox">
                <bk-checkbox
                    :true-value="settingOptions.value"
                    false-value="no"
                    :checked="true"
                >
                    是否启用白名单
                </bk-checkbox>

                <h3>{{ settingOptions.title }}</h3>
                <p>{{ settingOptions.desc }}</p>
            </label>
        </div>
    </div>
</template>

<script lang="ts">
    import Vue from 'vue'
    import { Component } from 'vue-property-decorator'
    @Component
    export default class Step0 extends Vue {
        settingOptions: object = {
          title: '快速配置属于自己的流水线',
          desc: '说明：创建并管理你自己的流水线',
          value: 'quick',
          disabled: true
        }
    }
</script>

<style lang="scss">
    @import '../../assets/scss/conf';
    .setting-title {
        &-main-text {
            font-size: 16px;
            color: $fontWeightColor;
        }
        &-desc {
            margin-left: 10px;
            font-size: 12px;
        }
    }
    .setting-option {
        box-shadow: 5px 0 10px rgba(0, 0, 0, 0.05);
        margin-top: 10px;
        border: 1px solid $borderColor;
        &.selected {
            background-color: $primaryLightColor;
            border-color: #c1ddfe;
        }
        &.disabled {
            pointer-events: none;
        }
        label {
            display: flex;
            height: 70px;
            padding: 0 0 0 21px;
            align-items: center;
            > h3 {
                color: $fontBoldColor;
                width: 240px;
                font-size: 14px;
            }
            > p {
                font-size: 12px;
            }
        }
    }
    footer {
        margin-top: 30px;
        text-align: right;
    }
</style>
