<template>
    <section class="trigger-config-detail">
        <bk-form
            form-type="vertical"
            class="trigger-field-from"
        >
            <form-field
                :label="$t('template.triggerEnabled')"
            >
                <section class="component-row">
                    <bk-radio-group
                        v-model="trigger.disabled"
                    >
                        <bk-radio
                            :value="true"
                            class="mr20"
                        >
                            {{ $t('template.enable') }}
                        </bk-radio>
                        <bk-radio
                            :value="false"
                        >
                            {{ $t('template.close') }}
                        </bk-radio>
                    </bk-radio-group>
                </section>
            </form-field>
            <form-field
                v-if="isTimerTriggerV2"
                :label="$t('template.timeCron')"
            >
                <section class="component-row">
                    <bk-input
                        @focus="handleShowTimeCronCom"
                    >
                    </bk-input>
                </section>
            </form-field>
            <form-field
                v-if="isTimerTriggerV2"
                :label="$t('details.startupParams')"
            >
                <section class="component-row">
                    <sub-parameter></sub-parameter>
                </section>
            </form-field>
        </bk-form>
    </section>
</template>

<script setup>
    import FormField from '@/components/AtomPropertyPanel/FormField'
    import SubParameter from '@/components/AtomFormComponent/SubParameter'
    import { defineProps, computed } from 'vue'
    const props = defineProps({
        trigger: Object
    })
    const isTimerTriggerV2 = computed(() => {
        const { version, atomCode } = props.trigger
        return atomCode === 'timerTrigger' && version.startsWith('2.')
    })

    function handleShowTimeCronCom () {
        console.log(123)
    }
</script>

<style lang="scss" scoped>
    .trigger-field-from {
        display: grid;
        grid-template-columns: repeat(2, minmax(200px, 1fr));
        grid-gap: 0 24px;
    }
</style>
