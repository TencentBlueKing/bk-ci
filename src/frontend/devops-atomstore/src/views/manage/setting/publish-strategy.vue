<template>
    <div class="publish-strategy">
        <section class="publish-strategy-row">
            <label>
                {{ $t('publishStrategy') }}
            </label>

            <bk-select v-if="editing">
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
                <span class="publish-strategy-desc">{{ strategyDesc }}</span>
                <i class="devops-icon icon-edit2" />
            </p>
        </section>
    </div>
</template>

<script>
    import { PUBLISH_STRATEGY } from '@/utils/constants'
    import { defineComponent, getCurrentInstance, ref } from 'vue'

    export default defineComponent({
        setup () {
            const vm = getCurrentInstance()
            const editing = ref(false)
            const strategyLabel = ref('自动发布')
            const strategyDesc = ref(vm.proxy.$t('（当源模版有新版本时，新版本自动发布到研发商店）'))
            const strategyOptions = Object.keys(PUBLISH_STRATEGY).map(key => ({
                id: key,
                name: vm.proxy.$t(`store.${key}`)
            }))
            return {
                editing,
                strategyOptions,
                strategyLabel,
                strategyDesc
            }
        }
    })
</script>

<style lang="scss">
    .publish-strategy {
        display: flex;
        background-color: white;
        padding: 24px;
        .publish-strategy-row {
            width: 100%;
            display: flex;
            grid-gap: 8px;
            font-size: 12px;
            
            .publish-strategy-detail {
                display: flex;
                flex: 1;
                align-items: center;
                .publish-strategy-desc {
                    color: #979BA5;
                }
            }
        }
        
    }
</style>
