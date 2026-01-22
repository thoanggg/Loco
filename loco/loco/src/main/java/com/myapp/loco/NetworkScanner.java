package com.myapp.loco;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;

public class NetworkScanner {

    /**
     * Quét TẤT CẢ các dải mạng nội bộ tìm thấy trên máy tính.
     * 
     * @return Danh sách các IP của Agent tìm thấy.
     */
    public List<String> scanAllNetworks() {
        Set<String> subnets = getAllLocalSubnets();
        List<String> activeIps = Collections.synchronizedList(new ArrayList<>());

        // Tăng số luồng lên 100 để quét nhanh hơn (vì quét nhiều subnet)
        ExecutorService executor = Executors.newFixedThreadPool(100);
        List<Future<?>> futures = new ArrayList<>();

        System.out.println("Starting scan on subnets: " + subnets);

        for (String subnet : subnets) {
            // Quét từ .1 đến .254 cho mỗi subnet
            for (int i = 1; i < 255; i++) {
                String ip = subnet + "." + i;
                futures.add(executor.submit(() -> {
                    if (checkPort(ip, 9876)) {
                        activeIps.add(ip);
                    }
                }));
            }
        }

        // Chờ tất cả hoàn thành
        executor.shutdown();
        try {
            // Đợi tối đa 5 giây cho toàn bộ quá trình
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }

        return activeIps;
    }

    /**
     * Tìm tất cả các Subnet từ tất cả Card mạng (Interface).
     * Ví dụ: [192.168.1, 192.168.30, 10.0.0]
     */
    private Set<String> getAllLocalSubnets() {
        Set<String> subnets = new HashSet<>();
        try {
            Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface netint : Collections.list(nets)) {
                if (netint.isLoopback() || !netint.isUp())
                    continue;

                Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
                for (InetAddress inetAddress : Collections.list(inetAddresses)) {
                    // Chỉ lấy địa chỉ IPv4 nội bộ (Site Local)
                    if (inetAddress.isSiteLocalAddress() && !inetAddress.isLoopbackAddress()
                            && !inetAddress.getHostAddress().contains(":")) {
                        String ip = inetAddress.getHostAddress();
                        int lastDot = ip.lastIndexOf('.');
                        if (lastDot > 0) {
                            subnets.add(ip.substring(0, lastDot));
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Fallback: Nếu không tìm thấy gì, thử thêm dải phổ biến
        if (subnets.isEmpty()) {
            subnets.add("192.168.1");
            subnets.add("192.168.30");
            subnets.add("10.0.0"); // Common in enterprise/cloud
        }
        return subnets;
    }

    /**
     * Kiểm tra kết nối đến cổng (Timeout 300ms)
     */
    private boolean checkPort(String ip, int port) {
        try (Socket socket = new Socket()) {
            // Tăng timeout lên 300ms để ổn định hơn qua WiFi
            socket.connect(new InetSocketAddress(ip, port), 300);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}