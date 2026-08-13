# 拾光手账 · 微信小程序

微信原生小程序（WXML/WXSS/JS，无框架），复用项目现有 Spring Boot 后端 API（JWT 鉴权）。

## 导入运行

1. 微信开发者工具 → 导入项目 → 选择仓库**根目录**（`project.config.json` 已配置 `miniprogramRoot: miniprogram/`）
2. 开发调试：详情 → 本地设置 → 勾选「不校验合法域名、TLS 版本以及 HTTPS 证书」
3. 编译运行，先用网页端注册的账号密码登录；或直接在小程序里注册（填对方邀请码即可加入同一个小窝）

## 后端地址

在 `miniprogram/app.js` 的 `globalData.apiBase` 一处修改：

- 模拟器/本机调试：默认 `http://localhost:8090/api`（即 Docker 部署的后端）
- 真机调试：改成电脑局域网 IP，如 `http://192.168.x.x:8090/api`（手机与电脑同 WiFi）
- 生产：必须 HTTPS 域名，并在小程序后台配置 request 合法域名；上传域名也要配（`/upload` 在同一域名下）

## 结构

```
miniprogram/
├── app.js            # 全局态（token/userInfo/features）、登录态恢复、功能开关
├── app.wxss          # 设计变量（复古手账配色）+ 全局工具类（card/btn/empty/sheet/rise…）
├── utils/api.js      # Promise 化 wx.request（JWT/401/错误 toast）+ 上传压缩 + fullUrl
├── utils/util.js     # 日期/心情/天气/分类常量
├── custom-tab-bar/   # 自定义底部导航（时间线/足迹/✏️写笔记/留言板/我的）
└── pages/            # 17 个页面（登录、时间线、写笔记、相册×2、纪念日、惊喜盒、心愿、
                        #   足迹、行程×2、吃什么、小事记、胶囊、留言板、统计、我的）
```

功能开关与网页端共用 `GET/PUT /api/features`，「我的 → 功能管理」里配置，宫格入口实时跟随。

## 校验

```bash
node scripts/check-miniprogram.mjs   # JS 语法 + JSON 合法 + WXML 标签配对
```
