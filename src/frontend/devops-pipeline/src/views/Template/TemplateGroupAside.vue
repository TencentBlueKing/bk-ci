<template>
    <aside class="template-aside">
        <div
            v-for="(item, idx) in navList"
            :key="idx"
        >
            <div
                :class="['nav-item', ($route.params.viewId || cacheViewId) === item.viewId ? 'active' : '']"
                @click="handleChangeMenu(item.viewId)"
            >
                <div>
                    <Logo
                        :class="item.icon"
                        size="16"
                        :name="item.icon"
                    />
                    <span>{{ $t(item.i18nKey) }}</span>
                </div>
                <span class="nav-num">{{ countMap[item.countKey] ?? 0 }}</span>
            </div>
            <p
                v-if="item.countKey === 'all'"
                class="item-border"
            />
        </div>
    </aside>
</template>

<script setup name='TemplateGroupAside'>
    import { ref, computed, onMounted } from 'vue'
    import UseInstance from '@/hook/useInstance'
    import Logo from '@/components/Logo'
    import {
        ALL_TEMPLATE_VIEW_ID,
        PIPELINE_TEMPLATE_VIEW_ID,
        STAGE_TEMPLATE_VIEW_ID,
        JOB_TEMPLATE_VIEW_ID,
        STEP_TEMPLATE_VIEW_ID,
        TEMPLATE_VIEW_ID_CACHE
    } from '@/store/modules/templates/constants'

    const { proxy } = UseInstance()
    const navList = ref([
        {
            viewId: ALL_TEMPLATE_VIEW_ID,
            i18nKey: ALL_TEMPLATE_VIEW_ID,
            icon: 'template-all',
            activeIcon: 'group',
            countKey: 'all'
        },
        {
            viewId: PIPELINE_TEMPLATE_VIEW_ID,
            i18nKey: PIPELINE_TEMPLATE_VIEW_ID,
            icon: 'template-pipeline',
            activeIcon: 'pipeline-template',
            countKey: 'PIPELINE'
        },
        {
            viewId: STAGE_TEMPLATE_VIEW_ID,
            i18nKey: STAGE_TEMPLATE_VIEW_ID,
            icon: 'template-stage',
            activeIcon: 'stage-template',
            countKey: 'STAGE'
        },
        {
            viewId: JOB_TEMPLATE_VIEW_ID,
            i18nKey: JOB_TEMPLATE_VIEW_ID,
            icon: 'template-job',
            activeIcon: 'job-template',
            countKey: 'JOB'
        },
        {
            viewId: STEP_TEMPLATE_VIEW_ID,
            i18nKey: STEP_TEMPLATE_VIEW_ID,
            icon: 'template-step',
            activeIcon: 'step-template',
            countKey: 'STEP'
        }
    ])
    const countMap = ref({})
    const projectId = computed(() => proxy.$route.params.projectId)
    const cacheViewId = localStorage.getItem(TEMPLATE_VIEW_ID_CACHE) || ALL_TEMPLATE_VIEW_ID
    async function getType2Count () {
        try {
            countMap.value = await proxy.$store.dispatch('templates/getType2Count', {
                projectId: projectId.value
            })
        } catch (err) {
            console.error(err)
        }
    }
    function handleChangeMenu (viewId) {
        localStorage.setItem(TEMPLATE_VIEW_ID_CACHE, viewId)
        proxy.$router.push({
            name: 'TemplateManageList',
            params: {
                viewId
            }
        })
    }
    onMounted(() => {
        getType2Count()
    })
</script>

<style lang="scss">
    .template-aside{
        height: 100%;
        background: #FFFFFF;
        padding-top: 8px;
        box-shadow: 1px 0 0 0 #EAEBF0, 1px 0 0 0 #DCDEE5;

        .nav-item {
            display: flex;
            justify-content: space-between;
            padding: 0 32px 0 24px;
            height: 40px;
            align-items: center;
            font-size: 14px;
            color: #4D4F56;
            cursor: pointer;
            svg {
                vertical-align: middle;
                color: #C4C6CC;
                margin: 0 10px;
            }
            .nav-num {
                height: 16px;
                padding: 0 8px;
                background: #F0F1F5;
                border-radius: 8px;
                font-size: 12px;
                color: #979BA5;
                text-align: center;
                line-height: 16px;
            }
        }
        .active{
            background-color: #E1ECFF;
            color: #3A84FF;
        }
        .item-border {
            border-bottom: 1px solid #DCDEE5;
            margin: 8px 24px;
        }
   }
</style>
