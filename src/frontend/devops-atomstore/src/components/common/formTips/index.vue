<template>
    <div class="form-tips">
        <div class="tips-icon">
            <i class="bk-icon icon-info-circle-shape"></i>
        </div>
        <div class="tips-content">
            <p class="tips-row">
                <span class="tips main-tips">{{ tipsContent }}</span>
            </p>
            <p class="tips-body" v-for="(row, index) in promptList" :key="index">
                <span class="tips">{{ row }}</span>
            </p>
        </div>
    </div>
</template>

<script>
    export default {
        props: {
            tipsContent: {
                type: String,
                default: ''
            },
            promptList: {
                type: Array,
                default: []
            }
        }
    }
</script>

<style lang="scss">
    @import '../../../assets/scss/conf.scss';
    .form-tips {
        display: flex;
        margin-top: 8px;
        border: 1px solid #e6e6e6;
        .tips-icon {
            display: flex;
            justify-content: center;
            align-items: center;
            min-width: 44px;
            background-color: #ffb400;
            i {
                display: inline-block;
                font-size: 18px;
                color: #fff;
            }
        }
        .tips-content {
            padding: 10px 18px;
            .tips-row,
            .tips-body {
                font-size: 12px;
            }
            .main-tips {
                line-height: 18px;
            }
        }
    }
</style>
