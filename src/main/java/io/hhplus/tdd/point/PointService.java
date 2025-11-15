package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
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
    public UserPoint getUserPoint(long id)
    {
        return userPointTable.selectById(id);
    }

    // 포인트 충전
    public UserPoint charge(long id, long chargeAmount)
    {
        // 기존 유저 포인트
        UserPoint userPoint = userPointTable.selectById(id);

        // 도메인에서 계산 (업데이트 값만)
        UserPoint chargedUserPoint = userPoint.charge(chargeAmount);

        // DB에서 실제로 저장되는 UserPoint
        UserPoint saved  = userPointTable.insertOrUpdate(id, chargedUserPoint.point());

        // 히스토리는 DB 저장값의 updateMillis로 기록
        pointHistoryTable.insert(id, chargeAmount, TransactionType.CHARGE,
                saved.updateMillis());

        return saved;
    }

    // 포인트 사용
    public UserPoint use(long id, long useAmount)
    {
        UserPoint userPoint = userPointTable.selectById(id);
        UserPoint updatedUserPoint = userPoint.use(useAmount);

        UserPoint savedUserPoint = userPointTable.insertOrUpdate(id, updatedUserPoint.point());

        pointHistoryTable.insert(id, useAmount, TransactionType.USE,
                savedUserPoint.updateMillis());

        return savedUserPoint;
    }

    // 특정 사용자의 포인트 사용 내역 조회
    public List<PointHistory> getPointHistories(long id)
    {
        return pointHistoryTable.selectAllByUserId(id);
    }

}


