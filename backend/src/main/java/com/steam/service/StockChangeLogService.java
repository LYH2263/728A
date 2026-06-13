package com.steam.service;

import com.steam.dto.PageResult;
import com.steam.entity.StockChangeLog;
import com.steam.mapper.StockChangeLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockChangeLogService {
    
    private final StockChangeLogMapper stockChangeLogMapper;
    
    public void createLog(Long gameId, Long adminId, String adminUsername,
                          Integer stockBefore, Integer stockAfter,
                          String changeType, String remark) {
        StockChangeLog changeLog = new StockChangeLog();
        changeLog.setGameId(gameId);
        changeLog.setAdminId(adminId);
        changeLog.setAdminUsername(adminUsername);
        changeLog.setStockBefore(stockBefore);
        changeLog.setStockAfter(stockAfter);
        changeLog.setChangeType(changeType);
        changeLog.setRemark(remark);
        stockChangeLogMapper.insert(changeLog);
    }
    
    public PageResult<StockChangeLog> getGameStockLogs(Long gameId, Integer page, Integer size) {
        int offset = (page - 1) * size;
        List<StockChangeLog> list = stockChangeLogMapper.findByGameId(gameId, offset, size);
        Long total = stockChangeLogMapper.countByGameId(gameId);
        return PageResult.of(list, total, page, size);
    }
}
