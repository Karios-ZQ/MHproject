package com.example.mhproject.controller;

import com.example.mhproject.entity.Advertisement;
import com.example.mhproject.repository.AdvertisementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 广告公开统计接口：供前端首页在广告曝光/点击时上报次数。
 */
@RestController
@RequestMapping("/api/ads")
@CrossOrigin
public class AdController {

    @Autowired
    private AdvertisementRepository adRepo;

    @PostMapping("/view")
    public String recordView(@RequestParam Long id) {
        adRepo.findById(id).ifPresent(ad -> {
            ad.setViewCount((ad.getViewCount() == null ? 0 : ad.getViewCount()) + 1);
            adRepo.save(ad);
        });
        return "ok";
    }

    @PostMapping("/click")
    public String recordClick(@RequestParam Long id) {
        adRepo.findById(id).ifPresent(ad -> {
            ad.setClickCount((ad.getClickCount() == null ? 0 : ad.getClickCount()) + 1);
            adRepo.save(ad);
        });
        return "ok";
    }
}
