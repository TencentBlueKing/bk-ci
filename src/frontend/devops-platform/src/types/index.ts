export enum TargetNetBehavior {
  UPLOAD = 'UPLOAD',
  DOWNLOAD = 'DOWNLOAD',
}

export interface VisitedSite {
  host: string
  port: string
  targetNetBehaviors: TargetNetBehavior[]
}

export interface providerConfig {
  api: string;
  credentialTypeList: [{
    credentialType: '',
    name: ''
  }];
  createTime: number;
  updateTime: number;
  desc: string;
  docUrl: string;
  logoUrl: string;
  merge: boolean;
  name: string;
  pac: boolean;
  providerCode: string;
  providerType: string;
  scmType: string;
  webhook: boolean;
  webhookProps: {
    eventTypeList: [];
    eventTypeActionMap: {}
  };
  webhookSecretType: string
}

export interface repoConfigFromData {
  scmCode: string;
  name: string;
  providerCode: string;
  scmType: string;
  hosts: string;
  logoUrl: string;
  credentialTypeList: [];
  oauthType: string;
  oauthScmCode: string;
  mergeEnabled: boolean;
  pacEnabled: boolean;
  webhookEnabled: boolean;
  props: {
    apiUrl: string;
    webUrl: string;
    clientId: string;
    webhookSecret: string;
    clientSecret: string;
    proxyEnabled: string;
  }
}

export interface UploadLogoResponse {
  type: string;
  file: Blob;
  filename: string;
}
