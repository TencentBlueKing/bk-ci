<template>
    <div class="scheme-container" v-if="programList.length">
        <div
            v-for="(scheme, index) in programList" :key="index"
            v-if="!scheme.isDisable"
            :class="['scheme-item', { 'scheme-current': scheme.id === accelerateId, 'scheme-disabled': scheme.isDisable }]"
            @click.stop="schemeChange(scheme)">
            <span class="scheme-horn" v-if="scheme.isRecommend">荐</span>
            <div class="item-wrapper">
                <span class="scheme-name">{{ scheme.name }}</span>
                <span class="scheme-desc">{{ scheme.desc }}</span>
                <i class="bk-icon scheme-current-icon icon-check-1" v-if="scheme.id === accelerateId"></i>
            </div>
        </div>
    </div>
</template>

<script>
    export default {
        name: 'schemes',
        props: {
            programList: {
                type: Array
            },
            accelerateId: {
                type: String
            }
        },
        methods: {
            schemeChange (scheme) {
                this.$emit('schemeChange', scheme)
            }
        }
    }
</script>

<style lang="scss">
    @import '../../../assets/scss/conf';

    .scheme-container {
        width: 820px;
        font-size: 0;
    }
    .scheme-item {
        position: relative;
        display: inline-block;
        margin: 0 10px 10px 0;
        border-radius: 2px;
        overflow: hidden;
        background: #fff;
        cursor: pointer;
        .item-wrapper {
            padding: 7px 13px 11px 14px;
            width: 400px;
            height: 42px;
            border: 1px solid $fontLigtherColor;
            border-radius: 2px;
        }
        .scheme-name {
            font-family: 'TTTGBMedium';
            font-size: 18px;
            color: $fontColorLabel;
        }
        .scheme-desc {
            font-family:'PingFangHK-Regular';
            font-size: 12px;
        }
        &.scheme-current {
            .item-wrapper {
                padding: 6px 13px 10px  14px;
                border: 2px solid $primaryColor;
            }
            .scheme-current-icon {
                float: right;
                line-height: 24px;
                font-size: 16px;
                color: #00C873;
            }
        }
        .scheme-horn {
            position: absolute;
            top: -4px;
            left: -4px;
            display: inline-block;
            width: 32px;
            height: 32px;
            padding-left: 4px;
            font-size: 12px;
            font-weight: bold;
            overflow: hidden;
            transform: scale(0.75);
            color: #fff;
            &::before {
                position: absolute;
                top: 0;
                left: 0;
                content: '';
                display: block;
                width: 0;
                height: 0;
                border-top: 32px solid #0082FF;
                border-right: 32px solid transparent;
                z-index: -5;
            }
        }
        &.scheme-disabled {
            border-color: $borderWeightColor;
            background: #fafbfd;
            cursor: not-allowed;
            .scheme-name, .scheme-desc {
                color: $fontLigtherColor;
            }
        }
    }
</style>
