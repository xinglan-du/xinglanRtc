package cn.duxinglan.media.impl.webrtc;

import cn.duxinglan.media.core.IConsumer;
import cn.duxinglan.media.core.IConsumerMediaSubscriber;
import cn.duxinglan.media.core.IMediaControl;
import cn.duxinglan.media.protocol.rtcp.*;
import cn.duxinglan.media.protocol.rtp.CachedRtpPacket;
import cn.duxinglan.media.protocol.rtp.RtpPacket;
import cn.duxinglan.media.protocol.rtp.SenderRtpPacket;
import cn.duxinglan.media.protocol.rtp.TimerRtpPacket;
import cn.duxinglan.sdp.entity.ssrc.SSRC;
import cn.duxinglan.sdp.entity.ssrc.SsrcGroup;
import cn.duxinglan.sdp.entity.ssrc.SsrcGroupType;
import io.netty.util.collection.LongObjectHashMap;
import io.netty.util.collection.LongObjectMap;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

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
@Data
@Slf4j
public class WebrtcMediaConsumer implements IConsumer {

    /**
     * 用于初始化延迟的时间常量，以纳秒为单位。
     * <ul>
     *   - 设置为 50_000_000 纳秒（即 50 毫秒）。
     *   - 通常用于音视频数据传输中，控制初始处理的时间窗口。
     */
    private static final long INITIAL_DELAY_NS = 50_000_000L; //50ms

    /**
     * 指定接收器缓存的时间窗口，单位为纳秒。
     * <p>
     * 该变量定义了一个时间窗口，用于控制数据包在接收器内的缓存时间。
     * 在此时间窗口内的数据被视为有效，以确保数据流的连续性。
     * 如果数据包在缓存时间窗口外，可能会被视为过期或无效。
     * <p>
     * 值为 500_000_000L，即 500 毫秒。
     */
    private static final long CACHE_WINDOW_NS = 500_000_000L; // 500ms


    @Getter
    private MediaLineInfo mediaLineInfo;

    private Long mainSsrc;

    /**
     * 表示WebRTC媒体消费者中与会话相关的CNAME (Canonical Name) 标识符。
     * <p>
     * cname是在RTCP (实时传输控制协议)会话中用于标识特定源的标准化名称，确保在多媒体会话期间关联的源能够被唯一标识。
     * 此字段一旦分配后是不可变的。
     */
    private String cname;

    /**
     * 表示WebRTC媒体消费方的流标识符。
     * 此变量用于唯一标识媒体流，常用于关联和管理特定的音视频流。
     * 通常，该变量在实例化对象时由外部提供，且在整个生命周期内保持不变。
     */
//    private final String streamId;

    /**
     * 表示一个消费者媒体订阅者，用于处理 RTP 媒体数据包与定时 RTP 数据包。
     * <p>
     * 该字段的作用是作为媒体流的订阅者接口，通过实现 {@link IConsumerMediaSubscriber}，
     * 提供对 RTP 数据包的处理逻辑，与 {@code WebrtcMediaConsumer} 类中的媒体流处理逻辑交互。
     * <p>
     * 此字段通常用于设置消费者在接收到 RTP 数据包时的回调处理方法，
     * 具体处理逻辑由订阅者的实现类定义（例如：处理音频、视频流或其它实时媒体）。
     */
    private IConsumerMediaSubscriber consumerMediaSubscriber;

    private final TimeAnchor timeAnchor = new TimeAnchor(System.currentTimeMillis() * 1_000_000L);

    private volatile TimeState timeState = TimeState.WAITING_SOURCE_TIME;

    private final PriorityQueue<CachedRtpPacket> sendQueue = new PriorityQueue<>(Comparator.comparingLong(CachedRtpPacket::getSendTimeNs));

    private final LongObjectMap<CachedRtpPacket> seqMap = new LongObjectHashMap<>();

    private long packetCount;
    private long octetCount;
    private long lastRtpTimestamp;

    private IMediaControl mediaControl;

    public WebrtcMediaConsumer(MediaLineInfo mediaLineInfo) {
        this.mediaLineInfo = mediaLineInfo;
        List<SsrcGroup> ssrcGroups = mediaLineInfo.getReadInfo().getSsrcGroups();
        for (SsrcGroup ssrcGroup : ssrcGroups) {
            if (ssrcGroup.getSsrcGroupType() == SsrcGroupType.FID) {
                this.mainSsrc = ssrcGroup.getSsrcList().getFirst();
                break;
            }
        }
        SSRC ssrc = mediaLineInfo.getReadInfo().getSsrcMap().get(this.mainSsrc);
        this.cname = ssrc.getCname();
    }



    @Override
    public void onRtpPacket(TimerRtpPacket timerRtpPacket) {
        if (timeState == TimeState.WAITING_ANCHOR) {
            timeAnchor.setAnchorSourceNs(timerRtpPacket.sourceTimeNs());
            timeAnchor.setAnchorSendNs(System.nanoTime() + INITIAL_DELAY_NS);
            timeState = TimeState.RUNNING;
        }

        CachedRtpPacket cached;

        if (timeState == TimeState.RUNNING) {
            //计算当前的发送时间
            long sendTimeNs =
                    timeAnchor.getAnchorSendNs()
                    + (timerRtpPacket.sourceTimeNs() - timeAnchor.getAnchorSourceNs());

            //原始数据
            RtpPacket rtpPacket = timerRtpPacket.rtpPacket();

            //缓存的数据
            cached = new CachedRtpPacket(
                    rtpPacket.getSequenceNumber(),
                    sendTimeNs,
                    rtpPacket
            );
        } else {
            RtpPacket rtpPacket = timerRtpPacket.rtpPacket();
            cached = new CachedRtpPacket(rtpPacket.getSequenceNumber(),
                    0,
                    rtpPacket);
        }


        // PriorityQueue 不是线程安全的，必须同步访问
        synchronized (sendQueue) {
            sendQueue.add(cached);
        }
    }

    @Override
    public void onSourceTimeReady() {
        if (timeState == TimeState.WAITING_SOURCE_TIME) {
            timeState = TimeState.WAITING_ANCHOR;
        }
    }


    /**
     * 构建一个用于发送者报告 (Sender Report) 的 RTCP 数据包。
     * 该方法生成符合 RTCP 协议的 Sender Report 数据包，
     * 并填充相关的统计信息，如 NTP 时间戳、RTP 时间戳、传输的包数和字节数等。
     *
     * @param nowNs 当前的系统时间戳（单位为纳秒），用于计算 NTP 时间戳。
     * @return 构建完成的 SenderReportRtcpPacket 对象。
     */
    public SenderReportRtcpPacket buildSenderReportRtcpPacket(long nowNs) {
        SenderReportRtcpPacket senderReportRtcpPacket = new SenderReportRtcpPacket();
        senderReportRtcpPacket.setVersion((byte) 2);
        senderReportRtcpPacket.setPadding((byte) 0);
        senderReportRtcpPacket.setRc(0);
        senderReportRtcpPacket.setPayloadType(RtcpPayloadType.SENDER_REPORT.value); // SR
        senderReportRtcpPacket.setLength((28 / 4) - 1);
        senderReportRtcpPacket.setSsrc(mainSsrc);


        long ntpNs = timeAnchor.anchorNtpNs
                     + (nowNs - timeAnchor.anchorSendNs);

        long ntpSec = ntpNs / 1_000_000_000L;
        long ntpFrac = ((ntpNs % 1_000_000_000L) << 32) / 1_000_000_000L;

        senderReportRtcpPacket.setNtpSec(ntpSec);
        senderReportRtcpPacket.setNtpFrac(ntpFrac);

        senderReportRtcpPacket.setRtpTimestamp(lastRtpTimestamp);

        senderReportRtcpPacket.setSenderPacketCount(packetCount);
        senderReportRtcpPacket.setSenderOctetCount(octetCount);
        senderReportRtcpPacket.setReportBlocks(Collections.emptyList());

        return senderReportRtcpPacket;
    }

    @Override
    public void onRtcpPacket(RtcpPacket packet, InetSocketAddress remoteAddress) {
        if (packet instanceof PsFbRtcpPacket psFbRtcpPacket) {
            if (this.mediaControl != null) {
                this.mediaControl.onPLI();
            }
        }

    }

    @Override
    public void setMediaSubscriber(IConsumerMediaSubscriber subscriber) {
        consumerMediaSubscriber = subscriber;
    }


    @Override
    public void removeMediaSubscriber() {
        this.consumerMediaSubscriber = null;
    }


    @Override
    public void close() {

    }


    public SenderRtpPacket pollReady(long nowNs) {
        synchronized (sendQueue) {
            CachedRtpPacket head = sendQueue.peek();
            if (head != null && head.getSendTimeNs() <= nowNs) {
                sendQueue.poll();
                lastRtpTimestamp = head.getPacket().getTimestamp();
                packetCount++;
                octetCount += head.getPacket().getPayload().readableBytes();
                return new SenderRtpPacket(mainSsrc, head.getPacket());
            }
        }
        return null;
    }

    @Override
    public void setMediaControl(IMediaControl mediaControl) {
        this.mediaControl = mediaControl;
    }

    @Override
    public void removeMediaControl() {
        this.mediaControl = null;
    }


    /**
     * 构建一个用于发送端信息描述 (Source Description) 的 RTCP 数据包。
     * 此方法生成符合 RTCP 协议的 SDES 数据包，用于携带发送端源属性信息，
     * 包括 SSRC 和 CNAME，用以标识发送端并确保 CNAME 的稳定性。
     *
     * @return 构建完成的 SdesRtcpPacket 对象，包含必要的 RTCP 头信息、
     * 一个 SDES Chunk，以及用于标识发送端的 CNAME 信息。
     */
    public SdesRtcpPacket buildSdesRtcpPacket() {
        SdesRtcpPacket sdesRtcpPacket = new SdesRtcpPacket();
        sdesRtcpPacket.setVersion((byte) 2);
        sdesRtcpPacket.setPadding((byte) 0);
        sdesRtcpPacket.setSc((byte) 1);
        sdesRtcpPacket.setPayloadType(RtcpPayloadType.SDES.value);
        //TODO 这里要注意 需要动态计算
        sdesRtcpPacket.setLength((28 / 4) - 1);
        SdesChunk chunk = new SdesChunk(mainSsrc);
        CnameSdesItem cnameItem = new CnameSdesItem(cname);
        chunk.addItem(cnameItem);
        sdesRtcpPacket.addChunk(chunk);
        return sdesRtcpPacket;
    }


    @Data
    public static class TimeAnchor {
        private long anchorSourceNs;
        private long anchorSendNs;
        private long anchorNtpNs;
        private boolean initialized;

        public TimeAnchor(long anchorNtpNs) {
            this.anchorNtpNs = anchorNtpNs;
        }
    }


    public enum TimeState {
        WAITING_SOURCE_TIME,
        WAITING_ANCHOR,
        RUNNING
    }

}
