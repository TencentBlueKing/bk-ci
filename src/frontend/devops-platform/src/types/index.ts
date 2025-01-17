export enum TargetNetBehavior {
  UPLOAD = 'UPLOAD',
  DOWNLOAD = 'DOWNLOAD',
}

export interface VisitedSite {
  host: string
  port: string
  targetNetBehaviors: TargetNetBehavior[]
}