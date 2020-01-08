<template>
    <div class="paas-ci-empty">
        <p class="code-check-title" v-if="isCodeCheck">{{ emptyTitle }}</p>
        <div class="empty-pic-box">
            <img :src="calcSrc" :alt="calcAlt">
            <p class="empty-pic-desc">
                <slot>
                    {{ calcAlt }}
                </slot>
            </p>
        </div>
    </div>
</template>

<script>
    import noData from '@/images/box.png'
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
                    this.calcAlt = this.$t('newlist.noSearchResult')
                    break
                default:
                    this.calcSrc = noData
                    this.calcAlt = this.$t('noData')
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
        .empty-pic-box {
            position: absolute;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
        }
        .empty-pic-desc {
            text-align: center;
        }
    }
</style>
