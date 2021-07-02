<template>
    <div class="infinite-tree" :class="{ 'bk-has-border-tree': isBorder }" @scroll="rootScroll">
        <div class="ghost-wrapper" :style="ghostStyle"></div>
        <div class="render-wrapper" ref="content">
            <div
                v-for="item in renderData" :key="item.id"
                :style="getNodeStyle(item)"
                :class="['node-item', { 'active': item.selected }, { 'is-disabled': item.disabled || isDisabled }]"
                :title="item.disabled ? $t(`m.common['该成员已添加']`) : ''"
                @click.stop="nodeClick(item)">
                <i
                    v-if="item.async && !item.loading"
                    :class="['arrow-icon bk-icon', item.expanded ? 'icon-down-shape' : 'icon-right-shape']"
                    @click.stop="expandNode(item)" />
                <i class="devops-icon icon-circle-2-1 spin-icon" v-if="item.loading"></i>
                <i class="bk-icon node-icon file-icon icon-folder-shape"></i>
                <span
                    :style="nameStyle(item)"
                    :class="['node-title', { 'node-selected': item.selected }]"
                    :title="item.name">
                    {{ item.type === 'USER' ? item.username : item.name }}
                    <template v-if="item.type === 'USER' && item.name !== ''">
                        ({{ item.name }})
                    </template>
                </span>
                <span class="red-dot" v-if="item.isNewMember"></span>
                <div class="node-radio" v-if="item.showRadio">
                    <span class="node-checkbox"
                        :class="{
                            'is-disabled': item.disabled || isDisabled,
                            'is-checked': item.is_selected,
                            'is-indeterminate': item.indeterminate
                        }"
                    >
                    </span>
                </div>
            </div>
        </div>
    </div>
</template>
<script>
    import _ from 'lodash'

    export default {
        name: 'infinite-tree',
        props: {
            // 所有数据
            allData: {
                type: Array,
                default: () => []
            },
            // 每个节点的高度
            itemHeight: {
                type: Number,
                default: 32
            },
            // 子节点左侧偏移的基础值
            leftBaseIndent: {
                type: Number,
                default: 18
            },
            isRatingManager: {
                type: Boolean,
                default: false
            },
            isDisabled: {
                type: Boolean,
                default: false
            }
        },
        data () {
            return {
                startIndex: 0,
                endIndex: 0
            }
        },
        computed: {
            ghostStyle () {
                return {
                    height: this.visiableData.length * this.itemHeight + 'px'
                }
            },
            visiableData () {
                return this.allData.filter(item => item.visiable)
            },
            renderData () {
                return this.visiableData.slice(this.startIndex, this.endIndex)
            },
            nameStyle () {
                return (payload) => {
                    if (payload.type === 'USER') {
                        return {
                            'maxWidth': 'calc(100% - 50px)'
                        }
                    }
                    let otherOffset = 14 + 17 + 22 + 33 + 35
                    if (payload.async) {
                        otherOffset += 14
                    }
                    return {
                        'maxWidth': `calc(100% - ${otherOffset}px)`
                    }
                }
            }
        },
        mounted () {
            const height = this.$el.clientHeight === 0
                ? parseInt(window.getComputedStyle(this.$el).height, 10)
                : this.$el.clientHeight

            this.endIndex = Math.ceil(height / this.itemHeight)
        },
        methods: {
            getNodeStyle (node) {
                return {
                    paddingLeft: (node.async ? node.level : node.level + 1) * this.leftBaseIndent + 'px'
                }
            },

            rootScroll: _.throttle(function () {
                this.updateRenderData(this.$el.scrollTop)
            }, 0),

            updateRenderData (scrollTop = 0) {
                // 可视区显示的条数
                const count = Math.ceil(this.$el.clientHeight / this.itemHeight)
                // 滚动后可视区新的 startIndex
                const newStartIndex = Math.floor(scrollTop / this.itemHeight)
                // 滚动后可视区新的 endIndex
                const newEndIndex = newStartIndex + count
                this.startIndex = newStartIndex
                this.endIndex = newEndIndex
                this.$refs.content.style.transform = `translate3d(0, ${newStartIndex * this.itemHeight}px, 0)`
            },

            nodeClick (node) {
                if (this.isDisabled) {
                    return
                }
                if (!node.disabled) {
                    node.is_selected = !node.is_selected
                    this.$emit('on-select', node.is_selected, node)
                    this.$emit('on-click', node)
                }
            },

            expandNode (node, isExpand) {
                if (isExpand) {
                    node.expanded = isExpand
                } else {
                    node.expanded = !node.expanded
                }
                
                if (node.children && node.children.length) {
                    const children = this.allData.filter(item => item.parentNodeId === node.id)
                    children.forEach(child => {
                        child.visiable = node.expanded
                        if (child.async && !node.expanded) {
                            this.collapseNode(child)
                        }
                    })
                } else {
                    if (node.async) {
                        this.$emit('async-load-nodes', node)
                    }
                }
                this.$emit('expand-node', node)
            },

            collapseNode (node) {
                node.expanded = false
                ;(node.children || []).forEach(child => {
                    child.visiable = false
                    if (child.async && !node.expanded) {
                        this.collapseNode(child)
                    }
                })
            },

            showAllRadio (flag) {
                this.allData.forEach(item => {
                    // 父节点
                    if (item.async) {
                        item.showRadio = flag
                    }
                })
            },

            nodeChange (newVal, oldVal, localVal, node) {
                this.$emit('on-select', newVal, node)
            },

            clearAllIsSelectedStatus () {
                this.allData.forEach(item => {
                    if (!item.disabled) {
                        item.is_selected = false
                    }
                })
            },

            setSingleSelectedStatus (nodeKey, isSelected) {
                this.allData.forEach(item => {
                    if (nodeKey === item.id) {
                        item.is_selected = isSelected
                    }
                })
            }
        }
    }
</script>
<style lang="scss" scoped>
    .infinite-tree {
        height: 862px;
        font-size: 14px;
        overflow: auto;
        position: relative;
        /* overflow: scroll; */
        will-change: transform;

        &::-webkit-scrollbar {
            width: 4px;
            background-color: lighten(transparent, 80%);
        }
        &::-webkit-scrollbar-thumb {
            height: 5px;
            border-radius: 2px;
            background-color: #e6e9ea;
        }

        .ghost-wrapper {
            position: absolute;
            left: 0;
            top: 0;
            right: 0;
            z-index: -1;
        }

        .render-wrapper {
            left: 0;
            right: 0;
            top: 0;
            position: absolute;
        }

        .node-item {
            position: relative;
            margin: 0;
            text-align: left;
            line-height: 32px;
            cursor: pointer;
            &.active {
                color: #3a84ff;
                background: #eef4ff;
            }
            &.is-disabled {
                color: #c4c6cc;
                /* cursor: not-allowed; */
            }
            &:hover {
                color: #3a84ff;
                background: #eef4ff;
            }
            &.is-disabled:hover {
                color: #c4c6cc;
                background: #eee;
            }
            &.is-selected {
                background: #eef4ff;
            }
            .spin-icon {
                display: inline-block;
            }
        }

        .node-svg {
            font-size: 16px;
            color: #a3c5fd;
        }

        .node-icon {
            position: relative;
            font-size: 16px;
            color: #a3c5fd;
            &.active {
                color: #3a84ff;
            }
            &.file-icon {
                font-size: 14px;
                margin: 0 2px;
            }
        }

        .arrow-icon {
            color: #c0c4cc;
        }

        .node-title {
            position: relative;
            display: inline-block;
            min-width: 14px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
            vertical-align: top;
            user-select: none;
        }

        .red-dot {
            display: inline-block;
            position: relative;
            top: -10px;
            left: -3px;
            width: 6px;
            height: 6px;
            border-radius: 50%;
            background-color: #ff0000;
        }

        .node-user-count {
            color: #c4c6cc;
        }

        .node-radio {
            margin-right: 5px;
            float: right;
            .node-checkbox {
                display: inline-block;
                position: relative;
                top: 3px;
                width: 16px;
                height: 16px;
                margin: 0 6px 0 0;
                border: 1px solid #979ba5;
                border-radius: 50%;
                &.is-checked {
                    border-color: #3a84ff;
                    background-color: #3a84ff;
                    background-clip: border-box;
                    &:after {
                        content: "";
                        position: absolute;
                        top: 1px;
                        left: 4px;
                        width: 4px;
                        height: 8px;
                        border: 2px solid #fff;
                        border-left: 0;
                        border-top: 0;
                        transform-origin: center;
                        transform: rotate(45deg) scaleY(1);
                    }
                    &.is-disabled {
                        background-color: #dcdee5;
                    }
                }
                &.is-disabled {
                    border-color: #dcdee5;
                    cursor: not-allowed;
                }
                &.is-indeterminate {
                    border-width: 7px 4px;
                    border-color: #3a84ff;
                    background-color: #fff;
                    background-clip: content-box;
                    &:after {
                        visibility: hidden;
                    }
                }
            }
        }

        .loading {
            display: inline-block;
            position: relative;
            top: -1px;
            width: 14px;
            height: 14px;
        }
    }
</style>
