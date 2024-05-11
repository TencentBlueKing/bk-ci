<template>
    <div class="record-list">
        <div class="search-area">
            <input :placeholder="$t('searchForMore')" v-bk-focus="1" :disabled="searching" @compositionstart="handleCompositionStart" @compositionend="handleCompositionEnd" @input="handleInput" v-model.trim="searchValue" />
            <i class="devops-icon icon-search"></i>
        </div>
        <div v-if="searching" class="record-list-searching-icon">
            <i class="devops-icon icon-circle-2-1 spin-icon" />
        </div>
        <ul v-else-if="records.length">
            <li v-for="item in records" :title="item[paramName]" :key="item[paramId]" :class="{ 'active': selectedValue === item[paramName] }" @click.stop="handleRecordClick(item)">
                {{ item[paramName] }}
            </li>
        </ul>
        <ul v-else>
            <empty-exception slot="empty" type="search-empty" @clear="clearFilter" />
        </ul>
    </div>
</template>

<script>
    import EmptyException from '@/components/common/exception'
    export default {
        name: 'crumb-records',
        components: {
            EmptyException
        },
        props: {
            records: {
                type: Array,
                default: []
            },
            handleRecordClick: {
                type: Function
            },
            searching: {
                type: Boolean
            },
            selectedValue: {
                type: String,
                default: ''
            },
            paramId: {
                type: String,
                default: 'id'
            },
            paramName: {
                type: String,
                default: 'name'
            }
        },
        data () {
            return {
                searchValue: '',
                isInputZH: false // 当前是否输入中文
            }
        },
        methods: {
            handleInput (e) {
                if (this.isInputZH) return
                this.$emit('searchInput', this.searchValue, e)
            },
            clearFilter () {
                this.searchValue = ''
                this.$emit('searchInput', this.searchValue)
            },
            handleCompositionStart () {
                this.isInputZH = true
            },
            handleCompositionEnd () {
                this.isInputZH = false
            }
        }
    }
</script>

<style lang="scss">
    @import '../../scss/mixins/ellipsis';
    @import "../../scss/conf";

    .record-list {
        position: absolute;
        background: white;
        width: 280px;
        border-radius: 2px;
        box-shadow: 0 0 8px 1px rgba(0,0,0,0.1);
        z-index: 100;
        top: 52px;
        .search-area {
            position: relative;
            border-bottom: 1px solid #e5e5e5;
            cursor: default;
            padding: 5px;
            line-height: 32px;
            input {
                width: 100%;
                height: 32px;
                line-height: 32px;
                padding: 10px;
                font-size: 14px;
                -webkit-box-shadow: none;
                box-shadow: none;
                outline: none;
                background-color: #fafbfd;
                border: 1px solid #dde4eb;
                border-radius: 2px;
                color: #63656E;
                &::placeholder { /* Chrome, Firefox, Opera, Safari 10.1+ */
                    color: #ccc;
                }
            }
            > i.icon-search {
                position: absolute;
                right: 14px;
                top: 5px;
                height: 30px;
                line-height: 30px;
                color: #ccc;
            }
        }
        .record-list-searching-icon {
            display: flex;
            margin: 20px 0;
            justify-content: center;
        }
        ul {
            overflow: auto;
            max-height: 360px;
            > li {
                height: 42px;
                line-height: 42px;
                width: 100%;
                border-left: #c3cdd7;
                border-right: #c3cdd7;
                background-color: #fff;
                cursor: pointer;
                text-align: left;
                padding: 0 10px;
                font-size: 14px;
                @include ellipsis();

                &:hover,
                &.active {
                    background-color: #eef6fe;
                    color: #3c96ff;
                }
            }
        }
    }
</style>
