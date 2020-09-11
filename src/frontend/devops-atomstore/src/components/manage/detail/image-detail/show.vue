<template>
    <article>
        <section class="show-detail">
            <img :src="detail.logoUrl || defaultPic" class="detail-img">
            <ul class="detail-items" ref="detail">
                <li class="detail-item">
                    <span class="item-name">{{ detail.imageName }}</span>
                </li>
                <li class="detail-item">
                    <span class="detail-label">{{ $t('store.标识') }}：</span>
                    <span>{{ detail.imageCode || '--' }}</span>
                </li>
                <li class="detail-item">
                    <span class="detail-label">{{ $t('store.范畴') }}：</span>
                    <span>{{ detail.categoryName || '--' }}</span>
                </li>
                <li class="detail-item">
                    <span class="detail-label">{{ $t('store.分类') }}：</span>
                    <span>{{ detail.classifyName || '--' }}</span>
                </li>
                <li class="detail-item">
                    <span class="detail-label">{{ $t('store.功能标签') }}：</span>
                    <label-list :label-list="detail.labelList.map(x => x.labelName)"></label-list>
                </li>
                <li class="detail-item">
                    <span class="detail-label">{{ $t('store.适用机器') }}：</span>
                    <label-list :label-list="detail.agentTypeScope" :formatter="agentFilter"></label-list>
                </li>
                <li class="detail-item">
                    <span class="detail-label">{{ $t('store.简介') }}：</span>
                    <span>{{ detail.summary || '--' }}</span>
                </li>
                <li class="detail-item">
                    <span class="detail-label">{{ $t('store.详细描述') }}：</span>
                    <mavon-editor
                        :editable="false"
                        default-open="preview"
                        :subfield="false"
                        :toolbars-flag="false"
                        :external-link="false"
                        :box-shadow="false"
                        preview-background="#fff"
                        v-model="detail.description"
                    />
                </li>
                <li class="detail-item">
                    <span class="detail-label">{{ $t('store.镜像') }}：</span>
                    <span>{{((detail.imageRepoUrl ? detail.imageRepoUrl + '/' : '') + (detail.imageRepoName ? detail.imageRepoName + ':' : '') + detail.imageTag) || '--'}}</span>
                </li>
                <li class="detail-item">
                    <span class="detail-label">{{ $t('store.镜像凭证') }}：</span>
                    <span>{{detail.ticketId || '--'}}</span>
                </li>
                <li class="detail-item">
                    <span class="detail-label">{{ $t('store.发布者') }}：</span>
                    <span>{{detail.publisher || '--'}}</span>
                </li>
                <li class="detail-item">
                    <span class="detail-label">{{ $t('store.发布类型') }}：</span>
                    <span>{{detail.releaseType|releaseFilter}}</span>
                </li>
                <li class="detail-item">
                    <span class="detail-label">{{ $t('store.版本') }}：</span>
                    <span>{{detail.version || '--'}}</span>
                </li>
                <li class="detail-item">
                    <span class="detail-label">{{ $t('store.发布描述') }}：</span>
                    <span>{{detail.versionContent || '--'}}</span>
                </li>
            </ul>
        </section>
    </article>
</template>

<script>
    import labelList from '../../../labelList'
    import defaultPic from '../../../../images/defaultPic.svg'

    export default {
        filters: {
            releaseFilter (value) {
                const local = window.devops || {}
                let res = ''
                switch (value) {
                    case 'NEW':
                        res = local.$t('store.初始化')
                        break
                    case 'INCOMPATIBILITY_UPGRADE':
                        res = local.$t('store.非兼容升级')
                        break
                    case 'COMPATIBILITY_UPGRADE':
                        res = local.$t('store.兼容式功能更新')
                        break
                    case 'COMPATIBILITY_FIX':
                        res = local.$t('store.兼容式问题修正')
                        break
                }
                return res
            }
        },

        components: {
            labelList
        },

        props: {
            detail: Object
        },

        data () {
            return {
                defaultPic
            }
        },

        methods: {
            agentFilter (value) {
                const local = window.devops || {}
                let res = ''
                switch (value) {
                    case 'DOCKER':
                        res = local.$t('store.Devnet 物理机')
                        break
                    case 'IDC':
                        res = 'IDC CVM'
                        break
                    case 'PUBLIC_DEVCLOUD':
                        res = 'DevCloud'
                        break
                }
                return res
            }
        }
    }
</script>
