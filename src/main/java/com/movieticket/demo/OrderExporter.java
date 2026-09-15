package com.movieticket.demo;

import com.movieticket.model.TicketOrder;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单导出与读取：把订单列表序列化成文件，再反序列化读回。
 *
 * <p>演示程序没有接真实数据库，把购票记录写成离线文件是最直接的持久化形式，
 * 也方便把订单交给别人查看。</p>
 */
public final class OrderExporter {

    private OrderExporter() {
    }

    /** 把订单写入文件（对象序列化）。 */
    public static void export(List<TicketOrder> orders, Path file) throws IOException {
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(file))) {
            out.writeObject(new ArrayList<>(orders));
        }
    }

    /** 从文件读回订单（反序列化）。 */
    public static List<TicketOrder> load(Path file) throws IOException, ClassNotFoundException {
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(file))) {
            return readOrders(in);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<TicketOrder> readOrders(ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        Object data = in.readObject();
        if (!(data instanceof List<?> list)) {
            throw new IOException("文件内容不是订单列表");
        }
        return (List<TicketOrder>) list;
    }
}
