package cn.duxinglan.media.transport.nio.webrtc;

import cn.duxinglan.ice.message.StunMessage;
import cn.duxinglan.media.core.IMediaNode;
import cn.duxinglan.media.core.IMediaTransport;
import cn.duxinglan.media.core.INetworkPacket;
import cn.duxinglan.media.impl.webrtc.NodeFlowManager;
import cn.duxinglan.media.protocol.rtcp.PsFbRtcpPacket;
import cn.duxinglan.media.protocol.rtcp.RtcpPacket;
import cn.duxinglan.media.protocol.rtp.RtpFactory;
import cn.duxinglan.media.protocol.rtp.RtpPacket;
import cn.duxinglan.media.protocol.rtp.SenderRtpPacket;
import cn.duxinglan.media.protocol.srtcp.SRtcpFactory;
import cn.duxinglan.media.protocol.srtcp.SRtcpPacket;
import cn.duxinglan.media.protocol.srtp.SRtpPacket;
import cn.duxinglan.media.protocol.srtp.SrtpFactory;
import cn.duxinglan.media.transport.nio.webrtc.handler.dtls.DtlsHandler;
import cn.duxinglan.media.transport.nio.webrtc.handler.ice.IceHandler;
import cn.duxinglan.media.transport.nio.webrtc.handler.ice.LocalIceInfo;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

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
public class RtpPackProcessor implements IMediaTransport, DtlsContext.DtlsShakeHandsCallback, IceHandler.IceHandlerCallback {

    /**
     * 表示一个不可变的 {@link DatagramChannel} 变量，用于处理RTP数据包的网络通信。
     * 该变量主要用于发送和接收基于UDP协议的网络数据。
     */
    private final DatagramChannel channel;

    /**
     * 表示连接的远程地址。
     * 该变量用于标识 RTP 数据包的来源或目的地址。
     */
    private final InetSocketAddress remoteAddress;


    private final IceHandler iceHandel = new IceHandler(this);

    private DtlsHandler dtlsHandler;


    /**
     * 表示是否正在处理数据的标志，用于确保在一个时间点内仅有一个线程执行数据处理操作。
     * 该字段通过原子操作支持多线程环境下的安全更新，以避免并发冲突。
     */
    @Getter
    private AtomicBoolean processing = new AtomicBoolean(false);

    /**
     * 表示用于处理实时传输协议 (RTP) 数据包的线程处理器。
     * 该变量负责协调对 RTP 数据包的分发与处理。
     */
    private RtpThreadProcessor rtpThreadProcessor;

    /**
     * 一个线程安全的阻塞队列，用于在数据处理流程中存放接收到的二进制数据缓冲区（ByteBuf）。
     * <p>
     * 该队列的主要用途是缓存待处理的数据包，以避免在数据到达时直接阻塞处理线程。
     * 在多线程环境下，生产者线程将数据放入队列，而消费者线程从队列中获取数据进行处理。
     */
    private final BlockingQueue<ByteBuf> acceptTheQueue = new LinkedBlockingQueue<>();

    private NodeFlowManager nodeFlowManager;

    private SrtpContextFactory srtpContextFactory;

    private IMediaNode mediaNode;

    @Getter
    private long lastProcessTime = System.currentTimeMillis();


    public RtpPackProcessor(DatagramChannel channel, InetSocketAddress remoteAddress, RtpThreadProcessor rtpThreadProcessor) {
        this.channel = channel;
        this.remoteAddress = remoteAddress;
        this.rtpThreadProcessor = rtpThreadProcessor;
    }

    /**
     * 将提供的字节缓冲区加入到接收队列，如果加入成功，增加引用计数。
     *
     * @param buf 表示包含数据包的字节缓冲区。如果缓冲区为 null，则不会执行任何操作。
     */
    public void addPacket(ByteBuf buf) {
        if (buf == null) return;
        lastProcessTime = System.currentTimeMillis();
        acceptTheQueue.add(buf);

    }

    /**
     * 处理接收队列中的所有数据包并对其进行适当处理。
     * 此方法会连续从接收队列中获取字节缓冲区进行处理，直到队列为空。
     * 每个数据包处理完成后，会释放其关联的资源。
     * <p>
     * 在处理数据包时：
     * 1. 如果发生异常，会记录错误日志，并继续处理下一个数据包。
     * 2. 始终确保缓冲区在处理完成后被释放。
     */
    public void processAll() {
        ByteBuf buf;
        while ((buf = acceptTheQueue.poll()) != null) {
            try {
                processData(buf);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    private void processData(ByteBuf buf) {
        log.debug("接受到数据ip：{},端口号：{}", remoteAddress.getHostString(), remoteAddress.getPort());
        //先判断当前数据是谁需要处理
        byte b = buf.getByte(buf.readerIndex());
        if ((b & 0xC0) == 0x80 && mediaNode != null) {
            processingEncryptionRtp(buf);
            return;
        } else if (b >= 20 && b <= 63 && mediaNode != null) {
            processDtls(buf);
            return;
        } else if ((b & 0xC0) == 0) {
            iceHandel.processIce(buf, remoteAddress);
            return;
        } else {
            log.warn("接收到到未知消息：{}", b);
            return;
        }
    }


    public void scheduleConsumerSecond(long tickNs) {
        Runnable task = () -> {
            if (nodeFlowManager != null && nodeFlowManager.isRunning()) {
                nodeFlowManager.sendReadyRtcpPackets(tickNs);
            }
        };

        rtpThreadProcessor.getWorkerPool().submit(task);
    }

    public void scheduleConsumerSending(long tickNs) {
        Runnable task = () -> {
            if (nodeFlowManager != null && nodeFlowManager.isRunning()) {
                nodeFlowManager.sendReadyPackets(tickNs);
            }
        };
        rtpThreadProcessor.getWorkerPool().submit(task);
    }


    /**
     * 解析并验证一个安全的RTCP数据包。
     * 根据提供的字节缓冲区，解析出包含的SRTP/SRTCP数据包，验证其合法性，
     * 然后解密并解析出其中的有效RTCP包列表，交由节点流管理器处理。
     *
     * @param buf 表示接收到的RTCP数据包的二进制数据缓冲区。
     *            此缓冲区应包含完整的RTCP数据包内容，包括可能的保护标签（Auth Tag）。
     */
    private void parseSecureRtcpPacket(ByteBuf buf) {
        int rtcpAuthTagLength = srtpContextFactory.getSrtpProfilesType().rtcpAuthTagLength;
        SRtcpPacket srtcpPacket = SRtcpFactory.parseBytebufToSrtcpPacket(buf, rtcpAuthTagLength);
        long ssrc = srtcpPacket.getEncryptSsrc();
        try {
            SRtcpContext clientSRtcpContext = srtpContextFactory.getClientSRtcpContext(ssrc);
            if (srtcpPacket.contrastAuthTag(clientSRtcpContext)) {
                ByteBuf decrypt = srtcpPacket.decrypt(clientSRtcpContext);
                List<RtcpPacket> rtcpPackets = SRtcpFactory.packetsSRtcpToRtcp(decrypt);
                nodeFlowManager.onRtcpPacket(rtcpPackets, remoteAddress);
            }

        } catch (InvalidAlgorithmParameterException | ShortBufferException | IllegalBlockSizeException |
                 BadPaddingException | InvalidKeyException e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 解析并验证一个安全的RTP数据包。
     * 该方法会根据提供的缓冲区数据进行SRTP验证，解析出有效的RTP包并交给关联的媒体生产者处理。
     *
     * @param buf 表示接收到的RTP数据包的二进制数据缓冲区。
     *            此缓冲区应包含完整的RTP数据包内容，包括可能的保护标签（Auth Tag）。
     */
    private void parseSecureRtpPacket(ByteBuf buf) {
        int rtpAuthTagLength = srtpContextFactory.getSrtpProfilesType().rtpAuthTagLength;
        SRtpPacket srtpPacket = SrtpFactory.parseBytebufToSrtpPacket(buf, rtpAuthTagLength);
        try {
            SRtpContext srtpContext = srtpContextFactory.getClientSrtpContext(srtpPacket.getEncryptSsrc());


            if (srtpPacket.contrastAuthTag(srtpContext)) {
                RtpPacket rtpPacket = RtpFactory.parseBytebufToRtpPacket(srtpPacket.decrypt(srtpContext));
                nodeFlowManager.onRtpPacket(rtpPacket);
            }

        } catch (InvalidKeyException |
                 InvalidAlgorithmParameterException | ShortBufferException | IllegalBlockSizeException |
                 BadPaddingException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void processingEncryptionRtp(ByteBuf buf) {
        int pt = buf.getByte(1) & 0xFF;
        if (SrtpUtils.isSrtp(pt)) {
            parseSecureRtpPacket(buf);
        } else {
            parseSecureRtcpPacket(buf);
        }


    }


    private void processDtls(ByteBuf buf) {
        if (dtlsHandler != null) {
            try {
                dtlsHandler.processDtls(buf);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }

    }


    public void stopProcessing() {
        processing.set(false);
    }

    public boolean isQueueEmpty() {
        return acceptTheQueue.isEmpty();
    }

    public void sendRtpMessage(INetworkPacket networkPacket) {
        rtpThreadProcessor.sendMessage(channel, remoteAddress, networkPacket);
    }


    @Override
    public void writePackage(INetworkPacket networkPacket) {
        sendRtpMessage(networkPacket);
    }

    @Override
    public void sendRtpPacket(SenderRtpPacket senderRtpPacket) {
        try {
            int rtpAuthTagLength = srtpContextFactory.getSrtpProfilesType().rtpAuthTagLength;
            SRtpContext serverSrtpContext = srtpContextFactory.getServerSrtpContext(senderRtpPacket.ssrc());

            ByteBuf rtpBytebuf = RtpFactory.parseRtpPacketToBytebuf(senderRtpPacket.ssrc(), senderRtpPacket.rtpPacket());
            SRtpPacket srtpPacket = SrtpFactory.parseDecryptBytebufToSrtpPacket(rtpBytebuf, serverSrtpContext, rtpAuthTagLength);
//            SrtpPacket srtpPacket = SrtpFactory.parseRtpToSrtpPacket(serverSrtpContext, rtpPackage, rtpAuthTagLength);
            writePackage(srtpPacket);
        } catch (InvalidAlgorithmParameterException | ShortBufferException | IllegalBlockSizeException |
                 BadPaddingException | InvalidKeyException e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public void sendRtcpPackets(List<RtcpPacket> rtcpPackets) {
        try {
            int rtcpAuthTagLength = srtpContextFactory.getSrtpProfilesType().rtcpAuthTagLength;
            SRtcpPacket srtcpPacket = SRtcpFactory.parseRtcpToSRtcp(rtcpPackets, rtcpAuthTagLength);
            SRtcpContext serverSRtcpContext = srtpContextFactory.getServerSRtcpContext(srtcpPacket.getDecryptSsrc());
            ByteBuf encrypt = srtcpPacket.encrypt(serverSRtcpContext);

            writePackage(srtcpPacket);
            serverSRtcpContext.addSentIndex();
        } catch (InvalidAlgorithmParameterException | ShortBufferException | IllegalBlockSizeException |
                 BadPaddingException | InvalidKeyException e) {
            log.error(e.getMessage(), e);
        }
    }


    @Override
    public void notifyHandshakeComplete(SrtpProfilesType srtpProfilesType, byte[] keyingMaterial) {
        try {
            this.srtpContextFactory = new SrtpContextFactory(srtpProfilesType, keyingMaterial);
            log.debug("创建srtpContextFactory成功");
        } catch (NoSuchPaddingException | NoSuchAlgorithmException e) {
            log.error(e.getMessage(), e);
        }

    }

    @Override
    public void onConnected(DtlsContext.DtlsState state) {
        if (state == DtlsContext.DtlsState.CONNECTED) {
            nodeFlowManager = mediaNode.getNodeFlowManager();
            nodeFlowManager.onMediaTransport(this);
            log.info("媒体通道建立成功");
        }
    }


    @Override
    public void callback(StunMessage message, LocalIceInfo localIceInfo, int priority) {
        sendRtpMessage(message);
        if (mediaNode == null) {
            mediaNode = localIceInfo.getMediaNode();
            dtlsHandler = new DtlsHandler(this, mediaNode.getDTLSKeyMaterial(), this);
        }
    }

    public void closeConnection() {
        log.info("当前连接被关闭");

    }
}
