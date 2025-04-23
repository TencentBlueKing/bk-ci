<template>
    <bk-dropdown-menu
        class="more-router-link-list"
        trigger="click"
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

    export default {
        data () {
            return {
                toggleIsMore: false
            }
        },
        computed: {
            routeName () {
                return this.$route.name
            },
            dropTitle () {
                return this.dropdownMenus.find(menu => menu.routeName === this.routeName)?.label ?? 'more'
            },
            dropdownMenus () {
                return [
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
            }

        },
        methods: {
            go (name) {
                this.$router.push({ name })
            }
        }
    }
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
                transition: all ease 0.2s;
                margin-left: 4px;
                font-size: 12px;
                &.icon-flip {
                    transform: rotate(180deg);
                }
            }
        }
        .active,
        li.active > a {
            color: $primaryColor;
        }
    }
</style>
