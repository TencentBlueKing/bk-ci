<template>
    <div>
        <header class="setting-title">
            <span class="setting-title-main-text">{{ $t('quickStart.configurationTip') }}</span>
            <span class="devops-desc setting-title-desc">
                {{ $t("quickStart.settintTitleDesc") }}
            </span>
        </header>
        <div class="setting-option selected disabled">
            <label class="bk-form-checkbox">
                <bk-checkbox
                    :true-value="settingOptions.value"
                    false-value="no"
                    :checked="true"
                >
                    {{ $t("quickStart.isEnableWhiteList") }}
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
        get settingOptions (): object {
            return {
                title: this.$t('quickStart.settingOptionsTitle'),
                desc: this.$t('quickStart.settingOptionsDesc'),
                value: 'quick',
                disabled: true
            }
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
                margin-left: 20px;
            }
            > p {
                font-size: 12px;
                margin-left: 20px;
            }
        }
    }
    footer {
        margin-top: 30px;
        text-align: right;
    }
</style>
