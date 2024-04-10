<template>
    <section class="main-body" v-bkloading="{ isLoading }">
        <bk-form class="progress-info">
            <bk-form-item :label="$t('store.可见范围')" :desc="$t('store.未设置可见范围时，仅微扩展成员可以安装到名下项目中使用。设置后，对应组织架构的用户可以在研发商店中安装使用')">
                <ul class="info-visable" v-if="deptInfoList.length">
                    <li v-for="dept in deptInfoList" :key="dept.deptId" class="visable-item">{{ dept.deptName }}<icon name="close" size="10" class="dept-close" @click.native="deleteDept(dept)" /></li>
                </ul>
                <p @click="showDialog = true" class="info-add-vis"><i class="bk-icon icon-plus"></i>{{ $t('store.添加') }}</p>
            </bk-form-item>
            <bk-form-item :label="$t('store.截图')">
                <upload type="PICTURE"
                    :file-list.sync="imageList"
                    :limit="6"
                    :size="2"
                    :tip="$t('store.支持jpg、png、gif、svg格式，不超过6张，每张不超过2M')"
                ></upload>
            </bk-form-item>
            <bk-form-item :label="$t('store.视频教程')">
                <upload type="VIDEO"
                    :file-list.sync="videoList"
                    :limit="4"
                    :size="50"
                    :tip="$t('store.支持mp4、ogg、webm格式，不超过4个，每个不超过50M')"
                ></upload>
            </bk-form-item>
        </bk-form>

        <footer class="main-footer">
            <bk-button theme="primary" @click="submit" :disabled="isToBackIng" :loading="isCommiting"> {{ $t('store.下一步') }} </bk-button>
            <bk-button @click="previousStep" :disabled="isCommiting" :loading="isToBackIng"> {{ $t('store.上一步') }} </bk-button>
        </footer>

        <organization-dialog :show-dialog="showDialog"
            :is-loading="false"
            @saveHandle="saveHandle"
            @cancelHandle="cancelHandle">
        </organization-dialog>
    </section>
</template>

<script>
    import upload from '@/components/upload'
    import organizationDialog from '@/components/organization-dialog'

    export default {
        components: {
            organizationDialog,
            upload
        },

        props: {
            currentStep: {
                type: Object
            },
            detail: {
                type: Object
            }
        },

        data () {
            return {
                imageList: [],
                videoList: [],
                deptInfoList: [],
                isCommiting: false,
                showDialog: false,
                isloading: false,
                isToBackIng: false
            }
        },

        created () {
            this.initData()
        },

        methods: {
            initData () {
                this.isloading = true
                this.$store.dispatch('store/requestServiceVisableList', this.detail.serviceCode).then((res) => {
                    const mediaList = this.detail.mediaList || []
                    this.imageList.push(...mediaList.filter(x => x.mediaType === 'PICTURE'))
                    this.videoList.push(...mediaList.filter(x => x.mediaType === 'VIDEO'))
                    this.deptInfoList = res.deptInfos || []
                }).catch((err) => {
                    this.$bkMessage({ message: err.message || err, theme: 'error' })
                }).finally(() => {
                    this.isloading = false
                })
            },

            deleteDept (dept) {
                const index = this.deptInfoList.findIndex((x) => (x.deptId === dept.deptId))
                this.deptInfoList.splice(index, 1)
            },

            saveHandle (params) {
                const deptInfos = params.deptInfos || []
                deptInfos.forEach((dept) => {
                    const index = this.deptInfoList.findIndex((x) => (x.deptId === dept.deptId))
                    if (index <= -1) this.deptInfoList.push(dept)
                })
                this.showDialog = false
            },

            cancelHandle () {
                this.showDialog = false
            },

            previousStep () {
                const confirmFn = () => {
                    this.isToBackIng = true
                    this.$store.dispatch('store/requestBackToTest', this.detail.serviceId).then(() => {
                        this.$emit('freshProgress', () => (this.isToBackIng = false))
                    }).catch((err) => {
                        this.isToBackIng = false
                        this.$bkMessage({ message: (err.message || err), theme: 'error' })
                    })
                }
                this.$bkInfo({
                    type: 'warning',
                    title: this.$t('store.返回上一步将会清空当前数据'),
                    confirmFn
                })
            },

            submit () {
                this.isCommiting = true
                const postData = {
                    serviceId: this.detail.serviceId,
                    commitInfo: {
                        mediaInfoList: [...this.imageList, ...this.videoList],
                        deptInfoList: this.deptInfoList
                    }
                }
                this.$store.dispatch('store/requestCommitServiceInfo', postData).then(() => {
                    this.$emit('freshProgress', () => (this.isCommiting = false))
                }).catch((err) => {
                    this.isCommiting = false
                    this.$bkMessage({ message: (err.message || err), theme: 'error' })
                })
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import '@/assets/scss/conf.scss';

    .progress-info {
        height: 100%;
        overflow: auto;
    }

    .info-visable {
        .visable-item {
            float: left;
            height: 32px;
            background: #ebedf0;
            border-radius: 2px;
            font-size: 14px;
            text-align: left;
            color: #222222;
            line-height: 14px;
            padding: 9px;
            margin-right: 19px;
            margin-bottom: 8px;
            .dept-close {
                cursor: pointer;
                margin-left: 16px;
                opacity: 0.6;
                color: #6d738b;
                &:hover {
                    opacity: 1;
                    color: #1592ff;
                }
            }
        }
        &:after {
            content: ' ';
            display: table;
            clear: both;
        }
    }

    .info-add-vis {
        cursor: pointer;
        font-size: 14px;
        color: #1592ff;
        line-height: 30px;
        .bk-icon {
            margin-right: 4px;
            font-size: 18px;
        }
    }
</style>
