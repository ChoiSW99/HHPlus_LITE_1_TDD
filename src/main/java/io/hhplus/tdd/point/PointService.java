package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
//@RequiredArgsConstructor
public class PointService {
    private final UserPointTable userPointTable;        // 사용자들의 포인트
    private final PointHistoryTable pointHistoryTable;  // 사용자들의 포인트 거래 내역

    public PointService(UserPointTable userPointTable, PointHistoryTable pointHistoryTable) {
        this.userPointTable = userPointTable;
        this.pointHistoryTable = pointHistoryTable;
    }

    // 사용자 포인트 조회
    public UserPoint getUserPointById(long id)
    {
        return userPointTable.selectById(id);
    }

    // 포인트 충전
    public UserPoint ChargePoint(long id, long chargeAmount)
    {
        UserPoint userPoint = userPointTable.selectById(id); // 기존 유저 포인트 조회
        long chargedAmount = userPoint.point() + chargeAmount; // 업데이트한 총합

        UserPoint chargedUserPoint = userPointTable.insertOrUpdate(id, chargedAmount);

        pointHistoryTable.insert(id, chargeAmount, TransactionType.CHARGE,
                chargedUserPoint.updateMillis());

        return chargedUserPoint;
    }

    // 포인트 사용
    public UserPoint UsePoint(long id, long useAmount)
    {
        UserPoint userPoint = userPointTable.selectById(id);
        long usedAmount = userPoint.point() - useAmount;

        UserPoint usedUserPoint = userPointTable.insertOrUpdate(id, usedAmount);

        pointHistoryTable.insert(id, useAmount, TransactionType.USE,
                usedUserPoint.updateMillis());

        return usedUserPoint;
    }

    // 특정 사용자의 포인트 사용 내역 조회
    public List<PointHistory> getPointHistoriesById(long id)
    {
        return pointHistoryTable.selectAllByUserId(id);
    }

}


