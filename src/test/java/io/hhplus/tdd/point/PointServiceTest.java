package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("포인트 서비스")
class PointServiceTest {

    //@Mock
    private UserPointTable userPointTable;

    //@Mock
    private PointHistoryTable pointHistoryTable;

    //InjectMocks // 위 Mock들이 자동 주입됨
    private PointService pointService;

    @BeforeEach
    public void setUp()
    {
        userPointTable = mock(UserPointTable.class);
        pointHistoryTable = mock(PointHistoryTable.class);
        pointService = new PointService(userPointTable, pointHistoryTable);
    }


    @Nested
    @DisplayName("사용자 포인트")
    class UserPointTest
    {
        private static final long validUserId = 1L;
        // private static final long invalidUserId = -1L;

        private static final long validInitialAmount = 1200L;
        private static final UserPoint validUserPoint = new UserPoint(validUserId, validInitialAmount, System.currentTimeMillis());

        @Nested
        @DisplayName("사용자 포인트 조회")
        class GetUserPointTest
        {
            @Test
            @DisplayName("유효 사용자 id로 포인트를 조회하면, 올바른 사용자 포인트를 반환")
            void givenValidUserId_whenGetUserPoint_thenReturnCorrectUserPoint() {

                // Given
                // userPointTable에 대해, userId로 조회하면, existingUserPoint를 반환하도록 설정.
                when(userPointTable.selectById(validUserId)).thenReturn(validUserPoint);

                // When
                // (pointService.getUserPointById의 내부 구현에 대해, Given을 적용하여 테스트하는 과정)
                UserPoint result = pointService.getUserPointById(validUserId);

                // Then
                assertThat(result).isEqualTo(validUserPoint);
                // userPointTable.selectById메소드가 1회만 호출됐는지 검증
                verify(userPointTable, times(1)).selectById(validUserId);
            }
        }

        @Nested
        @DisplayName("사용자 포인트 충전")
        class ChargeUserPointTest
        {
            @Test
            @DisplayName("유효 사용자 id와 유효 금액을 충전하면, 올바르게 충전된 사용자 포인트 반환")
            void givenValidUserIdAndAmount_whenChargePoint_thenReturnChargedUserPoint() {
                /// Given
                long chargeAmount = 500L;
                long chargedAmount = validInitialAmount + chargeAmount;
                long dbUpdateTime = 12345667890L;
                UserPoint chargedUserPoint = new UserPoint(validUserId, chargedAmount, dbUpdateTime);

                when(userPointTable.selectById(validUserId)).thenReturn(validUserPoint);
                // userPointTable에 대해 (userId, initialPoint)로 조회하면, chargedUserPoint 반환하도록 설정.
                when(userPointTable.insertOrUpdate(validUserId, chargedAmount)).thenReturn(chargedUserPoint);
                when(pointHistoryTable.insert(validUserId, chargeAmount, TransactionType.CHARGE, dbUpdateTime))
                        .thenReturn(new PointHistory(1L, validUserId, chargeAmount, TransactionType.CHARGE, dbUpdateTime));

                /// When
                // (pointService.ChargePoint 내부 구현에 대해, Given을 적용하여 테스트하는 과정)
                UserPoint result = pointService.ChargePoint(validUserId, chargeAmount);

                /// Then
                assertThat(result).isEqualTo(chargedUserPoint);
                assertThat(result.point()).isEqualTo(chargedUserPoint.point());
                verify(userPointTable, times(1)).selectById(validUserId);
                verify(userPointTable, times(1)).insertOrUpdate(validUserId, chargedAmount);
                verify(pointHistoryTable, times(1)).insert(validUserId, chargeAmount, TransactionType.CHARGE, dbUpdateTime);
            }
        }


        @Nested
        @DisplayName("사용자 포인트 사용")
        class UseUserPointTest
        {
            @Test
            @DisplayName("유효 사용자 id와 금액을 사용하면, 올바르게 사용된 사용자 포인트 반환")
            void givenValidUserIdAndAmount_whenUsePoint_thenReturnCorrectUsedUserPoint() {

                /// Given
                long usePoint = 500L;
                long usedPoint = validInitialAmount - usePoint;
                UserPoint usedUserPoint = new UserPoint(validUserId, usedPoint, System.currentTimeMillis());
                when(userPointTable.selectById(validUserId)).thenReturn(validUserPoint);
                when(userPointTable.insertOrUpdate(validUserId, usedPoint)).thenReturn(usedUserPoint);

                /// When
                UserPoint result = pointService.UsePoint(validUserId, usePoint);

                ///  Then
                assertThat(result).isEqualTo(usedUserPoint);
                verify(userPointTable, times(1)).selectById(validUserId);
                verify(userPointTable, times(1)).insertOrUpdate(validUserId, usedPoint);
                verify(pointHistoryTable, times(1)).insert(validUserId, usePoint, TransactionType.USE,
                        usedUserPoint.updateMillis());
            }
        }
    }



    @Nested
    @DisplayName("포인트 히스토리")
    class PointHistoryTest
    {
        private static final long validUserId = 1L;
        private static final long invalidUserId = -1L;

        @Nested
        @DisplayName("사용자 포인트 사용 내역 조회")
        class GetPointHistories
        {
            @Test
            @DisplayName("유효 사용자 id로 사용 내역 조회")
            void givenValidUserHistories_whenGetPointHistoriesById_thenReturnCorrectPointHistories() {

                ///  given
                List<PointHistory> histories = List.of(
                        new PointHistory(1L, validUserId, 1000L, TransactionType.CHARGE, System.currentTimeMillis()),
                        new PointHistory(2L, validUserId, 500L, TransactionType.USE, System.currentTimeMillis())
                );
                when(pointHistoryTable.selectAllByUserId(validUserId)).thenReturn(histories);

                ///  when
                List<PointHistory> result = pointService.getPointHistoriesById(validUserId);

                ///  then
                assertThat(result).hasSize(2);
                assertThat(result).isEqualTo(histories);
                assertThat(result).containsExactlyElementsOf(histories); // 순서 보장
                verify(pointHistoryTable, times(1)).selectAllByUserId(validUserId);
            }
        }
    }
}