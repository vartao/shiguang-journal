package com.qinglv.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qinglv.common.BusinessException;
import com.qinglv.common.JwtUtil;
import com.qinglv.common.UploadUrls;
import com.qinglv.dto.LoginDTO;
import com.qinglv.dto.LoginVO;
import com.qinglv.dto.RegisterDTO;
import com.qinglv.entity.Couple;
import com.qinglv.entity.User;
import com.qinglv.mapper.CoupleMapper;
import com.qinglv.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 认证服务 - 登录注册、JWT
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final CoupleMapper coupleMapper;
    private final JwtUtil jwtUtil;

    /**
     * 用户登录
     */
    public LoginVO login(LoginDTO loginDTO) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, loginDTO.getUsername())
        );
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }

        if (!BCrypt.checkpw(loginDTO.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        return buildLoginVO(user);
    }

    /**
     * 用户注册
     */
    @Transactional(rollbackFor = Exception.class)
    public LoginVO register(RegisterDTO registerDTO) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, registerDTO.getUsername())
        );
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }

        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(BCrypt.hashpw(registerDTO.getPassword()));
        user.setNickname(registerDTO.getNickname());
        user.setGender(resolveGender(registerDTO));

        if (StrUtil.isNotBlank(registerDTO.getInviteCode())) {
            // 通过邀请码加入已有空间：先插入用户拿到 ID，再绑定空间
            Couple couple = coupleMapper.selectByInviteCodeForUpdate(registerDTO.getInviteCode().trim().toUpperCase());
            if (couple == null) {
                throw new BusinessException("邀请码不正确，请向 TA 再确认一次～");
            }
            if (couple.getUserId1() != null && couple.getUserId2() != null) {
                throw new BusinessException("这个小窝已经住满两人啦，换一个邀请码试试");
            }

            user.setRole("member");
            insertNewUser(user);

            if (couple.getUserId1() == null) {
                couple.setUserId1(user.getId());
            } else {
                couple.setUserId2(user.getId());
            }
            coupleMapper.updateById(couple);

            user.setCoupleId(couple.getId());
            userMapper.updateById(user);
        } else {
            // 创建新的情侣空间
            user.setRole("owner");
            insertNewUser(user);

            Couple couple = new Couple();
            couple.setName(registerDTO.getNickname() + "的小窝");
            couple.setUserId1(user.getId());
            couple.setInviteCode(generateInviteCode());
            couple.setAnnouncement("欢迎回家~");

            if (StrUtil.isNotBlank(registerDTO.getTogetherDate())) {
                couple.setTogetherDate(LocalDate.parse(registerDTO.getTogetherDate(), DateTimeFormatter.ISO_LOCAL_DATE));
            }

            coupleMapper.insert(couple);

            user.setCoupleId(couple.getId());
            userMapper.updateById(user);
        }

        // 重新查询，保证 coupleId / role 等字段最新
        User saved = userMapper.selectById(user.getId());
        return buildLoginVO(saved);
    }

    /**
     * 获取当前用户资料
     */
    public LoginVO.UserInfo getProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(401, "用户不存在或已失效");
        }
        return toUserInfo(user);
    }

    /**
     * 更新头像
     */
    public LoginVO.UserInfo updateAvatar(Long userId, String avatar) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setAvatar(UploadUrls.toStoredPath(avatar));
        userMapper.updateById(user);
        return toUserInfo(user);
    }

    /**
     * 更新个人资料（昵称 / 头像）
     */
    public LoginVO.UserInfo updateProfile(Long userId, String nickname, String avatar) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (StrUtil.isNotBlank(nickname)) {
            user.setNickname(nickname.trim());
        }
        if (avatar != null) {
            user.setAvatar(avatar.isBlank() ? null : UploadUrls.toStoredPath(avatar));
        }
        userMapper.updateById(user);
        return toUserInfo(user);
    }

    private LoginVO buildLoginVO(User user) {
        LoginVO loginVO = new LoginVO();
        loginVO.setToken(jwtUtil.generateToken(user.getId(), user.getUsername()));
        loginVO.setUserId(user.getId());
        loginVO.setUsername(user.getUsername());
        loginVO.setNickname(user.getNickname());
        loginVO.setAvatar(user.getAvatar());
        loginVO.setCoupleId(user.getCoupleId());
        loginVO.setRole(user.getRole());
        loginVO.setUser(toUserInfo(user));
        return loginVO;
    }

    private LoginVO.UserInfo toUserInfo(User user) {
        LoginVO.UserInfo info = new LoginVO.UserInfo();
        info.setId(user.getId());
        info.setUserId(user.getId());
        info.setUsername(user.getUsername());
        info.setNickname(user.getNickname());
        info.setAvatar(user.getAvatar());
        info.setCoupleId(user.getCoupleId());
        info.setRole(user.getRole());
        info.setGender(user.getGender());
        return info;
    }

    private void insertNewUser(User user) {
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException duplicateUsername) {
            // 预查询只改善提示；唯一索引才是并发注册时的最终约束。
            throw new BusinessException("用户名已存在");
        }
    }

    /**
     * 解析性别：优先 gender 数值；否则兼容前端 role=boy/girl
     */
    private Integer resolveGender(RegisterDTO dto) {
        if (dto.getGender() != null) {
            return dto.getGender();
        }
        if (StrUtil.isNotBlank(dto.getRole())) {
            String role = dto.getRole().trim().toLowerCase();
            if ("boy".equals(role) || "male".equals(role) || "1".equals(role)) {
                return 1;
            }
            if ("girl".equals(role) || "female".equals(role) || "2".equals(role)) {
                return 2;
            }
        }
        return 0;
    }

    /**
     * 生成6位邀请码
     */
    private String generateInviteCode() {
        String code = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        Long count = coupleMapper.selectCount(
                new LambdaQueryWrapper<Couple>().eq(Couple::getInviteCode, code)
        );
        if (count > 0) {
            return generateInviteCode();
        }
        return code;
    }
}
