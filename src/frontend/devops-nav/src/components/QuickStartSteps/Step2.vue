<template>
    <div class="step-wrapper">
        <p class="step-desc">
            {{ $t('quickStart.pipelineStepDesc') }}
        </p>
        <form>
            <label>{{ $t('quickStart.codelib') }}</label>
            <bk-input
                readonly
                disabled
                :value="`Code Demo（${$t('accessGuide')}）`"
            />

            <div class="pipeline-preview">
                <label class="">
                    {{ $t("quickStart.previewPipeline") }}
                    <span class="desc">
                        {{ $t("quickStart.previewTips") }}
                    </span>
                </label>
                <img src="../../assets/images/codebase.jpg">
            </div>
        </form>
    </div>
</template>

<script lang="ts">
    import Vue from 'vue'
    import { Component } from 'vue-property-decorator'

    @Component
    export default class Step2 extends Vue {
    }
</script>

<style lang='scss'>
    @import '../../assets/scss/conf';
    .step-wrapper{
        padding: 30px 40px 95px;
        height: 100%;
        background-color: white;

        .pipeline-preview {
            height: 260px;
            margin-top: 40px;
            .desc {
                font-size: 12px;
                font-weight: normal;
            }
            img {
                width: 100%;
                border: 1px solid $borderColor;
                border-radius: 2px;
                background-color: $bgHoverColor;
            }
        }
    }
</style>
