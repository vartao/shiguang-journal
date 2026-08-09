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

function execSql(sql) {
  assert(mysqlContainer && mysqlPassword, 'direct SQL flows require --mysql-container and --mysql-password')
  const result = spawnSync(
    'docker',
    ['exec', '-e', `MYSQL_PWD=${mysqlPassword}`, mysqlContainer, 'mysql', '-uroot', 'qinglv_note', '-e', sql],
    { encoding: 'utf8' }
  )
  if (result.status !== 0) throw new Error(`failed to run SQL: ${result.stderr || result.stdout}`)
}

function detachUser(username) {
  assert(/^[a-zA-Z0-9_]+$/.test(username), 'unsafe generated username')
  execSql(
    [
      `SET @uid=(SELECT id FROM ql_user WHERE username='${username}' AND deleted=0 LIMIT 1)`,
      'SET @cid=(SELECT couple_id FROM ql_user WHERE id=@uid)',
      'UPDATE ql_couple SET user_id1=IF(user_id1=@uid,NULL,user_id1), user_id2=IF(user_id2=@uid,NULL,user_id2) WHERE id=@cid',
      "UPDATE ql_user SET couple_id=NULL, role='member' WHERE id=@uid",
      'DELETE FROM ql_couple WHERE id=@cid AND user_id1 IS NULL AND user_id2 IS NULL'
    ].join('; ')
  )
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
      body: { name: `Full Album ${suffix}`, description: '完整相册流程' }
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

  const covered = expectSuccess(
    await api('POST', '/albums', {
      token: state.tokenA,
      body: { name: `Covered Album ${suffix}`, coverImage: state.fileUrls[0] }
    }),
    'create album with cover'
  )
  equal(covered.photoCount, 1, 'cover album initial photo count')
  const coveredPhotos = expectSuccess(
    await api('GET', `/albums/${covered.id}/photos`, { token: state.tokenB }),
    'cover album photos'
  )
  equal(coveredPhotos.length, 1, 'cover album photo list count')
  equal(coveredPhotos[0].photoUrl, state.fileUrls[0], 'cover synced as first photo')
  expectSuccess(await api('DELETE', `/albums/${covered.id}`, { token: state.tokenA }), 'delete cover album')
  mark('create album with cover syncs first photo')
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

await group('旅行足迹创建、封面同步、筛选、地图聚合、照片与权限', async () => {
  const visited = expectSuccess(
    await api('POST', '/travels', {
      token: state.tokenA,
      body: {
        title: `成都之行 ${suffix}`,
        province: '四川',
        city: '成都',
        travelDate: localDate(-30),
        season: '夏',
        cover: state.fileUrls[0],
        status: 1
      }
    }),
    'create visited travel'
  )
  state.travelId = visited.id
  mark('create visited travel')

  const withCover = expectSuccess(
    await api('GET', `/travels/${state.travelId}`, { token: state.tokenB }),
    'travel detail with cover photo'
  )
  equal(withCover.photoCount, 1, 'cover synced travel photo count')
  equal(withCover.photos?.[0]?.photoUrl, state.fileUrls[0], 'cover synced as first travel photo')
  mark('create travel with cover syncs first photo')

  const planned = expectSuccess(
    await api('POST', '/travels', {
      token: state.tokenB,
      body: { title: `武汉计划 ${suffix}`, province: '湖北', city: '武汉', status: 0 }
    }),
    'create planned travel'
  )
  mark('partner creates planned travel')

  const all = expectSuccess(await api('GET', '/travels', { token: state.tokenB }), 'travel list')
  assert(containsId(all, state.travelId) && containsId(all, planned.id), 'travel list misses items')
  mark('partner lists travels')

  const visitedOnly = expectSuccess(await api('GET', '/travels?status=1', { token: state.tokenA }), 'visited travels')
  assert(containsId(visitedOnly, state.travelId) && !containsId(visitedOnly, planned.id), 'status filter wrong')
  mark('filter travels by status')

  const map = expectSuccess(await api('GET', '/travels/map', { token: state.tokenA }), 'travel map summary')
  const sichuan = map.find((item) => item.province === '四川')
  const hubei = map.find((item) => item.province === '湖北')
  assert(Number(sichuan?.visited) >= 1, 'map summary misses visited province')
  assert(Number(hubei?.planned) >= 1, 'map summary misses planned province')
  mark('travel map summary by province')

  const photo = expectSuccess(
    await api('POST', `/travels/${state.travelId}/photos`, {
      token: state.tokenA,
      body: { photoUrl: state.fileUrls[1] }
    }),
    'add travel photo'
  )
  const withPhoto = expectSuccess(
    await api('GET', `/travels/${state.travelId}`, { token: state.tokenB }),
    'travel detail after photo add'
  )
  equal(withPhoto.photoCount, 2, 'travel photo count after add')
  expectSuccess(
    await api('DELETE', `/travels/${state.travelId}/photos/${photo.id}`, { token: state.tokenB }),
    'delete travel photo'
  )
  const afterPhotoDelete = expectSuccess(
    await api('GET', `/travels/${state.travelId}`, { token: state.tokenA }),
    'travel detail after photo delete'
  )
  equal(afterPhotoDelete.photoCount, 1, 'travel photo count after delete')
  mark('add and delete travel photo')

  const updated = expectSuccess(
    await api('PUT', `/travels/${state.travelId}`, {
      token: state.tokenB,
      body: { title: `成都之行·改 ${suffix}`, province: '四川', city: '成都', status: 1 }
    }),
    'update travel'
  )
  equal(updated.title, `成都之行·改 ${suffix}`, 'updated travel title')
  mark('couple member updates travel')

  expectCode(await api('GET', `/travels/${state.travelId}`, { token: state.tokenC }), 403, 'cross-couple travel')
  mark('reject cross-couple travel read')

  expectSuccess(await api('DELETE', `/travels/${planned.id}`, { token: state.tokenA }), 'delete travel')
  expectCode(await api('GET', `/travels/${planned.id}`, { token: state.tokenB }), 400, 'deleted travel detail')
  mark('delete travel and detail is gone')
})

await group('行程计划创建、每日安排校验、更新、删除与权限', async () => {
  expectCode(
    await api('POST', '/plans', { token: state.tokenA, body: { title: `非法行程 ${suffix}`, days: 'not-json' } }),
    400,
    'invalid plan days'
  )
  mark('reject invalid plan days JSON')

  const plan = expectSuccess(
    await api('POST', '/plans', {
      token: state.tokenA,
      body: {
        title: `川西自驾 ${suffix}`,
        destination: '川西',
        startDate: localDate(10),
        endDate: localDate(15),
        budget: 5000,
        days: JSON.stringify([{ day: 1, items: ['出发'] }]),
        status: 0
      }
    }),
    'create plan'
  )
  state.planId = plan.id
  mark('create plan')

  const plans = expectSuccess(await api('GET', '/plans', { token: state.tokenB }), 'plan list')
  assert(containsId(plans, state.planId), 'partner plan list misses item')
  mark('partner lists plans')

  const updated = expectSuccess(
    await api('PUT', `/plans/${state.planId}`, {
      token: state.tokenB,
      body: { title: `川西自驾·改 ${suffix}`, status: 1 }
    }),
    'update plan'
  )
  equal(updated.status, 1, 'updated plan status')
  mark('couple member updates plan')

  expectCode(await api('GET', `/plans/${state.planId}`, { token: state.tokenC }), 403, 'cross-couple plan')
  mark('reject cross-couple plan read')

  expectSuccess(await api('DELETE', `/plans/${state.planId}`, { token: state.tokenB }), 'delete plan')
  expectCode(await api('GET', `/plans/${state.planId}`, { token: state.tokenA }), 400, 'deleted plan detail')
  mark('delete plan and detail is gone')
})

await group('菜品池、分类筛选、随机选菜与今日菜单', async () => {
  expectCode(await api('GET', '/dishes/random', { token: state.tokenA }), 400, 'random dish from empty pool')
  mark('reject random dish from empty pool')

  const dish = expectSuccess(
    await api('POST', '/dishes', { token: state.tokenA, body: { name: `红烧肉 ${suffix}`, category: 'dinner' } }),
    'create dish'
  )
  state.dishId = dish.id
  expectSuccess(
    await api('POST', '/dishes', { token: state.tokenB, body: { name: `蔬菜沙拉 ${suffix}`, category: 'snack' } }),
    'partner creates dish'
  )
  mark('couple members create dishes')

  const fallback = expectSuccess(
    await api('POST', '/dishes', { token: state.tokenA, body: { name: `未知分类菜 ${suffix}`, category: '荤菜' } }),
    'create dish with unknown category'
  )
  equal(fallback.category, 'lunch', 'unknown dish category fallback')
  mark('unknown dish category falls back to lunch')

  const dishes = expectSuccess(await api('GET', '/dishes', { token: state.tokenB }), 'dish list')
  assert(dishes.length >= 3, 'dish list misses items')
  const meat = expectSuccess(
    await api('GET', '/dishes?category=dinner', { token: state.tokenA }),
    'dish category filter'
  )
  equal(meat.length, 1, 'category filter count')
  equal(meat[0].name, `红烧肉 ${suffix}`, 'category filter result')
  mark('list and filter dishes by category')

  const random = expectSuccess(await api('GET', '/dishes/random', { token: state.tokenA }), 'random dish')
  assert(random?.name, 'random dish has no name')
  mark('random dish from pool')

  expectCode(
    await api('POST', '/menu', {
      token: state.tokenA,
      body: { mealDate: localDate(0), mealType: 'brunch', dishName: 'x' }
    }),
    400,
    'invalid meal type'
  )
  mark('reject invalid meal type')

  const item = expectSuccess(
    await api('POST', '/menu', {
      token: state.tokenA,
      body: { mealDate: localDate(0), mealType: 'lunch', dishName: `红烧肉 ${suffix}` }
    }),
    'add menu item'
  )
  const menu = expectSuccess(await api('GET', `/menu?date=${localDate(0)}`, { token: state.tokenB }), 'today menu')
  assert(containsId(menu, item.id), 'menu misses added item')
  mark('add menu item and partner reads menu')

  expectCode(await api('DELETE', `/menu/${item.id}`, { token: state.tokenC }), 403, 'cross-couple menu delete')
  mark('reject cross-couple menu item deletion')

  expectSuccess(await api('DELETE', `/menu/${item.id}`, { token: state.tokenB }), 'delete menu item')
  expectSuccess(await api('DELETE', `/dishes/${state.dishId}`, { token: state.tokenB }), 'delete dish')
  expectSuccess(await api('DELETE', `/dishes/${fallback.id}`, { token: state.tokenA }), 'delete fallback dish')
  mark('couple member deletes menu item and dish')
})

await group('时间胶囊封存、内容防泄露、到期开启与权限', async () => {
  expectCode(
    await api('POST', '/capsules', {
      token: state.tokenA,
      body: { title: `今天胶囊 ${suffix}`, message: 'x', openDate: localDate(0) }
    }),
    400,
    'capsule open date must be future'
  )
  mark('reject capsule with non-future open date')

  const capsule = expectSuccess(
    await api('POST', '/capsules', {
      token: state.tokenA,
      body: {
        title: `未来胶囊 ${suffix}`,
        message: '给未来的我们',
        openDate: localDate(30),
        photoUrls: [state.fileUrls[0]]
      }
    }),
    'create capsule'
  )
  state.capsuleId = capsule.id
  mark('create capsule')

  const list = expectSuccess(await api('GET', '/capsules', { token: state.tokenB }), 'capsule list')
  const locked = list.find((entry) => String(entry.id) === String(state.capsuleId))
  assert(locked, 'partner capsule list misses item')
  equal(locked.message ?? null, null, 'locked capsule leaks message')
  equal(locked.photoUrls ?? null, null, 'locked capsule leaks photos')
  equal(locked.canOpen, false, 'locked capsule canOpen')
  assert(Number(locked.daysLeft) >= 29, 'capsule daysLeft is wrong')
  mark('locked capsule hides content from partner')

  expectCode(
    await api('POST', `/capsules/${state.capsuleId}/open`, { token: state.tokenB, body: {} }),
    400,
    'early capsule open'
  )
  mark('reject early capsule open')

  expectCode(
    await api('POST', `/capsules/${state.capsuleId}/open`, { token: state.tokenC, body: {} }),
    403,
    'cross-couple capsule open'
  )
  mark('reject cross-couple capsule open')

  // 「到期开启」无法通过 API 到达（开启日期必须是未来），直改测试库把开启日期拨到今天
  assert(/^\d+$/.test(String(state.capsuleId)), 'unsafe capsule id')
  execSql(`UPDATE ql_capsule SET open_date=CURDATE() WHERE id=${state.capsuleId}`)
  const opened = expectSuccess(
    await api('POST', `/capsules/${state.capsuleId}/open`, { token: state.tokenB, body: {} }),
    'open due capsule'
  )
  equal(opened.isOpened, 1, 'opened capsule status')
  equal(opened.message, '给未来的我们', 'opened capsule message')
  equal(String(opened.openedBy), String(state.userBId), 'capsule opener')
  mark('partner opens due capsule after date adjustment')

  expectCode(
    await api('DELETE', `/capsules/${state.capsuleId}`, { token: state.tokenB }),
    403,
    'non-creator capsule deletion'
  )
  mark('reject non-creator capsule deletion')

  expectSuccess(await api('DELETE', `/capsules/${state.capsuleId}`, { token: state.tokenA }), 'delete capsule')
  mark('creator deletes capsule')
})

await group('小事记创建、分类与评分校验、筛选、更新与权限', async () => {
  expectCode(
    await api('POST', '/logs', {
      token: state.tokenA,
      body: { title: `评分越界 ${suffix}`, logDate: localDate(0), rating: 6 }
    }),
    400,
    'log rating out of range'
  )
  mark('reject log rating out of range')

  const log = expectSuccess(
    await api('POST', '/logs', {
      token: state.tokenA,
      body: { title: `一起看演唱会 ${suffix}`, category: 'show', logDate: localDate(-7), rating: 5 }
    }),
    'create log'
  )
  state.logId = log.id
  mark('create log')

  const weird = expectSuccess(
    await api('POST', '/logs', {
      token: state.tokenB,
      body: { title: `奇怪分类 ${suffix}`, category: 'whatever', logDate: localDate(-1) }
    }),
    'create log with unknown category'
  )
  equal(weird.category, 'other', 'unknown category fallback')
  mark('unknown category falls back to other')

  const all = expectSuccess(await api('GET', '/logs', { token: state.tokenB }), 'log list')
  assert(containsId(all, state.logId), 'partner log list misses item')
  const shows = expectSuccess(await api('GET', '/logs?category=show', { token: state.tokenA }), 'log category filter')
  assert(containsId(shows, state.logId) && !containsId(shows, weird.id), 'category filter wrong')
  const thisYear = expectSuccess(
    await api('GET', `/logs?year=${new Date().getFullYear()}`, { token: state.tokenA }),
    'log year filter'
  )
  assert(containsId(thisYear, state.logId), 'year filter misses log')
  mark('list and filter logs')

  const updated = expectSuccess(
    await api('PUT', `/logs/${state.logId}`, {
      token: state.tokenB,
      body: { title: `一起看演唱会·改 ${suffix}`, logDate: localDate(-7), rating: 4 }
    }),
    'update log'
  )
  equal(updated.rating, 4, 'updated log rating')
  mark('couple member updates log')

  expectCode(await api('DELETE', `/logs/${weird.id}`, { token: state.tokenC }), 403, 'cross-couple log delete')
  mark('reject cross-couple log deletion')

  expectSuccess(await api('DELETE', `/logs/${weird.id}`, { token: state.tokenA }), 'delete log')
  mark('couple member deletes log')
})

await group('留言板发布、列表、限量与删除权限', async () => {
  expectCode(await api('POST', '/messages', { token: state.tokenA, body: { content: '' } }), 400, 'blank message')
  mark('reject blank message')

  const first = expectSuccess(
    await api('POST', '/messages', { token: state.tokenA, body: { content: `早安 ${suffix}` } }),
    'create message'
  )
  const second = expectSuccess(
    await api('POST', '/messages', { token: state.tokenB, body: { content: `晚安 ${suffix}` } }),
    'partner creates message'
  )
  mark('couple members post messages')

  const list = expectSuccess(await api('GET', '/messages', { token: state.tokenB }), 'message list')
  assert(containsId(list, first.id) && containsId(list, second.id), 'message list misses items')
  assert(list.find((entry) => String(entry.id) === String(first.id))?.nickname, 'message misses nickname')
  mark('list messages with author nickname')

  const limited = expectSuccess(await api('GET', '/messages?limit=1', { token: state.tokenA }), 'limited messages')
  equal(limited.length, 1, 'message limit')
  mark('limit message list')

  expectCode(
    await api('DELETE', `/messages/${first.id}`, { token: state.tokenC }),
    403,
    'cross-couple message delete'
  )
  mark('reject cross-couple message deletion')

  expectCode(await api('DELETE', `/messages/${first.id}`, { token: state.tokenB }), 403, 'partner message delete')
  mark('reject deleting partner message')

  expectSuccess(await api('DELETE', `/messages/${first.id}`, { token: state.tokenA }), 'delete own message')
  expectSuccess(await api('DELETE', `/messages/${second.id}`, { token: state.tokenB }), 'partner deletes own')
  mark('authors delete their own messages')
})

await group('功能开关读取、更新、恢复与非法功能项', async () => {
  const initial = expectSuccess(await api('GET', '/features', { token: state.tokenA }), 'feature list')
  equal(Object.keys(initial).length, 12, 'feature key count')
  equal(initial.notes, true, 'default notes feature')
  equal(initial.board, true, 'default board feature')
  mark('default features are all enabled')

  const disabled = expectSuccess(
    await api('PUT', '/features/board', { token: state.tokenA, body: { enabled: false } }),
    'disable board feature'
  )
  equal(disabled.board, false, 'disabled board feature')
  mark('disable board feature')

  const reread = expectSuccess(await api('GET', '/features', { token: state.tokenB }), 'partner feature list')
  equal(reread.board, false, 'partner sees disabled feature')
  equal(reread.notes, true, 'other features stay enabled')
  mark('partner reads updated features')

  expectCode(
    await api('PUT', '/features/unknown', { token: state.tokenA, body: { enabled: false } }),
    400,
    'unknown feature'
  )
  mark('reject unknown feature key')

  const restored = expectSuccess(
    await api('PUT', '/features/board', { token: state.tokenB, body: { enabled: true } }),
    'restore board feature'
  )
  equal(restored.board, true, 'restored board feature')
  mark('restore board feature')
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
