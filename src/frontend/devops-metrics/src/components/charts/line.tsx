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
  label: string,
  list: Array<string | number>
}

export default defineComponent({
  props: {
    data: Array as PropType<Array<IData>>,
    labels: Array,
    title: String,
    type: String,
  },

  setup(props) {
    const backgroundColors = useColor(0.3);
    const borderColors = useColor();
    const canvasRef = ref(null);
    let chart;

    const destoryChart = () => {
      chart?.destroy();
    };
    const draw = () => {
      destoryChart();
      const { data, labels, title, type } = props;
      chart = new BKChart(canvasRef.value, {
        type: 'line',
        data: {
          labels,
          datasets: data.map((item, index) => ({
            label: item.label,
            backgroundColor: backgroundColors[index],
            borderColor: borderColors[index],
            lineTension: 0,
            borderWidth: 2,
            pointRadius: 2,
            pointHitRadius: 3,
            pointHoverRadius: 3,
            data: [...item.list],
          })),
        },
        options: {
          maintainAspectRatio: false,
          responsive: true,
          plugins: {
            tooltip: {
              bodySpacing: 10,
              mode: 'x',
              intersect: false,
              enableItemActive: true,
              singleInRange: true,
              callbacks: {
                title (data) {
                  return data[0].label
                },
                label (item) {
                  if (type === 'rate') {
                    return item.dataset.label + ': ' + item.raw + '%'
                  }
                  return item.dataset.label + ': ' + item.raw + 'min'
                }
            }
            },
            legend: {
              position: 'bottom',
              legendIcon: 'arc',
              align: 'center',
              labels: {
                padding: 10,
                usePointStyle: true,
                pointStyle: 'dash',
              },
            },
            crosshair: {
              enabled: true,
              mode: 'x',
              style: {
                x: {
                  enabled: true,
                  color: '#cde0ff',
                  weight: 1,
                  borderStyle: 'solid',
                },
                y: {
                  enabled: false,
                },
              },
            },
          },
          scales: {
            yAxes: {
              scaleLabel: {
                display: true,
                padding: 0,
              },
              grid: {
                drawTicks: false,
                borderDash: [5, 5],
              },
              title: {
                display: true,
                text: title,
                align: 'start',
                color: '#979BA5',
              },
              ticks: {
                padding: 10,
                color: '#979BA5',
              },
              min: 0,
            },
            xAxes: {
              scaleLabel: {
                display: true,
                padding: 0,
              },
              grid: {
                drawTicks: false,
                display: false,
              },
              ticks: {
                padding: 10,
                sampleSize: 20,
                autoSkip: true,
                maxRotation: 50,
              },
            },
          },
        },
      });
    };

    watch(
      [
        () => props.data,
        () => props.labels,
      ],
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
