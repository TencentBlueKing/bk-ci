<template>
    <div class="store-template-list">
        <bk-input
            v-model="searchValue"
            :placeholder="$t('template.searchTemplate')"
            right-icon="bk-icon icon-search"
            @change="handleChangeSearchValue"
        />
        <ul
            class="template-list"
            @scroll.passive="scrollLoadMore"
            v-bkloading="{ isLoading: isPageLoading }"
        >
            <li
                v-for="(temp, tIndex) in storeTemplate"
                :class="{
                    'active': activeTempIndex === tIndex
                }"
                :key="temp.name"
                @click="selectTemp(temp, tIndex)"
            >
                <span
                    v-if="activeTempIndex === tIndex"
                    class="pipeline-template-corner"
                >
                    <i class="bk-icon icon-check-1"></i>
                </span>
                <p class="pipeline-template-logo">
                    <img
                        :src="temp.logoUrl"
                        v-if="temp.logoUrl"
                    >
                    <logo
                        size="50"
                        :name="temp.icon || 'placeholder'"
                        v-else
                    ></logo>
                </p>
                <div class="pipeline-template-detail">
                    <p
                        class="pipeline-template-title"
                        :title="temp.name"
                    >
                        <span>{{ temp.name }}</span>
                    </p>
                    <p
                        class="pipeline-template-desc"
                        :title="temp.desc"
                    >
                        {{ temp.desc || '--' }}
                    </p>
                </div>
                <div
                    class="pipeline-template-status"
                >
                    <bk-button
                        text
                        size="small"
                        theme="primary"
                        @click.stop="previewTemp(tIndex)"
                    >
                        {{ $t('pipelinesPreview') }}
                    </bk-button>
                </div>
            </li>
        </ul>
        
        <pipeline-template-preview
            v-model="isShowPreview"
            :template-pipeline="activeTemp"
        />
    </div>
</template>

<script>
    import { defineComponent, ref, computed, onMounted } from '@vue/composition-api'
    import UseInstance from '@/hook/useInstance'
    import PipelineTemplatePreview from '@/components/PipelineTemplatePreview'
    export default defineComponent({
        name: 'StoreTemplateList',
        components: {
            PipelineTemplatePreview
        },
        setup (props, { root, emit }) {
            if (!root) return
            const { route, store, bkMessage } = UseInstance(root)
            const loadEnd = ref(false)
            const isPageLoading = ref(false)
            const isLoadingMore = ref(false)
            const searchValue = ref('')
            const page = ref(1)
            const pageSize = ref(50)
            const activeTempIndex = ref(0)
            const storeTemplateNum = ref(0)
            const storeTemplate = ref([])
            const isShowPreview = ref(false)
            const projectId = computed(() => route.params.projectId)
            const activeTemp = computed(() => storeTemplate.value[activeTempIndex.value] ?? null)
            async function requestMarkTemplates () {
                try {
                    if (page.value === 1) {
                        isPageLoading.value = true
                    }
                    isLoadingMore.value = true
                    const param = {
                        page: page.value,
                        pageSize: pageSize.value,
                        projectCode: projectId.value,
                        keyword: searchValue.value
                    }
                    const res = await store.dispatch('common/requestStoreTemplate', param)
                    page.value++
                    storeTemplateNum.value = res.data.count || 0
                    storeTemplate.value.push(...res.data.records)
                    loadEnd.value = res.data.count <= storeTemplate.value?.length
                    emit('change', activeTemp.value)
                } catch (e) {
                    bkMessage({
                        theme: 'error',
                        message: (e.message || e)
                    })
                    console.error(e)
                } finally {
                    isPageLoading.value = false
                    isLoadingMore.value = false
                }
            }
            function handleChangeSearchValue (val) {
                page.value = 1
                storeTemplate.value = []
                requestMarkTemplates()
            }
            function selectTemp (temp, index) {
                if (index === activeTempIndex.value) return
                activeTempIndex.value = index
                emit('change', activeTemp.value)
            }
            function scrollLoadMore (event) {
                const target = event.target
                const bottomDis = target.scrollHeight - target.clientHeight - target.scrollTop
                if (bottomDis <= 500 && !loadEnd.value && !isLoadingMore.value) requestMarkTemplates()
            }
            function previewTemp (index) {
                isShowPreview.value = true
                activeTempIndex.value = index
            }
            onMounted(() => {
                requestMarkTemplates()
            })
            return {
                isPageLoading,
                searchValue,
                activeTempIndex,
                storeTemplate,
                isShowPreview,
                activeTemp,
                selectTemp,
                scrollLoadMore,
                previewTemp,
                handleChangeSearchValue
            }
        }
    })
</script>

<style lang="scss">
    @import '@/scss/conf';
    @import "@/scss/mixins/ellipsis";
    .store-template-list {
        padding: 20px;
        .template-list {
            height: 350px;
            display: grid;
            grid-gap: 16px;
            margin: 16px 24px;
            grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
            grid-auto-rows: 72px;
            overflow: auto;
            flex: 1;
            > li {
                position: relative;
                padding: 12px 16px;
                border: 1px solid #DCDEE5;
                border-radius: 2px;
                display: grid;
                grid-auto-flow: column;
                grid-template-columns: 50px 1fr;
                grid-gap: 12px;
                cursor: pointer;
                transition: all .3s ease;
                box-shadow: 0 2px 2px 0 rgba(0,0,0,0.16), 0 0 0 1px rgba(0,0,0,0.08);
                overflow: hidden;

                &:hover {
                    box-shadow: 0 3px 8px 0 rgba(0,0,0,0.2), 0 0 0 1px rgba(0,0,0,0.08);
                    .pipeline-template-status {
                        display: block;
                    }
                }
                &.disabled {
                    cursor: not-allowed;
                    opacity: .5;
                }
                &.active {
                    border: 1px solid #3A84FF;
                    box-shadow: 0;
                    .pipeline-template-corner {
                        position: absolute;
                        border-style: solid;
                        border-color: #3A84FF;
                        border-top-width: 12px;
                        border-bottom-width: 12px;
                        border-right-width: 10px;
                        border-left-width: 10px;
                        border-right-color: transparent;
                        border-bottom-color: transparent;
                        width: 0;
                        height: 0;
                        color: #fff;
                        > i {
                            font-size: 16px;
                            position: absolute;
                            top: -12px;
                            left: -12px;
                        }
                    }
                }
                .pipeline-template-logo {
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    color: #979BA5;
                    > img {
                        width: 100%;
                        height: 100%;
                    }
                }
                .pipeline-template-detail {
                    font-size: 14px;
                    overflow: hidden;
                    .pipeline-template-title {
                        display: flex;
                        align-items: center;
                        grid-gap: 8px;
                        > span {
                            @include ellipsis();
                        }
                        .is-store-template {
                            flex-shrink: 0;
                        }
                    }
                    .pipeline-template-desc {
                        font-size: 12px;
                        color: #979BA5;
                    }
                }
                .pipeline-template-status {
                    display: none;
                    font-size: 12px;
                    color: #979BA5;
                    align-self: center;
                }
            }
        }
    }
</style>
