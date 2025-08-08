<template>
    <section>
        <span class="review-title">{{$t('pipeline.manualApprovals')}}</span>
        <span class="review-subtitle">{{$t('pipeline.approveDesc')}}</span>
        <section :class="{ 'review-desc': true, 'show-more': isShowMore }">{{ desc }}</section>
        <bk-button text title="primary" @click="isShowMore = !isShowMore" v-if="isShowMoreButton">
            <span v-if="!isShowMore" class="opt-button">{{$t('more')}}<i class="bk-icon icon-angle-down"></i></span>
            <span v-else class="opt-button">{{$t('collapse')}}<i class="bk-icon icon-angle-up"></i></span>
        </bk-button>
    </section>
</template>

<script>
    export default {
        props: {
            desc: String
        },

        data () {
            return {
                isShowMore: false,
                isShowMoreButton: false
            }
        },

        mounted () {
            this.showMoreButton()
        },

        methods: {
            showMoreButton () {
                const descEl = this.$el.querySelector('.review-desc')
                this.isShowMoreButton = descEl.scrollHeight > descEl.offsetHeight
            }
        }
    }
</script>

<style lang="postcss" scoped>
    .review-title {
        color: #666770;
        line-height: 20px;
        padding-left: 7px;
        position: relative;
        display: block;
        font-size: 14px;
        font-weight: bold;
        &:before {
            content: '';
            position: absolute;
            width: 2px;
            height: 16px;
            background: #3a84ff;
            left: 0;
            top: 2px;
        }
    }
    span.review-subtitle {
        margin-top: 16px;
    }
    .review-desc {
        display: -webkit-box;
        -webkit-line-clamp: 4;
        -webkit-box-orient: vertical;
        overflow: hidden;
        margin-bottom: 1px;
        word-break: break-all;
        font-size: 12px;
        color: #666770;
        white-space: pre;
        &.show-more {
            display: block;
        }
    }
    .opt-button {
        font-size: 12px;
        line-height: 17px;
        .bk-icon {
            margin-left: 3px;
            font-size: 18px;
        }
    }
</style>
