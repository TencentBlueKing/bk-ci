<template>
    <div class="paas-ci-empty">
        <p class="code-check-title" v-if="isCodeCheck">{{ emptyTitle }}</p>
        <img :src="calcSrc" alt="暂无数据" class="empty-pic">
    </div>
</template>

<script>
    import noData from '@/images/no_data.png'
    import noResult from '@/images/no_result.png'

    export default {
        props: {
            type: {
                type: String,
                default: 'no-data'
            },
            isCodeCheck: {
                type: Boolean,
                default: false
            },
            emptyTitle: String
        },
        data () {
            return {
                calcSrc: '',
                calcAlt: ''
            }
        },
        created () {
            const {
                type
            } = this

            switch (type) {
                case 'no-result':
                    this.calcSrc = noResult
                    this.calcAlt = '未搜索到任何的结果'
                    break
                default:
                    this.calcSrc = noData
                    this.calcAlt = '暂无数据'
            }
        }
    }
</script>

<style lang="scss">
    @import './../../../scss/conf';

    .paas-ci-empty {
        position: relative;
        width: 100%;
        height: 100%;
        min-height: 200px;
        text-align: center;
        .code-check-title {
            margin-top: 28px;
            color: $fontWeightColor;
            font-size: 15px;
            font-weight: 600;
        }
        .empty-pic {
            position: absolute;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
        }
    }
</style>
