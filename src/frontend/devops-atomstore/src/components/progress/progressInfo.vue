<template>
    <div class="progress-info-container">
        <bk-alert
            :title="alertTitle"
            type="info"
            :closable="true"
        />
        <div class="content-wrapper">
            <div class="info-section">
                <span class="label-text">{{ labelText }}</span>
                <div
                    v-if="list.length"
                    class="tags-grid"
                >
                    <slot />
                </div>
            </div>
            <div
                class="action-section"
                :class="{ 'with-list': list.length }"
            >
                <p
                    class="add-btn"
                    @click="handleAdd"
                >
                    <i class="bk-icon icon-plus-circle" />
                    <span class="add-text">{{ $t('store.添加') }}</span>
                </p>
                <i
                    v-if="tooltips"
                    v-bk-tooltips="tooltipsConfig"
                    class="bk-icon icon-info-circle"
                />
            </div>
        </div>

        <div class="button-group">
            <bk-button
                theme="primary"
                :disabled="isEdit"
                @click="() => handleGoStep(currentStep, 'next')"
            >
                {{ $t('store.下一步') }}
            </bk-button>
            <bk-button
                class="ml-24"
                :disabled="isEdit"
                @click="() => handleGoStep(currentStep, 'prev')"
            >
                {{ $t('store.上一步') }}
            </bk-button>
        </div>
    </div>
</template>

<script setup name="ProgressInfo">
    import { computed } from 'vue'
    import UseInstance from '@/hook/useInstance.js'

    const { proxy } = UseInstance()
    const { $t } = proxy

    const props = defineProps({
        list: {
            type: Array,
            required: true,
            default: () => []
        },
        labelText: {
            type: String,
            default: ''
        },
        tooltips: {
            type: String,
            default: ''
        },
        alertTitle: {
            type: String,
            default: ''
        },
        currentStep: {
            type: Object,
            required: true,
        },
        isEdit: {
            type: Boolean,
            required: true,
        },
    })

    const emit = defineEmits(['add', 'goStep'])

    const tooltipsConfig = computed(() => ({
        content: props.tooltips,
        placement: 'bottom-start'
    }))

    function handleAdd () {
        emit('add')
    }

    function handleGoStep (step, status) {
        emit('goStep', step, status)
    }
</script>

<style lang="scss" scoped>
.progress-info-container {
    position: relative;
    height: 100%;
}

.content-wrapper {
    display: flex;
    flex-wrap: wrap;
}

.info-section {
    display: flex;
    margin: 20px 0;
}

.label-text {
    width: 100px;
    font-size: 14px;
    color: #666;
    flex-shrink: 0;
}

.tags-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 16px;
    flex-wrap: wrap;
    flex: 1;
}

.action-section {
    display: flex;
    align-items: center;
    font-size: 14px;

    &.with-list {
        margin-left: 100px;
        width: 100%;
    }
}

.add-btn {
    color: #1592FF;
    cursor: pointer;
    display: flex;
    align-items: center;

    .bk-icon {
        margin-right: 4px;
    }

    .add-text {
        vertical-align: middle;
    }
}

.bk-icon.icon-info-circle {
    margin-left: 8px;
    padding-top: 4px;
    color: #979BA5;
    cursor: pointer;
}

.button-group {
    position: absolute;
    bottom: 0;

    .ml-24 {
        margin-left: 24px;
    }
}
</style>
