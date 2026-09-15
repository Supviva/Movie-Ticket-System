package com.movieticket.demo;

import com.movieticket.model.SeatPlan;
import com.movieticket.model.ShowSession;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 并发购票：同一个座位只能被卖出一次。 */
class ConcurrentBookingTest {

    /** 6 个线程抢同一个座位：只能有 1 个成功，其余都该拿到「座位已售出」。 */
    @Test
    void onlyOneBuyerWinsTheSameSeat() {
        DemoMovieTicketService service = new DemoMovieTicketService();
        ShowSession session = service.sessionsForMovie(service.movies().get(0).id()).get(0);

        int buyerCount = 6;
        List<String> buyers = registerBuyers(service, buyerCount, "buyer", "1390000000");
        String seat = freeSeats(service.seatPlan(session.id()), 1).get(0);

        ConcurrentBooking.Result result =
                ConcurrentBooking.run(service, buyers, session.id(), Set.of(seat), 5000);

        assertEquals(buyerCount, result.threads());
        assertEquals(1, result.success(), "同一座位只允许一个线程购票成功");
        assertEquals(buyerCount - 1, result.failed(), "其余线程都应拿到「座位已售出」");
        assertTrue(result.singleWinner());
        assertTrue(service.seatPlan(session.id()).soldSeats().contains(seat),
                "抢到的座位应变为已售");
    }

    /** 各买各的座位：互不冲突时应当全部成功。 */
    @Test
    void differentSeatsLetEveryoneSucceed() {
        DemoMovieTicketService service = new DemoMovieTicketService();
        ShowSession session = service.sessionsForMovie(service.movies().get(0).id()).get(0);

        int buyerCount = 3;
        List<String> buyers = registerBuyers(service, buyerCount, "vip", "1380000000");
        List<String> free = freeSeats(service.seatPlan(session.id()), buyerCount);

        Map<String, Set<String>> requests = new LinkedHashMap<>();
        for (int i = 0; i < buyerCount; i++) {
            requests.put(buyers.get(i), Set.of(free.get(i)));
        }
        ConcurrentBooking.Result result =
                ConcurrentBooking.runEach(service, requests, session.id(), 5000);

        assertEquals(buyerCount, result.success(), "座位不冲突时所有线程都应成功");
        assertEquals(0, result.failed());
    }

    private static List<String> registerBuyers(DemoMovieTicketService service, int count,
                                               String prefix, String phonePrefix) {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ids.add(service.register(prefix + i, "并发买家" + i,
                    String.format("%s%d", phonePrefix, i), "123456").id());
        }
        return ids;
    }

    /** 按行列顺序取出前 {@code count} 个未被占用的座位。 */
    private static List<String> freeSeats(SeatPlan plan, int count) {
        List<String> result = new ArrayList<>();
        for (int row = 0; row < plan.hall().rows() && result.size() < count; row++) {
            for (int col = 1; col <= plan.hall().cols() && result.size() < count; col++) {
                String seat = (char) ('A' + row) + String.valueOf(col);
                if (!plan.soldSeats().contains(seat)) {
                    result.add(seat);
                }
            }
        }
        if (result.size() < count) {
            throw new AssertionError("场次空闲座位不足 " + count + " 个");
        }
        return result;
    }
}
