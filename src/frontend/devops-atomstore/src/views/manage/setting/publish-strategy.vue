<template>
    <div class="publish-strategy">
        <section class="publish-strategy-row">
            <label>
                {{ $t('store.publishStrategy') }}
            </label>
            
            <form
                class="strategy-editing-form"
                v-if="editing"
            >
                <bk-select
                    class="publish-strategy-select"
                    v-model="publishStrategy"
                >
                    <bk-option
                        v-for="strategy in strategyOptions"
                        :key="strategy.id"
                        v-bind="strategy"
                    />
                </bk-select>
                <bk-button
                    text
                    @click="handleStrategyChange"
                    theme="primary"
                >
                    {{ $t('confirm') }}
                </bk-button>
                <bk-button
                    text
                    @click="cancelEditing"
                >
                    {{ $t('cancel') }}
                </bk-button>
            </form>
            <p
                v-else
                class="publish-strategy-detail"
            >
                <span>{{ strategyLabel }}</span>
                <span class="publish-strategy-desc">( {{ strategyDesc }} )</span>
                <i
                    class="devops-icon icon-edit-line"
                    @click="editStrategy"
                />
            </p>
        </section>
    </div>
</template>

<script>
    import { PUBLISH_STRATEGY } from '@/utils/constants'
    import { computed, defineComponent, getCurrentInstance, ref, watch } from 'vue'

    export default defineComponent({

        setup () {
            const vm = getCurrentInstance()
            const strategy = computed(() => vm.proxy.$store.getters['store/getDetail']?.publishStrategy ?? 'AUTO')
            const editing = ref(false)
            const publishStrategy = ref(strategy.value)

            const strategyLabel = computed(() => vm.proxy.$t(`store.${strategy.value}`))
            const strategyDesc = computed(() => vm.proxy.$t(`store.${strategy.value}-upgradeStrategyDesc`))
            const strategyOptions = Object.keys(PUBLISH_STRATEGY).map(key => ({
                id: key,
                name: `${vm.proxy.$t(`store.${key}`)} (${vm.proxy.$t(`store.${key}-upgradeStrategyDesc`)})`
            }))

            watch(strategy, (newVal) => {
                publishStrategy.value = newVal
            })

            function editStrategy () {
                editing.value = true
            }

            function cancelEditing () {
                publishStrategy.value = strategy.value
                editing.value = false
            }

            async function handleStrategyChange () {
                if (publishStrategy.value === strategy.value) {
                    cancelEditing()
                    return
                }

                try {
                    const { code } = vm.proxy.$route.params
                    await vm.proxy.$store.dispatch('store/updatePublishStrategy', {
                        templateCode: code,
                        publishStrategy: publishStrategy.value
                    })
                    const res = await vm.proxy.$store.dispatch('store/requestTemplate', code)
                    vm.proxy.$store.dispatch('store/setDetail', res)

                    vm.proxy.$bkMessage({
                        message: vm.proxy.$t('store.操作成功'),
                        theme: 'success'
                    })
                } catch (error) {
                    console.error(error.message)
                } finally {
                    cancelEditing()
                }
            }
            return {
                editing,
                strategyOptions,
                strategyLabel,
                strategyDesc,
                editStrategy,
                cancelEditing,
                handleStrategyChange,
                publishStrategy
            }
        }
    })
</script>

<style lang="scss">
    .publish-strategy {
        display: flex;
        background-color: white;
        padding: 32px 24px;
        .publish-strategy-row {
            width: 100%;
            display: flex;
            grid-gap: 8px;
            font-size: 12px;
            align-items: center;
            > label {
                margin-left: 56px;
                color: #979BA5;
            }

            .strategy-editing-form {
                display: flex;
                align-items: center;
                grid-gap: 8px;
                .publish-strategy-select {
                    width: 360px;
                }
            }
            
            .publish-strategy-detail {
                display: flex;
                flex: 1;
                align-items: center;
                grid-gap: 6px;
                line-height: 32px;
                .publish-strategy-desc {
                    color: #979BA5;
                }
                .devops-icon.icon-edit-line {
                    cursor: pointer;
                    padding: 0 6px;
                    &:hover {
                        color: #3a84ff;
                    }
                }
            }
        }
        
    }
</style>
