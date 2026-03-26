<template>
    <div class="append-menu-wrapper">
        <div
            class="append-menu-trigger"
            @click.stop="toggleMenu"
        >
            <i class="add-plus" />
            <span class="trigger-text">{{ t('append') }}</span>
        </div>
        <transition name="menu-fade" v-if="reactiveData.isCreativeStream">
            <div
                v-if="isMenuVisible"
                class="append-menu-dropdown"
                @click.stop
            >
                <span
                    class="menu-item"
                    @click="handleMenuClick('create')"
                >
                    {{ t('appendCreateJob') }}
                </span>

                <span
                    class="menu-item"
                    @click="handleMenuClick('cloud')"
                >
                    {{ t('appendCloudJob') }}
                </span>
            </div>
        </transition>
    </div>
</template>

<script setup>
    import { inject, onBeforeUnmount, onMounted, ref } from 'vue'
import { ADD_STAGE, APPEND_JOB } from './constants'
import { t } from './locale'
import { eventBus } from './util'

    const props = defineProps({
        stageIndex: {
            type: Number,
            required: true,
        },
    })

    const emit = defineEmits([APPEND_JOB])
    const reactiveData = inject("reactiveData");

    const isMenuVisible = ref(false)

    const toggleMenu = () => {
        if (reactiveData.isCreativeStream) {
            isMenuVisible.value = !isMenuVisible.value
        } else {
            eventBus.$emit(ADD_STAGE, {
                stageIndex: props.stageIndex,
                isParallel: true,
                isFinally: false,
            });
        }
    }

    const handleMenuClick = (type) => {
        emit(APPEND_JOB, {
            stageIndex: props.stageIndex,
            jobType: type,
        })
        isMenuVisible.value = false
    }

    const handleClickOutside = (event) => {
        isMenuVisible.value = false
    }

    onMounted(() => {
        document.addEventListener('click', handleClickOutside)
    })

    onBeforeUnmount(() => {
        document.removeEventListener('click', handleClickOutside)
    })
</script>

<style lang="scss">
@import './conf';

.append-menu-wrapper {
    position: relative;
    display: inline-flex;
}

.append-menu-trigger {
    display: flex;
    align-items: center;
    padding: 6px 16px;
    height: 32px;
    background: white;
    border: 1px solid #addaff;
    border-radius: 16px;
    color: $primaryColor;
    font-size: 14px;
    cursor: pointer;
    transition: all 0.3s ease;
    box-shadow: 0px 2px 4px 0px rgba(60, 150, 255, 0.2);

    &:hover {
        background-color: $primaryColor;
        border-color: $primaryColor;
        color: white;

        .add-plus {
            @include add-plus-icon(white, white, white, 18px, false);
        }
    }

    .add-plus {
        @include add-plus-icon($primaryColor, $borderColor, white, 18px, false);
    }

    .trigger-text {
        white-space: nowrap;
        line-height: 1;
    }
}

.append-menu-dropdown {
    position: absolute;
    top: calc(100% + 8px);
    left: 50%;
    transform: translateX(-50%);
    background: white;
    border: 1px solid #dcdee5;
    min-width: 160px;
    border-radius: 2px;
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
    z-index: 1000;

    .menu-item {
        display: flex;
        align-items: center;
        padding: 0 16px;
        height: 36px;
        font-size: 14px;
        color: #63656e;
        cursor: pointer;
        transition: all 0.2s ease;

        &:hover {
            background-color: #eaf3ff;
            color: $primaryColor;
        }

        &:first-child {
            border-radius: 2px 2px 0 0;
        }

        &:last-child {
            border-radius: 0 0 2px 2px;
        }
    }
}

.menu-fade-enter-active,
.menu-fade-leave-active {
    transition: opacity 0.2s ease, transform 0.2s ease;
}

.menu-fade-enter-from,
.menu-fade-leave-to {
    opacity: 0;
    transform: translateX(-50%) translateY(-4px);
}

.menu-fade-enter-to,
.menu-fade-leave-from {
    opacity: 1;
    transform: translateX(-50%) translateY(0);
}
</style>
