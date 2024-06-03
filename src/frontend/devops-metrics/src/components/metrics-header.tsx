import {
  defineComponent,
} from 'vue';


export default defineComponent({
  props: {
    title: String,
  },

  setup(props, { slots }) {
    return () => (
      <h3 class="metrics-header">
        { slots.default?.() ?? props.title }
      </h3>
    );
  },
});
