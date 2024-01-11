<template>
    <ul v-if="outputsList.length > 0" class="pipeline-exec-artifact-list">
        <li v-for="output in outputsList" :key="output.title">
            <header @click="collapsedMenu(output)">
                <i :class="['devops-icon icon-down-shape', {
                    'is-collaped': output.collapsed
                }]" />
                {{ output.title }}
            </header>
            <ul v-show="!output.collapsed" class="pipeline-exec-sub-artifact-list">
                <li v-for="item in output.children" :key="item.title" class="pipeline-exec-sub-sub-artifact-list">
                    <header>#{{ item.title }}</header>
                    <ul>
                        <li v-for="subItem in item.children" :key="subItem.id">
                            <span>{{ subItem.name }}</span>
                        </li>
                    </ul>
                </li>
            </ul>
        </li>
    </ul>
</template>

<script>
    export default {
        props: {
            outputs: {
                type: Array,
                default: () => []
            }
        },
        data () {
            return {
                outputsList: this.formatOutputs(this.outputs)
            }
        },
        mounted () {
            console.log(this.outputs)
        },
        methods: {
            collapsedMenu (output) {
                output.collapsed = !output.collapsed
            },
            formatOutputs (outputs) {
                console.log(outputs)
                const buildNumMap = outputs.reduce((acc, output) => {
                    if (!acc.has(output.buildNum)) {
                        acc.set(output.buildNum, {
                            title: output.buildNum,
                            children: []
                        })
                    }
                    acc.get(output.buildNum).children.push(output)
                    return acc
                }, new Map())
                console.log(buildNumMap)
                const createTimeMap = {}
                buildNumMap.forEach((value) => {
                    const outputChildren = value.children
                    const firstChild = outputChildren[0]
                    if (!createTimeMap[firstChild.createTime]) {
                        createTimeMap[firstChild.createTime] = []
                    }

                    createTimeMap[firstChild.createTime] = [
                        value,
                        ...createTimeMap[firstChild.createTime]
                    ]
                })
                console.log(createTimeMap)
                return Object.keys(createTimeMap).reduce((acc, createTime) => {
                    acc.push({
                        title: createTime,
                        collapsed: false,
                        children: createTimeMap[createTime]
                    })
                    return acc
                }, [])
            }
        }
    }
</script>

<style lang="scss">
    .pipeline-exec-artifact-list {
        display: flex;
        flex-direction: column;
        font-size: 12px;
        margin: 0 16px;
        > li {
            border-bottom: 1px solid #DCDEE5;
            > header {
                height: 22px;
                line-height: 22px;
                margin: 12px 0;
                font-size: 14px;
                cursor: pointer;
                .devops-icon.icon-down-shape {
                    display: inline-flex;
                    transition: all 0.3s ease;
                    &.is-collaped {
                        transform: rotate(-90deg);
                    }
                }
            }
        }
        .pipeline-exec-sub-artifact-list {

            .pipeline-exec-sub-sub-artifact-list {
                display: grid;
                grid-auto-flow: column;
                grid-auto-columns: auto 1fr;
                grid-gap: 12px;
                > header {
                    position: relative;
                    line-height: 34px;
                    width: 5ch;
                    &:after {
                        content: '';
                        position: absolute;
                        height: calc(100% - 45px);
                        border-left: 1px dashed #DCDEE5;
                        left: 6px;
                        top: 32px;
                    }

                }
                > ul > li {
                    display: flex;
                    align-items: center;
                    height: 32px;

                }
            }
        }
    }
</style>
