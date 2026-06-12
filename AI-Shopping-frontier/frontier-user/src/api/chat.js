import { request } from './request'

export const sendMessage = (message, sessionId) =>
  request.post('/api/user/chat/chat', { message, sessionId })

export const createSession = () =>
  request.post('/api/user/chat/session')

export const getSessions = () =>
  request.get('/api/user/chat/sessions')

export const getSessionMessages = (sessionId) =>
  request.get('/api/user/chat/session/' + sessionId + '/messages')

export const deleteSession = (sessionId) =>
  request.delete('/api/user/chat/session/' + sessionId)
