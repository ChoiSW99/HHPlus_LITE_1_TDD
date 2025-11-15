package io.hhplus.tdd.point;

import io.hhplus.tdd.point.exception.InsufficientPointException;
import io.hhplus.tdd.point.exception.NegativePointException;
import io.hhplus.tdd.point.exception.ZeroPointException;

public record UserPoint(
        long id,
        long point,
        long updateMillis
)
{
    public static UserPoint empty(long id) {
        return new UserPoint(id, 0, System.currentTimeMillis());
    }

    public UserPoint charge(long amount) {
        validateAmount(amount);

        long newAmount = this.point + amount;
        return new UserPoint(this.id, newAmount, System.currentTimeMillis());
    }

    public UserPoint use(long amount) {
        validateAmount(amount);

        if (this.point - amount < 0)
            throw new InsufficientPointException("사용 후 금액이 0보다 크거나 같아야 합니다.");

        long newAmount = this.point - amount;
        return new UserPoint(this.id, newAmount, System.currentTimeMillis());
    }

    private static void validateAmount(long amount) {
        if (amount < 0) throw new NegativePointException("금액은 음수일 수 없습니다.");
        if (amount == 0) throw new ZeroPointException("금액은 0일 수 없습니다.");
    }
}
