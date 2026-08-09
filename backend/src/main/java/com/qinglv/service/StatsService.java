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
    private final TravelMapper travelMapper;
    private final CoupleLogMapper coupleLogMapper;

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

    /**
     * 那年今天：历年同月同日的笔记/足迹/小事记（只取往年，不含今年）
     */
    public List<Map<String, Object>> getOnThisDay(Long coupleId) {
        if (coupleId == null) {
            throw new BusinessException("尚未加入情侣空间");
        }
        LocalDate today = LocalDate.now();
        int month = today.getMonthValue();
        int day = today.getDayOfMonth();
        int year = today.getYear();

        List<Map<String, Object>> items = new ArrayList<>();

        noteMapper.selectList(
                new LambdaQueryWrapper<Note>()
                    .eq(Note::getCoupleId, coupleId)
                    .apply("MONTH(note_date) = {0} AND DAY(note_date) = {1}", month, day)
                    .apply("YEAR(note_date) < {0}", year)
                    .orderByDesc(Note::getNoteDate)
        ).forEach(n -> items.add(onThisDayItem("note", n.getId(), n.getTitle(), n.getNoteDate(), null)));

        travelMapper.selectList(
                new LambdaQueryWrapper<Travel>()
                    .eq(Travel::getCoupleId, coupleId)
                    .isNotNull(Travel::getTravelDate)
                    .apply("MONTH(travel_date) = {0} AND DAY(travel_date) = {1}", month, day)
                    .apply("YEAR(travel_date) < {0}", year)
                    .orderByDesc(Travel::getTravelDate)
        ).forEach(t -> items.add(onThisDayItem("travel", t.getId(), t.getTitle(), t.getTravelDate(), t.getCity())));

        coupleLogMapper.selectList(
                new LambdaQueryWrapper<CoupleLog>()
                    .eq(CoupleLog::getCoupleId, coupleId)
                    .apply("MONTH(log_date) = {0} AND DAY(log_date) = {1}", month, day)
                    .apply("YEAR(log_date) < {0}", year)
                    .orderByDesc(CoupleLog::getLogDate)
        ).forEach(l -> items.add(onThisDayItem("log", l.getId(), l.getTitle(), l.getLogDate(), l.getCategory())));

        items.sort((a, b) -> ((LocalDate) b.get("date")).compareTo((LocalDate) a.get("date")));
        return items;
    }

    private Map<String, Object> onThisDayItem(String type, Long id, String title, LocalDate date, String extra) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("type", type);
        item.put("id", id);
        item.put("title", title);
        item.put("date", date);
        item.put("year", date.getYear());
        if (extra != null && !extra.isBlank()) {
            item.put("extra", extra);
        }
        return item;
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
