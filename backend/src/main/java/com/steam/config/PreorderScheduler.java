package com.steam.config;

import com.steam.service.PreorderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PreorderScheduler {

    private final PreorderService preorderService;

    @Scheduled(cron = "0 0 * * * ?")
    public void convertDuePreorders() {
        try {
            int count = preorderService.processDuePreorders();
            if (count > 0) {
                log.info("[定时任务] 预购转正完成，共处理 {} 条记录", count);
            }
        } catch (Exception e) {
            log.error("[定时任务] 预购转正执行失败", e);
        }
    }
}
