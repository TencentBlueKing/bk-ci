<template>
    <ul class="bk-trees-list">
        <li class="bk-trees-item" :key="index" v-for="(item, index) of list" v-if="item.folder">
            <div class="bk-tree-title"
                :class="{ open: item.isOpen, selected: isActive(index) }"
                :style="{ &quot;padding-left&quot;: 23 * _deepCount + &quot;px&quot; }"
                @dblclick.stop="itemDbClickHandler({
                    index,
                    deepCount,
                    item
                })"
                @click.stop="itemClickHandler({
                    index,
                    deepCount,
                    item
                })">
                <i @click.stop="itemDbClickHandler({ index, deepCount, item })" class="devops-icon arrow-icon" :class="item.isOpen ? item.arrowOpenIcon : item.arrowIcon"></i>
                <i class="devops-icon title-icon" :class="item.isOpen ? item.openIcon : item.icon"></i>
                <span class="title-text pr15">{{ item.name }}</span>
            </div>
            <CollapseTransition>
                <bk-trees
                    v-if="item.children && item.children.length"
                    v-show="item.isOpen"
                    :list.sync="item.children"
                    :deep-count="_deepCount"
                    :is-root="false"
                    :road-map="index">
                </bk-trees>
            </CollapseTransition>
        </li>
    </ul>
</template>

<script>
    import CollapseTransition from '@/utils/collapse-transition.js'
    import { mapState } from 'vuex'
    import { bus } from '../../../utils/bus'
    export default {
        name: 'bk-trees',
        components: {
            CollapseTransition
        },
        props: {
            list: {
                type: Array,
                default () {
                    return []
                }
            },
            deepCount: {
                type: Number,
                default: 1
            },
            isRoot: {
                type: Boolean,
                default: true
            },
            roadMap: {
                type: [String, Number],
                default: '0'
            }
        },
        computed: {
            ...mapState('artifactory', [
                'curNodeOnDialogTree'
            ]),
            _deepCount () {
                return this.deepCount + 1
            },
            projectId () {
                return this.$route.params.projectId
            }
        },
        methods: {
            /**
             *  双击某一项的回调函数
             */
            async itemDbClickHandler (data) {
                this.timer && clearTimeout(this.timer)
                this.changeRoadMap(data)
            },
            /**
             *  单击某一项的回调函数
             */
            itemClickHandler (data) {
                this.timer && clearTimeout(this.timer)
                this.timer = setTimeout(() => {
                    this.handleClick(data)
                }, 300)
            },
            // src参数: right,表示右侧出发  click，表示单击菜单，dbClick，表示双击菜单或点击箭头
            changeRoadMap ({ index, deepCount, item, src = 'click', roadMap = '' }) {
                item.isOpen = !item.isOpen
                const map = this.getRoadMap(index)
                bus.$emit('dialog-tree-click', map)
            },
            handleClick ({ index, deepCount, item }) {
                const roadMap = this.getRoadMap(index)
                const fullPath = item.fullPath || '/'
                this.$store.commit('artifactory/updateDialogTree', {
                    roadMap,
                    fullPath
                })
            },
            getRoadMap (index) {
                let roadMap = ''
                let parent = this
                roadMap = `${~~parent.roadMap},${index}`
                while (!parent.isRoot) {
                    parent = parent.$parent
                    roadMap = `${~~parent.roadMap},${roadMap}`
                }
                return roadMap.slice(2)
            },
            isActive (index) {
                return this.getRoadMap(index) === this.curNodeOnDialogTree.roadMap
            }
        }
    }
</script>

<style lang="scss">
    @import './../../../scss/conf';

    .bk-trees-list {
    }
    .bk-tree-title {
        position: relative;
        height: 37px;
        line-height: 37px;
        padding-right: 5px;
        color: $fontWeightColor;
        font-size: 0;
        white-space: nowrap;
        cursor: default;
        &.selected {
            background-color: $primaryLightColor;
            color: $primaryColor;
            .title-icon,
            .left-icon {
                color: $primaryColor;
            }
        }
        &:hover {
            // background-color: $primaryLightColor;
            color: $primaryColor;
            .item-indicator,
            .arrow-icon,
            .title-icon {
                color: $primaryColor;
            }
        }
        .item-indicator {
            position: relative;
            top: 2px;
            display: inline-block;
            transition: transform linear .2s;
            color: $fontWeightColor;
            font-size: 12px;
        }
        .title-icon {
            position: relative;
            display: inline-block;
            top: 4px;
            margin: 0 10px;
            color: #c4cdd6;
            font-size: 18px;
        }
        .arrow-icon {
            position: relative;
            display: inline-block;
            top: 2px;
            margin: 0 -2px;
            font-size: 12px;
        }
        .title-text {
            font-size: 12px;
        }
        &.open {
            .item-indicator {
                transform: rotate(90deg);
            }
        }
        .hidden {
            visibility: hidden;
        }
    }
</style>
