#Requires -Version 5.1
<#
.SYNOPSIS
  拾光手账 API 验收脚本（注册→绑定→笔记→点赞→惊喜→统计）

.USAGE
  # 确保后端已在 8080 启动，MySQL 已初始化
  powershell -ExecutionPolicy Bypass -File scripts/acceptance.ps1
  powershell -ExecutionPolicy Bypass -File scripts/acceptance.ps1 -BaseUrl http://localhost:8090/api
#>
param(
    [string]$BaseUrl = "http://localhost:8080/api"
)

$ErrorActionPreference = "Stop"
$script:Passed = 0
$script:Failed = 0
$script:Results = @()

function Write-Step($msg) { Write-Host "`n==> $msg" -ForegroundColor Cyan }
function Ok($name, $detail = "") {
    $script:Passed++
    $script:Results += [pscustomobject]@{ Status = "PASS"; Name = $name; Detail = $detail }
    Write-Host "  [PASS] $name $(if($detail){"- $detail"})" -ForegroundColor Green
}
function Fail($name, $detail) {
    $script:Failed++
    $script:Results += [pscustomobject]@{ Status = "FAIL"; Name = $name; Detail = $detail }
    Write-Host "  [FAIL] $name - $detail" -ForegroundColor Red
}

function Invoke-Api {
    param(
        [string]$Method,
        [string]$Path,
        [object]$Body = $null,
        [string]$Token = $null,
        [int]$ExpectCode = 200
    )
    $headers = @{ "Content-Type" = "application/json" }
    if ($Token) { $headers["Authorization"] = "Bearer $Token" }
    $uri = "$BaseUrl$Path"
    $params = @{
        Method      = $Method
        Uri         = $uri
        Headers     = $headers
        UseBasicParsing = $true
    }
    if ($null -ne $Body) {
        $params.Body = ($Body | ConvertTo-Json -Depth 8 -Compress)
    }
    try {
        $resp = Invoke-WebRequest @params
        $json = $resp.Content | ConvertFrom-Json
        return $json
    } catch {
        $ex = $_.Exception
        $bodyText = $null
        if ($ex.Response) {
            try {
                $reader = New-Object System.IO.StreamReader($ex.Response.GetResponseStream())
                $bodyText = $reader.ReadToEnd()
                $reader.Close()
            } catch {}
        }
        if ($bodyText) {
            try { return ($bodyText | ConvertFrom-Json) } catch {}
            throw "HTTP error: $bodyText"
        }
        throw $_
    }
}

function Assert-BizOk($json, $stepName) {
    if ($null -eq $json) { Fail $stepName "empty response"; return $false }
    if ($json.code -ne 200 -and $json.code -ne 0) {
        Fail $stepName "code=$($json.code) message=$($json.message)"
        return $false
    }
    return $true
}

$suffix = Get-Date -Format "yyyyMMddHHmmss"
$userA = "acc_a_$suffix"
$userB = "acc_b_$suffix"
$pass = "Test1234"

Write-Host "========================================" -ForegroundColor Yellow
Write-Host "  拾光手账 API 验收  BaseUrl=$BaseUrl" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Yellow

# 0. 连通性
Write-Step "0. 后端连通性探测"
try {
    # 未登录应 401
    $probe = Invoke-Api -Method GET -Path "/auth/profile" -ExpectCode 401
    if ($probe.code -eq 401) { Ok "API reachable" "401 without token (expected)" }
    else { Ok "API reachable" "got code=$($probe.code)" }
} catch {
    Fail "API reachable" $_.Exception.Message
    Write-Host "`n后端不可达，请先启动：" -ForegroundColor Red
    Write-Host "  cd backend; mvn spring-boot:run" -ForegroundColor Gray
    Write-Host "或 Docker: docker compose up -d --build" -ForegroundColor Gray
    exit 1
}

# 1. 注册用户 A（建空间）
Write-Step "1. 注册用户 A（创建小窝）"
$regA = Invoke-Api -Method POST -Path "/auth/register" -Body @{
    username     = $userA
    password     = $pass
    nickname     = "验收甲方"
    togetherDate = "2024-05-20"
    role         = "boy"
}
if (Assert-BizOk $regA "register A") {
    $tokenA = $regA.data.token
    $coupleIdA = $regA.data.coupleId
    if ($tokenA -and $coupleIdA) { Ok "register A" "coupleId=$coupleIdA" }
    else { Fail "register A" "missing token or coupleId: $($regA | ConvertTo-Json -Compress)" }
}

# 2. 获取邀请码
Write-Step "2. 获取情侣空间 / 邀请码"
$couple = Invoke-Api -Method GET -Path "/couple/info" -Token $tokenA
$invite = $null
if (Assert-BizOk $couple "couple info") {
    $invite = $couple.data.inviteCode
    if ($invite) { Ok "invite code" $invite }
    else { Fail "invite code" "empty" }
}

# 3. 注册用户 B（带邀请码）
Write-Step "3. 注册用户 B（加入小窝）"
$regB = Invoke-Api -Method POST -Path "/auth/register" -Body @{
    username   = $userB
    password   = $pass
    nickname   = "验收乙方"
    inviteCode = $invite
    role       = "girl"
}
$tokenB = $null
if (Assert-BizOk $regB "register B") {
    $tokenB = $regB.data.token
    $coupleIdB = $regB.data.coupleId
    if ($coupleIdB -eq $coupleIdA) { Ok "bind couple" "both coupleId=$coupleIdB" }
    else { Fail "bind couple" "A=$coupleIdA B=$coupleIdB" }
}

# 4. 登录 A
Write-Step "4. 登录用户 A"
$loginA = Invoke-Api -Method POST -Path "/auth/login" -Body @{
    username = $userA
    password = $pass
}
if (Assert-BizOk $loginA "login A") {
    $tokenA = $loginA.data.token
    Ok "login A" "token ok"
}

# 5. 发笔记
Write-Step "5. 用户 A 发布笔记"
$note = Invoke-Api -Method POST -Path "/notes" -Token $tokenA -Body @{
    title   = "验收笔记-$suffix"
    content = "今天一起验收拾光手账，心情很好。"
    mood    = "happy"
    weather = "sunny"
    isTop   = 0
}
$noteId = $null
if (Assert-BizOk $note "create note") {
    $noteId = $note.data.id
    if ($noteId) { Ok "create note" "id=$noteId mood=$($note.data.mood)" }
    else { Fail "create note" "no id" }
}

# 6. 时间线
Write-Step "6. 时间线列表"
$timeline = Invoke-Api -Method GET -Path "/notes?page=1&size=10" -Token $tokenB
if (Assert-BizOk $timeline "timeline") {
    $list = $timeline.data.records
    if (-not $list) { $list = $timeline.data.list }
    if (-not $list) { $list = $timeline.data }
    $found = $false
    if ($list -is [System.Array]) {
        $found = ($list | Where-Object { $_.id -eq $noteId }).Count -gt 0
    } elseif ($list.id -eq $noteId) { $found = $true }
    if ($found) { Ok "timeline contains note" "id=$noteId" }
    else { Fail "timeline contains note" "response keys: $(($timeline.data | Get-Member -MemberType NoteProperty).Name -join ',')" }
}

# 7. 点赞
Write-Step "7. 用户 B 点赞 / 取消"
$like1 = Invoke-Api -Method POST -Path "/notes/$noteId/like" -Token $tokenB
if (Assert-BizOk $like1 "like") {
    if ($like1.data.liked -eq $true -or $like1.data.liked -eq "True") {
        Ok "like toggle on" "likeCount=$($like1.data.likeCount)"
    } else { Fail "like toggle on" ($like1 | ConvertTo-Json -Compress) }
}
$like2 = Invoke-Api -Method POST -Path "/notes/$noteId/like" -Token $tokenB
if (Assert-BizOk $like2 "unlike") {
    if ($like2.data.liked -eq $false -or $like2.data.liked -eq "False") {
        Ok "like toggle off" "likeCount=$($like2.data.likeCount)"
    } else { Fail "like toggle off" ($like2 | ConvertTo-Json -Compress) }
}
# 再点一次方便后续统计
Invoke-Api -Method POST -Path "/notes/$noteId/like" -Token $tokenB | Out-Null

# 8. 惊喜 click
Write-Step "8. 惊喜盒子 click 解锁"
$surp = Invoke-Api -Method POST -Path "/surprises" -Token $tokenA -Body @{
    title           = "验收惊喜-$suffix"
    content         = "拆开后的甜蜜内容"
    hint            = "点一下就知道"
    unlockCondition = "click"
}
$surpId = $null
if (Assert-BizOk $surp "create surprise") {
    $surpId = $surp.data.id
    $contentHidden = [string]::IsNullOrEmpty([string]$surp.data.content)
    # 创建后可能仍未解锁，content 应为空
    Ok "create surprise" "id=$surpId unlocked=$($surp.data.unlocked)"
}
$detailLocked = Invoke-Api -Method GET -Path "/surprises/$surpId" -Token $tokenB
if (Assert-BizOk $detailLocked "surprise locked detail") {
    if ([string]::IsNullOrEmpty([string]$detailLocked.data.content)) {
        Ok "content hidden when locked" ""
    } else {
        Fail "content hidden when locked" "content leaked"
    }
}
$unlock = Invoke-Api -Method POST -Path "/surprises/$surpId/unlock" -Token $tokenB -Body @{}
if (Assert-BizOk $unlock "unlock surprise") {
    if ($unlock.data.content -eq "拆开后的甜蜜内容") {
        Ok "unlock reveals content" ""
    } else {
        Fail "unlock reveals content" "content=$($unlock.data.content)"
    }
}

# 9. 统计
Write-Step "9. 恋爱数据统计"
$stats = Invoke-Api -Method GET -Path "/stats" -Token $tokenA
if (Assert-BizOk $stats "stats") {
    $nc = $stats.data.noteCount
    $md = $stats.data.moodDistribution
    if ($nc -ge 1) { Ok "stats noteCount" "noteCount=$nc" } else { Fail "stats noteCount" "noteCount=$nc" }
    if ($md -and $md.happy -ge 1) {
        Ok "stats moodDistribution" "happy=$($md.happy)"
    } else {
        Fail "stats moodDistribution" "missing happy (got: $($md | ConvertTo-Json -Compress))"
    }
}

# 10. 权限：B 不能删 A 的笔记
Write-Step "10. 权限：B 删除 A 的笔记应失败"
$del = Invoke-Api -Method DELETE -Path "/notes/$noteId" -Token $tokenB
if ($del.code -eq 403 -or ($del.message -match "只能删除|无权")) {
    Ok "author-only delete" "code=$($del.code) msg=$($del.message)"
} elseif ($del.code -eq 200) {
    Fail "author-only delete" "B was able to delete A's note"
} else {
    Fail "author-only delete" "unexpected code=$($del.code) msg=$($del.message)"
}

# 汇总
Write-Host "`n========================================" -ForegroundColor Yellow
Write-Host "  结果: PASS=$script:Passed  FAIL=$script:Failed" -ForegroundColor $(if ($script:Failed -eq 0) { "Green" } else { "Red" })
Write-Host "========================================" -ForegroundColor Yellow
$script:Results | Format-Table -AutoSize

if ($script:Failed -gt 0) { exit 1 } else { exit 0 }
