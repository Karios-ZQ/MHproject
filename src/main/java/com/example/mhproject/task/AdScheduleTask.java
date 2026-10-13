package com.example.mhproject.task;

import com.example.mhproject.entity.Advertisement;
import com.example.mhproject.repository.AdvertisementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 广告排期定时任务：每隔一段时间检查一次，
 * 到了排期开始时间自动上线，到了排期结束时间自动下线。
 */
@Component
public class AdScheduleTask {

    @Autowired
    private AdvertisementRepository adRepo;

    @Scheduled(fixedDelay = 60000)
    public void applySchedule() {
        LocalDateTime now = LocalDateTime.now();
        List<Advertisement> ads = adRepo.findAll();
        for (Advertisement ad : ads) {
            // 禁用状态下，若已到排期开始时间则自动启用
            if (ad.getStartTime() != null && ad.getStatus() == 0 && !ad.getStartTime().isAfter(now)) {
                ad.setStatus(1);
                adRepo.save(ad);
            }
            // 启用状态下，若已到排期结束时间则自动禁用
            if (ad.getEndTime() != null && ad.getStatus() == 1 && !ad.getEndTime().isAfter(now)) {
                ad.setStatus(0);
                adRepo.save(ad);
            }
        }
    }
}
