<template>
    <article class="setting-private">
        <h3 class="private-title">私有配置</h3>

        <section class="private-main" v-bkloading="{ isLoading }" v-if="privateList.length">
            <bk-button theme="primary" title="新增配置" @click="showAdd = true">新增配置</bk-button>
            <bk-table style="margin-top: 15px;" :data="privateList">
                <bk-table-column label="名称" prop="fieldName" width="180"></bk-table-column>
                <bk-table-column label="描述" prop="fieldDesc"></bk-table-column>
                <bk-table-column label="修改者" prop="modifier" width="180"></bk-table-column>
                <bk-table-column label="修改时间" prop="updateTime" width="180"></bk-table-column>
                <bk-table-column label="操作" width="120" class-name="handler-btn">
                    <template slot-scope="props">
                        <span class="update-btn" @click="handleEdit(props.row)">编辑</span>
                        <span class="update-btn" @click="handleDelete(props.row, props.$index)">删除</span>
                    </template>
                </bk-table-column>
            </bk-table>
        </section>

        <section class="g-empty private-empty" v-else>
            暂无私有配置
            <p>
                插件级别的敏感配置信息，可以在此添加，不暴露在插件代码中。插件逻辑中用到时从输入获取即可
            </p>
            <button @click="showAdd = true">添加</button>
        </section>

        <transition name="atom-fade">
            <private-form v-if="showAdd" :sensitive-conf-id="sensitiveConfId" :private-obj="privateObj" @cancle="cancleOperate" @refresh="getAllConf"></private-form>
        </transition>
    </article>
</template>

<script>
    import { mapActions } from 'vuex'
    import privateForm from '@/components/common/privateForm'

    export default {
        components: {
            privateForm
        },

        data () {
            return {
                privateList: [],
                isLoading: false,
                showAdd: false,
                sensitiveConfId: '',
                privateObj: {
                    fieldName: '',
                    fieldValue: '',
                    fieldDesc: ''
                }
            }
        },

        created () {
            this.getAllConf()
        },

        methods: {
            ...mapActions('store', ['getSensitiveConf', 'deleteSensitiveConf']),

            getAllConf () {
                this.isLoading = true
                this.getSensitiveConf(this.$route.params.atomCode).then((res) => {
                    this.privateList = res || []
                }).catch(err => this.$bkMessage({ message: err.message || err, theme: 'error' })).finally(() => {
                    this.isLoading = false
                })
            },

            cancleOperate () {
                this.showAdd = false
                this.sensitiveConfId = ''
                Object.assign(this.privateObj, { fieldName: '', fieldValue: '', fieldDesc: '' })
            },

            handleEdit (row) {
                this.sensitiveConfId = row.fieldId
                Object.assign(this.privateObj, row)
                delete this.privateObj.fieldId
                this.showAdd = true
            },

            handleDelete ({ fieldId, fieldName }, index) {
                const h = this.$createElement
                const subHeader = h('p', {
                    style: {
                        textAlign: 'center'
                    }
                }, `确定删除${fieldName}？`)

                this.$bkInfo({
                    title: '删除',
                    subHeader,
                    confirmFn: () => {
                        const data = { atomCode: this.$route.params.atomCode, id: fieldId }
                        this.deleteSensitiveConf(data).then(() => {
                            this.privateList.splice(index, 1)
                            this.$bkMessage({ message: '删除成功', theme: 'success' })
                        }).catch((err) => {
                            this.$bkMessage({ message: (err.message || err), theme: 'error' })
                        })
                    }
                })
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import './../../assets/scss/conf';

    .setting-private {
        height: 100%;
        .private-title {
            height: 60px;
            line-height: 60px;
            font-size: 16px;
            font-weight: normal;
            padding-left: 20px;
            background-color: $white;
            border-bottom: 1px solid $borderWeightColor;
            box-shadow: 0px 2px 5px 0px rgba(51, 60, 72, 0.03);
        }
        .private-main {
            padding: 20px;
        }
        .private-empty {
            margin-top: 90px;
            width: 100%;
            font-size: 18px;
            line-height: 26px;
            color: $fontBlack;
            margin-bottom: 24px;
            p {
                margin-top: 10px;
                margin-bottom: 20px;
                color: $fontWeightColor;
                font-size: 14px;
            }
            button {
                background: $primaryColor;
                border: 1px solid $primaryColor;
                color: $white;
                height: 36px;
                line-height: 34px;
                border-radius: 2px;
                padding: 0 19px;
                text-align: center;
                font-size: 14px;
            }
        }
        
        .private-table {
            margin-top: 20px;
            width: 100%;
            border: 1px solid $borderWeightColor;
            text-align: left;
            .private-wider {
                min-width: 170px;
            }
            tbody {
                background-color: $white;
                tr:hover {
                    background: $bgHoverColor;
                }
            }
            th {
                height: 42px;
                padding: 2px 10px;
                color: $fontBlack;
                font-weight: normal;
                &:first-child {
                    padding-left: 20px;
                }
            }
            td {
                height: 42px;
                padding: 2px 10px;
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;
                &:first-child {
                    padding-left: 20px;
                }
                &:last-child {
                    padding-right: 30px;
                }
            }
            .handler-btn {
                span {
                    display: inline-block;
                    margin-right: 20px;
                    color: $primaryColor;
                    cursor: pointer;
                }
            }
        }
    }
</style>
