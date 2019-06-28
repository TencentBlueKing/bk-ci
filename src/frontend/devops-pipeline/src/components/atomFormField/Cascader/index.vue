
<template>
    <div class="bk-cascader"
        :class="[{ 'open': showList }]"
        v-bk-clickoutside="close">
        <input type="text"
            class="bk-form-input"
            autocomplete="off"
            :name="name"
            :value="currentValue"
            :disabled="disabled"
            :placeholder="placeholder"
            @input="input"
            @mousedown="showList = true"
        />
        <i :class="['bk-icon icon-angle-down bk-selector-icon',{ 'disabled': disabled }]" @click.stop="showList = !showList"></i>
        <div class="bk-cascader-list bk-cascader-items" v-if="showList && !searchKey && atomTreeList.length">
            <ul v-if="atomTreeList.length > 1">
                <li
                    v-for="node in atomTreeList"
                    class="bk-cascader-list-item"
                    :class="{ 'bk-cascader-selected': selected[node.level] === node[classifyKey] }"
                    :key="node[classifyKey]"
                    @click.stop="handleSelect(node, classifyKey)">
                    
                    <div class="bk-cascader-node" :class="{ 'is-disabled': node.disabled }">
                        <div class="text">
                            {{ node.classifyName }}
                            <!-- <span class="unread-icon" v-if='has'></span> -->
                        </div>
                        <i v-if="node.children && node.children.length" class="bk-icon icon-angle-right"></i>
                    </div>
                </li>
            </ul>
            <ul v-if="chList && chList.length" class="bk-cascader-list-ulg">
                <li
                    v-for="subNode in chList"
                    class="bk-cascader-list-item"
                    :class="{ 'bk-cascader-selected': selected[subNode.level] === subNode[leafKey] }"
                    :key="subNode[leafKey]"
                    @click.stop="handleSelect(subNode, leafKey)">
                    <div class="bk-cascader-node" :class="{ 'is-disabled': subNode.disabled }">
                        <bk-popover placement="top-start" :content="tips" :disabled="!subNode.disabled">
                            <div class="text">
                                {{ subNode.name }}
                                <span class="new-service-icon" v-if="subNode.atomLabelList.length">{{subNode.atomLabelList}}</span>
                            </div>
                        </bk-popover>
                    </div>
                </li>
            </ul>
        </div>

        <div class="bk-cascader-list bk-cascader-groups" v-if="showList && searchKey">
            <ul>
                <li class="bk-cascader-group-list-item"
                    v-for="(item, index) in searchList" :key="index">
                    <div class="bk-cascader-group-name">{{ item.classifyName }}</div>
                    <ul class="bk-cascader-group-list">
                        <li v-for="(child, cIndex) in item.children" class="bk-cascader-list-item" :key="cIndex">
                            <div class="bk-cascader-node bk-cascader-sub-node"
                                :class="{ 'bk-cascader-selected': child[leafKey] === value, 'is-disabled': child.disabled }">
                                <bk-popover placement="top-start" :content="tips" :disabled="!child.disabled">
                                    <div class="text" @click.stop="handleSelect(child, leafKey)">
                                        {{ child.name }}
                                        <span class="new-service-icon" v-if="child.atomLabelList.length">{{child.atomLabelList}}</span>
                                    </div>
                                </bk-popover>
                            </div>
                        </li>
                    </ul>
                </li>
                <li v-if="searchList.length === 0" class="bk-cascader-list-item">
                    <div class="text">暂无数据</div>
                </li>
            </ul>
        </div>
    </div>
</template>

<script>
    import atomFieldMixin from '../atomFieldMixin'

    export default {
        name: 'cascader',
        mixins: [atomFieldMixin],
        props: {
            name: {
                type: String,
                required: true
            },
            classifyKey: {
                type: String,
                default: 'classifyCode'
            },
            leafKey: {
                type: String,
                default: 'atomCode'
            },
            atomMap: {
                type: Object,
                default: () => ({})
            },
            tree: {
                type: Object,
                default: () => ({})
            },
            atomTreeList: {
                type: Array,
                default: []
            },
            atoms: {
                type: Array,
                default: []
            },
            value: {
                type: String
            },
            placeholder: {
                type: String,
                default: '请选择'
            },
            list: {
                type: Array,
                default: []
            },
            disabled: {
                type: Boolean,
                default: false
            },
            category: {
                type: Number,
                default: 1
            },
            tips: {
                type: String,
                default: '该插件不属于当前构建环境'
            }
        },
        data () {
            return {
                showList: false,
                includeNewAtomType: [],
                searchKey: '',
                searching: false,
                selected: []
            }
        },
        computed: {
            chList () {
                const { selected } = this
                const currentNode = this.tree[selected[0]]
                return currentNode ? currentNode.children : []
            },
            searchList () {
                const { searchKey, atoms, classifyKey, tree } = this
                if (!searchKey) return []
                const filteredAtoms = atoms.filter(atom => atom.name.toLowerCase().indexOf(searchKey.toLowerCase()) > -1).reduce((filterRes, atom) => {
                    if (!filterRes[atom[classifyKey]]) {
                        filterRes[atom[classifyKey]] = {
                            ...tree[atom[classifyKey]],
                            children: [
                                atom
                            ]
                        }
                    } else {
                        filterRes[atom[classifyKey]].children.push(atom)
                    }
                    return filterRes
                }, {})
                return Object.values(filteredAtoms)
            },
            currentValue () {
                const { atomMap, value, searchKey, searching } = this
                const atomName = atomMap[value] ? atomMap[value].name : ''
                return searching ? searchKey : atomName
            },
            firstClassify () {
                return this.atomTreeList[0] ? this.atomTreeList[0].classifyCode : ''
            }
        },
        watch: {
            value () {
                this.clacSelected()
            },
            atomMap () {
                this.clacSelected()
            }
        },
        created () {
            this.clacSelected()
        },
        methods: {
            close () {
                this.showList = false
                this.searching = false
                this.searchKey = ''
            },
            clacSelected () {
                const atomCode = this.value
                const atom = this.atomMap[atomCode]
                const classifyCode = atom ? atom.classifyCode : this.firstClassify
                
                this.selected = [
                    classifyCode,
                    this.value || ''
                ]
            },
            isLeaf (node) {
                return !(node && Array.isArray(node.children))
            },
            input (e) {
                const { value } = e.target
                this.searchKey = value
                this.searching = true
                this.showList = true
            },
            // 选中分类
            handleSelect (node, key) {
                if (node.disabled) return
                const { level } = node
                if (this.isLeaf(node) && node[key] !== this.value) { // 页面节点
                    this.handleChange(this.name, node[key])
                    this.close()
                }
                this.selected = [
                    ...this.selected.slice(0, level),
                    node[key]
                ]
            }
        }
    }
</script>
<style lang='scss'>
    @import '../../../scss/conf.scss';
    .bk-cascader {
        &.open .bk-selector-icon {
            transform: rotate(180deg);
        }
        .bk-selector-icon.disabled {
            cursor: no-drop;
        }
        .bk-cascader-list {
            display: flex;
            max-width: 480px;
            position: absolute;
            top: 38px;
            left: 0;
            box-shadow: 0px 3px 6px 0px rgba(51,60,72,0.1);
            background-color: #fff;
            z-index: 100;
            overflow-y: hidden;
            border-radius: 2px;
            ul {
                padding: 0;
                margin: 0;
                list-style: none;
                overflow-y: auto;
                max-height: 320px;
                border: 1px solid $borderWeightColor;
                width: 160px;
                float: left;
            }
            .bk-cascader-list-ulg {
                width: 280px;
                li, .bk-cascader-node, .bk-tooltip {
                    height: 38px;
                }
            }
            .bk-cascader-group-list-item.bk-cascader-selected,
            .bk-cascader-group-list-item:hover {
                background-color: inherit;
                color: inherit;
            }
            .bk-tooltip {
                width: 100%;
                .bk-tooltip-rel {
                    width: 100%;
                }
                .bk-tooltip-inner {
                    min-height: 30px;
                    color: $dangerColor;
                    background-color: #ffe6e6;
                    border: 1px solid $dangerColor;
                    border-radius: 2px;
                    padding: 4px 9px;
                }
                .bk-tooltip-popper[x-placement^="top"] {
                    margin-left: 10px;
                    margin-top: 10px;
                    padding-left: 8px;
                    .bk-tooltip-arrow {
                        border-top-color: $dangerColor;
                        &:after{
                            content: '';
                            position: absolute;
                            width: 0;
                            height: 0;
                            border-color: transparent;
                            border-style: solid;
                            bottom: 2px;
                            left: -5px;
                            border-width: 5px 5px 0;
                            border-top-color: #ffe6e6;
                        }
                    }
                }
            }
        }
        .bk-cascader-groups {
            display: block;
            width: 100%;
            .bk-cascader-group-name {
                height: 38px;
                line-height: 35px;
                background: #fafbfd;
                padding-left: 15px;
                font-weight: bold;
                font-size: 14px;
                color: #63656E;
                border-bottom: 1px solid $borderWeightColor;
                border-top: 1px solid $borderWeightColor;
            }
            ul {
                width: 100%;
                float: none;
                height: auto;
                max-height: 425px;
            }
            .bk-cascader-group-list {
                height: 100%;
                border: none;
                .text {
                    padding-left: 34px;
                    height: 34px;
                }
            }
        }
        .bk-cascader-list-item {
            position:relative;
            width:100%;
            border-left:#c3cdd7;
            border-right:#c3cdd7;
            background-color:#fff;
            cursor:pointer;
            &:first-child {
                border-top:#c3cdd7;
            }
            &:last-child {
                border-bottom:#c3cdd7;
            }
            &.bk-cascader-selected, &:hover {
                background-color:#eef6fe;
                color:#3c96ff;
            }
            .text {
                padding:0 14px;
                line-height: 38px;
                font-size: 14px;
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;
            }
        }
        .bk-cascader-node {
            i {
                position: absolute;
                right: 11px;
                top: 0;
                line-height: 38px;
            }
            &.is-disabled {
                color:#c3cdd7;
                background:#fff;
                cursor:not-allowed;
            }
            &.bk-cascader-selected {
                background-color:#eef6fe;
                color:#3c96ff;
            }
            .unread-icon {
                display: inline-block;
                position: relative;
                top: -2px;
                // left: 0;
                width: 6px;
                height: 6px;
                border-radius: 50%;
                background-color: $iconFailColor;
            }
            .new-service-icon,
            .hot-service-icon {
                display: inline-block;
                width: 26px;
                height: 14px;
                background-color: #00C873;
                color: #fff;
                font-size: 10px;
                text-align: center;
                line-height: 13px;
            }
            .hot-service-icon {
                line-height: 12px;
                background-color: $iconFailColor;
            }
        }
    }
</style>
