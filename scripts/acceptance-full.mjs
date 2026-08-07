#!/usr/bin/env node

import { spawnSync } from 'node:child_process'

const args = process.argv.slice(2)
let baseUrl = process.env.BASE_URL || 'http://127.0.0.1:8080/api'
let mysqlContainer = process.env.E2E_MYSQL_CONTAINER || ''
let mysqlPassword = process.env.E2E_MYSQL_PASSWORD || ''

for (let index = 0; index < args.length; index += 1) {
  const arg = args[index]
  if (arg === '--base-url') baseUrl = requiredArg(args, ++index, arg)
  else if (arg === '--mysql-container') mysqlContainer = requiredArg(args, ++index, arg)
  else if (arg === '--mysql-password') mysqlPassword = requiredArg(args, ++index, arg)
  else if (arg === '--help' || arg === '-h') {
    console.log(`Usage: node scripts/acceptance-full.mjs [options]

Options:
  --base-url URL            API base URL
  --mysql-container NAME    Test MySQL container used to exercise unbound-user flows
  --mysql-password VALUE    Root password for the test MySQL container

Environment equivalents: BASE_URL, E2E_MYSQL_CONTAINER, E2E_MYSQL_PASSWORD, E2E_SUFFIX`)
    process.exit(0)
  } else throw new Error(`Unknown argument: ${arg}`)
}

baseUrl = baseUrl.replace(/\/$/, '')
const appOrigin = new URL(baseUrl).origin
const suffix = process.env.E2E_SUFFIX || `${Date.now().toString(36)}${Math.random().toString(36).slice(2, 7)}`
const password = 'Test1234!'
const passed = []
const groups = []
const state = { suffix, password }

function requiredArg(values, index, name) {
  if (!values[index]) throw new Error(`${name} requires a value`)
  return values[index]
}

function assert(condition, message) {
  if (!condition) throw new Error(message)
}

function equal(actual, expected, message) {
  if (actual !== expected) throw new Error(`${message}: expected ${JSON.stringify(expected)}, got ${JSON.stringify(actual)}`)
}

function mark(name) {
  passed.push(name)
  console.log(`[PASS] ${name}`)
}

function expectCode(response, code, name) {
  equal(response.payload?.code, code, `${name} business code`)
  return response.payload?.data
}

function expectSuccess(response, name) {
  const code = response.payload?.code
  assert(code === 200 || code === 0, `${name} failed: HTTP ${response.status}, body ${response.text}`)
  return response.payload?.data
}

function containsId(list, id) {
  return Array.isArray(list) && list.some((item) => String(item?.id) === String(id))
}

function records(data) {
  if (Array.isArray(data)) return data
  return data?.records || data?.list || []
}

async function api(method, path, { token, body, form } = {}) {
  const headers = {}
  if (token) headers.Authorization = `Bearer ${token}`
  let requestBody
  if (form) requestBody = form
  else if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
    requestBody = JSON.stringify(body)
  }

  const response = await fetch(`${baseUrl}${path}`, { method, headers, body: requestBody })
  const text = await response.text()
  let payload
  try {
    payload = text ? JSON.parse(text) : null
  } catch {
    throw new Error(`${method} ${path} returned non-JSON response: HTTP ${response.status}, ${text.slice(0, 300)}`)
  }
  return { status: response.status, payload, text }
}

async function group(name, action) {
  console.log(`\n=== ${name} ===`)
  await action()
  groups.push(name)
}

function localDate(offsetDays = 0) {
  const date = new Date()
  date.setDate(date.getDate() + offsetDays)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function pngBlob() {
  const base64 = 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII='
  return new Blob([Buffer.from(base64, 'base64')], { type: 'image/png' })
}

function detachUser(username) {
  assert(mysqlContainer && mysqlPassword, 'unbound-user flows require --mysql-container and --mysql-password')
  assert(/^[a-zA-Z0-9_]+$/.test(username), 'unsafe generated username')
  const sql = [
    `SET @uid=(SELECT id FROM ql_user WHERE username='${username}' AND deleted=0 LIMIT 1)`,
    'SET @cid=(SELECT couple_id FROM ql_user WHERE id=@uid)',
    'UPDATE ql_couple SET user_id1=IF(user_id1=@uid,NULL,user_id1), user_id2=IF(user_id2=@uid,NULL,user_id2) WHERE id=@cid',
    "UPDATE ql_user SET couple_id=NULL, role='member' WHERE id=@uid",
    'DELETE FROM ql_couple WHERE id=@cid AND user_id1 IS NULL AND user_id2 IS NULL'
  ].join('; ')
  const result = spawnSync(
    'docker',
    ['exec', '-e', `MYSQL_PWD=${mysqlPassword}`, mysqlContainer, 'mysql', '-uroot', 'qinglv_note', '-e', sql],
    { encoding: 'utf8' }
  )
  if (result.status !== 0) throw new Error(`failed to detach ${username}: ${result.stderr || result.stdout}`)
}

async function register(username, nickname, extra = {}) {
  const response = await api('POST', '/auth/register', {
    body: { username, password, nickname, ...extra }
  })
  const data = expectSuccess(response, `register ${username}`)
  assert(data?.token && data?.userId, `register ${username} did not return token/userId`)
  return data
}

await group('健康、鉴权、注册登录与个人资料', async () => {
  const healthResponse = await fetch(`${appOrigin}/actuator/health`)
  const health = await healthResponse.json()
  equal(health.status, 'UP', 'actuator health')
  mark('actuator health')

  const unauthorized = await api('GET', '/auth/profile')
  equal(unauthorized.status, 401, 'protected API HTTP status')
  expectCode(unauthorized, 401, 'protected API')
  mark('JWT protects private APIs')

  state.userA = `full_a_${suffix}`
  state.userB = `full_b_${suffix}`
  const registeredA = await register(state.userA, 'Full Acceptance A', {
    togetherDate: '2024-05-20',
    role: 'boy'
  })
  state.tokenA = registeredA.token
  state.userAId = registeredA.userId
  state.coupleAId = registeredA.coupleId
  mark('register owner and create couple')

  const couple = expectSuccess(await api('GET', '/couple/info', { token: state.tokenA }), 'couple info')
  assert(couple?.inviteCode, 'couple invite code is missing')
  state.inviteA = couple.inviteCode
  mark('read couple invite code')

  const registeredB = await register(state.userB, 'Full Acceptance B', {
    inviteCode: state.inviteA,
    role: 'girl'
  })
  state.tokenB = registeredB.token
  state.userBId = registeredB.userId
  equal(String(registeredB.coupleId), String(state.coupleAId), 'invite registration couple')
  mark('register partner through invite code')

  const wrongLogin = await api('POST', '/auth/login', {
    body: { username: state.userA, password: 'wrong-password' }
  })
  expectCode(wrongLogin, 400, 'wrong password')
  mark('reject wrong password')

  const login = expectSuccess(
    await api('POST', '/auth/login', { body: { username: state.userA, password } }),
    'login'
  )
  assert(login?.token, 'login token is missing')
  state.tokenA = login.token
  mark('login with valid credentials')

  const profile = expectSuccess(await api('GET', '/auth/profile', { token: state.tokenA }), 'profile')
  equal(String(profile.id), String(state.userAId), 'profile user id')
  mark('read current profile')

  const avatar = '/uploads/e2e-avatar-placeholder.png'
  const avatarUpdate = expectSuccess(
    await api('PUT', '/auth/avatar', { token: state.tokenA, body: { avatar } }),
    'update avatar'
  )
  equal(avatarUpdate.avatar, avatar, 'updated avatar')
  mark('update avatar')

  const profileUpdate = expectSuccess(
    await api('PUT', '/auth/profile', {
      token: state.tokenA,
      body: { nickname: 'Full Acceptance A Updated', avatar }
    }),
    'update profile'
  )
  equal(profileUpdate.nickname, 'Full Acceptance A Updated', 'updated nickname')
  mark('update profile')

  const duplicate = await api('POST', '/auth/register', {
    body: { username: state.userA, password, nickname: 'Duplicate' }
  })
  expectCode(duplicate, 400, 'duplicate username')
  mark('reject duplicate username')
})

await group('情侣空间读取、更新、独立创建、绑定与容量限制', async () => {
  const updated = expectSuccess(
    await api('PUT', '/couple/update', {
      token: state.tokenA,
      body: {
        name: `全量验收小窝 ${suffix}`,
        togetherDate: '2024-05-21',
        announcement: '全量业务验收通过中',
        avatar: '/uploads/couple-cover.png'
      }
    }),
    'update couple'
  )
  equal(updated.name, `全量验收小窝 ${suffix}`, 'updated couple name')
  mark('update couple information')

  const partnerView = expectSuccess(await api('GET', '/couple/info', { token: state.tokenB }), 'partner couple info')
  equal(partnerView.announcement, '全量业务验收通过中', 'partner sees announcement')
  mark('partner reads updated couple')

  const duplicateCreate = await api('POST', '/couple/create', {
    token: state.tokenA,
    body: { name: '重复小窝' }
  })
  expectCode(duplicateCreate, 400, 'duplicate couple creation')
  mark('reject duplicate couple creation')

  state.userC = `full_c_${suffix}`
  const registeredC = await register(state.userC, 'Full Acceptance C')
  state.tokenC = registeredC.token
  state.userCId = registeredC.userId
  detachUser(state.userC)
  const createdC = expectSuccess(
    await api('POST', '/couple/create', {
      token: state.tokenC,
      body: { name: `独立建窝 ${suffix}`, startDate: '2023-01-02' }
    }),
    'create couple for unbound user'
  )
  state.coupleCId = createdC.id
  state.inviteC = createdC.inviteCode
  assert(state.inviteC, 'new couple invite code missing')
  mark('unbound user creates couple')

  state.userD = `full_d_${suffix}`
  const registeredD = await register(state.userD, 'Full Acceptance D')
  state.tokenD = registeredD.token
  detachUser(state.userD)
  const boundD = expectSuccess(
    await api('POST', '/couple/bind', { token: state.tokenD, body: { inviteCode: state.inviteC } }),
    'bind unbound partner'
  )
  equal(String(boundD.id), String(state.coupleCId), 'direct bind couple id')
  mark('unbound user binds through couple endpoint')

  const bindAgain = await api('POST', '/couple/bind', {
    token: state.tokenD,
    body: { inviteCode: state.inviteA }
  })
  expectCode(bindAgain, 400, 'already-bound user')
  mark('reject already-bound user')

  state.userE = `full_e_${suffix}`
  const registeredE = await register(state.userE, 'Full Acceptance E')
  state.tokenE = registeredE.token
  detachUser(state.userE)
  const badInvite = await api('POST', '/couple/bind', {
    token: state.tokenE,
    body: { inviteCode: 'INVALID' }
  })
  expectCode(badInvite, 400, 'invalid invite code')
  mark('reject invalid invite code')

  const fullCouple = await api('POST', '/couple/bind', {
    token: state.tokenE,
    body: { inviteCode: state.inviteC }
  })
  expectCode(fullCouple, 400, 'full couple')
  mark('reject third member in full couple')
})

await group('单文件、批量文件、静态访问与非法上传', async () => {
  const singleForm = new FormData()
  singleForm.append('file', pngBlob(), `single-${suffix}.png`)
  const singleUrl = expectSuccess(
    await api('POST', '/upload', { token: state.tokenA, form: singleForm }),
    'single upload'
  )
  assert(singleUrl?.startsWith('/uploads/'), 'single upload URL is invalid')
  state.fileUrls = [singleUrl]
  mark('single file upload')

  const staticResponse = await fetch(`${appOrigin}${singleUrl}`)
  equal(staticResponse.status, 200, 'uploaded file static response')
  assert((staticResponse.headers.get('content-type') || '').includes('image/png'), 'uploaded PNG content type is wrong')
  mark('uploaded file is served through Nginx')

  const batchForm = new FormData()
  batchForm.append('files', pngBlob(), `batch-a-${suffix}.png`)
  batchForm.append('files', pngBlob(), `batch-b-${suffix}.png`)
  const batchUrls = expectSuccess(
    await api('POST', '/upload/batch', { token: state.tokenA, form: batchForm }),
    'batch upload'
  )
  equal(batchUrls.length, 2, 'batch upload count')
  state.fileUrls.push(...batchUrls)
  mark('batch file upload')

  const badMimeForm = new FormData()
  badMimeForm.append('file', new Blob(['not an image'], { type: 'text/plain' }), `bad-${suffix}.png`)
  expectCode(await api('POST', '/upload', { token: state.tokenA, form: badMimeForm }), 400, 'bad MIME upload')
  mark('reject extension and MIME mismatch')

  const unsupportedForm = new FormData()
  unsupportedForm.append('file', new Blob(['binary'], { type: 'application/octet-stream' }), `bad-${suffix}.exe`)
  expectCode(
    await api('POST', '/upload', { token: state.tokenA, form: unsupportedForm }),
    400,
    'unsupported upload'
  )
  mark('reject unsupported file type')

  const anonymousForm = new FormData()
  anonymousForm.append('file', pngBlob(), `anonymous-${suffix}.png`)
  const anonymous = await api('POST', '/upload', { form: anonymousForm })
  equal(anonymous.status, 401, 'anonymous upload HTTP status')
  expectCode(anonymous, 401, 'anonymous upload')
  mark('reject anonymous upload')

  expectSuccess(
    await api('PUT', '/auth/avatar', { token: state.tokenA, body: { avatar: singleUrl } }),
    'update avatar to uploaded URL'
  )
  mark('uploaded asset can be used as avatar')
})

await group('笔记创建、详情、分页、筛选、搜索、更新、点赞与权限', async () => {
  const mainNote = expectSuccess(
    await api('POST', '/notes', {
      token: state.tokenA,
      body: {
        title: `Full Journey ${suffix}`,
        content: '完整笔记业务流程',
        mood: 'happy',
        weather: 'sunny',
        isTop: 0,
        noteDate: '2024-05-20',
        images: [state.fileUrls[0]]
      }
    }),
    'create note'
  )
  state.noteId = mainNote.id
  equal(mainNote.noteDate, '2024-05-20', 'created note date')
  equal(mainNote.images?.[0], state.fileUrls[0], 'created note image')
  mark('create note with selected date and image')

  const detail = expectSuccess(await api('GET', `/notes/${state.noteId}`, { token: state.tokenB }), 'note detail')
  equal(detail.content, '完整笔记业务流程', 'partner note detail')
  mark('partner reads note detail')

  const updated = expectSuccess(
    await api('PUT', `/notes/${state.noteId}`, {
      token: state.tokenA,
      body: {
        title: `Updated Journey ${suffix}`,
        content: '更新后的完整笔记流程',
        mood: 'love',
        weather: 'rainy',
        isTop: 0,
        noteDate: '2024-05-21',
        images: [state.fileUrls[1], state.fileUrls[2]]
      }
    }),
    'update note'
  )
  equal(updated.noteDate, '2024-05-21', 'updated note date')
  equal(updated.images?.length, 2, 'updated note image count')
  mark('author updates note')

  expectCode(
    await api('PUT', `/notes/${state.noteId}`, {
      token: state.tokenB,
      body: { title: '越权编辑', content: '不应成功' }
    }),
    403,
    'partner note update'
  )
  mark('reject non-author note update')

  const filtered = expectSuccess(
    await api('GET', '/notes?page=1&size=10&mood=love', { token: state.tokenB }),
    'filtered timeline'
  )
  assert(containsId(records(filtered), state.noteId), 'mood-filtered timeline misses updated note')
  mark('timeline pagination and mood filter')

  const searched = expectSuccess(
    await api('GET', `/notes/search?keyword=${encodeURIComponent(`Updated Journey ${suffix}`)}&page=1&size=10`, {
      token: state.tokenB
    }),
    'note search'
  )
  assert(containsId(records(searched), state.noteId), 'search misses updated note')
  mark('search notes by keyword')

  const likeOn = expectSuccess(
    await api('POST', `/notes/${state.noteId}/like`, { token: state.tokenB, body: {} }),
    'like note'
  )
  equal(likeOn.liked, true, 'like toggle on')
  mark('like note')

  const likeOff = expectSuccess(
    await api('POST', `/notes/${state.noteId}/like`, { token: state.tokenB, body: {} }),
    'unlike note'
  )
  equal(likeOff.liked, false, 'like toggle off')
  mark('unlike note')

  const likeForStats = expectSuccess(
    await api('POST', `/notes/${state.noteId}/like`, { token: state.tokenB, body: {} }),
    'like note for stats'
  )
  equal(likeForStats.liked, true, 'final like state')

  expectCode(await api('GET', `/notes/${state.noteId}`, { token: state.tokenC }), 403, 'cross-couple note detail')
  mark('reject cross-couple note read')

  expectCode(await api('DELETE', `/notes/${state.noteId}`, { token: state.tokenB }), 403, 'partner note delete')
  mark('reject non-author note delete')

  const uiNote = expectSuccess(
    await api('POST', '/notes', {
      token: state.tokenA,
      body: {
        title: `UI Timeline ${suffix}`,
        content: '用于前端页面验收',
        mood: 'calm',
        weather: 'cloudy',
        noteDate: '2025-06-01'
      }
    }),
    'create UI note'
  )
  state.uiNoteId = uiNote.id
  const allTimeline = expectSuccess(await api('GET', '/notes?page=1&size=10', { token: state.tokenA }), 'timeline')
  equal(String(records(allTimeline)[0]?.id), String(state.uiNoteId), 'timeline note-date ordering')
  mark('timeline orders notes by selected date')

  const disposable = expectSuccess(
    await api('POST', '/notes', {
      token: state.tokenA,
      body: { title: `Disposable Note ${suffix}`, content: '待删除', noteDate: '2023-01-01' }
    }),
    'create disposable note'
  )
  expectSuccess(await api('DELETE', `/notes/${disposable.id}`, { token: state.tokenA }), 'delete note')
  expectCode(await api('GET', `/notes/${disposable.id}`, { token: state.tokenA }), 400, 'deleted note detail')
  mark('author deletes note')
})

await group('相册、URL 照片、Multipart 照片、封面刷新与权限', async () => {
  const album = expectSuccess(
    await api('POST', '/albums', {
      token: state.tokenA,
      body: { name: `Full Album ${suffix}`, description: '完整相册流程', coverImage: state.fileUrls[1] }
    }),
    'create album'
  )
  state.albumId = album.id
  mark('create album')

  const albumList = expectSuccess(await api('GET', '/albums', { token: state.tokenB }), 'album list')
  assert(containsId(albumList, state.albumId), 'partner album list misses created album')
  mark('partner lists albums')

  const updatedAlbum = expectSuccess(
    await api('PUT', `/albums/${state.albumId}`, {
      token: state.tokenB,
      body: { name: `Updated Album ${suffix}`, description: '伴侣更新相册', coverImage: state.fileUrls[1] }
    }),
    'update album'
  )
  equal(updatedAlbum.name, `Updated Album ${suffix}`, 'updated album name')
  mark('couple member updates album')

  const urlPhoto = expectSuccess(
    await api('POST', `/albums/${state.albumId}/photos`, {
      token: state.tokenB,
      body: { url: state.fileUrls[1], caption: 'URL 照片' }
    }),
    'add URL photo'
  )
  mark('add photo by uploaded URL')

  const multipart = new FormData()
  multipart.append('file', pngBlob(), `album-${suffix}.png`)
  multipart.append('albumId', String(state.albumId))
  multipart.append('description', 'Multipart 照片')
  const multipartPhoto = expectSuccess(
    await api('POST', '/photos/upload', { token: state.tokenA, form: multipart }),
    'upload album photo'
  )
  mark('upload multipart photo into album')

  const thirdPhoto = expectSuccess(
    await api('POST', `/albums/${state.albumId}/photos`, {
      token: state.tokenA,
      body: { photoUrl: state.fileUrls[2], description: '第三张照片' }
    }),
    'add third photo'
  )

  const photos = expectSuccess(
    await api('GET', `/albums/${state.albumId}/photos`, { token: state.tokenB }),
    'album photos'
  )
  equal(photos.length, 3, 'album photo list count')
  mark('list album photos')

  const detail = expectSuccess(
    await api('GET', `/albums/${state.albumId}`, { token: state.tokenB }),
    'album detail'
  )
  equal(detail.photoCount, 3, 'album detail photo count')
  mark('read album detail with photos')

  expectCode(await api('GET', `/albums/${state.albumId}`, { token: state.tokenC }), 403, 'cross-couple album')
  mark('reject cross-couple album read')

  expectSuccess(
    await api('DELETE', `/albums/${state.albumId}/photos/${urlPhoto.id}`, { token: state.tokenB }),
    'delete nested photo'
  )
  const afterCoverDelete = expectSuccess(
    await api('GET', `/albums/${state.albumId}`, { token: state.tokenA }),
    'album after cover deletion'
  )
  equal(afterCoverDelete.photoCount, 2, 'photo count after cover delete')
  assert(afterCoverDelete.coverImage !== state.fileUrls[1], 'deleted cover URL was not replaced')
  mark('delete cover photo and refresh album cover')

  expectSuccess(await api('DELETE', `/photos/${multipartPhoto.id}`, { token: state.tokenA }), 'delete photo alias')
  const remaining = expectSuccess(
    await api('GET', `/albums/${state.albumId}/photos`, { token: state.tokenA }),
    'remaining photos'
  )
  equal(remaining.length, 1, 'remaining album photo count')
  equal(String(remaining[0].id), String(thirdPhoto.id), 'remaining photo id')
  mark('delete photo through compatibility route')

  expectCode(await api('DELETE', `/albums/${state.albumId}`, { token: state.tokenC }), 403, 'cross-couple album delete')
  mark('reject cross-couple album deletion')

  const disposable = expectSuccess(
    await api('POST', '/albums', { token: state.tokenA, body: { name: `Disposable Album ${suffix}` } }),
    'create disposable album'
  )
  expectSuccess(await api('DELETE', `/albums/${disposable.id}`, { token: state.tokenB }), 'delete album')
  expectCode(await api('GET', `/albums/${disposable.id}`, { token: state.tokenA }), 400, 'deleted album detail')
  mark('delete album and child records')
})

await group('纪念日创建、倒计时、列表、更新、删除与格式校验', async () => {
  const invalidDate = await api('POST', '/anniversaries', {
    token: state.tokenA,
    body: { title: '非法日期', date: '2025-02-30', repeatYearly: 1 }
  })
  equal(invalidDate.status, 400, 'invalid anniversary date HTTP status')
  expectCode(invalidDate, 400, 'invalid anniversary date')
  mark('reject invalid anniversary date')

  const anniversary = expectSuccess(
    await api('POST', '/anniversaries', {
      token: state.tokenA,
      body: {
        title: `Leap Anniversary ${suffix}`,
        date: '2024-02-29',
        type: 'custom',
        repeatYearly: 1,
        icon: '💕',
        remark: '闰日纪念'
      }
    }),
    'create anniversary'
  )
  state.anniversaryId = anniversary.id
  assert(Number.isFinite(Number(anniversary.daysUntilNext)), 'anniversary countdown is missing')
  mark('create recurring leap-day anniversary')

  const list = expectSuccess(await api('GET', '/anniversaries', { token: state.tokenB }), 'anniversary list')
  assert(containsId(list, state.anniversaryId), 'partner anniversary list misses item')
  mark('partner lists anniversaries')

  const updated = expectSuccess(
    await api('PUT', `/anniversaries/${state.anniversaryId}`, {
      token: state.tokenB,
      body: {
        name: `Updated Anniversary ${suffix}`,
        date: '2025-05-20',
        type: 'custom',
        repeatYearly: 0,
        icon: '📷',
        remark: '伴侣更新'
      }
    }),
    'update anniversary'
  )
  equal(updated.name, `Updated Anniversary ${suffix}`, 'updated anniversary name')
  mark('couple member updates anniversary')

  expectCode(
    await api('PUT', `/anniversaries/${state.anniversaryId}`, {
      token: state.tokenC,
      body: { name: '跨空间', date: '2025-05-20' }
    }),
    403,
    'cross-couple anniversary update'
  )
  mark('reject cross-couple anniversary update')

  expectCode(
    await api('DELETE', `/anniversaries/${state.anniversaryId}`, { token: state.tokenC }),
    403,
    'cross-couple anniversary delete'
  )
  mark('reject cross-couple anniversary delete')

  const disposable = expectSuccess(
    await api('POST', '/anniversaries', {
      token: state.tokenA,
      body: { title: `Disposable Anniversary ${suffix}`, date: '2026-01-01', repeatYearly: 0 }
    }),
    'create disposable anniversary'
  )
  expectSuccess(
    await api('DELETE', `/anniversaries/${disposable.id}`, { token: state.tokenB }),
    'delete anniversary'
  )
  mark('delete anniversary')
})

await group('惊喜 click/password/date 解锁、内容隐藏、删除与权限', async () => {
  state.uiSurpriseTitle = `UI 安全惊喜 ${suffix}`
  state.uiSurpriseContent = '<img src=x onerror=alert(1)>\n这段内容必须按纯文本展示'
  const clickSurprise = expectSuccess(
    await api('POST', '/surprises', {
      token: state.tokenA,
      body: {
        title: state.uiSurpriseTitle,
        content: state.uiSurpriseContent,
        hint: '点击拆开',
        unlockCondition: 'click'
      }
    }),
    'create click surprise'
  )
  state.uiSurpriseId = clickSurprise.id
  assert(clickSurprise.content == null, 'new locked surprise leaked content')
  mark('create click surprise without content leak')

  const surpriseList = expectSuccess(await api('GET', '/surprises', { token: state.tokenB }), 'surprise list')
  const listedClick = surpriseList.find((item) => String(item.id) === String(state.uiSurpriseId))
  assert(listedClick && listedClick.content == null, 'surprise list leaked locked content')
  mark('list surprises without locked content')

  const locked = expectSuccess(
    await api('GET', `/surprises/${state.uiSurpriseId}`, { token: state.tokenB }),
    'locked surprise detail'
  )
  assert(locked.content == null, 'locked detail leaked content')
  mark('locked surprise detail hides content')

  expectCode(
    await api('GET', `/surprises/${state.uiSurpriseId}`, { token: state.tokenC }),
    403,
    'cross-couple surprise detail'
  )
  mark('reject cross-couple surprise read')

  const clickUnlocked = expectSuccess(
    await api('POST', `/surprises/${state.uiSurpriseId}/unlock`, { token: state.tokenB, body: {} }),
    'unlock click surprise'
  )
  equal(clickUnlocked.content, state.uiSurpriseContent, 'click surprise content')
  mark('unlock click surprise')

  const passwordSurprise = expectSuccess(
    await api('POST', '/surprises', {
      token: state.tokenA,
      body: {
        title: `Password Surprise ${suffix}`,
        content: '密码惊喜内容',
        hint: '暗号',
        unlockCondition: 'password',
        unlockPassword: 'love520'
      }
    }),
    'create password surprise'
  )
  expectCode(
    await api('POST', `/surprises/${passwordSurprise.id}/unlock`, {
      token: state.tokenB,
      body: { password: 'wrong' }
    }),
    400,
    'wrong surprise password'
  )
  mark('reject wrong surprise password')

  const passwordUnlocked = expectSuccess(
    await api('POST', `/surprises/${passwordSurprise.id}/unlock`, {
      token: state.tokenB,
      body: { password: 'love520' }
    }),
    'unlock password surprise'
  )
  equal(passwordUnlocked.content, '密码惊喜内容', 'password surprise content')
  mark('unlock password surprise')

  const futureSurprise = expectSuccess(
    await api('POST', '/surprises', {
      token: state.tokenA,
      body: {
        title: `Future Surprise ${suffix}`,
        content: '未来内容',
        unlockCondition: 'date',
        unlockDate: localDate(1)
      }
    }),
    'create future date surprise'
  )
  expectCode(
    await api('POST', `/surprises/${futureSurprise.id}/unlock`, { token: state.tokenB, body: {} }),
    400,
    'early date unlock'
  )
  mark('reject early date surprise unlock')

  const dueSurprise = expectSuccess(
    await api('POST', '/surprises', {
      token: state.tokenA,
      body: {
        title: `Due Surprise ${suffix}`,
        content: '到期自动内容',
        unlockCondition: 'date',
        unlockDate: localDate(0)
      }
    }),
    'create due date surprise'
  )
  const autoUnlocked = expectSuccess(
    await api('GET', `/surprises/${dueSurprise.id}`, { token: state.tokenB }),
    'auto-unlocked surprise detail'
  )
  equal(autoUnlocked.content, '到期自动内容', 'auto-unlocked surprise content')
  equal(autoUnlocked.unlockedBy ?? null, null, 'auto-unlock user')
  mark('date surprise auto-unlocks on due date')

  const disposable = expectSuccess(
    await api('POST', '/surprises', {
      token: state.tokenA,
      body: { title: `Disposable Surprise ${suffix}`, content: '待撤回', unlockCondition: 'click' }
    }),
    'create disposable surprise'
  )
  expectCode(
    await api('DELETE', `/surprises/${disposable.id}`, { token: state.tokenB }),
    403,
    'non-creator surprise deletion'
  )
  mark('reject non-creator surprise deletion')

  expectSuccess(await api('DELETE', `/surprises/${disposable.id}`, { token: state.tokenA }), 'delete surprise')
  mark('creator deletes surprise')
})

await group('心愿创建、列表、筛选、更新、完成、删除与权限', async () => {
  const wish = expectSuccess(
    await api('POST', '/wishes', {
      token: state.tokenA,
      body: {
        title: `Full Wish ${suffix}`,
        description: '完整心愿流程',
        icon: '✈️',
        category: 'travel',
        priority: 2
      }
    }),
    'create wish'
  )
  state.wishId = wish.id
  mark('create wish')

  const wishes = expectSuccess(await api('GET', '/wishes', { token: state.tokenB }), 'wish list')
  assert(containsId(wishes, state.wishId), 'partner wish list misses item')
  mark('partner lists wishes')

  const pending = expectSuccess(await api('GET', '/wishes?status=0', { token: state.tokenB }), 'pending wishes')
  assert(containsId(pending, state.wishId), 'pending filter misses wish')
  mark('filter pending wishes')

  expectCode(
    await api('PUT', `/wishes/${state.wishId}`, {
      token: state.tokenB,
      body: { title: '越权修改', category: 'other', priority: 0 }
    }),
    403,
    'non-creator wish update'
  )
  mark('reject non-creator wish update')

  const updated = expectSuccess(
    await api('PUT', `/wishes/${state.wishId}`, {
      token: state.tokenA,
      body: {
        title: `Updated Wish ${suffix}`,
        description: '更新后的心愿',
        icon: '🎁',
        category: 'gift',
        priority: 1
      }
    }),
    'update wish'
  )
  equal(updated.category, 'gift', 'updated wish category')
  mark('creator updates wish')

  expectCode(
    await api('POST', `/wishes/${state.wishId}/complete`, { token: state.tokenC, body: {} }),
    403,
    'cross-couple wish completion'
  )
  mark('reject cross-couple wish completion')

  const completed = expectSuccess(
    await api('POST', `/wishes/${state.wishId}/complete`, { token: state.tokenB, body: {} }),
    'complete wish'
  )
  equal(completed.status, 1, 'completed wish status')
  equal(String(completed.completedBy), String(state.userBId), 'wish completer')
  mark('partner completes wish')

  const done = expectSuccess(await api('GET', '/wishes?status=1', { token: state.tokenA }), 'completed wishes')
  assert(containsId(done, state.wishId), 'completed filter misses wish')
  mark('filter completed wishes')

  const disposable = expectSuccess(
    await api('POST', '/wishes', {
      token: state.tokenA,
      body: { title: `Disposable Wish ${suffix}`, category: 'other', priority: 0 }
    }),
    'create disposable wish'
  )
  expectCode(
    await api('DELETE', `/wishes/${disposable.id}`, { token: state.tokenB }),
    403,
    'non-creator wish deletion'
  )
  mark('reject non-creator wish deletion')

  expectSuccess(await api('DELETE', `/wishes/${disposable.id}`, { token: state.tokenA }), 'delete wish')
  mark('creator deletes wish')
})

await group('恋爱统计聚合', async () => {
  const stats = expectSuccess(await api('GET', '/stats', { token: state.tokenA }), 'stats')
  assert(Number(stats.noteCount) >= 2, 'stats noteCount is below expected')
  assert(Number(stats.photoCount) >= 1, 'stats photoCount is below expected')
  assert(Number(stats.albumCount) >= 1, 'stats albumCount is below expected')
  assert(Number(stats.anniversaryCount) >= 1, 'stats anniversaryCount is below expected')
  assert(Number(stats.surpriseCount) >= 4, 'stats surpriseCount is below expected')
  assert(Number(stats.unlockedSurpriseCount) >= 3, 'stats unlockedSurpriseCount is below expected')
  assert(Number(stats.totalLikes) >= 1, 'stats totalLikes is below expected')
  assert(Number(stats.activeDays30) >= 1, 'stats activeDays30 is below expected')
  assert(Number(stats.longestStreak) >= 1, 'stats longestStreak is below expected')
  assert(Number(stats.moodDistribution?.love) >= 1, 'stats mood distribution misses love')
  mark('stats aggregate all core resources')
})

console.log(`\nFull acceptance complete: ${passed.length} checks passed across ${groups.length} feature groups.`)
console.log(`Browser fixture: username=${state.userA} password=${password} suffix=${suffix}`)
