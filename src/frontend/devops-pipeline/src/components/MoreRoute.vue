<template>
    <bk-dropdown-menu
        class="more-router-link-list"
        trigger="click"
        @show="toggle"
        @hide="toggle"
    >
        <div slot="dropdown-trigger">
            <span
                class="pipeline-dropdown-trigger"
                :class="{ 'active': dropTitle !== 'more' }"
            >
                {{ $t(dropTitle) }}
                <i
                    :class="['devops-icon icon-angle-down', {
                        'icon-flip': toggleIsMore
                    }]"
                ></i>
            </span>
        </div>
        <ul
            class="bk-dropdown-list"
            slot="dropdown-content"
        >
            <li
                v-for="menu in dropdownMenus"
                :class="{
                    'active': menu.routeName === routeName
                }"
                :key="menu.label"
                @click="go(menu.routeName)"
            >
                <a href="javascript:;">
                    {{ $t(menu.label) }}
                </a>
            </li>
        </ul>
    </bk-dropdown-menu>
</template>

<script>
    import { computed, defineComponent, getCurrentInstance, ref } from 'vue'

    export default defineComponent({
        setup () {
            const toggleIsMore = ref(false)
            const vm = getCurrentInstance()
            const routeName = computed(() => vm.proxy.$route.name)
            const dropdownMenus = [
                {
                    label: 'labelManage',
                    routeName: 'pipelinesGroup'
                },
                {
                    label: 'templateManage',
                    routeName: 'pipelinesTemplate'
                },
                {
                    label: 'pluginManage',
                    routeName: 'atomManage'
                },
                {
                    label: 'operatorAudit',
                    routeName: 'pipelinesAudit'
                }
            ]
            const dropTitle = computed(() => {
                return dropdownMenus.find(menu => menu.routeName === routeName.value)?.label ?? 'more'
            })

            function go (name) {
                vm.proxy.$router.push({ name })
            }

            function toggle () {
                toggleIsMore.value = !toggleIsMore.value
            }

            return {
                toggleIsMore,
                routeName,
                dropdownMenus,
                dropTitle,
                toggle,
                go
            }
        }
    })
</script>
<style lang="scss">
    @import './../scss/conf';

    .more-router-link-list {
        display: flex;
        margin-right: 24px;
        .pipeline-dropdown-trigger {
            font-size: 14px;
            cursor: pointer;
            display: flex;
            align-items: center;
            .devops-icon {
                display: inline-block;
                transition: all ease-in-out 0.3s;
                margin-left: 4px;
                font-size: 10px;
                font-weight: 700;
                &.icon-flip {
                    transform: rotate(180deg);
                }
            }
        }
        .active,
        li.active > a {
            color: $primaryColor;
        }
        .bk-dropdown-list > li {
            display: list-item !important;
        }
    }
</style>
