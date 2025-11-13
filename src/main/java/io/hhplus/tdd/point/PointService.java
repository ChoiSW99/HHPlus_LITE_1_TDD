package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.exception.InsufficientPointException;
import io.hhplus.tdd.point.exception.NegativePointException;
import io.hhplus.tdd.point.exception.ZeroPointException;
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
        if (chargeAmount < 0) throw new NegativePointException("충전 금액은 음수일 수 없습니다.");
        if (chargeAmount == 0) throw new ZeroPointException("충전 금액은 0일 수 없습니다.");

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
        if (useAmount < 0) throw new NegativePointException("사용 금액은 0보다 커야합니다.");
        if (useAmount == 0) throw new ZeroPointException("사용 금액은 0일 수 없습니다.");

        UserPoint userPoint = userPointTable.selectById(id);
        long usedAmount = userPoint.point() - useAmount;
        if (usedAmount < 0) throw new InsufficientPointException("사용 후 금액이 0보다 크거나 같아야 합니다.");

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


