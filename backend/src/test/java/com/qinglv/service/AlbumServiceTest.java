package com.qinglv.service;

import com.qinglv.common.BusinessException;
import com.qinglv.entity.Album;
import com.qinglv.entity.Photo;
import com.qinglv.mapper.AlbumMapper;
import com.qinglv.mapper.PhotoMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlbumServiceTest {

    @Mock
    private AlbumMapper albumMapper;
    @Mock
    private PhotoMapper photoMapper;
    @Mock
    private FileService fileService;

    @InjectMocks
    private AlbumService albumService;

    private Album album;

    @BeforeEach
    void setUp() {
        album = new Album();
        album.setId(10L);
        album.setCoupleId(100L);
        album.setName("旅行");
        album.setPhotoCount(0);
    }

    @Test
    void addPhotoByUrlPersistsPhotoAndInitializesAlbumSummary() {
        when(albumMapper.selectById(10L)).thenReturn(album);
        when(photoMapper.selectCount(any())).thenReturn(1L);

        Photo photo = albumService.addPhotoByUrl(
                1L, 100L, 10L, "/uploads/first.jpg", "第一张");

        assertEquals("/uploads/first.jpg", photo.getPhotoUrl());
        assertEquals("/uploads/first.jpg", album.getCoverImage());
        assertEquals(1, album.getPhotoCount());
        verify(photoMapper).insert(photo);
        verify(albumMapper).updateById(album);
    }

    @Test
    void deletingCoverPhotoSelectsAReplacementAndRefreshesCount() {
        Photo deleted = new Photo();
        deleted.setId(7L);
        deleted.setAlbumId(10L);
        deleted.setCoupleId(100L);
        deleted.setPhotoUrl("/uploads/old-cover.jpg");
        album.setCoverImage(deleted.getPhotoUrl());

        Photo replacement = new Photo();
        replacement.setPhotoUrl("/uploads/replacement.jpg");
        when(photoMapper.selectById(7L)).thenReturn(deleted);
        when(albumMapper.selectById(10L)).thenReturn(album);
        when(photoMapper.selectCount(any())).thenReturn(1L);
        when(photoMapper.selectOne(any())).thenReturn(replacement);

        albumService.deletePhoto(7L, 100L);

        assertEquals(1, album.getPhotoCount());
        assertEquals("/uploads/replacement.jpg", album.getCoverImage());
        verify(photoMapper).deleteById(7L);
        verify(albumMapper).updateById(album);
    }

    @Test
    void deletingLastCoverPhotoClearsCover() {
        Photo deleted = new Photo();
        deleted.setId(7L);
        deleted.setAlbumId(10L);
        deleted.setCoupleId(100L);
        deleted.setPhotoUrl("/uploads/only.jpg");
        album.setCoverImage(deleted.getPhotoUrl());

        when(photoMapper.selectById(7L)).thenReturn(deleted);
        when(albumMapper.selectById(10L)).thenReturn(album);
        when(photoMapper.selectCount(any())).thenReturn(0L);
        when(photoMapper.selectOne(any())).thenReturn(null);

        albumService.deletePhoto(7L, 100L);

        assertEquals(0, album.getPhotoCount());
        assertNull(album.getCoverImage());
    }

    @Test
    void deletingPhotoFromAnotherCoupleIsRejectedWithoutMutation() {
        Photo photo = new Photo();
        photo.setId(7L);
        photo.setCoupleId(999L);
        when(photoMapper.selectById(7L)).thenReturn(photo);

        BusinessException error = assertThrows(
                BusinessException.class, () -> albumService.deletePhoto(7L, 100L));

        assertEquals(403, error.getCode());
        verify(photoMapper, never()).deleteById(any());
    }
}
