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
                        @change="(val) => handleChangeTrigger('disabled', index, val)"
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
            <!-- <form-field
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
            <sub-parameter
                v-if="isTimerTriggerV2"
                :title="$t('details.startupParams')"
                :value="trigger.variables"
            /> -->
        </bk-form>
    </section>
</template>

<script setup>
    import FormField from '@/components/AtomPropertyPanel/FormField'
    // import SubParameter from '@/components/AtomFormComponent/SubParameter'
    import { defineProps } from 'vue'
    const props = defineProps({
        index: Boolean,
        trigger: Object,
        handleChangeTrigger: Function
    })
    // const isTimerTriggerV2 = computed(() => {
    //     const { version, atomCode } = props.trigger
    //     return atomCode === 'timerTrigger' && version.startsWith('2.')
    // })

    // function handleShowTimeCronCom () {
    // }
</script>

<style lang="scss" scoped>
    .trigger-field-from {
        display: grid;
        grid-template-columns: repeat(2, minmax(200px, 1fr));
        grid-gap: 0 24px;
    }
</style>
