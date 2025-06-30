<template>
    <div class="node-group-tree">
        <div
            v-for="group in nodeGroup"
            :key="group.type"
        >
            <div
                class="group-title"
                v-if="group.type === 'label_type'"
            >
                <span>{{ group.tltle }}</span>
                <p class="title-right">
                    <i class="devops-icon icon-plus"></i>
                    <span>{{ $t('environment.新增标签') }}</span>
                </p>
            </div>

            <ul
                class="node-list"
                v-if="group.type === 'node_type'"
            >
                <li
                    v-for="item in group.groups"
                    :key="item.id"
                    class="node-item"
                    :class="{ 'active': $route.params.asideId === item.id }"
                    @click="handleNodeClick(item.id)"
                >
                    <span class="node-name">{{ item.name }}</span>
                    <span
                        class="count-tag"
                        :class="{ 'active': $route.params.asideId === item.id }"
                    >{{ item.count }}</span>
                </li>
            </ul>

            <ul
                class="label-list"
                v-if="group.type === 'label_type'"
            >
                <li
                    v-for="groupItem in group.groups"
                    :key="groupItem.id"
                    class="label-group"
                >
                    <div
                        class="group-header"
                    >
                        <p @click="toggleGroupExpand(groupItem.id)">
                            <i :class="['devops-icon', 'icon-expanded', isGroupExpanded(groupItem.id) ? 'icon-down-shape' : 'icon-right-shape']"></i>
                            <span class="group-name">{{ groupItem.name }}</span>
                        </p>
                        
                        <bk-dropdown-menu
                            ref="dropdown"
                            trigger="click"
                            align="center"
                            ext-cls="drop-menu"
                            :position-fixed="true"
                        >
                            <div
                                class="dropdown-trigger-btn"
                                slot="dropdown-trigger"
                            >
                                <i class="manage-icon manage-icon-more-fill"></i>
                            </div>
                            <ul
                                class="bk-dropdown-list"
                                slot="dropdown-content"
                            >
                                <li @click="handleEditLabel(groupItem.id)">
                                    <a href="javascript:;">{{ $t('environment.修改标签') }}</a>
                                </li>
                                <li @click="handleDeleteLabel(groupItem.id)">
                                    <a href="javascript:;">{{ $t('environment.delete') }}</a>
                                </li>
                            </ul>
                        </bk-dropdown-menu>
                    </div>

                    <ul
                        class="sub-node-list"
                        v-if="groupItem.children"
                        v-show="isGroupExpanded(groupItem.id)"
                    >
                        <li
                            v-for="child in groupItem.children"
                            :key="child.id"
                            class="node-item sub-node-item"
                            :class="{ 'active': $route.params.asideId === child.id }"
                            @click="handleNodeClick(child.id)"
                        >
                            <span class="node-name">{{ child.name }}</span>
                            <span
                                class="count-tag"
                                :class="{ 'active': $route.params.asideId === child.id }"
                            >{{ child.count }}</span>
                        </li>
                    </ul>
                </li>
            </ul>
        </div>
    </div>
</template>
  
<script>
    export default {
        name: 'NodeGroupTree',
        data () {
            return {
                expandedGroupIds: []
            }
        },
        computed: {
            nodeGroup () {
                return [
                    {
                        type: 'node_type',
                        groups: [
                            { id: 'allNode', name: this.$t('environment.全部节点'), count: 20 },
                            { id: 'privateNode', name: this.$t('environment.私有构建节点'), count: 5 },
                            { id: 'deployNode', name: this.$t('environment.部署节点'), count: 15 }
                        ]
                    },
                    {
                        type: 'label_type',
                        tltle: this.$t('environment.按节点标签'),
                        groups: [
                            {
                                id: 'OS',
                                name: 'OS',
                                children: [
                                    { id: 'xco', name: 'xco', count: 5 }
                                ]
                            },
                            {
                                id: 'architecture',
                                name: 'architecture',
                                children: [
                                    { id: 'X86', name: 'X86', count: 10 },
                                    { id: 'AMD64', name: 'AMD64', count: 10 }
                                ]
                            }
                        ]
                    }
                ]
            },
            allGroupIds () {
                const labelGroup = this.nodeGroup.find(g => g.type === 'label_type')
                return labelGroup ? labelGroup.groups.map(g => g.id) : []
            }
        },
        watch: {
            nodeGroup () {
                this.expandedGroupIds = this.allGroupIds
            }
        },
        created () {
            this.expandedGroupIds = this.allGroupIds
        },
        methods: {
            isGroupExpanded (groupId) {
                return this.expandedGroupIds.includes(groupId)
            },
            toggleGroupExpand (groupId) {
                if (this.isGroupExpanded(groupId)) {
                    this.expandedGroupIds = this.expandedGroupIds.filter(id => id !== groupId)
                } else {
                    this.expandedGroupIds.push(groupId)
                }
            },
            handleNodeClick (asideId) {
                this.$router.push({ name: 'nodeList', params: { asideId } })
            },
            handleEditLabel (id) {
                console.log('🚀 ~ handleEditLabel ~ id:', id)
            },
            handleDeleteLabel (id) {
                console.log('🚀 ~ handleDeleteLabel ~ id:', id)
            }
        }
    }
</script>
  
<style scoped lang="scss">
  .node-group-tree {
    padding-top: 4px;
    background-color: #fff;
  }
  
  .group-title {
    display: flex;
    justify-content: space-between;
    position: relative;
    padding: 10px 16px;
    font-size: 12px;
    color: #979BA5;

    .title-right {
        color: #3A84FF;
        cursor: pointer;

        span {
            margin-left: 4px;
        }
    }
  }

  .group-title::before {
    content: '';
    position: absolute;
    top: 0;
    left: 16px;
    right: 16px;
    height: 1px;
    border-top: 1px solid #DCDEE5;
  }

  .label-list {
    margin-bottom: 4px;
    color: #63656E;
  }

  .node-list {
    margin-bottom: 28px;
    color: #313238;
  }

  .node-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 4px;
    padding: 0 28px 0 16px;
    height: 32px;
    line-height: 32px;
    cursor: pointer;
  }
  
  .node-item:hover, .sub-node-item:hover {
    background-color: #E1ECFF;
  }
  
  .node-item.active, .sub-node-item.active {
    background-color: #E1ECFF;
    border-right: 2px solid #3A84FF;
    color: #3A84FF;
  }
  
  .node-name {
    flex: 1;
  }
  
  .count-tag {
    display: inline-block;
    width: 30px;
    height: 16px;
    line-height: 16px;
    background: #F0F1F5;
    border-radius: 8px;
    font-size: 12px;
    text-align: center;
    color: #979BA5;
  }

  .count-tag.active {
    color: #3A84FF;
    background: #FFFFFF;
  }

  .label-group {
    margin-bottom: 8px;
  }
  
  .group-header {
    padding: 8px 12px;
    cursor: pointer;
    border-radius: 4px;
    display: flex;
    justify-content: space-between;
    color: #63656E;
    align-items: center;

    &:hover {
        background-color: #F0F1F5;
    }

    &:hover .dropdown-trigger-btn {
        display: block;
    }

    p {
        flex: 1;
    }

    .dropdown-trigger-btn {
        display: none;
    }
  }

  .icon-expanded {
    color: #979BA5;
    vertical-align: middle;
  }
  
  .group-name {
    flex: 1;
    margin-left: 4px;
  }

  .sub-node-item {
    margin-top: 0;
    padding-left: 40px;
  }
</style>
