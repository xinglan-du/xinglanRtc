package cn.duxinglan.media.transport.nio.webrtc;

import cn.duxinglan.media.core.INetworkPacket;
import cn.duxinglan.media.transport.nio.webrtc.server.WebrtcUdpServer;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.Map;
import java.util.concurrent.*;

/**
 *
 * 版权所有 (c) 2025 www.duxinglan.cn
 * <p>
 * 项目名称：xinglanRtc
 * <p>
 * 本文件属于 xinglanRtc 项目的一部分。
 * <p>
 * 本软件依据 XinglanRtc 非商业许可证（XNCL）授权，仅限个人非商业使用。
 * 禁止任何形式的商业用途，包括但不限于：收费安装、收费部署、
 * 收费运维、收费技术支持等行为。
 * <p>
 * 详情请参阅项目根目录下的 LICENSE 文件。
 **/
@Slf4j
public class RtpThreadProcessor {

    private final Map<SocketAddress, RtpPackProcessor> rtpPackProcessorHashMap = new ConcurrentHashMap<>();

    private ScheduledExecutorService scheduler;
    //单位毫秒
    private long TIME_OUT = 15 * 1000;


    @Getter
    private final ExecutorService workerPool = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors()
    );


    private final WebrtcUdpServer webrtcUdpServer;

    public RtpThreadProcessor(WebrtcUdpServer webrtcUdpServer) {
        this.webrtcUdpServer = webrtcUdpServer;
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            for (RtpPackProcessor processor : rtpPackProcessorHashMap.values()) {
                processor.scheduleConsumerSending(System.nanoTime());
            }
        }, 0, 1, TimeUnit.MILLISECONDS);

        scheduler.scheduleAtFixedRate(() -> {
            for (RtpPackProcessor processor : rtpPackProcessorHashMap.values()) {
                processor.scheduleConsumerSecond(System.nanoTime());
            }

        }, 0, 200, TimeUnit.MILLISECONDS);

        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                long currentTimeMillis = System.currentTimeMillis();
                rtpPackProcessorHashMap.entrySet().removeIf(entry -> {
                    long lastProcessTime = entry.getValue().getLastProcessTime();
                    if (currentTimeMillis - lastProcessTime >= TIME_OUT) {
                        log.info("超过15s没有数据执行关闭操作");
                        entry.getValue().closeConnection();
                        return true;
                    }
                    return false;
                });
            }
        }, 1, 5, TimeUnit.MINUTES);
    }


    public void addRtpByteBuf(DatagramChannel channel, InetSocketAddress remoteAddress, ByteBuf byteBuf) {
        RtpPackProcessor rtpPackProcessor = rtpPackProcessorHashMap.computeIfAbsent(remoteAddress, socketAddress -> {
            log.info("创建连接服务：{},端口号：{}", remoteAddress.getHostString(), remoteAddress.getPort());
            return new RtpPackProcessor(channel, remoteAddress, this);
        });
        rtpPackProcessor.addPacket(byteBuf);
        if (!rtpPackProcessor.getProcessing().compareAndSet(false, true)) {
            return; // 已经有线程在处理
        }
        workerPool.submit(() -> {
            try {
                rtpPackProcessor.processAll();
            } finally {
                rtpPackProcessor.stopProcessing();
            }
            // 队列中可能又有新数据加入
            if (!rtpPackProcessor.isQueueEmpty()) {
                addRtpByteBuf(channel, remoteAddress, null); // 触发再次处理
            }

        });

    }


    public void sendMessage(DatagramChannel channel, InetSocketAddress remoteAddress, INetworkPacket networkPacket) {
        webrtcUdpServer.sendMessage(channel, remoteAddress, networkPacket);
    }
}
