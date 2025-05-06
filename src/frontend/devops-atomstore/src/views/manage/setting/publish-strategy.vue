<template>
    <div class="publish-strategy">
        <section class="publish-strategy-row">
            <label>
                {{ $t('store.publishStrategy') }}
            </label>
            
            <bk-select
                v-if="editing"
                class="publish-strategy-select"
                :value="strategy"
                @change="handleStrategyChange"
            >
                <bk-option
                    v-for="strategy in strategyOptions"
                    :key="strategy.id"
                    v-bind="strategy"
                />
            </bk-select>
            <p
                v-else
                class="publish-strategy-detail"
            >
                <span>{{ strategyLabel }}</span>
                <span class="publish-strategy-desc">( {{ strategyDesc }} )</span>
                <i
                    class="devops-icon icon-edit2"
                    @click="editStrategy"
                />
            </p>
        </section>
    </div>
</template>

<script>
    import { PUBLISH_STRATEGY } from '@/utils/constants'
    import { computed, defineComponent, getCurrentInstance, ref } from 'vue'

    export default defineComponent({

        setup () {
            const vm = getCurrentInstance()
            const strategy = computed(() => vm.proxy.$store.getters['store/getDetail']?.publishStrategy ?? 'AUTO')
            const editing = ref(false)

            const strategyLabel = computed(() => vm.proxy.$t(`store.${strategy.value}`))
            const strategyDesc = computed(() => vm.proxy.$t(`store.${strategy.value}-upgradeStrategyDesc`))
            const strategyOptions = Object.keys(PUBLISH_STRATEGY).map(key => ({
                id: key,
                name: `${vm.proxy.$t(`store.${key}`)} (${vm.proxy.$t(`store.${key}-upgradeStrategyDesc`)})`
            }))

            function editStrategy () {
                editing.value = true
            }

            async function handleStrategyChange (newVal) {
                // TODO:
                if (!newVal || newVal === strategy) {
                    editing.value = false
                    return
                }

                try {
                    console.log(newVal)
                    const res = await vm.proxy.$store.dispatch('store/updatePublishStrategy', {
                        projectId: vm.proxy.$route.params.projectId,
                        templateCode: vm.proxy.$route.params.code,
                        strategy: newVal
                    })
                    console.log(res)
                } catch (error) {
                    console.error(error.message)
                } finally {
                    editing.value = false
                }
            }
            return {
                editing,
                strategyOptions,
                strategyLabel,
                strategyDesc,
                editStrategy,
                handleStrategyChange,
                strategy
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
            .publish-strategy-select {
                width: 360px;
            }
            
            .publish-strategy-detail {
                display: flex;
                flex: 1;
                align-items: center;
                grid-gap: 6px;
                .publish-strategy-desc {
                    color: #979BA5;
                }
                .devops-icon.icon-edit2 {
                    cursor: pointer;
                    font-weight: 700;
                    padding: 0 6px;
                    &:hover {
                        color: #3a84ff;
                    }
                }
            }
        }
        
    }
</style>
