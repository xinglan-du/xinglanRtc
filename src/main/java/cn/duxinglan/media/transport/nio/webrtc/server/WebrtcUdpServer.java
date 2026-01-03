package cn.duxinglan.media.transport.nio.webrtc.server;

import cn.duxinglan.media.core.INetworkPacket;
import cn.duxinglan.media.transport.nio.webrtc.RtpThreadProcessor;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;

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
public class WebrtcUdpServer {

    private Selector selector;

    private DatagramChannel channel;

    private volatile boolean running;

    private RtpThreadProcessor rtpThreadProcessor;

    public WebrtcUdpServer() {
        this.rtpThreadProcessor = new RtpThreadProcessor(this);
    }

    public void start(int port) throws IOException {
        running = true;
        channel = DatagramChannel.open();
        channel.configureBlocking(false);
        channel.bind(new InetSocketAddress(port));
        selector = Selector.open();
        channel.register(selector, SelectionKey.OP_READ);
        log.debug("udp服务器启动，绑定端口：{}", port);
        while (running) {
            selector.select();
            Iterator<SelectionKey> it = selector.selectedKeys().iterator();
            while (it.hasNext()) {
                SelectionKey key = it.next();
//                log.debug("接收到数据");
                it.remove();
                if (key.isReadable()) {
//                    log.debug("读取数据");
                    handleRead((DatagramChannel) key.channel());
                }
            }
        }
    }

    public void sendMessage(DatagramChannel channel,
                            InetSocketAddress remoteAddress,
                            INetworkPacket networkPacket) {
        int totalLength = networkPacket.getTotalLength();
        ByteBuf buf = Unpooled.buffer(totalLength);
        networkPacket.writeTo(buf);
        ByteBuffer byteBuffer = buf.nioBuffer();
        try {
           /* if (networkPacket instanceof SRtpPacket){
                log.debug("发送数据到:{},数据为rtp数据，长度为：{}", remoteAddress, totalLength);

            }else {
                log.debug("发送数据到:{},数据为:{}", remoteAddress, ByteBufUtil.hexDump(buf));
            }*/
            channel.send(byteBuffer, remoteAddress);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    // 停止服务器
    public void stop() throws IOException {
        running = false;          // 停止循环
        selector.wakeup();        // 打断 select() 阻塞

        // 关闭资源
        if (channel != null && channel.isOpen()) {
            channel.close();
        }
        if (selector != null && selector.isOpen()) {
            selector.close();
        }

        // 如果有线程池，也在这里关闭
//        workerPool.shutdown();
    }

    private void handleRead(DatagramChannel channel) throws IOException {
        // 分配 Direct ByteBuf
        ByteBuf buf = Unpooled.buffer(1500);
        ByteBuffer nioBuffer = buf.nioBuffer(0, buf.capacity());

        InetSocketAddress remoteAddress = (InetSocketAddress) channel.receive(nioBuffer);

        if (remoteAddress == null) {
            return;
        }

        // 更新 writerIndex
        int position = nioBuffer.position();
        buf.writerIndex(position);
        ByteBuf readableBuf = buf.readRetainedSlice(position);
        rtpThreadProcessor.addRtpByteBuf(channel, remoteAddress, readableBuf);
    }


}
