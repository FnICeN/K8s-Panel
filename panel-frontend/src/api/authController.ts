// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 此处后端没有提供注释 GET /auth/devicecode */
export async function getDeviceCode(options?: { [key: string]: any }) {
  return request<API.BaseResponseDeviceCodeResponse>('/auth/devicecode', {
    method: 'GET',
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 GET /auth/polltoken */
export async function pollToken(options?: { [key: string]: any }) {
  return request<API.BaseResponsePollTokenResponse>('/auth/polltoken', {
    method: 'GET',
    ...(options || {}),
  })
}
