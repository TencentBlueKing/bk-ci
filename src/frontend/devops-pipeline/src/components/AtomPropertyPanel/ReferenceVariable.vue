<template>
    <div class="reference-var">
        <span>引用变量 <i class="bk-icon icon-angle-down"></i><i class="bk-icon icon-angle-up"></i></span>
        <div v-if="globalEnvs" class="env-layout">
            <bk-table
                :data="envsData"
                :show-header="false"
                size="small"
            >
                <bk-table-column v-for="col in columnList" v-bind="col" :key="col.prop">
                    <template v-if="col.prop === 'clipboard'" v-slot="props">
                        <i class="bk-icon icon-clipboard env-copy-icon" :data-clipboard-text="&quot;${&quot; + props.row.name + &quot;}&quot;"></i>
                    </template>
                    <template v-else v-slot="props">
                        {{ props.row[col.prop] }}
                    </template>
                </bk-table-column>
            </bk-table>
        </div>
    </div>
</template>

<script>
    import Clipboard from 'clipboard'
    import { mapGetters } from 'vuex'
    export default {
        props: {
            container: Object,
            stages: {
                type: Array,
                default: []
            },
            globalEnvs: {
                type: Array,
                default: []
            }
        },

        computed: {
            ...mapGetters('atom', [
                'getAppEnvs',
                'hasBuildNo',
                'userParams'
            ]),
            appEnvs () {
                return this.getAppEnvs(this.container.baseOS)
            },
            columnList () {
                return [
                    {
                        prop: 'name',
                        label: '构建号',
                        width: 120
                    },
                    {
                        prop: 'desc',
                        label: '源材料'
                    },
                    {
                        prop: 'clipboard',
                        label: '触发方式',
                        width: 60
                    }
                ]
            },
            userEnvs () {
                // 获取用户全局变量列表
                try {
                    return this.userParams.map(param => ({
                        name: param.id,
                        desc: param.desc
                    }))
                } catch (e) {
                    console.warn(e.message)
                    return []
                }
            },
            buildEnvs () {
                // 根据构建环境依赖，获取环境变量
                try {
                    const { container, appEnvs } = this
                    if (!container.baseOS) return []
                    let buildEnvs = []
                    for (const app in container.buildEnv) {
                        if (container.buildEnv.hasOwnProperty(app) && Array.isArray(appEnvs[app])) {
                            buildEnvs = [
                                ...buildEnvs,
                                ...appEnvs[app]
                            ]
                        }
                    }
                    return buildEnvs
                } catch (e) {
                    console.warn(e.message)
                    return []
                }
            },
            envsData () {
                const { stages, globalEnvs, userEnvs, buildEnvs, hasBuildNo } = this
                const buildNo = hasBuildNo(stages) ? [
                    {
                        name: 'BuildNo',
                        desc: '构建号'
                    }
                ] : []
                return [
                    ...globalEnvs,
                    ...userEnvs,
                    ...buildEnvs,
                    ...buildNo
                ]
            }
        },

        created () {
            this.clipboard = new Clipboard('.env-copy-icon').on('success', e => {
                this.$showTips({
                    theme: 'success',
                    message: '变量复制成功'
                })
            })
        },

        beforeDestroy () {
            this.clipboard.destroy()
        }
    }
</script>

<style lang="scss">
    @import '../../scss/conf';
    @import '../../scss/mixins/ellipsis';
     .reference-var {
            color: $primaryColor;
            padding: 10px;
            > span {
                cursor: pointer;
                .icon-angle-down, .icon-angle-up {
                    font-size: 10px;
                }
                .icon-angle-up {
                   display: none;
                }
            }

            &:hover {
                .env-layout {
                    display: block;
                }
                .icon-angle-down {
                   display: none;
                }
                .icon-angle-up {
                   display: inline-block;
                }
            }
            .env-layout {
                display: none;
                position: absolute;
                background-color: white;
                right: 0;
                top: 52px;
                z-index: 1111;
                width: 585px;
                margin-bottom: 10px;
                box-shadow: 0 3px 7px 0 rgba(0,0,0,0.1);
                transition: all .3s ease;

                &:before {
                    content: '';
                    position: absolute;
                    border: 1px solid $borderWeightColor;
                    border-bottom-color: transparent;
                    border-right-color: transparent;
                    right: 20px;
                    top: -5px;
                    width: 8px;
                    height: 8px;
                    background: white;
                    transform: rotate(45deg);
                }

                .bk-table {
                    border-collapse: collapse;
                    table-layout: fixed;
                    &table,
                    td{
                        border: 1px solid $borderWeightColor;
                        .icon-clipboard {
                            cursor: pointer;
                            font-size: 18px;
                            color: $lineColor;
                            display: flex;
                            width: 100%;
                            height: 100%;
                            align-items: center;
                            justify-content: center;
                            &:hover {
                                color: $primaryColor;
                            }
                        }
                        .tooltip-content {
                           white-space: normal;
                           word-break: break-word;
                        }
                    }
                    tr > td:first-child,
                    tr > th:first-child {
                        width: 210px;
                    }

                    tr > td:last-child,
                    tr > th:last-child {
                        padding: 0;
                        width: 54px;
                        text-align: center;
                    }

                    td,
                    th {
                        @include ellipsis();
                        display: table-cell;
                        height: 40px;

                        .bk-tooltip, .bk-tooltip-rel, .bk-tooltip-rel span {
                            width: 100%;
                        }
                        .bk-tooltip-rel span {
                            @include ellipsis();
                            vertical-align: middle;
                        }
                    }

                    th {
                        border: 0;
                    }
                }
            }
        }
</style>
