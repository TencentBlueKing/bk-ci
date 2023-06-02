<template>
    <section>
        <span class="review-title">{{ $t('stageReview.manualApprovals') }}</span>
        <span class="review-subtitle">{{ $t('stageReview.approvalDescription') }}</span>
        <section>
            <mavon-editor
                class="markdown-desc"
                :editable="false"
                default-open="preview"
                :subfield="false"
                :toolbars-flag="false"
                :external-link="false"
                :box-shadow="false"
                preview-background="#fff"
                v-model="desc"
            >
            </mavon-editor>
        </section>
    </section>
</template>

<script>
    export default {
        props: {
            desc: String
        }
    }
</script>

<style lang="scss" scoped>
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
    .markdown-desc {
        min-height: 50px;
        max-height: 250px;
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
