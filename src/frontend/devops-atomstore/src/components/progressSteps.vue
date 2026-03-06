<template>
    <ul class="progress-steps">
        <li
            v-for="(step, index) in stepData"
            :key="step.code"
            class="step-item"
            :class="{ 'has-line': index < steps.length - 1 }"
        >
            <p
                class="step-content"
                :class="step.clickableCls"
                @click="handleJumpStep(step)"
            >
                <span
                    class="step-icon"
                    :class="statusColorMap[step.status]"
                >
                    <bk-spin
                        v-if="step.status === STATUS.DOING"
                        size="mini"
                        theme="primary"
                    />
                    <span
                        v-else-if="step.status === STATUS.UNDO"
                        class="step-number"
                    >
                        {{ step.step }}
                    </span>
                    <i
                        v-else
                        class="bk-icon"
                        :class="`icon-${statusIconMap[step.status]}`"
                    />
                </span>
                <span :class="['step-name', step.stepNameCls]">
                    {{ step.name }}
                </span>
            </p>
            <p
                v-if="index < steps.length - 1"
                class="step-line"
            />
        </li>
    </ul>
</template>

<script setup name="ProgressSteps">
    import { computed } from 'vue'

    const STATUS = {
        SUCCESS: 'success',
        FAIL: 'fail',
        DOING: 'doing',
        UNDO: 'undo',
    }

    const props = defineProps({
        steps: {
            type: Array,
            required: true,
        },
        currentStep: {
            type: Object,
            required: true,
        }
    })

    const emit = defineEmits(['jumpStep'])

    const statusColorMap = {
        [STATUS.SUCCESS]: 'status-success',
        [STATUS.FAIL]: 'status-fail',
        [STATUS.DOING]: 'status-doing',
        [STATUS.UNDO]: 'status-undo',
    }

    const statusIconMap = {
        [STATUS.SUCCESS]: 'check-circle',
        [STATUS.FAIL]: 'close-circle',
    }

    const stepData = computed(() => {
        return props.steps.map(step => {
            const isCursor = step.status !== STATUS.UNDO && !['begin', 'end'].includes(step.code) ? 'is-clickable' : ''

            return {
                ...step,
                clickableCls: isCursor,
                stepNameCls: props.currentStep?.name === step.name ? 'is-active' : '',
            }
        })
    })

    function handleJumpStep (step) {
        if (step.status !== STATUS.UNDO && !['begin', 'end', props.currentStep.code].includes(step.code)) {
            emit('jumpStep', step)
        }
    }
</script>

<style lang="scss" scoped>
.progress-steps {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0 16px;
    width: 100%;
    height: 80px;
    background: #FAFBFD;
}

.step-item {
    display: flex;
    align-items: center;
    position: relative;
    height: 100%;
    font-size: 14px;

    &.has-line {
        flex: 1;
    }
}

.step-content {
    display: flex;
    align-items: center;

    &.is-clickable {
        cursor: pointer;
    }
}

.step-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 24px;
    margin-right: 8px;

    &.status-success {
        color: #3A84FF;
    }

    &.status-fail {
        color: #EA3636;
    }

    &.status-doing {
        color: #3A84FF;
    }

    &.status-undo {
        color: #979BA5;
    }
}

.step-number {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 20px;
    height: 20px;
    border: 1px solid #979BA5;
    border-radius: 50%;
    font-size: 12px;
    font-style: normal;
}

.step-name {
    &.is-active {
        color: #3A84FF;
    }
}

.step-line {
    flex: 1;
    height: 2px;
    background: #3A84FF;
    margin: 0 16px;
    align-self: center;
}
</style>
