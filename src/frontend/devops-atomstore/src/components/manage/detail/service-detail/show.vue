<template>
    <article>
        <section class="show-detail">
            <img :src="detail.logoUrl || defaultPic" class="detail-img">
            <ul class="detail-items" ref="detail">
                <li class="detail-item">
                    <span class="item-name">{{ detail.serviceName }}</span>
                </li>
                <li class="detail-item">
                    <span class="detail-label">{{ $t('store.标识') }}：</span>
                    <span>{{ detail.serviceCode || '--' }}</span>
                </li>
                <li class="detail-item">
                    <span class="detail-label">{{ $t('store.扩展点') }}：</span>
                    <span>{{ detail.itemName || '--' }}</span>
                </li>
                <li class="detail-item">
                    <span class="detail-label">{{ $t('store.调试项目') }}：</span>
                    <span>{{ detail.projectCode || '--' }}</span>
                </li>
                <li class="detail-item">
                    <span class="detail-label">{{ $t('store.功能标签') }}：</span>
                    <label-list :label-list="detail.labelList.map(x => x.labelName)"></label-list>
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
                    <span class="detail-label">{{ $t('store.媒体信息') }}：</span>
                    <media-list class="media-list" :list="detail.mediaList" v-if="detail.mediaList.length"></media-list>
                </li>
                <slot></slot>
            </ul>
        </section>
    </article>
</template>

<script>
    import labelList from '../../../labelList'
    import mediaList from '@/components/common/mediaList'
    import defaultPic from '../../../../images/defaultPic.svg'

    export default {
        components: {
            labelList,
            mediaList
        },

        props: {
            detail: Object
        },

        data () {
            return {
                defaultPic
            }
        }
    }
</script>
