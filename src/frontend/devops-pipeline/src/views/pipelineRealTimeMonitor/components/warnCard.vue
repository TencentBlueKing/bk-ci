<template>
    <div class="capacity-card">
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
            </div>
            <div class="capacity-value">
                {{ value }} <span class="capacity-unit">{{ unit }}</span>
            </div>
            <div
                class="dividing-line"
                v-if="label === '总告警数'"
            ></div>
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
            }
        }
    })
</script>

<style lang="scss" scoped>
.capacity-card {
    position: relative;
    display: flex;
    flex-direction: row;
    justify-content: space-between;
    border-radius: 2px;
    padding: 12px 0px 0px 16px;
    text-align: center;
    cursor: pointer;
    transition: all 0.3s ease;
    
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
.capacity-value {
    text-align: left;
    font-size: 24px;
    color: #313238;
    padding-left: 14px;
    padding-bottom: 16px;
}
.capacity-unit {
    font-size: 14px;
    color: #4D4F56;
    margin-left: 4px;
}
.dividing-line {
   width: 1px;
   height: 64px;
   background: #DCDEE5;
   position: absolute;
   right: -8px;
}

</style>
