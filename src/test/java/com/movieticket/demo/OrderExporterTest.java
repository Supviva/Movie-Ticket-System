package com.movieticket.demo;

import com.movieticket.model.Account;
import com.movieticket.model.SeatPlan;
import com.movieticket.model.ShowSession;
import com.movieticket.model.TicketOrder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 订单导出：序列化写文件后读回，内容必须一致。 */
class OrderExporterTest {

    @Test
    void exportedOrdersCanBeLoadedBack(@TempDir Path dir) throws Exception {
        DemoMovieTicketService service = new DemoMovieTicketService();
        Account customer = service.login("customer", "123456");
        ShowSession session = service.sessionsForMovie(service.movies().get(0).id()).get(0);

        String seat = firstFreeSeat(service.seatPlan(session.id()));
        TicketOrder order = service.buySeatsNow(customer.id(), session.id(), Set.of(seat));
        service.payOrder(order.id());

        List<TicketOrder> orders = service.ordersForUser(customer.id());
        assertFalse(orders.isEmpty(), "支付后应当能查到订单");

        Path file = dir.resolve("orders.ser");
        OrderExporter.export(orders, file);
        assertTrue(Files.exists(file));
        assertTrue(Files.size(file) > 0, "导出的文件不应为空");

        List<TicketOrder> loaded = OrderExporter.load(file);
        assertEquals(orders.size(), loaded.size(), "读回的订单数量应一致");

        TicketOrder before = orders.get(0);
        TicketOrder after = loaded.get(0);
        assertEquals(before.id(), after.id());
        assertEquals(before.status(), after.status());
        assertEquals(before.originalFen(), after.originalFen());
        assertEquals(before.discountFen(), after.discountFen());
        assertEquals(before.payableFen(), after.payableFen());
        assertEquals(before.membershipLabel(), after.membershipLabel());
        assertEquals(before.ticketCode(), after.ticketCode());
        assertEquals(before.items().size(), after.items().size());
        assertEquals(before.items().get(0).seats(), after.items().get(0).seats(),
                "座位信息应在序列化后保持完整");
        assertEquals(before.items().get(0).movieTitle(), after.items().get(0).movieTitle());
    }

    /** 空订单列表也应能正常往返。 */
    @Test
    void emptyListRoundTrips(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("empty.ser");
        OrderExporter.export(new ArrayList<>(), file);
        assertEquals(0, OrderExporter.load(file).size());
    }

    private static String firstFreeSeat(SeatPlan plan) {
        for (int row = 0; row < plan.hall().rows(); row++) {
            for (int col = 1; col <= plan.hall().cols(); col++) {
                String seat = (char) ('A' + row) + String.valueOf(col);
                if (!plan.soldSeats().contains(seat)) {
                    return seat;
                }
            }
        }
        throw new AssertionError("场次已无空座");
    }
}
