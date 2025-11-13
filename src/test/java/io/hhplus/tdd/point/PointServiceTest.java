package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.exception.NegativePointException;
import io.hhplus.tdd.point.exception.OverPointException;
import io.hhplus.tdd.point.exception.ZeroPointException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

            @Nested
            @DisplayName("예외) 잘못된 금액으로 충전")
            class InvalidChargeAmountTest
            {
                @Test
                @DisplayName("음수 금액 충전 시 예외 발생")
                void givenNegativeAmount_whenCharge_thenThrowException()
                {
                    //Given : 음수 금액
                    // 작성 이유: 음수 금액 충전은 불가능해야 함을 확인하기 위함
                    long chargeAmount = -1000L;
                    when(userPointTable.selectById(validUserId)).thenReturn(validUserPoint);
                    // Mock 설정: insertOrUpdate가 호출될 때를 대비 (예외 처리가 없으면 이 부분까지 도달)
                    when(userPointTable.insertOrUpdate(eq(validUserId), anyLong()))
                            .thenReturn(new UserPoint(validUserId, 200L, System.currentTimeMillis()));

                    //When, Then
                    // Red 단계: 예외 처리가 없으므로 예외가 발생하지 않아 테스트가 실패해야 함
                    assertThrows(NegativePointException.class, () -> pointService.ChargePoint(validUserId, chargeAmount));
                }

                @Test
                @DisplayName("0원 충전 시 예외 발생")
                void givenZeroAmount_whenCharge_thenThorwException()
                {
                    //Given : 0원
                    // 작성 이유: 0원 충전은 의미가 없으므로 예외 처리 필요
                    long chargeAmount = 0L;
                    when(userPointTable.selectById(validUserId)).thenReturn(validUserPoint);
                    // Mock 설정: insertOrUpdate가 호출될 때를 대비
                    when(userPointTable.insertOrUpdate(eq(validUserId), anyLong()))
                            .thenReturn(new UserPoint(validUserId, validInitialAmount, System.currentTimeMillis()));

                    //When, Then
                    // Red 단계: 예외 처리가 없으므로 예외가 발생하지 않아 테스트가 실패해야 함
                    assertThrows(ZeroPointException.class, () -> pointService.ChargePoint(validUserId, chargeAmount));
                }
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

            @Nested
            @DisplayName("예외) 잔고 부족")
            class InefficientPointTest
            {
                @Test
                @DisplayName("잔고보다 많은 금액 사용하려고 하면 예외 발생")
                void givenInsufficientPoint_whenUsePoint_thenThrowException()
                {
                    //Given : 잔고보다 많은 사용 금액 설정
                    // 작성 이유: 요구사항에 따라 잔고가 부족할 경우 포인트 사용은 실패해야 함
                    long useAmount = validInitialAmount + 1000L;
                    when(userPointTable.selectById(validUserId)).thenReturn(validUserPoint);
                    // Mock 설정: insertOrUpdate가 호출될 때를 대비 (예외 처리가 없으면 이 부분까지 도달)
                    when(userPointTable.insertOrUpdate(eq(validUserId), anyLong()))
                            .thenReturn(new UserPoint(validUserId, -1000L, System.currentTimeMillis()));

                    //When, Then
                    // Red 단계: 예외 처리가 없으므로 예외가 발생하지 않아 테스트가 실패해야 함
                    assertThrows(OverPointException.class, () -> pointService.UsePoint(validUserId, useAmount));
                }
            }

            @Nested
            @DisplayName("예외) 잘못된 금액 사용")
            class InvalidUseAmountTest
            {
                @Test
                @DisplayName("음수 금액 사용 시, 예외 발생")
                void givenNegativeAmount_whenUsePoint_thenThrowsException()
                {
                    //Given
                    // 작성 이유: 음수 금액 사용은 불가능해야 함
                    long useAmount = -1000L;
                    when(userPointTable.selectById(validUserId)).thenReturn(validUserPoint);
                    // Mock 설정: insertOrUpdate가 호출될 때를 대비
                    when(userPointTable.insertOrUpdate(eq(validUserId), anyLong()))
                            .thenReturn(new UserPoint(validUserId, validInitialAmount + 1000L, System.currentTimeMillis()));

                    //When,Then
                    // Red 단계: 예외 처리가 없으므로 예외가 발생하지 않아 테스트가 실패해야 함
                    assertThrows(NegativePointException.class, ()->pointService.UsePoint(validUserId, useAmount));
                }

                @Test
                @DisplayName("0원 사용 시, 예외 발생")
                void givenZeroAmount_whenUsePoint_thenThrowsException()
                {
                    //Given
                    // 작성 이유: 0원 사용은 의미가 없으므로 예외 처리 필요
                    long useAmount = 0L;
                    when(userPointTable.selectById(validUserId)).thenReturn(validUserPoint);
                    // Mock 설정: insertOrUpdate가 호출될 때를 대비
                    when(userPointTable.insertOrUpdate(eq(validUserId), anyLong()))
                            .thenReturn(new UserPoint(validUserId, validInitialAmount, System.currentTimeMillis()));

                    //When, Then
                    // Red 단계: 예외 처리가 없으므로 예외가 발생하지 않아 테스트가 실패해야 함
                    assertThrows(ZeroPointException.class, () -> pointService.UsePoint(validUserId, useAmount));
                }

            }

        }
    }



    @Nested
    @DisplayName("포인트 히스토리")
    class PointHistoryTest
    {
        private static final long validUserId = 1L;

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