<template>
    <ul
        class="pipeline-label-selector"
        ref="labelSelectorParent"
        v-bkloading="{ isLoading }"
    >
        <template v-if="tagSelectModelList.length > 0">
            <li
                v-for="item in tagSelectModelList"
                :key="item.id"
            >
                <label class="pipeline-selector-label"> {{ item.name }} </label>
                <bk-select
                    :disabled="!editable"
                    class="sub-label-select"
                    :value="labelMap[item.id]"
                    @change="item.handleChange"
                    :popover-options="{
                        appendTo: $refs.labelSelectorParent
                    }"
                    multiple
                >
                    <bk-option
                        v-for="label in item.labels"
                        :key="label.id"
                        :id="label.id"
                        :name="label.name"
                    >
                    </bk-option>
                </bk-select>
            </li>
        </template>
        <span
            class="no-label-placeholder"
            v-else
        >
            {{ $t('noLabels') }}
        </span>
    </ul>
</template>

<script>
    import { mapActions, mapState } from 'vuex'
    export default {
        emits: ['input', 'change'],
        props: {
            value: {
                type: Array,
                default: () => []
            },
            editable: {
                type: Boolean,
                default: true
            }
        },
        data () {
            return {
                isLoading: false,
                labelMap: {},
                labelSet: new Set(this.value)
            }
        },
        computed: {
            ...mapState('pipelines', [
                'tagGroupList'
            ]),
            tagSelectModelList () {
                return this.tagGroupList.map(item => ({
                    ...item,
                    handleChange: (...args) => this.handleChange(item.id, ...args)
                }))
            }
        },
        watch: {
            value (newVal) {
                this.updateValue(newVal)
                this.labelSet = new Set(newVal)
            }
        },
        created () {
            this.init()
        },
        methods: {
            ...mapActions('pipelines', [
                'requestTagList'
            ]),
            async init () {
                this.isLoading = true
                await this.requestTagList(this.$route.params)
                this.updateValue(this.value)
                this.emitChange(Array.from(this.labelSet), this.labelMap)
                this.isLoading = false
            },
            updateValue (val = []) {
                const labelIdMap = this.tagGroupList.map(tag => tag.labels).flat().reduce((acc, label) => {
                    acc[label.id] = label
                    return acc
                }, {})
                this.labelMap = this.tagGroupList.reduce((acc, tag) => {
                    acc[tag.id] = val.filter(id => labelIdMap[id]?.groupId === tag.id)
                    return acc
                }, {})
            },
            handleChange (groupId, labelIds) {
                if (labelIds.length === 0) {
                    this.labelMap[groupId].forEach(id => {
                        this.labelSet.delete(id)
                    })
                } else {
                    this.labelSet = new Set([
                        ...this.labelSet,
                        ...labelIds
                    ])
                }
                this.labelMap[groupId] = labelIds

                this.emitChange(Array.from(this.labelSet), this.labelMap)
            },
            emitChange (value, labelMap) {
                this.$emit('change', value, labelMap)
                this.$emit('input', value, labelMap)
            }
        }
    }
</script>
<style lang="scss">
    @import '@/scss/conf';
    @import '@/scss/mixins/ellipsis';

    .pipeline-label-selector {
        border-radius: 2px;
        border: 1px solid #DCDEE5;
        padding: 16px;
        display: grid;
        grid-gap: 16px;
        > li {
            width: 100%;
            display: flex;
            overflow: hidden;
            .pipeline-selector-label {
                width: 80px;
                text-align: right;
                font-size: 12px;
                @include ellipsis();
                margin-right: 22px;
            }
            .sub-label-select {
                flex: 1;
                overflow: hidden;
            }

        }
        .no-label-placeholder {
            display: flex;
            align-items: center;
            justify-content: center;
            color: #979BA5;
            font-size: 12px;
        }
    }
</style>
