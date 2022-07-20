import {
  defineComponent,
  PropType,
  watch,
  onMounted,
  onBeforeUnmount,
  ref,
} from 'vue';
import BKChart from '@blueking/bkcharts';
import useColor from '@/composables/use-color';

export interface IData {
  labels: Array<string>,
  list: Array<string | number>
  errorTypes: Array<number>
}

export default defineComponent({
  props: {
    data: Object as PropType<IData>,
  },

  emits: ['doughnut-click'],

  setup(props, { emit }) {
    const canvasRef = ref(null);
    let chart;

    const destoryChart = () => {
      chart?.destroy();
    };
    const draw = () => {
      destoryChart();
      const { data } = props;
      chart = new BKChart(canvasRef.value, {
        type: 'doughnut',
        data: {
          labels: data.labels,
          datasets: [{
            backgroundColor: useColor(),
            borderAlign: 'center',
            borderWidth: 2,
            clip: 1.5,
            data: [...data.list],
            hoverBorderWidth: 1,
            weight: 1,
          }],
        },
        options: {
          maintainAspectRatio: false,
          responsive: true,
          aspectRatio: 1.5,
          cutoutPercentage: 20,
          rotation: -1.5707963267948966,
          plugins: {
            tooltip: {
              bodySpacing: 10
            },
            legend: {
              position: 'bottom',
              labels: {
                padding: 30
              },
            },
            animation: {
              animateRotate: true,
              animateScale: false,
            },
          },
          onClick (_, datasets) {
            if (datasets.length > 0) {
              emit('doughnut-click', datasets)
            }
          }
        },
      });
    };

    watch(
      props.data,
      draw,
    );

    onMounted(draw);

    onBeforeUnmount(destoryChart);

    return () => (
      <div class="canvas-wrapper">
        <canvas ref={canvasRef}></canvas>
      </div>
    );
  },
});
