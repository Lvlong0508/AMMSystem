import { request } from './request'

const BASE = '/api/seller/knowledge'

export const uploadKnowledgeFiles = (files) => {
  const formData = new FormData()
  files.forEach(f => formData.append('files', f))
  return request.post(`${BASE}/upload`, formData)
}

export const listUploadFiles = () => request.post(`${BASE}/list/upload`)

export const listFinishFiles = () => request.post(`${BASE}/list/finish`)

export const deleteUploadFiles = (fileNames) => request.post(`${BASE}/delete/upload`, fileNames)

export const deleteFinishFiles = (fileNames) => request.post(`${BASE}/delete/finish`, fileNames)

export const ingestFiles = (fileNames) => request.post(`${BASE}/ingest`, fileNames)
