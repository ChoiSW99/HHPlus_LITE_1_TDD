package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PointService {
    private final UserPointTable userPointTable;        // 사용자들의 포인트
    private final PointHistoryTable pointHistoryTable;  // 사용자들의 포인트 거래 내역

    public PointService(PointHistoryTable pointHistoryTable, UserPointTable userPointTable) {
        this.userPointTable = userPointTable;
        this.pointHistoryTable = pointHistoryTable;
    }

    // 사용자 포인트 조회
    public UserPoint getUserPointById(long userId)
    {
        return userPointTable.selectById(userId);
    }

    // 포인트 충전
    public UserPoint ChargePoint(long id, long amount)
    {
        UserPoint userPoint = userPointTable.selectById(id); // 기존 유저 포인트
        long chargedAmount = userPoint.point() + amount; // 업데이트한 총합
        
        // TODO : amount 예외처리

        UserPoint userPoint_AfterCharge = userPointTable.insertOrUpdate(id, chargedAmount);

        pointHistoryTable.insert(id, chargedAmount, TransactionType.CHARGE,
                userPoint_AfterCharge.updateMillis());

        return userPoint_AfterCharge;
    }

    // 포인트 사용
    public UserPoint UsePoint(long id, long amount)
    {
        UserPoint userPoint = userPointTable.selectById(id);
        long usedAmount = userPoint.point() - amount;

        UserPoint userPoint_AfterUse = userPointTable.insertOrUpdate(id, usedAmount);

        pointHistoryTable.insert(id, usedAmount, TransactionType.USE,
                userPoint_AfterUse.updateMillis());

        // TODO : amount 예외처리

        return userPoint_AfterUse;
    }

    // 특정 사용자의 포인트 사용 내역 조회
    public List<PointHistory> getPointHistoriesById(long userId)
    {
        return pointHistoryTable.selectAllByUserId(userId);
    }

}


