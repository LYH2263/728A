package com.steam.controller;

import com.steam.dto.AdminGameQueryDTO;
import com.steam.dto.BatchStatusDTO;
import com.steam.dto.PageResult;
import com.steam.dto.Result;
import com.steam.dto.StockAdjustDTO;
import com.steam.entity.Game;
import com.steam.service.GameService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin/games")
@RequiredArgsConstructor
public class AdminGameController {
    
    private final GameService gameService;
    
    private boolean isAdmin(HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        return "ADMIN".equals(role);
    }
    
    @GetMapping
    public Result<PageResult<Game>> listGames(AdminGameQueryDTO query,
                                               HttpServletRequest request,
                                               HttpServletResponse response) {
        if (!isAdmin(request)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return Result.error(403, "没有权限访问");
        }
        PageResult<Game> result = gameService.adminSearchGames(query);
        return Result.success(result);
    }
    
    @PutMapping("/{id}/stock")
    public Result<Void> updateStock(@PathVariable Long id,
                                     @RequestBody Map<String, Integer> body,
                                     HttpServletRequest request,
                                     HttpServletResponse response) {
        if (!isAdmin(request)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return Result.error(403, "没有权限访问");
        }
        Integer stock = body.get("stock");
        gameService.updateGameStock(id, stock);
        return Result.successMessage("库存更新成功");
    }
    
    @PutMapping("/stock/batch")
    public Result<Void> batchUpdateStock(@RequestBody StockAdjustDTO dto,
                                          HttpServletRequest request,
                                          HttpServletResponse response) {
        if (!isAdmin(request)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return Result.error(403, "没有权限访问");
        }
        int count = gameService.batchUpdateStock(dto.getGameIds(), dto.getStock());
        return Result.successMessage("成功更新 " + count + " 个游戏的库存");
    }
    
    @PutMapping("/status/batch")
    public Result<Void> batchUpdateStatus(@RequestBody BatchStatusDTO dto,
                                           HttpServletRequest request,
                                           HttpServletResponse response) {
        if (!isAdmin(request)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return Result.error(403, "没有权限访问");
        }
        int count = gameService.batchUpdateStatus(dto.getGameIds(), dto.getStatus());
        return Result.successMessage("成功更新 " + count + " 个游戏的状态");
    }
    
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats(HttpServletRequest request,
                                                 HttpServletResponse response) {
        if (!isAdmin(request)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return Result.error(403, "没有权限访问");
        }
        Map<String, Object> stats = new HashMap<>();
        stats.put("lowStockCount", gameService.getLowStockCount());
        stats.put("stockThreshold", gameService.getStockThreshold());
        return Result.success(stats);
    }
}
