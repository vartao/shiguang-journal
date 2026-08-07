import axios from 'axios'
import { useUserStore } from '@/stores/user'
import { getBusinessError, getTransportErrorMessage } from './requestError'
import { showToast } from '@/utils/toast'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器：附加 token
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('ql_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器：统一错误处理
request.interceptors.response.use(
  (response) => {
    const res = response.data
    const businessError = getBusinessError(res)
    if (businessError) {
      showToast(businessError.message)
      if (businessError.unauthorized) handleUnauthorized()
      return Promise.reject(new Error(businessError.message))
    }
    return res
  },
  (error) => {
    showToast(getTransportErrorMessage(error))
    if (Number(error.response?.status) === 401) handleUnauthorized()
    return Promise.reject(error)
  }
)

function handleUnauthorized() {
  const userStore = useUserStore()
  userStore.logout()
}

export default request
