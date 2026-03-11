<template>
    <div class="step-wrapper">
        <i class="devops-icon icon-check-circle quick-acess-success-icon" />
        <p>
            {{ $t('quickStart.completeTips', { projectName: demo.projectName }) }}
        </p>

        <a :href="pipelineUrl">
            <bk-button theme="primary">
                {{ $t('quickStart.getStarted') }}
            </bk-button>
        </a>
    </div>
</template>

<script lang="ts">
    import Vue from 'vue'
    import { Component } from 'vue-property-decorator'
    import { State } from 'vuex-class'
    @Component
    export default class Step3 extends Vue {
        @State demo!: ObjectMap

        get pipelineUrl (): string {
            const { pipelineId, projectId } = this.demo
            return `/console/pipeline/${projectId}/${pipelineId}/edit`
        }
    }
</script>

<style lang='scss' scoped>
    @import '../../assets/scss/conf';
    
    .step-wrapper {
        text-align: center;
        .quick-acess-success-icon {
            display: block;
            font-size: 40px;
            font-weight: normal;
            color: #00c873;
            text-align: center;
            margin-bottom: 30px;
        }
        > p {
            font-size: 16px;
            margin-bottom: 30px;
            padding-bottom: 30px;
            color: #333c48;
            border-bottom: 1px solid $borderWeightColor;
        }

        > a {
            cursor: pointer;
        }
    }
</style>
