<template>
    <div class="step-wrapper">
        <p class="step-desc">
            根据不同类型的代码库项目，我们为你提供了不同的流水线套餐，目前已支持Code平台的SVN、Git库以及蓝鲸Gitlab库
            <a
                :href="`${DOCS_URL_PREFIX}/所有服务/代码库/summary.html`"
                class="text-link"
                target="_blank"
            >了解更多信息</a>
        </p>
        <form>
            <label>代码库：</label>
            <bk-input
                readonly
                disabled
                value="Code Demo（新手接入专用）"
            />

            <div class="pipeline-preview">
                <label class="">
                    预览流水线:
                    <span class="desc">根据你选择的代码库，我们推荐如下流水线以完成持续交付</span>
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
        DOCS_URL_PREFIX: string = DOCS_URL_PREFIX
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
