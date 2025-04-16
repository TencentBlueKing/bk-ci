<template>
    <section class="instance-config-constant">
        <header class="config-header">
            <div class="left">
                {{ $t('template.instanceConfig') }}
                <span class="line">|</span>
                <span class="instance-name">实例1111</span>
            </div>
            <div class="right">
                todo
            </div>
        </header>
        <div class="config-content">
            <template>
                <section class="params-content-item">
                    <header
                        :class="['params-collapse-trigger', {
                            'params-collapse-expand': activeName.has(1)
                        }]"
                        @click="toggleCollapse(1)"
                    >
                        <bk-icon
                            type="right-shape"
                            class="icon-angle-right"
                        />

                        {{ $t('template.pipelineBuildParams') }}
                    </header>
                    <div
                        v-if="activeName.has(1)"
                        class="params-collapse-content"
                    >
                        参数
                    </div>
                </section>
            </template>
        </div>
    </section>
</template>

<script setup>
    import { ref } from 'vue'
    // import PipelineVersionsForm from '@/components/PipelineVersionsForm.vue'
    // import PipelineParamsForm from '@/components/pipelineParamsForm.vue'
    const activeName = ref(new Set([1, 2, 3, 4, 5, 6]))

    function toggleCollapse (id) {
        console.log(id)
        if (activeName.value.has(id)) {
            activeName.value.delete(id)
        } else {
            activeName.value.add(id)
        }
        activeName.value = new Set(activeName.value)
    }
</script>

<style lang="scss">
    $header-height: 36px;

    .instance-config-constant {
        padding: 20px;
        .config-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            .left {
                font-weight: 700;
                font-size: 14px;
                color: #313238;
            }
            .line {
                display: inline-block;
                margin: 0 10px;
                color: #DCDEE5;
            }
            .instance-name {
                color: #979BA5;
                font-weight: 400;
            }
        }
        .config-content {
            overflow: auto !important;
            margin-top: 20px;

            .params-content-item {
                background: #FFFFFF;
                box-shadow: 0 2px 4px 0 #1919290d;
                border-radius: 2px;
                margin-bottom: 20px;
            }

            .params-collapse-content {
                padding: 16px 24px;
            }

            @for $i from 1 through 6 {
                :nth-child(#{$i} of .params-collapse-trigger) {
                    top: $header-height * ($i - 1);
                }
            }

            .params-collapse-trigger {
                display: flex;
                flex-shrink: 0;
                align-items: center;
                font-size: 14px;
                font-weight: 700;
                height: $header-height;
                cursor: pointer;
                top: 0;
                margin: 0 24px;
                position: sticky;
                grid-gap: 10px;
                color: #313238;
                background-color: white;
                z-index: 6;

                &.params-collapse-expand {
                    .icon-angle-right {
                        transform: rotate(90deg);
                    }
                }

                .icon-angle-right {
                    transition: all 0.3 ease;
                    color: #4D4F56;
                }
            }
        }
    }
</style>
