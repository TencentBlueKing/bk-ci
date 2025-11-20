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
                            :value="false"
                            :disabled="trigger.isFollowTemplate"
                            class="mr20"
                        >
                            {{ $t('template.enable') }}
                        </bk-radio>
                        <bk-radio
                            :value="true"
                            :disabled="trigger.isFollowTemplate"
                        >
                            {{ $t('template.close') }}
                        </bk-radio>
                    </bk-radio-group>
                </section>
            </form-field>
            <template v-if="isTimerTriggerV2">
                <form-field
                    :label="$t('template.timeCron')"
                    name="cron"
                >
                    <section class="component-row">
                        <bk-input
                            v-model="trigger.cron"
                            :disabled="trigger.isFollowTemplate"
                            @focus="handleShowTimeCronCom"
                        />
                    </section>
                </form-field>
                <sub-parameter
                    name="variables"
                    :title="$t('details.startupParams')"
                    :value="trigger.variables"
                    :disabled="trigger.isFollowTemplate"
                    :param="timerTriggerParamConfig"
                    :handle-change="handleChangeStartParam"
                />
            </template>
        </bk-form>
        <bk-dialog
            v-model="showTimeCronCom"
            :title="$t('template.setTimeCron')"
            header-position="left"
            width="700px"
            render-directive="if"
            :mask-close="false"
            @confirm="handleConfirmChangeCron"
        >
            <timer-cron-tab
                :value="trigger.cron"
                name="cron"
                :handle-change="handleChangeCron"
            />
        </bk-dialog>
    </section>
</template>

<script setup>
    import { ref, computed } from 'vue'
    import FormField from '@/components/AtomPropertyPanel/FormField'
    import SubParameter from '@/components/AtomFormComponent/SubParameter'
    import TimerCronTab from '@/components/atomFormField/TimerCrontab/'

    import { defineProps } from 'vue'
    const props = defineProps({
        index: Boolean,
        trigger: Object,
        handleChangeTrigger: Function
    })
    const showTimeCronCom = ref(false)
    const isTimerTriggerV2 = computed(() => {
        const { version, atomCode } = props.trigger
        return atomCode === 'timerTrigger' && version.startsWith('2.')
    })
    const newCron = ref('')
    const timerTriggerParamConfig = computed(() => {
        return {
            paramType: 'url',
            url: '/process/api/user/buildParam/{projectId}/{pipelineId}/buildParamFormProp',
            urlQuery: {
                includeConst: false,
                includeNotRequired: false,
                isTemplate: ""
            },
            parameters: []
        }
    })
    
    function handleChangeStartParam (name, val) {
        props.handleChangeTrigger(name, props.index, JSON.parse(val))
    }

    function handleShowTimeCronCom () {
        showTimeCronCom.value = true
    }
    function handleChangeCron (name, val) {
        newCron.value = val
    }
    function handleConfirmChangeCron () {
        props.handleChangeTrigger('cron', props.index, newCron.value)
        showTimeCronCom.value = false
    }
</script>

<style lang="scss" scoped>
    .trigger-field-from {
        display: grid;
        grid-template-columns: repeat(2, minmax(200px, 1fr));
        grid-gap: 0 24px;
    }
</style>
