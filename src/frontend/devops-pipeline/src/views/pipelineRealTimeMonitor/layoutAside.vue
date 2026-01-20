<template>
    <aside class="metric-nav">
        <ul class="nav-list">
            <div
                v-for="(nav, index) in navList1"
                :key="nav.name"
                :class="{ 'nav-item': true, 'active': activeIndex === index }"
                @click="handleNavClick(index)"
            >
                <Logo
                    size="16"
                    :name="nav.icon"
                    :class="{ 'devops-icon': activeIndex === index }"
                />
                <span style="margin-left: 8px;">{{ $t(nav.name) }}</span>
            </div>
        </ul>
    </aside>
</template>

<script >
    import Logo from '@/components/Logo'
    import { ref, defineComponent  } from 'vue'
    export default defineComponent({
        components: {
            Logo
        },
        setup () {
            const navList1 = ref([
                {
                    name: 'realTimeMonitorOverview',
                    router: 'MetricsOverview',
                    icon: 'metrics-overview'
                }
            ])
            
            // 默认选中第一个
            const activeIndex = ref(0)
            
            const handleNavClick = (index) => {
                activeIndex.value = index
            }
            
            return {
                navList1,
                activeIndex,
                handleNavClick
            }
        }
    })

</script>

<style lang="scss" scoped>
.metric-nav {
    width: 240px;
    background: #fff;
    font-size: 14px;

    .nav-list {
        margin-top: 8px;
        display: flex;
        flex-direction: column;

        .nav-item {
            position: relative;
            display: flex;
            align-items: center;
            padding: 0 23px;
            height: 40px;
            line-height: 40px;
            color: #63656e;
            cursor: pointer;
            transition: all 0.3s ease;

            &:hover {
                background: #f5f7fa;
            }

            .metrics-icon {
                font-family: 'metrics' !important;
                speak: none;
                font-style: normal;
                font-weight: normal;
                font-variant: normal;
                text-transform: none;
                line-height: 1;
                text-align: center;
                -webkit-font-smoothing: antialiased;
                -moz-osx-font-smoothing: grayscale;
                font-size: 16px;
                margin-right: 8px;
            }
            
            // 选中状态样式
            &.active {
                background: #e1ecff;
                color: #3a84ff;

                // Logo 图标也变成深蓝色
                :deep(.devops-icon) {
                    color: #3a84ff;
                }
            }
        }

    }
}

.metric-main {
    height: 100%;
    width: calc(100% - 240px);
}
</style>
