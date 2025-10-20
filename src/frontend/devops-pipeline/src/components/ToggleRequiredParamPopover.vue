<template>
    <bk-popover
        placement="top"
        max-width="300"
        theme="light toggle-required"
        transfer
        class="toggle-required-param-popover"
    >
        <span
            :class="[
                'icon-item',
                {
                    active: props.isRequiredParam,
                    'is-collapsed': props.isCollapsed
                }
            ]"
            @click.stop="handleChangeStatus"
        >
            <span
                v-if="isLoading"
                class="bk-icon icon-circle-2-1 spin-icon"
            >
            </span>
            <Logo
                v-else
                :name="props.isRequiredParam ? 'set-param-active' : 'set-param-default'"
                size="12"
            />
        </span>
        <template slot="content">
            <div
                class="required-popover-content"
                v-bkloading="{
                    isLoading,
                    theme: 'primary',
                    size: 'small'
                }"
            >
                <div>
                    <span class="current-status">{{ $t('template.currentStatus') }}</span>
                    <div class="option-item current">
                        {{ props.isRequiredParam ? $t('template.buildParam') : $t('template.notBuildParam') }}
                        <span class="current-instance">
                            ({{ $t('template.currentInstance') }})
                        </span>
                    </div>
                </div>
                <div
                    class="option-item"
                    @click.stop="handleChangeStatus"
                >
                    {{ props.isRequiredParam ? $t('template.cancelParticipant') : $t('template.setParticipant') }}
                </div>
            </div>
        </template>
    </bk-popover>
</template>
<script setup name="ToggleRequiredParamPopover">
    import { ref, defineProps } from 'vue'
    import Logo from '@/components/Logo'
    const props = defineProps({
        isCollapsed: {
            type: Boolean,
            required: false
        },
        isRequiredParam: {
            type: Boolean,
            required: true
        },
        handleChange: {
            type: Function,
            required: true
        }
    })
    const isLoading = ref(false)
  
    function handleChangeStatus (event) {
        event.preventDefault()
        isLoading.value = true
        props.handleChange(!props.isRequiredParam)
        setTimeout(() => {
            isLoading.value = false
        }, 200)
    }
</script>

<style lang="scss" scoped>
    .icon-item {
        position: relative;
        display: flex;
        align-items: center;
        justify-content: space-around;
        width: 22px;
        height: 22px;
        background: #EAEBF0;
        border-radius: 2px;
        margin-left: 6px;
        cursor: pointer;
        &.active{
            background: #E1ECFF;
            &:hover {
                background: #CDDFFE !important;
            }
        }
        &:hover {
            background: #DCDEE5;
        }
    }
   .is-collapsed {
        background: #fff !important;
        &.active{
            background: #CDDFFE !important;
        }
        // &:hover {
        //     background: #CDDFFE !important;
        // }
    }
</style>

<style lang="scss">
    .toggle-required-param-popover {
        display: inline-flex;
    }
    .toggle-required-theme {
        padding: 0 !important;
        .required-popover-content {
            .current-status {
                position: relative;
                bottom: 2px;
                padding: 0 4px;
                font-size: 10px;
                color: #1768EF;
                height: 16px;
                background: #E1ECFF;
                border-radius: 0 0 2px 0;
            }
            .current-instance {
                color: #979BA5;
                padding-left: 5px;
            }
            .option-item {
                padding: 0 20px 0 8px;
                height: 30px;
                line-height: 30px;
                color: #4D4F56;
                font-size: 12px;
                background-color: #f5f7fa;
                cursor: pointer;
                &.current {
                    cursor: default;
                    background-color: #fff;
                }
            }
        }
    }
</style>
