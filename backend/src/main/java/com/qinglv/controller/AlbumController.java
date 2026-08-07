package com.qinglv.controller;

import com.qinglv.common.JwtInterceptor;
import com.qinglv.common.Result;
import com.qinglv.dto.AlbumDTO;
import com.qinglv.dto.PhotoDTO;
import com.qinglv.entity.Album;
import com.qinglv.entity.Photo;
import com.qinglv.entity.User;
import com.qinglv.mapper.UserMapper;
import com.qinglv.service.AlbumService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 相册控制器
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AlbumController {

    private final AlbumService albumService;
    private final UserMapper userMapper;

    /**
     * 获取所有相册
     * GET /api/albums
     */
    @GetMapping("/albums")
    public Result<List<Album>> getAlbums() {
        Long coupleId = currentCoupleId();
        List<Album> albums = albumService.getAlbums(coupleId);
        return Result.success(albums);
    }

    /**
     * 创建相册
     * POST /api/albums
     */
    @PostMapping("/albums")
    public Result<Album> createAlbum(@Valid @RequestBody AlbumDTO dto) {
        Long userId = JwtInterceptor.getCurrentUserId();
        Long coupleId = currentCoupleId();
        Album album = albumService.createAlbum(userId, coupleId, dto);
        return Result.success("创建成功", album);
    }

    /**
     * 更新相册
     * PUT /api/albums/{id}
     */
    @PutMapping("/albums/{id}")
    public Result<Album> updateAlbum(@PathVariable Long id, @RequestBody AlbumDTO dto) {
        Long coupleId = currentCoupleId();
        Album album = albumService.updateAlbum(id, coupleId, dto);
        return Result.success("更新成功", album);
    }

    /**
     * 相册详情（含照片）
     * GET /api/albums/{id}
     */
    @GetMapping("/albums/{id}")
    public Result<Map<String, Object>> getAlbumDetail(@PathVariable Long id) {
        Long coupleId = currentCoupleId();
        Album album = albumService.getAlbum(id, coupleId);
        List<Photo> photos = albumService.getPhotos(id, coupleId);

        Map<String, Object> data = new HashMap<>();
        data.put("id", album.getId());
        data.put("name", album.getName());
        data.put("description", album.getDescription());
        data.put("coverImage", album.getCoverImage());
        data.put("cover", album.getCoverImage());
        data.put("photoCount", album.getPhotoCount());
        data.put("createTime", album.getCreateTime());
        data.put("createdAt", album.getCreateTime());
        data.put("photos", photos);
        return Result.success(data);
    }

    /**
     * 获取相册下的照片列表
     * GET /api/albums/{id}/photos
     */
    @GetMapping("/albums/{id}/photos")
    public Result<List<Photo>> getPhotos(@PathVariable Long id) {
        Long coupleId = currentCoupleId();
        List<Photo> photos = albumService.getPhotos(id, coupleId);
        return Result.success(photos);
    }

    /**
     * 上传照片（multipart）
     * POST /api/photos/upload
     */
    @PostMapping("/photos/upload")
    public Result<Photo> uploadPhoto(
            @RequestParam("file") MultipartFile file,
            @RequestParam("albumId") Long albumId,
            @RequestParam(value = "description", required = false) String description) {
        Long userId = JwtInterceptor.getCurrentUserId();
        Long coupleId = currentCoupleId();

        PhotoDTO photoDTO = new PhotoDTO();
        photoDTO.setAlbumId(albumId);
        photoDTO.setDescription(description);

        Photo photo = albumService.uploadPhoto(userId, coupleId, file, photoDTO);
        return Result.success("上传成功", photo);
    }

    /**
     * 通过已上传 URL 添加照片到相册
     * POST /api/albums/{id}/photos
     */
    @PostMapping("/albums/{id}/photos")
    public Result<Photo> addPhotoByUrl(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Long userId = JwtInterceptor.getCurrentUserId();
        Long coupleId = currentCoupleId();
        String url = body != null ? body.getOrDefault("url", body.get("photoUrl")) : null;
        String description = body != null ? body.getOrDefault("description", body.get("caption")) : null;
        Photo photo = albumService.addPhotoByUrl(userId, coupleId, id, url, description);
        return Result.success("添加成功", photo);
    }

    /**
     * 删除照片
     * DELETE /api/photos/{id}
     * 兼容 DELETE /api/albums/{albumId}/photos/{id}
     */
    @DeleteMapping({"/photos/{id}", "/albums/{albumId}/photos/{id}"})
    public Result<Void> deletePhoto(@PathVariable Long id, @PathVariable(required = false) Long albumId) {
        Long coupleId = currentCoupleId();
        albumService.deletePhoto(id, coupleId);
        return Result.success("删除成功", null);
    }

    /**
     * 删除相册
     * DELETE /api/albums/{id}
     */
    @DeleteMapping("/albums/{id}")
    public Result<Void> deleteAlbum(@PathVariable Long id) {
        Long coupleId = currentCoupleId();
        albumService.deleteAlbum(id, coupleId);
        return Result.success("删除成功", null);
    }

    private Long currentCoupleId() {
        Long userId = JwtInterceptor.getCurrentUserId();
        User user = userMapper.selectById(userId);
        return user != null ? user.getCoupleId() : null;
    }
}
