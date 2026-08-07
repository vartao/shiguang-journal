import { describe, expect, it } from 'vitest'
import { getBusinessError, getTransportErrorMessage } from './requestError'

describe('request error normalization', () => {
  it('returns backend business messages and detects unauthorized responses', () => {
    expect(getBusinessError({ code: 400, message: '参数错误' })).toEqual({ message: '参数错误', unauthorized: false })
    expect(getBusinessError({ code: 401 })).toEqual({ message: '请求失败', unauthorized: true })
    expect(getBusinessError({ code: 200, message: 'ok' })).toBeNull()
  })

  it('normalizes http, timeout and offline failures', () => {
    expect(getTransportErrorMessage({ response: { status: 403 } })).toBe('没有权限执行此操作')
    expect(getTransportErrorMessage({ response: { status: 500 } })).toBe('服务器开小差了，请稍后再试')
    expect(getTransportErrorMessage({ code: 'ECONNABORTED' })).toBe('请求超时，请检查网络')
    expect(getTransportErrorMessage({})).toBe('网络连接失败，请检查网络')
  })
})
