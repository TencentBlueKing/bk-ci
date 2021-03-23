<template>
    <article>
        <section class="show-detail">
            <img :src="detail.logoUrl || defaultPic" class="detail-img">
            <ul class="detail-items" ref="detail">
                <li class="detail-item">
                    <span class="item-name">{{ detail.name }}</span>
                </li>
                <li class="detail-item">
                    <span class="detail-label">{{ $t('store.标识') }}：</span>
                    <span>{{ detail.atomCode || '--' }}</span>
                </li>
                <li class="detail-item">
                    <span class="detail-label">{{ $t('store.范畴') }}：</span>
                    <span>{{ categoryMap[detail.category] || '--' }}</span>
                </li>
                <li class="detail-item">
                    <span class="detail-label">{{ $t('store.分类') }}：</span>
                    <span>{{ detail.classifyName || '--' }}</span>
                </li>
                <li class="detail-item">
                    <span class="detail-label">{{ $t('store.适用机器类型') }}：</span>
                    <div v-if="detail.os">{{ jobTypeMap[detail.jobType] }}
                        <span v-if="detail.jobType === 'AGENT'">（
                            <i class="devops-icon icon-linux-view" v-if="detail.os.indexOf('LINUX') !== -1"></i>
                            <i class="devops-icon icon-windows" v-if="detail.os.indexOf('WINDOWS') !== -1"></i>
                            <i class="devops-icon icon-macos" v-if="detail.os.indexOf('MACOS') !== -1"></i>）
                        </span>
                    </div>
                </li>
                <li class="detail-item">
                    <span class="detail-label">{{ $t('store.发布包') }}：</span>
                    <span>{{ detail.pkgName || '--' }}</span>
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
                <slot></slot>
            </ul>
        </section>
    </article>
</template>

<script>
    import labelList from '../../../labelList'
    import defaultPic from '../../../../images/defaultPic.svg'

    export default {
        components: {
            labelList
        },

        props: {
            detail: Object
        },

        data () {
            return {
                defaultPic,
                categoryMap: {
                    'TASK': this.$t('store.流水线插件'),
                    'TRIGGER': this.$t('store.流水线触发器')
                },
                jobTypeMap: {
                    'AGENT': this.$t('store.编译环境'),
                    'AGENT_LESS': this.$t('store.无编译环境')
                }
            }
        }
    }
</script>

<style lang="scss" scoped>
.show-detail {
    display: flex;
    align-items: flex-start;
    position: relative;
    .detail-img {
        width: 100px;
        height: 100px;
        margin-right: 32px;
    }
    .detail-items {
        flex: 1;
        max-width: calc(100% - 132px);
        overflow-x: hidden;
    }
    .detail-item {
        font-size: 14px;
        line-height: 18px;
        display: flex;
        align-items: flex-start;
        &:not(:nth-child(1)) {
            margin-top: 18px;
        }
    }
    .detail-label {
        color: #999;
        min-width: 100px;
    }
    .item-name {
        font-size: 20px;
        line-height: 24px;
    }
    .overflow {
        max-height: 290px;
        overflow: hidden;
    }
    .summary-all {
        cursor: pointer;
        color: #1592ff;
        font-size: 14px;
        line-height: 20px;
        display: block;
        text-align: center;
        position: absolute;
        bottom: -22px;
        left: 50%;
        transform: translateX(-50%);
        &::before {
            content: '';
            position: absolute;
            top: 4px;
            left: calc(50% - 50px);
            width: 6px;
            height: 6px;
            display: block;
            transform: rotate(-45deg);
            border-left: 2px solid #1592ff;
            border-bottom: 2px solid #1592ff;
        }
    }
}
</style>
