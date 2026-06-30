import { request } from './request'

const FILE_BASE = '/api/seller/knowledge/file'
const EMBEDDING_BASE = '/api/seller/knowledge/embedding'

export const uploadKnowledgeFiles = (files) => {
  const formData = new FormData()
  files.forEach(f => formData.append('files', f))
  return request.post(`${FILE_BASE}/upload`, formData)
}

export const listUploadFiles = () => request.post(`${FILE_BASE}/list/upload`)

export const listFinishFiles = () => request.post(`${FILE_BASE}/list/finish`)

export const deleteUploadFiles = (fileNames) => request.post(`${FILE_BASE}/delete/upload`, fileNames)

export const deleteFinishFiles = (fileNames) => request.post(`${FILE_BASE}/delete/finish`, fileNames)

export const ingestFiles = (fileNames) => request.post(`${FILE_BASE}/ingest`, fileNames)

export const getVectorCollections = () => request.post(`${EMBEDDING_BASE}/collections`)

export const getVectorDocuments = () => request.post(`${EMBEDDING_BASE}/documents`)

export const searchVector = (query, topK = 5) =>
  request.post(`${EMBEDDING_BASE}/search`, { query, topK })

export const deleteVectorDocuments = (fileNames) =>
  request.post(`${EMBEDDING_BASE}/delete`, fileNames)
