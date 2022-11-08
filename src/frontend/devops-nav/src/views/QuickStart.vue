<template>
    <div class="devops-quickstart">
        <h2>{{ $t('quickStart.quickstartTitle') }}</h2>
        <quick-start-steps
            ref="step"
            v-bkloading="{ isLoading }"
            :is-error="isError"
            :step-index="stepIndex"
            :step-list="stepList"
        />
        <footer v-if="!done">
            <router-link to="/console/">
                <bk-button>{{ $t('cancelLabel') }}</bk-button>
            </router-link>
            <bk-button
                v-if="stepIndex > 0"
                theme="primary"
                @click="prev"
            >
                {{ $t('form.prev') }}
            </bk-button>
            <bk-button
                v-if="stepIndex < stepList.length - 1"
                theme="success"
                @click="next"
            >
                {{ $t('form.next') }}
            </bk-button>
            <bk-button
                v-if="stepIndex === stepList.length - 1"
                theme="success"
                @click="doCreate"
            >
                {{ $t('form.submit') }}
            </bk-button>
        </footer>
    </div>
</template>

<script lang="ts">
    import Vue from 'vue'
    import { Component, Watch } from 'vue-property-decorator'
    import { Action, State } from 'vuex-class'
    import QuickStartSteps from '../components/QuickStartSteps/index.vue'

    @Component({
        components: {
            QuickStartSteps
        }
    })
    export default class QuickStart extends Vue {
        stepIndex: number = 0
        done: boolean = false
        isLoading: boolean = false
        isError: boolean = false

        @Action createDemo
        @Action setDemoPipelineId
        @State demo

        $refs: {
            step: QuickStartSteps
        }

        get stepList () {
            return [
                this.$t('configProject'),
                this.$t('configPipeline'),
                this.$t('complete')
            ]
        }

        next (): void {
            if (this.$refs.step.validate()) {
                this.isError = false
                this.stepIndex++
            } else {
                this.isError = true
            }
        }

        prev (): void {
            this.stepIndex--
        }

        @Watch('demo')
        watchDemo () {
            this.isError = !this.$refs.step.validate()
        }

        async doCreate () {
            try {
                if (!this.demo) {
                    throw Error(this.$t('exception.noProject').toString())
                }
                const { projectId, projectName } = this.demo
                this.isLoading = true
                const { id } = await this.createDemo({
                    projectId,
                    projectName
                })
                this.setDemoPipelineId({
                    pipelineId: id
                })
                this.done = true
                this.next()
            } catch (e) {
                this.$bkMessage({
                    message: e.message,
                    theme: 'error'
                })
            } finally {
                this.isLoading = false
            }
        }
    }
</script>

<style lang="scss">
    @import '../assets/scss/conf';
    .devops-quickstart {
        width: 1280px;
        margin: 49px auto;
        h2 {
            margin-bottom: 37px;
            font-size: 24px;
            color: $fontBoldColor;
            text-align: center;
            font-weight: normal;
        }
        
        footer {
            margin-top: 30px;
            text-align: right;
        }
    }
</style>
