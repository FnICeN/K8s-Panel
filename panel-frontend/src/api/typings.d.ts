declare namespace API {
  type BaseResponseBoolean = {
    code?: number
    data?: boolean
    msg?: string
  }

  type BaseResponseDeviceCodeResponse = {
    code?: number
    data?: DeviceCodeResponse
    msg?: string
  }

  type BaseResponsePollTokenResponse = {
    code?: number
    data?: PollTokenResponse
    msg?: string
  }

  type BaseResponseString = {
    code?: number
    data?: string
    msg?: string
  }

  type DeviceCodeResponse = {
    interval?: number
    device_code?: string
    user_code?: string
    verification_uri?: string
    verification_uri_complete?: string
    expires_in?: number
  }

  type Metadata = {
    name?: string
  }

  type Node = {
    metadata?: Metadata
  }

  type NodeListResponse = {
    kind?: string
    apiVersion?: string
    items?: Node[]
  }

  type PollTokenResponse = {
    scope?: string
    access_token?: string
    expires_in?: number
    refresh_expires_in?: number
    refresh_token?: string
    token_type?: string
    id_token?: string
    'not-before-policy'?: number
    session_state?: string
  }
}
