<template>
    <div
        class="capacity-card"
        @click="handleClick"
    >
        <div class="capacity-content">
            <div class="capacity-header">
                <i
                    v-if="color !== ''"
                    class="legend-dot"
                    :style="{ background: color }"
                ></i>
                <span
                    class="capacity-label"
                    :style="{ textAlign: color !== '' ? 'center' : 'left' }"
                >{{ label }}</span>
                <i
                    v-if="showIcon"
                    class="bk-icon icon-shape"
                    :class="iconName"
                    :style="{ fontSize: iconSize }"
                ></i>
                <Logo
                    v-if="showIconLogo"
                    size="14"
                    :name="iconLogoName"
                ></Logo>
            </div>
            <div class="capacity-value">
                {{ value }} <span class="capacity-unit">{{ unit }}</span>
            </div>
        </div>
        <div
            v-if="showLogo"
            class="capacity-logo"
        >
            <Logo
                size="56"
                :name="logoName"
            ></Logo>
        </div>
    </div>
</template>

<script>
    import { defineComponent } from 'vue'
    import Logo from '@/components/Logo'
    export default defineComponent({
        name: 'CapacityCard',
        components: {
            Logo
        },
        emits: ['click'],
        props: {
            label: {
                type: String,
                required: true
            },
            value: {
                type: [String, Number],
                required: true
            },
            color: {
                type: String,
                default: ''
            },
            unit: {
                type: String,
                default: ''
            },
            showIcon: {
                type: Boolean,
                default: false
            },
            iconName: {
                type: String,
                default: 'icon-plus-circle'
            },
            showIconLogo: {
                type: Boolean,
                default: false
            },
            iconLogoName: {
                type: String,
                default: 'icon-plus-circle'
            },
            iconSize: {
                type: String,
                default: '14px'
            },
            showLogo: {
                type: Boolean,
                default: false
            },
            logoName: {
                type: String,
                default: 'metrics-zhipin-total'
            }
        },
        setup (props, { emit }) {
            const handleClick = () => {
                emit('click')
            }
            
            return {
                handleClick
            }
        }
    })
</script>

<style lang="scss" scoped>
.capacity-card {
    display: flex;
    flex-direction: row;
    // gap: 4px;
    justify-content: space-between;
    border-radius: 2px;
    padding: 12px 0px 0px 16px;
    text-align: center;
    cursor: pointer;
    transition: all 0.3s ease;
    background: #FAFBFD;
    
    &:hover {
        box-shadow: 0 2px 6px 0 rgba(58, 132, 255, 0.15);
        transform: translateY(-2px);
    }
}
.capacity-content{
    flex-direction: column;
    display: flex;
    justify-content: space-between;
}
.capacity-header {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 8px;
}
.legend-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    display: inline-block;
}
.capacity-label {
    font-size: 14px;
    color: #979BA5;
}

.icon-shape {
    color: #c4c6cc;
}

.capacity-value {
    text-align: left;
    font-size: 24px;
    color: #313238;
    padding-bottom: 16px;
}

.capacity-unit {
    font-size: 14px;
    color: #4D4F56;
    margin-left: 4px;
}

.capacity-logo {
    display: flex;
    align-items: end;
}
</style>
