package cn.duxinglan.media.transport.nio.webrtc;

import cn.duxinglan.media.core.IMediaTransport;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.tls.DatagramTransport;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

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
public class DatagramTransportImpl implements DatagramTransport {

    private static final int MAX_PACKET_SIZE = 1500;

    /**
     * DTLS 数据缓冲队列（线程安全）
     */
    private final ConcurrentLinkedQueue<byte[]> inboundQueue = new ConcurrentLinkedQueue<>();

    private final IMediaTransport mediaTransport;

    // 用于阻塞/唤醒 receive 的监视器
    private final Object receiveMonitor = new Object();

    /**
     * 是否已关闭
     */
    private volatile boolean closed = false;

    public DatagramTransportImpl(IMediaTransport mediaTransport) {
        this.mediaTransport = mediaTransport;
    }

    /**
     * 提交收到的 DTLS 原始包（由上层 Netty handler 调用）
     */
    public void offerPacket(byte[] data) {
        if (!closed && data != null && data.length > 0) {
            inboundQueue.offer(data);
            synchronized (receiveMonitor) {
                receiveMonitor.notifyAll();
            }
        }
    }

    @Override
    public int getReceiveLimit() {
        return 1500 - 20 - 8;
    }

    @Override
    public int getSendLimit() {
        return 1500 - 84 - 8;
    }

    /**
     * 阻塞读取 DTLS 数据：
     * - 在 waitMillis 内等待队列有数据；若超时则返回 0
     * - 返回单个 UDP 数据报的完整字节
     */
    @Override
    public int receive(byte[] buf, int off, int len, int waitMillis) throws IOException {
        if (closed) return -1;

        byte[] data = inboundQueue.poll();
        if (data == null) {
            if (waitMillis <= 0) {
                return 0;
            }
            long deadline = System.currentTimeMillis() + waitMillis;
            synchronized (receiveMonitor) {
                while (!closed && (data = inboundQueue.poll()) == null) {
                    long remaining = deadline - System.currentTimeMillis();
                    if (remaining <= 0) break;
                    try {
                        receiveMonitor.wait(remaining);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return 0;
                    }
                }
            }
            if (closed){
                log.info("返回错误信息");
                return -1;
            }
            if (data == null) {
                log.info("返回0数据");
                return 0;
            }
        }
        if (len < data.length) {
            throw new IOException("Buffer too small for DTLS packet: " + data.length + " > " + len);
        }
        System.arraycopy(data, 0, buf, off, data.length);
        return data.length;
    }

    /**
     * 通过底层媒体传输发送 DTLS 加密包（单个 UDP 数据报）
     */
    @Override
    public void send(byte[] buf, int off, int len) throws IOException {
        if (closed) return;
        if (mediaTransport == null) {
            throw new IOException("DatagramTransport 未初始化 Netty 上下文或远端地址");
        }
        DtlsOutputPacket dtlsOutputPacket = new DtlsOutputPacket(buf, off, len);
        mediaTransport.writePackage(dtlsOutputPacket);
    }

    @Override
    public void close() throws IOException {
        closed = true;
        inboundQueue.clear();
        synchronized (receiveMonitor) {
            receiveMonitor.notifyAll();
        }
        log.info("DatagramTransport 已关闭");
    }
}
