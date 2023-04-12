<template>
    <ul class="pipeline-label-selector" v-bkloading="{ isLoading }">
        <template v-if="tagSelectModelList.length > 0">
            <li
                v-for="item in tagSelectModelList"
                :key="item.id"
            >
                <label class="pipeline-selector-label"> {{item.name}} </label>
                <bk-select
                    class="sub-label-select"
                    :value="labelIdMap[item.id]"
                    @change="item.handleChange"
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
        <span class="no-label-placeholder" v-else>
            {{$t('noLabels')}}
        </span>
    </ul>
</template>

<script>
    import { mapActions, mapState } from 'vuex'
    export default {
        emits: ['input', 'change'],
        props: {
            value: {
                type: Object,
                default: () => ({})
            }
        },
        data () {
            return {
                isLoading: false,
                labelIdMap: this.value
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
                this.updateValue()
                this.isLoading = false
            },
            updateValue (val = {}) {
                this.labelIdMap = this.tagGroupList.reduce((acc, tag) => {
                    acc[tag.id] = val[tag.id] ?? []
                    return acc
                }, {})
            },
            handleChange (groupId, labelIds) {
                this.labelIdMap[groupId] = labelIds
                this.emitChange(this.labelIdMap)
            },
            emitChange (labelIdMap) {
                this.$emit('change', labelIdMap)
                this.$emit('input', labelIdMap)
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
        > li {
            display: flex;
            &:not(:last-child) {
                padding-bottom: 16px;
            }
            .pipeline-selector-label {
                width: 80px;
                text-align: right;
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
