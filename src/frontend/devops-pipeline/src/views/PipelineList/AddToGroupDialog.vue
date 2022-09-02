<template>
    <bk-dialog
        ext-cls="add-group-dialog"
        width="800"
        :value="addToDialogShow"
        :close-icon="false"
        :draggable="false"
        @cancel="handleClose"
    >
        <main v-if="addToDialogShow" class="add-group-main">
            <aside class="add-group-left">
                <header>
                    {{ $t('addTo') }}
                </header>
                <p>
                    {{ $t('addToGroupTitle', [pipeline.pipelineName]) }}
                </p>
                <bk-input :placeholder="$t('searchPipelineGroup')" v-model="filterKeyword" right-icon="bk-icon icon-search" />
                <ul class="add-to-pipeline-group-list">
                    <li v-for="(item, index) in pipelineGroups" :key="index" :class="{ disabled: item.disabled }">
                        <bk-checkbox
                            :disabled="item.disabled"
                            :value="selectedGroupNameMap[item.name]"
                            @change="(checked) => handleChecked(checked, item)"
                        ></bk-checkbox>
                        <span class="add-to-pipeline-group-item-name">{{item.name}}</span>
                        <span v-if="item.disabled">{{$t('added')}}</span>
                    </li>
                </ul>
            </aside>
            <aside class="add-group-right">
                <header>
                    {{ $t('resultPreview') }}
                </header>
                <p>
                    <span>{{$t('selectedGroup', [selectedGroups.length])}}</span>
                    <bk-button v-if="selectedGroups.length > 0" text @click="emptySelectedGroups">{{$t('newlist.reset')}}</bk-button>
                </p>
                <ul class="add-group-result-preview-list">
                    <li v-for="(group, index) in selectedGroups" :key="group.id">
                        <span class="add-selected-group-name">{{ group.name }}</span>
                        <span @click="remove(index)" class="bk-icon icon-close">
                        </span>
                    </li>
                </ul>
            </aside>
        </main>
    </bk-dialog>
</template>

<script>
    import { mapState } from 'vuex'
    export default {

        props: {
            pipeline: {
                type: Object,
                default: () => ({})
            },
            addToDialogShow: Boolean
        },
        data () {
            return {
                selectedGroups: [],
                filterKeyword: ''
            }
        },
        computed: {
            ...mapState('pipelines', [
                'allPipelineGroup'
            ]),
            pipelineGroups () {
                return this.allPipelineGroup.filter(group => group.viewType === 2 && group.name.indexOf(this.filterKeyword) > -1)
                    .map(group => ({
                        ...group,
                        disabled: this.selectedGroupNameMap[group.name]
                    }))
            },
            groupIdMap () {
                return this.pipelineGroups.reduce((acc, group) => ({
                    ...acc,
                    [group.name]: group.id
                }), {})
            },
            selectedGroupNameMap () {
                return this.selectedGroups.reduce((acc, group) => ({
                    ...acc,
                    [group.name]: true
                }), {})
            }
        },
        watch: {
            pipeline: {
                handler: function (pipeline) {
                    this.selectedGroups = pipeline?.viewNames.map(name => ({
                        id: this.groupIdMap[name],
                        name
                    })) ?? []
                },
                immediate: true
            }
        },
        methods: {
            handleClose () {
                this.$emit('close')
            },
            handleChecked (checked, { id, name }) {
                if (checked) {
                    this.selectedGroups.push({
                        id,
                        name
                    })
                }
            },
            emptySelectedGroups () {
                this.selectedGroups = []
            },
            remove (index) {
                this.selectedGroups.splice(index, 1)
            }
        }
    }
</script>

<style lang="scss">
    @import '@/scss/mixins/ellipsis';
    .add-group-dialog {
        .bk-dialog-tool {
            display: none;
        }
        .bk-dialog-body {
            padding: 0;
        }
        .add-group-main {
            display: flex;
            height: 480px;

            .add-group-left {
                display: flex;
                flex-direction: column;
                flex: 3;
                background: white;
                padding: 24px;
                overflow: hidden;
                > p {
                    font-size: 12px;
                    margin: 16px 0;
                    color: #979BA5;
                }
                .add-to-pipeline-group-list {
                    flex: 1;
                    overflow: auto;
                    margin-top: 8px;
                    > li {
                        display: flex;
                        align-items: center;
                        justify-content: space-between;
                        padding: 0 16px 0 24px;
                        font-size: 12px;
                        height: 32px;
                        &:hover {
                            background: #E1ECFF;
                        }
                        &.disabled {
                            color: #C4C6CC;
                        }
                        .add-to-pipeline-group-item-name {
                            flex: 1;
                            margin: 0 10px;
                            @include ellipsis();
                        }
                    }
                }
            }
            .add-group-right {
                flex: 2;
                padding: 24px;
                background: #F5F7FA;
                box-shadow: -1px 0 0 0 #DCDEE5;
                overflow: hidden;
                > p {
                    margin: 16px 0;
                    > span {
                        padding-right: 10px;
                    }
                }
                .add-group-result-preview-list {
                    > li {
                        font-size: 12px;
                        height: 30px;
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                        background: white;
                        padding: 0 10px;
                        .add-selected-group-name {
                            flex: 1;
                            @include ellipsis();
                        }
                        .icon-close {
                            font-size: 20px;
                            cursor: pointer;
                            padding: 5px;
                        }
                        &:hover {
                            background: #E1ECFF;
                        }
                    }
                }
            }
        }
    }
</style>
