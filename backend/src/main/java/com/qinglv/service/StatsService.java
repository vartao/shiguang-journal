package com.qinglv.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qinglv.common.BusinessException;
import com.qinglv.entity.*;
import com.qinglv.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final NoteMapper noteMapper;
    private final PhotoMapper photoMapper;
    private final AlbumMapper albumMapper;
    private final AnniversaryMapper anniversaryMapper;
    private final SurpriseMapper surpriseMapper;
    private final NoteLikeMapper noteLikeMapper;

    public Map<String, Object> getCoupleStats(Long coupleId, Long userId) {
        if (coupleId == null) {
            throw new BusinessException("尚未加入情侣空间");
        }

        Map<String, Object> stats = new LinkedHashMap<>();

        // 笔记统计
        long noteCount = noteMapper.selectCount(
            new LambdaQueryWrapper<Note>().eq(Note::getCoupleId, coupleId)
        );
        stats.put("noteCount", noteCount);

        // 照片统计
        long photoCount = photoMapper.selectCount(
            new LambdaQueryWrapper<Photo>().eq(Photo::getCoupleId, coupleId)
        );
        stats.put("photoCount", photoCount);

        // 相册统计
        long albumCount = albumMapper.selectCount(
            new LambdaQueryWrapper<Album>().eq(Album::getCoupleId, coupleId)
        );
        stats.put("albumCount", albumCount);

        // 纪念日统计
        long anniversaryCount = anniversaryMapper.selectCount(
            new LambdaQueryWrapper<Anniversary>().eq(Anniversary::getCoupleId, coupleId)
        );
        stats.put("anniversaryCount", anniversaryCount);

        // 惊喜统计
        long surpriseCount = surpriseMapper.selectCount(
            new LambdaQueryWrapper<Surprise>().eq(Surprise::getCoupleId, coupleId)
        );
        long unlockedSurpriseCount = surpriseMapper.selectCount(
            new LambdaQueryWrapper<Surprise>()
                .eq(Surprise::getCoupleId, coupleId)
                .eq(Surprise::getUnlocked, 1)
        );
        stats.put("surpriseCount", surpriseCount);
        stats.put("unlockedSurpriseCount", unlockedSurpriseCount);

        // 点赞统计
        long totalLikes = noteMapper.selectList(
            new LambdaQueryWrapper<Note>()
                .eq(Note::getCoupleId, coupleId)
                .select(Note::getLikeCount)
        ).stream().mapToLong(n -> n.getLikeCount() != null ? n.getLikeCount() : 0).sum();
        stats.put("totalLikes", totalLikes);

        // 最近30天活跃天数
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
        List<Note> recentNotes = noteMapper.selectList(
            new LambdaQueryWrapper<Note>()
                .eq(Note::getCoupleId, coupleId)
                .ge(Note::getCreateTime, thirtyDaysAgo.atStartOfDay())
                .select(Note::getCreateTime)
        );
        long activeDays = recentNotes.stream()
            .map(n -> n.getCreateTime().toLocalDate())
            .distinct()
            .count();
        stats.put("activeDays30", activeDays);

        // 最长连续写作天数 + 心情分布（需同时查出 mood，否则分布恒为空）
        List<Note> allNotes = noteMapper.selectList(
            new LambdaQueryWrapper<Note>()
                .eq(Note::getCoupleId, coupleId)
                .orderByAsc(Note::getCreateTime)
                .select(Note::getCreateTime, Note::getMood)
        );
        long longestStreak = calculateLongestStreak(allNotes);
        stats.put("longestStreak", longestStreak);

        // 心情分布
        Map<String, Long> moodDistribution = new LinkedHashMap<>();
        allNotes.forEach(n -> {
            if (n.getMood() != null && !n.getMood().isBlank()) {
                moodDistribution.merge(n.getMood(), 1L, Long::sum);
            }
        });
        stats.put("moodDistribution", moodDistribution);

        return stats;
    }

    private long calculateLongestStreak(List<Note> notes) {
        if (notes.isEmpty()) return 0;

        Set<LocalDate> dates = new LinkedHashSet<>();
        notes.forEach(n -> dates.add(n.getCreateTime().toLocalDate()));

        List<LocalDate> sorted = new ArrayList<>(dates);
        Collections.sort(sorted);

        long maxStreak = 1;
        long currentStreak = 1;

        for (int i = 1; i < sorted.size(); i++) {
            if (sorted.get(i).equals(sorted.get(i - 1).plusDays(1))) {
                currentStreak++;
                maxStreak = Math.max(maxStreak, currentStreak);
            } else {
                currentStreak = 1;
            }
        }
        return maxStreak;
    }
}
