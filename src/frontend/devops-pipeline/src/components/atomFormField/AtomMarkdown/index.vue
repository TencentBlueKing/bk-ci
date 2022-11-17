<template>
    <section class="atom-markdown">
        <mavon-editor
            class="remark-input"
            :placeholder="$t('stageReviewInputDescTip')"
            name="reviewDesc"
            :editable="!disabled"
            :value="value"
            :toolbars="toolbars"
            :external-link="false"
            :box-shadow="false"
            :subfield="false"
            preview-background="#fff"
            @change="(val, render) => handleChange(name, val)"
        />
    </section>
</template>

<script>
    import atomFieldMixin from '../atomFieldMixin'
    import { toolbars } from '@/utils/util'
    export default {
        name: 'atom-markdown',
        mixins: [atomFieldMixin],
        data () {
            return {
                toolbars
            }
        }
    }
</script>

<style lang="scss" scoped>
    .atom-markdown {
        display: -webkit-box;
        overflow: hidden;
        white-space: pre-wrap;
        height: 300px;
    }
    .remark-input {
        width: 100% !important;
    }
    ::v-deep .v-note-show {
        overflow-y: visible !important;
    }
</style>
