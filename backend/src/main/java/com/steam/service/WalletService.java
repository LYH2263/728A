package com.steam.service;

import com.steam.dto.PageResult;
import com.steam.entity.User;
import com.steam.entity.WalletTransaction;
import com.steam.mapper.UserMapper;
import com.steam.mapper.WalletTransactionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

    private final UserMapper userMapper;
    private final WalletTransactionMapper transactionMapper;

    public static final String TYPE_RECHARGE = "RECHARGE";
    public static final String TYPE_PURCHASE = "PURCHASE";
    public static final String TYPE_REFUND = "REFUND";
    public static final String TYPE_GIFT = "GIFT";

    @Transactional
    public void recharge(Long userId, BigDecimal amount, String description) {
        updateBalance(userId, amount, TYPE_RECHARGE, null, description);
    }

    @Transactional
    public void purchase(Long userId, BigDecimal amount, String orderNo, String description) {
        updateBalance(userId, amount.negate(), TYPE_PURCHASE, orderNo, description);
    }

    @Transactional
    public void refund(Long userId, BigDecimal amount, String orderNo, String description) {
        updateBalance(userId, amount, TYPE_REFUND, orderNo, description);
    }

    @Transactional
    public void gift(Long userId, BigDecimal amount, String description) {
        updateBalance(userId, amount, TYPE_GIFT, null, description);
    }

    @Transactional
    public void updateBalance(Long userId, BigDecimal amount, String type, String orderNo, String description) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        BigDecimal balanceBefore = user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO;
        BigDecimal balanceAfter = balanceBefore.add(amount);

        if (balanceAfter.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("余额不足");
        }

        userMapper.updateBalance(userId, balanceAfter);

        WalletTransaction transaction = new WalletTransaction();
        transaction.setUserId(userId);
        transaction.setType(type);
        transaction.setAmount(amount);
        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setOrderNo(orderNo);
        transaction.setDescription(description);
        transaction.setCreatedAt(LocalDateTime.now());

        transactionMapper.insert(transaction);

        log.info("钱包余额变动: userId={}, type={}, amount={}, before={}, after={}, orderNo={}",
                userId, type, amount, balanceBefore, balanceAfter, orderNo);
    }

    public PageResult<WalletTransaction> getTransactions(Long userId, String type,
                                                         String month, int page, int size) {
        int offset = (page - 1) * size;

        LocalDateTime startDate = null;
        LocalDateTime endDate = null;
        if (month != null && !month.isEmpty()) {
            YearMonth yearMonth = YearMonth.parse(month);
            startDate = yearMonth.atDay(1).atStartOfDay();
            endDate = yearMonth.atEndOfMonth().atTime(23, 59, 59);
        }

        List<WalletTransaction> list = transactionMapper.findByUserIdAndFilters(
                userId, type, startDate, endDate, offset, size);
        long total = transactionMapper.countByUserIdAndFilters(userId, type, startDate, endDate);

        return PageResult.of(list, total, page, size);
    }

    public Map<String, Object> getMonthlySummary(Long userId, String month) {
        LocalDateTime startDate;
        LocalDateTime endDate;
        if (month != null && !month.isEmpty()) {
            YearMonth yearMonth = YearMonth.parse(month);
            startDate = yearMonth.atDay(1).atStartOfDay();
            endDate = yearMonth.atEndOfMonth().atTime(23, 59, 59);
        } else {
            YearMonth currentMonth = YearMonth.now();
            startDate = currentMonth.atDay(1).atStartOfDay();
            endDate = currentMonth.atEndOfMonth().atTime(23, 59, 59);
        }

        List<Map<String, Object>> summaryList = transactionMapper.getMonthlySummary(
                userId, startDate, endDate);

        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;

        for (Map<String, Object> item : summaryList) {
            String type = (String) item.get("type");
            BigDecimal total = (BigDecimal) item.get("total");
            if (TYPE_RECHARGE.equals(type) || TYPE_REFUND.equals(type) || TYPE_GIFT.equals(type)) {
                income = income.add(total);
            } else if (TYPE_PURCHASE.equals(type)) {
                expense = expense.add(total.abs());
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("month", month != null ? month : YearMonth.now().toString());
        result.put("income", income);
        result.put("expense", expense);
        result.put("netIncome", income.subtract(expense));

        return result;
    }

    public List<Map<String, Object>> getTrendData(Long userId) {
        LocalDateTime startDate = LocalDate.now().minusMonths(5).withDayOfMonth(1).atStartOfDay();
        List<Map<String, Object>> rawData = transactionMapper.getTrendData(userId, startDate);

        List<String> months = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            months.add(YearMonth.now().minusMonths(i).toString());
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (String month : months) {
            Map<String, Object> monthData = new HashMap<>();
            monthData.put("month", month);
            monthData.put("income", BigDecimal.ZERO);
            monthData.put("expense", BigDecimal.ZERO);

            for (Map<String, Object> raw : rawData) {
                if (month.equals(raw.get("month"))) {
                    BigDecimal income = (BigDecimal) raw.get("income");
                    BigDecimal expense = (BigDecimal) raw.get("expense");
                    monthData.put("income", income != null ? income : BigDecimal.ZERO);
                    monthData.put("expense", expense != null ? expense : BigDecimal.ZERO);
                    break;
                }
            }
            result.add(monthData);
        }

        return result;
    }

    public Map<String, Object> getWalletOverview(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("balance", user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO);
        result.put("monthlySummary", getMonthlySummary(userId, null));
        result.put("trendData", getTrendData(userId));

        return result;
    }
}
