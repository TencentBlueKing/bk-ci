<template>
    <div
        class="resource-selector"
        @click="toggleDropdown"
        :class="{ 'is-open': isDropdownOpen }"
    >
        <div class="resource-type">
            <div class="resource-icon">
                <Logo
                    :name="currentIcon"
                    :size="14"
                />
            </div>
            <span class="resource-name">{{ displayName }}</span>
        </div>
        <div
            class="dropdown-arrow"
            :class="{ 'is-open': isDropdownOpen }"
        >
            <i class="bk-icon icon-angle-down" />
        </div>
        
        <!-- 下拉菜单 -->
        <div
            v-if="isDropdownOpen"
            class="dropdown-menu"
        >
            <div
                v-for="option in resourceOptions"
                :key="option.value"
                class="dropdown-item"
                :class="{ 'is-active': option.value === activeValue }"
                @click.stop="selectResource(option.value)"
            >
                <div class="resource-icon">
                    <Logo
                        :name="option.icon"
                        :size="14"
                    />
                </div>
                <span class="resource-name">{{ option.label }}</span>
                <i
                    v-if="option.value === activeValue"
                    class="bk-icon icon-check-1 check-icon"
                />
            </div>
        </div>
    </div>
</template>

<script>
    import { ref, computed, onMounted, onUnmounted } from 'vue'
    import {
        SERVICE_RESOURCE_TYPE
    } from '@/store/constants'
    import UseInstance from '@/hooks/useInstance'
    import Logo from '@/components/Logo'
    
    export default {
        components: {
            Logo
        },
        setup () {
            const { proxy } = UseInstance()
            // 从路由参数中读取 resType，默认为 PIPELINE
            const routeResType = proxy.$route.params.resType
            const activeValue = ref(routeResType || SERVICE_RESOURCE_TYPE.PIPELINE)
            const isDropdownOpen = ref(false)
            
            const resourceOptions = computed(() => [
                {
                    value: SERVICE_RESOURCE_TYPE.PIPELINE,
                    label: proxy.$t('environment.pipelineResource'),
                    icon: 'color-logo-pipeline'
                },
                {
                    value: SERVICE_RESOURCE_TYPE.CREATE,
                    label: proxy.$t('environment.creativeResource'),
                    icon: 'color-logo-pipeline'
                }
            ])
            
            const displayName = computed(() => {
                const option = resourceOptions.value.find(item => item.value === activeValue.value)
                return option ? option.label : ''
            })
            
            const currentIcon = computed(() => {
                const option = resourceOptions.value.find(item => item.value === activeValue.value)
                return option ? option.icon : 'color-logo-pipeline'
            })
            
            const toggleDropdown = () => {
                isDropdownOpen.value = !isDropdownOpen.value
            }
            
            const selectResource = (value) => {
                if (value !== activeValue.value) {
                    activeValue.value = value
                    proxy.$router.replace({
                        params: {
                            ...proxy.$route.params,
                            resType: value
                        }
                    })
                }
                isDropdownOpen.value = false
            }
            
            const handleClickOutside = (event) => {
                const selector = event.target.closest('.resource-selector')
                if (!selector) {
                    isDropdownOpen.value = false
                }
            }
            
            onMounted(() => {
                document.addEventListener('click', handleClickOutside)
            })
            
            onUnmounted(() => {
                document.removeEventListener('click', handleClickOutside)
            })

            return {
                activeValue,
                isDropdownOpen,
                resourceOptions,
                displayName,
                currentIcon,
                toggleDropdown,
                selectResource
            }
        }
    }
</script>

<style lang="scss" scoped>
    .resource-selector {
        position: relative;
        height: 40px;
        background-color: #fff;
        border: 1px solid #DCDEE5;
        box-shadow: 0 0 8px 0 rgba(220, 222, 229, 0.3);
        border-radius: 3px;
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 0 12px;
        margin: 16px;
        cursor: pointer;
        transition: all 0.2s ease;
        
        &:hover {
            border-color: #3A84FF;
            box-shadow: 0 0 8px 0 rgba(58, 132, 255, 0.2);
        }
        
        &.is-open {
            border-color: #3A84FF;
            box-shadow: 0 0 8px 0 rgba(58, 132, 255, 0.2);
        }
        
        .resource-type {
            display: flex;
            align-items: center;
            gap: 8px;
            
            .resource-icon {
                display: flex;
                align-items: center;
                justify-content: center;
            }
            
            .resource-name {
                font-size: 12px;
                color: #313238;
                font-weight: 400;
            }
        }
        
        .dropdown-arrow {
            display: flex;
            align-items: center;
            justify-content: center;
            transition: transform 0.2s ease;
            
            &.is-open {
                transform: rotate(180deg);
            }
            .icon-angle-down {
                font-size: 20px;
            }
        }
    }
    
    .dropdown-menu {
        position: absolute;
        top: 40px;
        left: -1px;
        right: -1px;
        background: #fff;
        border: 1px solid #DCDEE5;
        border-top: none;
        border-radius: 0 0 3px 3px;
        box-shadow: 0 2px 8px 0 rgba(0, 0, 0, 0.1);
        z-index: 1000;
        
        .dropdown-item {
            display: flex;
            align-items: center;
            gap: 8px;
            padding: 8px 12px;
            height: 36px;
            cursor: pointer;
            transition: background-color 0.2s ease;
            
            &:hover {
                background-color: #F5F7FA;
            }
            
            &.is-active {
                background-color: #E1ECFF;
                
                .resource-name {
                    color: #3A84FF;
                }
            }
            
            .resource-icon {
                display: flex;
                align-items: center;
                justify-content: center;
            }
            
            .resource-name {
                flex: 1;
                font-size: 12px;
                color: #313238;
            }
            
            .check-icon {
                color: #3A84FF;
                font-size: 20px;
            }
        }
    }
</style>
