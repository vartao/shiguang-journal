package com.qinglv.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qinglv.common.BusinessException;
import com.qinglv.dto.AlbumDTO;
import com.qinglv.dto.PhotoDTO;
import com.qinglv.entity.Album;
import com.qinglv.entity.Photo;
import com.qinglv.mapper.AlbumMapper;
import com.qinglv.mapper.PhotoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;

/**
 * 相册服务 - 相册和照片管理
 */
@Service
@RequiredArgsConstructor
public class AlbumService {

    private final AlbumMapper albumMapper;
    private final PhotoMapper photoMapper;
    private final FileService fileService;

    /**
     * 获取空间下所有相册
     */
    public List<Album> getAlbums(Long coupleId) {
        if (coupleId == null) {
            throw new BusinessException("尚未加入情侣空间");
        }
        return albumMapper.selectList(
                new LambdaQueryWrapper<Album>()
                        .eq(Album::getCoupleId, coupleId)
                        .orderByDesc(Album::getCreateTime)
        );
    }

    /**
     * 获取单个相册
     */
    public Album getAlbum(Long albumId, Long coupleId) {
        Album album = albumMapper.selectById(albumId);
        if (album == null) {
            throw new BusinessException("相册不存在");
        }
        if (!album.getCoupleId().equals(coupleId)) {
            throw new BusinessException(403, "无权查看该相册");
        }
        return album;
    }

    /**
     * 创建相册
     */
    public Album createAlbum(Long userId, Long coupleId, AlbumDTO dto) {
        if (coupleId == null) {
            throw new BusinessException("尚未加入情侣空间");
        }

        Album album = new Album();
        album.setCoupleId(coupleId);
        album.setUserId(userId);
        album.setName(dto.getName());
        album.setDescription(dto.getDescription());
        // 兼容 cover / coverImage
        String cover = dto.getCoverImage();
        if ((cover == null || cover.isEmpty()) && dto.getCover() != null) {
            cover = dto.getCover();
        }
        album.setCoverImage(cover);
        album.setPhotoCount(0);

        albumMapper.insert(album);
        return album;
    }

    /**
     * 更新相册信息
     */
    public Album updateAlbum(Long albumId, Long coupleId, AlbumDTO dto) {
        Album album = getAlbum(albumId, coupleId);

        if (dto.getName() != null && !dto.getName().isBlank()) {
            album.setName(dto.getName().trim());
        }
        if (dto.getDescription() != null) {
            album.setDescription(dto.getDescription());
        }
        String cover = dto.getCoverImage();
        if ((cover == null || cover.isEmpty()) && dto.getCover() != null) {
            cover = dto.getCover();
        }
        if (cover != null) {
            album.setCoverImage(cover.isEmpty() ? null : cover);
        }

        albumMapper.updateById(album);
        return album;
    }

    /**
     * 通过已上传的图片 URL 添加照片
     */
    @Transactional(rollbackFor = Exception.class)
    public Photo addPhotoByUrl(Long userId, Long coupleId, Long albumId, String photoUrl, String description) {
        if (coupleId == null) {
            throw new BusinessException("尚未加入情侣空间");
        }
        if (photoUrl == null || photoUrl.isBlank()) {
            throw new BusinessException("照片地址不能为空");
        }

        Album album = getAlbum(albumId, coupleId);
        Photo photo = createPhoto(userId, coupleId, albumId, photoUrl, description, null);
        refreshAlbumAfterAdd(album, photoUrl);
        return photo;
    }

    /**
     * 获取相册下的照片列表
     */
    public List<Photo> getPhotos(Long albumId, Long coupleId) {
        getAlbum(albumId, coupleId);

        return photoMapper.selectList(
                new LambdaQueryWrapper<Photo>()
                        .eq(Photo::getAlbumId, albumId)
                        .orderByDesc(Photo::getCreateTime)
        );
    }

    /**
     * 上传照片到相册
     */
    @Transactional(rollbackFor = Exception.class)
    public Photo uploadPhoto(Long userId, Long coupleId, MultipartFile file, PhotoDTO dto) {
        if (coupleId == null) {
            throw new BusinessException("尚未加入情侣空间");
        }

        Album album = getAlbum(dto.getAlbumId(), coupleId);

        // 上传文件
        String fileUrl = fileService.uploadFile(file, userId);

        Photo photo = createPhoto(
                userId, coupleId, dto.getAlbumId(), fileUrl, dto.getDescription(), file.getSize());
        refreshAlbumAfterAdd(album, fileUrl);

        return photo;
    }

    /**
     * 删除照片
     */
    @Transactional(rollbackFor = Exception.class)
    public void deletePhoto(Long photoId, Long coupleId) {
        Photo photo = photoMapper.selectById(photoId);
        if (photo == null) {
            throw new BusinessException("照片不存在");
        }
        if (!photo.getCoupleId().equals(coupleId)) {
            throw new BusinessException(403, "无权删除该照片");
        }

        photoMapper.deleteById(photoId);

        Album album = albumMapper.selectById(photo.getAlbumId());
        if (album != null) {
            refreshAlbumAfterDelete(album, photo);
        }
    }

    /**
     * 删除相册
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteAlbum(Long albumId, Long coupleId) {
        Album album = albumMapper.selectById(albumId);
        if (album == null) {
            throw new BusinessException("相册不存在");
        }
        if (!album.getCoupleId().equals(coupleId)) {
            throw new BusinessException(403, "无权删除该相册");
        }

        // 删除相册下所有照片
        photoMapper.delete(
                new LambdaQueryWrapper<Photo>().eq(Photo::getAlbumId, albumId)
        );

        // 删除相册
        albumMapper.deleteById(albumId);
    }

    private Photo createPhoto(
            Long userId,
            Long coupleId,
            Long albumId,
            String photoUrl,
            String description,
            Long fileSize) {
        Photo photo = new Photo();
        photo.setAlbumId(albumId);
        photo.setCoupleId(coupleId);
        photo.setUserId(userId);
        photo.setPhotoUrl(photoUrl);
        photo.setThumbnailUrl(photoUrl);
        photo.setDescription(description);
        photo.setFileSize(fileSize);
        photoMapper.insert(photo);
        return photo;
    }

    private void refreshAlbumAfterAdd(Album album, String addedPhotoUrl) {
        album.setPhotoCount(countPhotos(album.getId()));
        if (album.getCoverImage() == null || album.getCoverImage().isBlank()) {
            album.setCoverImage(addedPhotoUrl);
        }
        albumMapper.updateById(album);
    }

    private void refreshAlbumAfterDelete(Album album, Photo deletedPhoto) {
        album.setPhotoCount(countPhotos(album.getId()));
        if (Objects.equals(album.getCoverImage(), deletedPhoto.getPhotoUrl())) {
            Photo replacement = photoMapper.selectOne(
                    new LambdaQueryWrapper<Photo>()
                            .eq(Photo::getAlbumId, album.getId())
                            .orderByAsc(Photo::getCreateTime)
                            .last("LIMIT 1")
            );
            album.setCoverImage(replacement == null ? null : replacement.getPhotoUrl());
        }
        albumMapper.updateById(album);
    }

    private int countPhotos(Long albumId) {
        Long count = photoMapper.selectCount(
                new LambdaQueryWrapper<Photo>().eq(Photo::getAlbumId, albumId)
        );
        return count == null ? 0 : Math.toIntExact(count);
    }
}
