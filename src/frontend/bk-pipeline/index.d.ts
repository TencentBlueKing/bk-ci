import { DefineComponent, Plugin } from "vue";

/**
 * Pipeline Stage Container Interface
 */
export interface PipelineContainer {
  "@type"?: string;
  [key: string]: any;
}

/**
 * Pipeline Stage Interface
 */
export interface PipelineStage {
  id: string;
  name: string;
  containers: PipelineContainer[];
  finally?: boolean;
  [key: string]: any;
}

/**
 * Pipeline Model Interface
 */
export interface PipelineModel {
  stages: PipelineStage[];
  [key: string]: any;
}

/**
 * Match Rule Interface
 */
export interface MatchRule {
  [key: string]: any;
}

/**
 * Event Payload Interfaces
 */
export interface ClickEventPayload {
  stageIndex?: number;
  containerIndex?: number;
  atomIndex?: number;
  [key: string]: any;
}

export interface DeleteEventPayload {
  stageId?: string;
  containerId?: string;
  atomId?: string;
  [key: string]: any;
}

export interface AtomReviewEventPayload {
  stageId: string;
  containerId: string;
  atomId: string;
  [key: string]: any;
}

export interface AtomContinueEventPayload {
  stageId: string;
  containerId: string;
  atomId: string;
  [key: string]: any;
}

export interface AtomExecEventPayload {
  stageId: string;
  containerId: string;
  atomId: string;
  [key: string]: any;
}

export interface AtomQualityCheckEventPayload {
  stageId: string;
  containerId: string;
  atomId: string;
  [key: string]: any;
}

export interface AddAtomEventPayload {
  stageIndex: number;
  containerIndex: number;
  atomIndex?: number;
  [key: string]: any;
}

export interface AddStageEventPayload {
  stageIndex: number;
  [key: string]: any;
}

export interface StageCheckEventPayload {
  stageId: string;
  [key: string]: any;
}

export interface StageRetryEventPayload {
  stageId: string;
  [key: string]: any;
}

export interface DebugContainerEventPayload {
  stageId: string;
  containerId: string;
  [key: string]: any;
}

export interface AppendJobEventPayload {
  stageIndex: number;
  [key: string]: any;
}

/**
 * BkPipeline Component Events
 */
export interface BkPipelineEvents {
  /**
   * Emitted when pipeline model changes
   * @param pipeline - Updated pipeline model
   */
  input: (pipeline: PipelineModel) => void;

  /**
   * Emitted when pipeline model changes (alias of input)
   * @param pipeline - Updated pipeline model
   */
  change: (pipeline: PipelineModel) => void;

  /**
   * Emitted when an element is clicked
   * @param payload - Click event payload
   */
  click: (payload: ClickEventPayload) => void;

  /**
   * Emitted when an element is deleted
   * @param payload - Delete event payload
   */
  delete: (payload: DeleteEventPayload) => void;

  /**
   * Emitted when atom review is triggered
   * @param payload - Atom review event payload
   */
  "atom-review": (payload: AtomReviewEventPayload) => void;

  /**
   * Emitted when atom continue is triggered
   * @param payload - Atom continue event payload
   */
  "atom-continue": (payload: AtomContinueEventPayload) => void;

  /**
   * Emitted when atom execution is triggered
   * @param payload - Atom exec event payload
   */
  "atom-exec": (payload: AtomExecEventPayload) => void;

  /**
   * Emitted when atom quality check is triggered
   * @param payload - Atom quality check event payload
   */
  "atom-quality-check": (payload: AtomQualityCheckEventPayload) => void;

  /**
   * Emitted when adding an atom
   * @param payload - Add atom event payload
   */
  "add-atom": (payload: AddAtomEventPayload) => void;

  /**
   * Emitted when adding a stage
   * @param payload - Add stage event payload
   */
  "add-stage": (payload: AddStageEventPayload) => void;

  /**
   * Emitted when stage check is triggered
   * @param payload - Stage check event payload
   */
  "stage-check": (payload: StageCheckEventPayload) => void;

  /**
   * Emitted when stage retry is triggered
   * @param payload - Stage retry event payload
   */
  "stage-retry": (payload: StageRetryEventPayload) => void;

  /**
   * Emitted when debug container is triggered
   * @param payload - Debug container event payload
   */
  "debug-container": (payload: DebugContainerEventPayload) => void;

  /**
   * Emitted when appending a job
   * @param payload - Append job event payload
   */
  "append-job": (payload: AppendJobEventPayload) => void;
}

/**
 * BkPipeline Component Props
 */
export interface BkPipelineProps {
  /**
   * Whether the pipeline is editable
   * @default true
   */
  editable?: boolean;

  /**
   * Whether in preview mode
   * @default false
   */
  isPreview?: boolean;

  /**
   * Current execution count
   * @default 1
   */
  currentExecCount?: number;

  /**
   * Whether in execution detail mode
   * @default false
   */
  isExecDetail?: boolean;

  /**
   * Whether this is the latest build
   * @default false
   */
  isLatestBuild?: boolean;

  /**
   * Whether elements can be skipped
   * @default false
   */
  canSkipElement?: boolean;

  /**
   * Pipeline model data (required)
   */
  pipeline: PipelineModel;

  /**
   * User ID who cancelled the pipeline
   * @default "unknow"
   */
  cancelUserId?: string;

  /**
   * Current user name
   * @default "unknow"
   */
  userName?: string;

  isCreativeStream?: boolean;

  /**
   * Match rules array
   * @default []
   */
  matchRules?: MatchRule[];

  /**
   * Whether to expand all matrix by default
   * @default true
   */
  isExpandAllMatrix?: boolean;

  /**
   * Event handlers
   */
  onInput?: BkPipelineEvents["input"];
  onChange?: BkPipelineEvents["change"];
  onClick?: BkPipelineEvents["click"];
  onDelete?: BkPipelineEvents["delete"];
  onAtomReview?: BkPipelineEvents["atom-review"];
  onAtomContinue?: BkPipelineEvents["atom-continue"];
  onAtomExec?: BkPipelineEvents["atom-exec"];
  onAtomQualityCheck?: BkPipelineEvents["atom-quality-check"];
  onAddAtom?: BkPipelineEvents["add-atom"];
  onAddStage?: BkPipelineEvents["add-stage"];
  onStageCheck?: BkPipelineEvents["stage-check"];
  onStageRetry?: BkPipelineEvents["stage-retry"];
  onDebugContainer?: BkPipelineEvents["debug-container"];
  onAppendJob?: BkPipelineEvents["append-job"];
}

/**
 * BkPipeline Component Exposed Methods
 */
export interface BkPipelineExpose {
  /**
   * Expand post action for a specific stage/container
   * @param stageId - Stage ID
   * @param matrixId - Optional matrix ID
   * @param containerId - Optional container ID
   * @returns Promise that resolves to true if successful
   */
  expandPostAction: (
    stageId: string,
    matrixId?: string,
    containerId?: string
  ) => Promise<boolean>;

  /**
   * Expand or collapse a matrix
   * @param stageId - Stage ID
   * @param matrixId - Matrix ID
   * @param containerId - Container ID
   * @param expand - Whether to expand (default: true)
   * @returns Promise that resolves to true if successful
   */
  expandMatrix: (
    stageId: string,
    matrixId: string,
    containerId: string,
    expand?: boolean
  ) => Promise<boolean>;

  /**
   * Expand or collapse a job
   * @param stageId - Stage ID
   * @param containerId - Container ID
   * @param expand - Whether to expand (default: true)
   * @returns Promise that resolves to true if successful
   */
  expandJob: (
    stageId: string,
    containerId: string,
    expand?: boolean
  ) => Promise<boolean>;
}

/**
 * BkPipeline Component
 */
export type BkPipelineComponent = DefineComponent<
  BkPipelineProps,
  BkPipelineExpose
> & {
  install: Plugin["install"];
};

/**
 * Load i18n messages
 * @param i18n - i18n instance
 */
export function loadI18nMessages(i18n?: any): void;

/**
 * Use language hook
 * @returns Language utilities
 */
export function useLang(): any;

declare const BkPipeline: BkPipelineComponent;

export default BkPipeline;
