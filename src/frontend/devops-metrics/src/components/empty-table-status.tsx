import { defineComponent } from "vue";
import { useI18n } from "vue-i18n";

export default defineComponent({
    props: {
        type: {
            type: String,
            default: 'empty'
        }
    },

    setup(props) {
        const { t } = useI18n();
        const typeMap = {
            empty: t('noData'),
            'search-empty': t('searchEmpty')
        } 
        function handleClear () {

        }
        return () => (
            <bk-exception type={props.type} scene="part">
                <div style="font-size: 14px;">{ typeMap[props.type] }</div>
                {props.type === 'search-empty' && <template>
                    <i18n-t 
                        tag="div"
                        keypath="可以尝试 调整关键词 或 清空筛选条件"
                        class="empty-tips"
                    >
                        <bk-button
                            text
                            theme="primary"
                            onClick={handleClear}
                        >
                            {t('清空筛选条件')}
                        </bk-button>
                    </i18n-t>
                </template>}
            </bk-exception>
        )
    }
})