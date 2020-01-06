<template>
    <div class="layout-inner">
        <!-- <nav-top /> -->
        <main class="page-main">
            <div class="page-sider">
                <div class="task-selector">
                    <bk-select :value="taskId" class="select-task" @selected="handleTaskChange" :clearable="false" searchable>
                        <bk-option v-for="(option, index) in allTasks"
                            :key="index"
                            :id="option.taskId"
                            :disabled="option.status === 1"
                            :name="option.nameCn">
                        </bk-option>
                    </bk-select>
                </div>
                <nav class="nav">
                    <bk-navigation-menu
                        ref="menu"
                        class="menu"
                        @select="handleMenuSelect"
                        :default-active="activeMenu.id"
                        :toggle-active="true"
                        item-hover-bg-color="#e1ecff"
                        item-hover-color="#3a84ff"
                        item-active-bg-color="#e1ecff"
                        item-active-color="#3a84ff"
                        sub-menu-open-bg-color="#f0f1f5"
                        item-default-bg-color="#fff"
                        item-default-color="#63656e"
                    >
                        <bk-navigation-menu-item
                            :has-child="item.children && !!item.children.length"
                            :group="item.group"
                            v-for="item in menus"
                            :key="item.id"
                            :icon="item.icon"
                            :disabled="item.disabled"
                            :id="item.id"
                            :href="item.href"
                            :toggle-handle="handleToggleActive"
                        >
                            <span>{{item.name}}</span>
                            <template v-slot:child>
                                <bk-navigation-menu-item
                                    :id="child.id"
                                    :disabled="child.disabled"
                                    :icon="child.icon"
                                    :key="child.id"
                                    :href="child.href"
                                    v-for="child in item.children"
                                >
                                    <span>{{child.name}}</span>
                                </bk-navigation-menu-item>
                            </template>
                        </bk-navigation-menu-item>
                    </bk-navigation-menu>
                </nav>
            </div>
            <div class="page-content">
                <template v-if="$route.meta.breadcrumb !== 'inside'">
                    <div class="breadcrumb">
                        <div class="breadcrumb-name">{{breadcrumb.name}}</div>
                        <div class="breadcrumb-extra" v-if="$route.meta.record !== 'none'">
                            <a @click="openSlider"><i class="bk-icon icon-order"></i>{{$t('nav.操作记录')}}</a>
                        </div>
                    </div>
                    <div class="main-container">
                        <slot />
                    </div>
                </template>
                <template v-else>
                    <slot />
                </template>
            </div>
            <Record :visiable.sync="show" :data="this.$route.name" />
        </main>
    </div>
</template>

<script>
    // import NavTop from './nav-top'
    import { mapState } from 'vuex'
    import Record from '@/components/operate-record/index'

    export default {
        components: {
            // NavTop,
            Record
        },
        data () {
            return {
                show: false
            }
        },
        computed: {
            ...mapState([
                'toolMeta',
                'taskId',
                'constants'
            ]),
            ...mapState('task', {
                taskList: 'list',
                taskDetail: 'detail'
            }),
            ...mapState('tool', {
                toolMap: 'mapList'
            }),
            menus () {
                const { enableToolList } = this.taskDetail
                const routeParams = { ...this.$route.params }
                // delete routeParams.toolId
                const menuBase = [
                    {
                        id: 'task-detail',
                        name: this.$t('nav.任务详情'),
                        icon: 'icon-order',
                        href: this.$router.resolve({ name: 'task-detail', params: routeParams }).href
                    },
                    {
                        id: 'task-settings',
                        name: this.$t('nav.任务设置'),
                        icon: 'icon-cog',
                        group: true,
                        href: this.$router.resolve({ name: 'task-settings', params: routeParams }).href
                    }
                ]
                const menuTool = enableToolList.map(tool => {
                    const toolName = tool.toolName
                    const toolPattern = tool.toolPattern.toLocaleLowerCase()
                    const { TOOL_PATTERN } = this.constants
                    const params = { ...this.$route.params, toolId: toolName }
                    const childMenu = [
                        {
                            name: this.$t('nav.告警管理'),
                            id: `${toolName}-defect-list`,
                            toolId: toolName,
                            href: this.$router.resolve({ name: `defect-${toolPattern}-list`, params }).href
                        },
                        {
                            name: this.$t('nav.数据报表'),
                            id: `${toolName}-chart`,
                            toolId: toolName,
                            href: this.$router.resolve({ name: `defect-${toolPattern}-charts`, params }).href
                        }
                    ]

                    // 目前仅linter类工具支持规则配置
                    if (toolPattern === TOOL_PATTERN.LINT.toLocaleLowerCase()) {
                        childMenu.push({
                            name: this.$t('nav.规则配置'),
                            id: `${toolName}-rule`,
                            toolId: toolName,
                            href: this.$router.resolve({ name: 'tool-rules', params }).href
                        })
                    }

                    return {
                        id: toolName,
                        name: this.$t(`toolName.${tool.toolDisplayName}`),
                        children: childMenu,
                        group: true
                    }
                })

                return menuBase.concat(menuTool)
            },
            activeMenu () {
                // 从所有菜单项中找出path与$route中的path一致或包含则为当前菜单项
                const routePath = this.$route.path
                let activeMenu = {}
                for (const menu of this.menus) {
                    if (menu.children) {
                        for (const subMenu of menu.children) {
                            if (routePath.indexOf(subMenu.href) !== -1) {
                                activeMenu = { id: subMenu.id, name: subMenu.name, toolId: subMenu.toolId }
                                break
                            }
                        }
                    } else {
                        if (routePath.indexOf(menu.href) !== -1) {
                            activeMenu = { id: menu.id, name: menu.name, toolId: menu.toolId }
                            break
                        }
                    }
                }

                return activeMenu
            },
            breadcrumb () {
                const toolId = this.activeMenu.toolId
                let toolDisplayName = (this.toolMap[toolId] || {}).displayName || ''
                const name = this.activeMenu.name
                // name = this.$t(`nav.${name}`)
                const names = [name]
                if (toolDisplayName) {
                    toolDisplayName = this.$t(`toolName.${toolDisplayName}`)
                    names.unshift(toolDisplayName)
                }

                return { name: names.join(' / ') }
            },
            allTasks () {
                return this.taskList.enableTasks.concat(this.taskList.disableTasks)
            }
        },
        methods: {
            handleTaskChange (taskId) {
                const task = this.taskList.enableTasks.find(task => task.taskId === taskId) || {}
                this.$router.push({
                    name: task.toolNames && task.toolNames.length ? 'task-detail' : 'task-new',
                    params: { ...this.$route.params, taskId },
                    query: { step: 'tools' }
                })
            },
            handleMenuSelect (id, item) {
                // console.log(item.href)
                this.$router.push(item.href)
            },
            openSlider () {
                this.show = true
            }
        }
    }
</script>

<style lang="postcss">
    .layout-inner {
        .page-sider {
            flex: 0 0 var(--siderWidth);
            width: var(--siderWidth);
            background: #fff;
            border-right: 1px solid #d1d1d1;

            .task-selector {
                height: 60px;
                border-bottom: 1px solid #dcdee5;
                padding: 14px 8px;
            }
            .select-task {
                border: 0 none;
                font-size: 16px!important;

                &.is-focus,
                &:focus {
                    outline: none!important;
                    box-shadow: none!important;
                }
            }

            .nav {
                margin: 12px 0;
            }
            .menu {
                background: #fff;
                .navigation-menu-item[group],
                .navigation-sbmenu[group] {
                    border-bottom: 1px solid #f0f1f5;
                }
            }
        }

        .page-content {
            flex: auto;
            width: calc(100% - var(--siderWidth));
            background: #f5f7fa;
            overflow: hidden;

            .breadcrumb {
                display: flex;
                align-items: center;
                height: 60px;
                background: #fff;
                color: #333;
                padding: 0 16px;
                border-bottom: 1px solid #dcdee5;
                .breadcrumb-name {
                    flex: 1;
                }
                .breadcrumb-extra {
                    flex: none;
                    font-size: 12px;

                    .line {
                        color: #dcdee5;
                        margin: 0 8px;
                    }

                    a {
                        .bk-icon {
                            margin-right: 2px;
                        }
                        cursor: pointer;
                    }
                }
            }
        }

        .page-main {
            display: flex;
            min-height: calc(100vh - var(--navTopHeight));
        }
        .main-container {
            padding: 20px;
            height: calc(100vh - 60px);
            min-height: 638px;
            overflow-y: auto;
        }

        .main-content {
            height: 100%;
            >.bk-tab {
                height: 100%;
            }
        }
    }
</style>
