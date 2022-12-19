<template>
    <bk-sideslider
        :is-show.sync="isShow"
        :title="sliderOpt.title"
        :quick-close="sliderOpt.quickClose"
        :width="sliderOpt.width"
        @hidden="hideSlide">
        <div slot="content" class="filter-wrapper">
            <div class="filter-content">
                <div>
                    <div class="form-group">
                        <label for="pipelineName" class="filter-label">{{ $t('pipelineName') }}：</label>
                        <bk-input
                            v-validate.initial="'max:40'"
                            name="pipelineName"
                            id="pipelineName"
                            :placeholder="$t('newlist.filterByNameTips')"
                            :class="{
                                'is-danger': errors.has('pipelineName'),
                                'input-text': true
                            }"
                            clearable
                            v-model.trim="currentFilter.filterByPipelineName"
                            @enter="filterCommit"
                        />
                        <p :class="errors.has('pipelineName') ? 'error-tips' : 'normal-tips'">{{errors.first("pipelineName")}}</p>
                    </div>
                    <div class="form-group">
                        <form-field :label="$t('creator')">
                            <user-input :handle-change="handleChange"
                                name="users"
                                v-model="currentFilter.filterByCreator">
                            </user-input>
                        </form-field>
                    </div>
                    <div class="form-group"
                        v-for="(group, index) in tagGroupList" :key="index">
                        <label class="filter-label">{{group.name}}：</label>
                        <bk-select
                            v-model="currentFilter[group.id]"
                            multiple="true">
                            <bk-option v-for="(option, oindex) in group.labels" :key="oindex" :id="option.id" :name="option.name">
                            </bk-option>
                        </bk-select>
                    </div>
                    <div class="form-group filter-modify">
                        <bk-button theme="primary" size="small" :disabled="isDisabled" @click.stop.prevent="filterCommit">{{ $t('newlist.filter') }}</bk-button>
                        <bk-button text class="btn" @click="resetFilter">{{ $t('newlist.reset') }}</bk-button>
                    </div>
                </div>
            </div>
        </div>
    </bk-sideslider>
</template>

<script>
    import { mapGetters } from 'vuex'
    import FormField from '@/components/AtomPropertyPanel/FormField.vue'
    import UserInput from '@/components/atomFormField/UserInput/index.vue'

    export default {
        components: {
            FormField,
            UserInput
        },
        props: {
            isDisabled: {
                type: Boolean,
                default: false
            },
            isShow: {
                type: Boolean,
                default: false
            },
            selectedFilter: {
                type: Object
            }
        },
        data () {
            return {
                sliderOpt: {
                    title: this.$t('newlist.filterTitle'),
                    quickClose: true,
                    width: 360
                },
                show: true,
                currentFilter: {
                    filterByPipelineName: '',
                    filterByCreator: [],
                    groups: []
                }
            }
        },
        computed: {
            ...mapGetters({
                tagGroupList: 'pipelines/getTagGroupList'
            }),
            projectId () {
                return this.$route.params.projectId
            }
        },
        watch: {
            projectId () {
                this.$emit('filter', {}, {}, false)
            },
            tagGroupList () {
                this.empty()
            }
        },
        created () {
            this.init()
        },
        methods: {
            handleChange (name, value) {
                this.currentFilter.filterByCreator = value
            },
            async init () {
                if (Object.keys(this.selectedFilter).length > 0) {
                    Object.assign(this.currentFilter, this.selectedFilter)
                } else {
                    this.empty()
                }
            },
            handleCurrentFilter () {
                this.tagGroupList.forEach(item => {
                    const res = this.currentFilter.groups.find(iitem => (iitem.id === item.id))
                    if (res) {
                        Object.assign(this.currentFilter, { [item.id]: res.labels })
                    } else {
                        Object.assign(this.currentFilter, { [item.id]: [] })
                    }
                })
            },
            async filterCommit () {
                let labels = []
                let labelIds = ''
                this.tagGroupList.forEach(item => {
                    if (this.currentFilter[item.id] && this.currentFilter[item.id].length > 0) {
                        labels = labels.concat(this.currentFilter[item.id])
                    }
                })
                labelIds = labels.join(',')
                this.isDisabled = true
                await this.$emit('filter', {
                    projectId: this.projectId,
                    filterByLabels: labelIds,
                    filterByPipelineName: this.currentFilter.filterByPipelineName,
                    filterByCreator: this.currentFilter.filterByCreator.join(',')
                }, this.currentFilter)
            },
            resetFilter () {
                this.empty()
                this.$nextTick(this.filterCommit)
            },
            empty () {
                this.currentFilter = {
                    filterByPipelineName: '',
                    filterByCreator: [],
                    groups: []
                }
                this.tagGroupList.forEach(item => {
                    Object.assign(this.currentFilter, { [item.id]: [] })
                })
            },
            hideSlide () {
                this.$emit('showSlide', false)
            }
        }
    }
</script>

<style lang="scss">
    @import './../../../scss/conf';

    .bk-sideslider-wrapper {
        top: 0;
    }
    .pipeline-filter {
        .bk-sideslider-closer {
            display: none;
        }
        .bk-sideslider-title {
            padding: 0 0 0 20px!important;
            color: #333C48;
        }
        .filter-wrapper {
            position: relative;
            padding: 17px 20px;
        }
        .filter-header {
            position: absolute;
            top: -40px;
            right: 21px;
            .menu-content {
                line-height: 20px;
                font-size: 14px;
                color: $fontWeightColor;
                .devops-icon {
                    font-size: 10px;
                    color: $fontLighterColor;
                }
                .menu-btn {
                    position: relative;
                    display: inline-block;
                    width: 250px;
                    padding-right: 20px;
                    vertical-align: middle;
                    text-align: right;
                    white-space: nowrap;
                    text-overflow: ellipsis;
                    overflow: hidden;
                    cursor: pointer;
                    .devops-icon {
                        position: absolute;
                        right: 0;
                        top: 4px;
                        margin-left: 5px;
                    }
                }
                .menu-list {
                    position: absolute;
                    top: 23px;
                    right: 0;
                    width: 240px;
                    max-height: 200px;
                    padding: 10px 0;
                    border: 1px solid $borderWeightColor;
                    border-radius: 2px;
                    box-shadow: 0px 3px 6px 0px rgba(51,60,72,0.15);
                    background-color: #fff;
                    overflow: auto;
                    z-index: 999;
                    li {
                        position: relative;
                        height: 36px;
                        &:hover,&.active {
                            color: $iconPrimaryColor;
                            background-color: #ECF3FF;
                            cursor: pointer;
                        }
                    }
                    a {
                        display: inline-block;
                        width: 100%;
                        padding: 0 30px 0 20px;
                        height: 36px;
                        line-height: 36px;
                        white-space: nowrap;
                        text-overflow: ellipsis;
                        overflow: hidden;
                        color: $fontWeightColor;
                    }
                    .icon-close {
                        position: absolute;
                        top: 12px;
                        right: 15px;
                        cursor: pointer;
                    }
                }
                &.active .menu-list {
                    display: block;
                }
            }

            .bk-dropdown-content {
                width: 240px;
                padding: 10px 0;
                .bk-dropdown-list li {
                    position: relative;
                    a {
                        padding: 0 21px;
                        line-height: 36px;
                    }
                    .devops-icon {
                        display: none;
                        position: absolute;
                        top: 12px;
                        right: 16px;
                        font-size: 10px;
                        &:hover {
                            color: $iconPrimaryColor;
                            cursor: pointer;
                        }

                    }
                    &:hover {
                        background-color: #ebf4ff;
                        .devops-icon {
                            display: inline-block;
                        }
                    }
                }
            }
        }
        .filter-content {
            .form-group {
                margin-bottom: 15px;
                ::-webkit-input-placeholder { /* WebKit browsers */
                    color: $fontLighterColor;
                }
                :-moz-placeholder { /* Mozilla Firefox 4 to 18 */
                    color: $fontLighterColor;
                }
                ::-moz-placeholder { /* Mozilla Firefox 19+ */
                    color: $fontLighterColor;
                }
                :-ms-input-placeholder { /* Internet Explorer 10+ */
                    color: $fontLighterColor;
                }
                .input-text {
                    cursor: text;
                }
                .devops-icon {
                    color: $fontLighterColor;
                    &:hover {
                        color:$fontWeightColor;
                    }
                }
                &.filter-modify {
                    position: relative;
                    a {
                        margin-left: 20px;
                        color: $primaryColor;
                        font-size: 14px;
                        .filter-view {
                            position: absolute;
                            top: 37px;
                            left: 0;
                            width: 320px;
                            padding: 16px 21px 21px 21px;
                            border: 1px solid $borderWeightColor;
                            border-radius: 2px;
                            box-shadow:0px 3px 6px 0px rgba(0,0,0,0.1);
                            font-size: 0;
                            background-color: #fff;
                            &:after {
                                position: absolute;
                                content: '';
                                width: 8px;
                                height: 8px;
                                border: 1px solid $borderWeightColor;
                                border-bottom: 0;
                                border-right: 0;
                                transform: rotate(45deg);
                                background: white;
                                top: -5px;
                                right: 120px;
                            }
                            #viewName  {
                                width: 228px;
                                vertical-align: initial;
                            }
                            .devops-icon {
                                margin-left: 10px;
                                font-size: 10px;
                                cursor: pointer;
                                &:hover {
                                    color: $iconPrimaryColor;
                                }
                            }
                        }
                    }
                    .btn {
                        cursor: pointer;
                    }
                    .bk-button {
                        width: 70px;
                    }
                    .form-group {
                        margin-bottom: 0;
                    }
                }
                .select-tags {
                    margin: 0;
                    .form-input{
                        line-height: 20px;
                    }
                }
            }
            .filter-label, .bk-label {
                display: inline-block;
                margin-bottom: 8px;
                font-size: 14px;
                color: $fontWeightColor;
            }
        }
        .input-text.is-danger {
            border-color: #ff5656;
            background-color: #fff4f4;
            color: #ff5656;
        }
    }
</style>
