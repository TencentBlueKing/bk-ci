<template>
    <step0
        v-if="stepIndex === 0"
        ref="step"
        :is-error="isError"
    />
    <div
        v-else
        class="quick-start-step-wrapper"
    >
        <aside>
            <ul class="step-list">
                <li
                    v-for="(step, index) in stepList"
                    :key="index"
                    :class="{ &quot;step&quot;: true, &quot;done&quot;: isDone(index + 1), &quot;active&quot;: isActive(index + 1) }"
                >
                    <span v-if="isDone(index + 1) || (isActive(index + 1) && index + 1 === stepList.length) "><i class="bk-icon icon-check-1" /></span>
                    <span v-else>{{ index + 1 }}</span>
                    <p>{{ step }}</p>
                </li>
            </ul>
        </aside>
        <main>
            <component
                :is="currentStep"
                ref="step"
                :is-error="isError"
            />
        </main>
    </div>
</template>

<script lang="ts">
    import Vue from 'vue'
    import { Component, Prop } from 'vue-property-decorator'
    import Step0 from './Step0.vue'
    import Step1 from './Step1.vue'
    import Step2 from './Step2.vue'
    import Step3 from './Step3.vue'
    @Component({
      components: {
        Step0,
        Step1,
        Step2,
        Step3
      }
    })
    export default class QuickStartSteps extends Vue {
        @Prop({ required: true, default: [] })
        stepList: string []

        @Prop({ required: true, default: 0 })
        stepIndex: number

        @Prop({ default: false })
        isError: boolean

        $refs: {
            step: QuickStartSteps
        }

        isDone (index: number): boolean {
          return index < this.stepIndex
        }

        isActive (index: number): boolean {
          return index === this.stepIndex
        }

        validate () {
          return !(this.$refs.step.validate && !this.$refs.step.validate())
        }

        get currentStep (): string {
          return `Step${this.stepIndex}`
        }
    }
</script>

<style lang='scss'>
    @import '../../assets/scss/conf';
    .quick-start-step-wrapper {
        display: -webkit-box;
        display: flex;
        min-height: 400px;
        border: 1px solid $borderWeightColor;
        box-shadow: 5px 0px 10px rgba(0, 0, 0, 0.05);
        aside {
            width: 200px;
            padding: 32px 0 0 30px;
            border-right: 1px solid $borderWeightColor;
            background-color: transparent;
            .step-list {
                $marginHeight: 43px;
                $numSize: 36px;
                .step {
                    position: relative;
                    display: flex;
                    align-items: center;
                    margin-bottom: $marginHeight;
                    &.active {
                        color: $iconPrimaryColor;
                        > span {
                            color: $iconPrimaryColor;
                            border-color: $iconPrimaryColor;
                            box-shadow: 3px 3px 10px rgba(60, 150, 255, 0.2);
                        }
                        &:before {
                            content: '';
                            position: absolute;
                            width: 10px;
                            height: 10px;
                            border: 1px solid $borderWeightColor;
                            background-color: white;
                            right: -7px;
                            transform: rotate(45deg);
                            border-right-color: transparent;
                            border-top-color: transparent;
                        }
                    }

                     &:last-child {
                        &.active {
                            > span {
                                color: white;
                                background-color: $iconPrimaryColor;
                                border-color: $iconPrimaryColor;
                            }
                        }
                    }

                    &.done {
                        > span {
                            color: white;
                            background-color: $fontLigtherColor;
                            border-color: $fontLigtherColor;
                        }
                    }
                    > span {
                        display: inline-block;
                        width: $numSize;
                        height: $numSize;
                        line-height: $numSize;
                        border: 1px solid $borderWeightColor;
                        border-radius: 50%;
                        background-color: white;
                        font-weight: bold;
                        text-align: center;
                        font-size: 16px;
                        margin-right: 10px;
                    }
                    &:not(:last-child):after {
                        content: '';
                        position: absolute;
                        border-left: 2px dotted $borderWeightColor;
                        height: $marginHeight;
                        top: $numSize;
                        left: $numSize / 2;

                    }
                }
            }
        }
        main {
            flex: 1;
            .step-wrapper {
                padding: 30px 40px 95px;
                height: 100%;
                background-color: white;
                .step-desc {
                    font-size: 12px;
                    margin-bottom: 30px;
                }
                .text-link {
                    color: #4098FF;
                }
            }
        }
    }
</style>
