// DO NOT EDIT: generated file by scala-tsi

export interface IAuthor {
  type: string
  name: string
}

export interface IConfigMeta {
  key: string
  value: string
  updatedAt: string
  updatedBy: string
}

export interface ICopyright {
  license: ILicense
  contributors: IAuthor[]
}

export interface ICoverPhoto {
  url: string
  metaUrl: string
}

export interface IDescription {
  description: string
  language: string
}

export interface IEmbedUrlV2 {
  url: string
  embedType: string
}

export interface IError {
  code: string
  description: string
  occuredAt: string
}

export interface IIntroduction {
  introduction: string
  language: string
}

export interface ILearningPathStatus {
  status: string
}

export interface ILearningPathSummaryV2 {
  id: number
  revision?: number
  title: ITitle
  description: IDescription
  introduction: IIntroduction
  metaUrl: string
  coverPhotoUrl?: string
  duration?: number
  status: string
  lastUpdated: string
  tags: ILearningPathTags
  copyright: ICopyright
  supportedLanguages: string[]
  isBasedOn?: number
  message?: string
}

export interface ILearningPathTags {
  tags: string[]
  language: string
}

export interface ILearningPathTagsSummary {
  language: string
  supportedLanguages: string[]
  tags: string[]
}

export interface ILearningPathV2 {
  id: number
  revision: number
  isBasedOn?: number
  title: ITitle
  description: IDescription
  metaUrl: string
  learningsteps: ILearningStepV2[]
  learningstepUrl: string
  coverPhoto?: ICoverPhoto
  duration?: number
  status: string
  verificationStatus: string
  lastUpdated: string
  tags: ILearningPathTags
  copyright: ICopyright
  canEdit: boolean
  supportedLanguages: string[]
  ownerId?: string
  message?: IMessage
}

export interface ILearningStepContainerSummary {
  language: string
  learningsteps: ILearningStepSummaryV2[]
  supportedLanguages: string[]
}

export interface ILearningStepSeqNo {
  seqNo: number
}

export interface ILearningStepStatus {
  status: string
}

export interface ILearningStepSummaryV2 {
  id: number
  seqNo: number
  title: ITitle
  type: string
  metaUrl: string
}

export interface ILearningStepV2 {
  id: number
  revision: number
  seqNo: number
  title: ITitle
  description?: IDescription
  embedUrl?: IEmbedUrlV2
  showTitle: boolean
  type: string
  license?: ILicense
  metaUrl: string
  canEdit: boolean
  status: string
  supportedLanguages: string[]
}

export interface ILicense {
  license: string
  description?: string
  url?: string
}

export interface IMessage {
  message: string
  date: string
}

export interface ISearchResultV2 {
  totalCount: number
  page?: number
  pageSize: number
  language: string
  results: ILearningPathSummaryV2[]
}

export interface ITitle {
  title: string
  language: string
}
