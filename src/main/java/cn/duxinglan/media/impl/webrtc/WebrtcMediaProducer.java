 /*
  * 版权所有 (c) 2025 www.duxinglan.cn
  *
  * 项目名称：xinglanRtc
  *
  * 本文件属于 xinglanRtc 项目的一部分。
  *
  * 本软件依据 XinglanRtc 非商业许可证（XNCL）授权，仅限个人非商业使用。
  * 禁止任何形式的商业用途，包括但不限于：收费安装、收费部署、
  * 收费运维、收费技术支持等行为。
  *
  * 详情请参阅项目根目录下的 LICENSE 文件。
  */
package cn.duxinglan.media.impl.webrtc;

import cn.duxinglan.media.core.IMediaControl;
import cn.duxinglan.media.core.IProducer;
import cn.duxinglan.media.core.IProducerMediaSubscriber;
import cn.duxinglan.media.impl.webrtc.rtcp.ReceiverReportSnapshot;
import cn.duxinglan.media.impl.webrtc.rtcp.RtpReceiveStats;
import cn.duxinglan.media.impl.webrtc.rtcp.SequenceNumberTracker;
import cn.duxinglan.media.protocol.rtcp.*;
import cn.duxinglan.media.protocol.rtp.RtpPacket;
import cn.duxinglan.media.protocol.rtp.RtpTimeState;
import cn.duxinglan.media.protocol.rtp.TimerRtpPacket;
import cn.duxinglan.sdp.entity.rtp.FmtpAttributes;
import cn.duxinglan.sdp.entity.rtp.RtpPayload;
import cn.duxinglan.sdp.entity.ssrc.SsrcGroup;
import cn.duxinglan.sdp.entity.ssrc.SsrcGroupType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;


@Slf4j
public class WebrtcMediaProducer implements IProducer, IMediaControl {

    private static final int RTCP_RTPFB_NACK_FMT = 1;
    private static final int MAX_NACK_PID_BLOCKS = 32;


    @Getter
    private MediaLineInfo mediaLineInfo;

    /**
     * 表示主同步信源的标识符 (SSRC, Synchronization Source Identifier)。
     * 在WebRTC媒体传输中，SSRC用于标识单独的媒体流。
     * 该变量通常用于关联特定的RTP数据流，从而支持数据包的正确解复用和处理。
     */
    private Long mainSsrc;

    /**
     * 表示一个媒体生产者的订阅者，用于接收并处理媒体流相关的回调事件。
     * 该变量用于存储实现了 {@link IProducerMediaSubscriber} 接口的实例，
     * 负责处理与特定媒体生产者相关联的事件，如 RTP 包解析或源时间信息等。
     * <p>
     * 主要功能包括：
     * - 处理绑定了时间信息的 RTP 数据包。
     * - 在源时间信息就绪时进行回调通知。
     * <p>
     * 这是 WebrtcMediaProducer 类中的一个关键组件，通常用于与媒体流的订阅和交互逻辑。
     */
    private IProducerMediaSubscriber producerMediaSubscriber;

    /**
     * 表示 RTP 时间状态的变量。
     * <p>
     * 该变量是一个不可变的 {@code RtpTimeState} 实例，用于存储和管理 RTP (Real-time Transport Protocol)
     * 的时间同步状态，包括 NTP 时间戳、基准 RTP 时间戳等信息。
     * <p>
     * 使用此变量可以实现以下功能：
     * - 管理 RTP 与 NTP 时间的对齐关系。
     * - 跟踪和更新基准时间，用于多媒体流的时间同步。
     * - 确定是否接收到 RTCP (Real-time Transport Control Protocol) 的 Sender Report。
     * <p>
     * 该变量在类中主要用于 RTP 数据包和 RTCP 消息的处理与时间同步逻辑相关的任务。
     */
    private final RtpTimeState rtpTimeState = new RtpTimeState();

    /**
     * 表示是否请求关键帧的标志位。
     * 該变量用于控制WebRTC媒体生产者是否发送关键帧请求。
     * 当设置为true时，表明需要向媒体源发出关键帧请求。
     * 使用AtomicBoolean保证对该标志位的线程安全操作。
     */
    private final AtomicBoolean requestKeyframes = new AtomicBoolean(false);

    /**
     * 表示接收端的 RTP 数据包统计信息。
     * 该变量用于记录和维护接收端相关的 RTP 数据包状态信息，
     * 包括丢包情况、抖动统计和接收速率等。
     *
     * 该属性为只读（final），初始化后不可更改。
     * receiveStats 的初始值为一个新的 RtpReceiveStats 实例，
     * RtpReceiveStats 类提供详细的统计功能，用于分析 RTP 数据包的传输效率和质量。
     *
     * 注意：该实例对象由类 WebrtcMediaProducer 使用，
     * 用于在接收端进行相关的实时媒体流管理。
     */
    private final RtpReceiveStats receiveStats = new RtpReceiveStats();

    private final SequenceNumberTracker seqTracker = new SequenceNumberTracker();


    public WebrtcMediaProducer(MediaLineInfo mediaLineInfo) {
        this.mediaLineInfo = mediaLineInfo;
        initMainSsrc();
    }

    /**
     * 初始化主要的SSRC（Synchronizing Source）。
     * <p>
     * 该方法从媒体信息中提取主要的SSRC值，并设置到当前实例的mainSsrc字段中。
     * 主要的SSRC被用于标识和管理音视频流的数据源。
     * <p>
     * 实现逻辑如下:
     * 1. 如果readInfo中的SSRC映射数量仅有一个，则直接将其键值作为主要SSRC。
     * 2. 如果SSRC映射包含多个值，则查找是否存在类型为FID的SSRC组。
     * 在找到符合条件的SSRC组后，将其第一个SSRC设置为主要SSRC。
     * <p>
     * 备注:
     * - mainSsrc的初始化是用于确保对SSRC的唯一性和识别性要求。
     * - 如果未能从现有SSRC映射或组中找到符合条件的值，该方法不会设置mainSsrc。
     */
    private void initMainSsrc() {
        MediaLineInfo.Info readInfo = mediaLineInfo.getReadInfo();
        if (readInfo.getSsrcMap().size() == 1) {
            for (Long l : readInfo.getSsrcMap().keySet()) {
                this.mainSsrc = l;
                return;
            }
        }
        List<SsrcGroup> ssrcGroups = readInfo.getSsrcGroups();
        for (SsrcGroup ssrcGroup : ssrcGroups) {
            if (ssrcGroup.getSsrcGroupType() == SsrcGroupType.FID) {
                this.mainSsrc = ssrcGroup.getSsrcList().getFirst();
                break;
            }
        }
    }

    /**
     * 将 RTP 数据包的负载类型和序列号转换为主流格式，以支持处理重传消息。
     * 如果 RTP 负载对应的 FMTP (格式参数) 中存在关联负载类型，则将负载类型替换为关联类型，
     * 同时解析载荷的前 2 字节为新的序列号，并进行更新。
     *
     * @param rtpPacket 输入的 RTP 数据包，包含待转换的负载类型和序列号。
     * @return 转换后的 RTP 数据包，更新后的负载类型与序列号。
     */
    private RtpPacket rtcToMainPacket(RtpPacket rtpPacket) {
        int payloadType = rtpPacket.getPayloadType();
        RtpPayload rtpPayload = mediaLineInfo.getReadInfo().getRtpPayloads().get(payloadType);
        FmtpAttributes fmtp = rtpPayload.getFmtp();
        //这个是重传消息
        if (fmtp != null && fmtp.getAssociatedPayloadType() != null) {
            Integer associatedPayloadType = fmtp.getAssociatedPayloadType();
            rtpPacket.setPayloadType(associatedPayloadType);
            int seq = rtpPacket.getPayload().readUnsignedShort();
            rtpPacket.setSequenceNumber(seq);
            log.info("当前为重传消息：{},序列号为：{}", rtpPayload, rtpPacket.getSequenceNumber());
        }
        return rtpPacket;
    }

    @Override
    public void onRtpPacket(RtpPacket packet) {
        packet = rtcToMainPacket(packet);
        int payloadType = packet.getPayloadType();
        RtpPayload rtpPayload = mediaLineInfo.getReadInfo().getRtpPayloads().get(payloadType);
        long arrivalTimeNs = System.nanoTime();
        receiveStats.onRtpPacket(packet, arrivalTimeNs, rtpPayload.getClockRate());

        seqTracker.receive(packet.getSequenceNumber());


        TimerRtpPacket timerRtpPacket;
        if (producerMediaSubscriber != null) {
            Long sourceTimeNs = null;
            if (rtpTimeState.isHasSr()) {
                sourceTimeNs = rtpTimeState.toSourceTimeNs(packet.getTimestamp(), rtpPayload.getClockRate());
            }
            timerRtpPacket = new TimerRtpPacket(sourceTimeNs, packet);
            producerMediaSubscriber.onRtpPacket(this, timerRtpPacket);
        }
    }


    @Override
    public void onRtcpPacket(RtcpPacket packet) {
        if (packet instanceof SenderReportRtcpPacket senderReportRtcpPacket) {
            this.receiveStats.onSenderReport(senderReportRtcpPacket.getNtpSec(), senderReportRtcpPacket.getNtpFrac());
            this.rtpTimeState.updateFromSr(senderReportRtcpPacket.getNtpSec(), senderReportRtcpPacket.getNtpFrac(), senderReportRtcpPacket.getRtpTimestamp());
            this.producerMediaSubscriber.onSourceTimeReady(this);
//            log.info("当前的数据：{}", this.rtpTimeState);
        }
    }


    @Override
    public void setMediaSubscriber(IProducerMediaSubscriber subscriber) {
        this.producerMediaSubscriber = subscriber;
    }

    @Override
    public void removeMediaSubscriber() {
        this.producerMediaSubscriber = null;
    }


    @Override
    public void close() {

    }

    @Override
    public boolean isSourceTimeReady() {
        return this.rtpTimeState.isHasSr();
    }

    @Override
    public void onPLI() {
        requestKeyframes.compareAndSet(false, true);
    }


    @Override
    public IMediaControl getMediaControl() {
        return this;
    }

    /**
     * 构建并返回一个接收者报告类型的 RTCP 数据包 (Receiver Report RTCP Packet)。
     * 该方法会生成一个包含接收者报告块的 RTCP 数据包，用于报告接收到的 RTP 数据包状态。
     *
     * @param nowNs 当前时间的纳秒时间戳，用于生成报告或计算延迟等信息。
     * @return 一个包含接收者报告块的 ReceiverReportRtcpPacket 对象，该对象用于描述接收到的 RTP 流状态。
     */
    public ReceiverReportRtcpPacket consumeReceiverReport(long nowNs) {

        ReceiverReportSnapshot snapshot = receiveStats.snapshot(nowNs);

        ReceiverReportRtcpPacket receiverReportRtcpPacket = new ReceiverReportRtcpPacket();
        receiverReportRtcpPacket.setVersion((byte) 2);
        receiverReportRtcpPacket.setPadding((byte) 0);
        receiverReportRtcpPacket.setRc(1);
        receiverReportRtcpPacket.setLength(7);
        receiverReportRtcpPacket.setPayloadType(RtcpPayloadType.RECEIVER_REPORT.value);
        receiverReportRtcpPacket.setSsrc(1);
        ReceiverReportBlock receiverReportBlock = new ReceiverReportBlock();
        receiverReportBlock.setSourceSsrc(mainSsrc);
        receiverReportBlock.setFractionLost(snapshot.getFractionLost());
        receiverReportBlock.setLost(snapshot.getCumulativeLost());
        receiverReportBlock.setExtHighestSeq(snapshot.getExtendedMaxSeq());
        receiverReportBlock.setCycles(0);
        receiverReportBlock.setJitter(snapshot.getJitter());
        receiverReportBlock.setLsr(snapshot.getLsr());
        receiverReportBlock.setDlsr(snapshot.getDlsr());
        receiverReportBlock.setDelaySeconds(0);
        receiverReportRtcpPacket.addReceiverReportBlock(receiverReportBlock);

        return receiverReportRtcpPacket;
    }

    /**
     * 消费请求关键帧标志并生成一个PSFB (Picture Loss Indication) RTCP数据包。
     * 当检测到需要生成PLI数据包时，此方法会创建一个新的数据包，
     * 设置其相关字段，并返回该数据包。如果没有请求关键帧的标志被触发，则返回null。
     *
     * @return 一个设置完成的PsFbRtcpPacket对象，用于指示视频关键帧丢失的控制信令。
     * 返回null表示当前未触发任何关键帧请求信号。
     */
    public PsFbRtcpPacket consumePli() {
//        if (requestKeyframes.compareAndSet(true, false)) {
        if (false) {
            PsFbRtcpPacket psFbRtcpPacket = new PsFbRtcpPacket();
            psFbRtcpPacket.setVersion((byte) 2);
            psFbRtcpPacket.setPadding((byte) 0);
            psFbRtcpPacket.setFmt(1);
            psFbRtcpPacket.setPayloadType(RtcpPayloadType.PSFB.value);
            psFbRtcpPacket.setLength(2);
            psFbRtcpPacket.setSenderSsrc(1);
            psFbRtcpPacket.setMediaSsrc(mainSsrc);
            return psFbRtcpPacket;
        }
        return null;

    }

    /**
     * 根据当前的丢包序列数据生成并返回一个 RTCP NACK (Negative Acknowledgment) 包。
     * <p>
     * 该方法会检查丢失的扩展序列号列表，如果存在丢包，则按 NACK 协议格式生成相应的 NACK 数据包。
     * 它将丢包信息编码到 FCI (Feedback Control Information) 字段中，并设置 RTCP 报文的相关字段。
     * 如果当前不存在丢包，则返回 null。
     *
     * @return 一个构造完整的 PsFbRtcpPacket 对象，表示基于当前丢包状态的 NACK 包；
     * 如果没有丢包，则返回 null。
     */
    public PsFbRtcpPacket consumeNack() {
        log.debug("当前丢包数据：{}", seqTracker.getMissingSeqs().toString());

        List<Long> missingExtendedSeqs = seqTracker.getMissingExtendedSeqs();
        if (missingExtendedSeqs.isEmpty()) {
            return null;
        }

        missingExtendedSeqs.sort(Long::compareTo);
        ByteBuf fci = Unpooled.buffer(Math.min(missingExtendedSeqs.size(), MAX_NACK_PID_BLOCKS) * 4);

        int pidBlocks = 0;
        for (int i = 0; i < missingExtendedSeqs.size() && pidBlocks < MAX_NACK_PID_BLOCKS; ) {
            long pidExtended = missingExtendedSeqs.get(i);
            int pid = (int) (pidExtended & 0xFFFF);
            int blp = 0;
            int j = i + 1;
            while (j < missingExtendedSeqs.size()) {
                long delta = missingExtendedSeqs.get(j) - pidExtended;
                if (delta <= 0) {
                    j++;
                    continue;
                }
                if (delta > 16) {
                    break;
                }
                blp |= 1 << ((int) delta - 1);
                j++;
            }

            fci.writeShort(pid);
            fci.writeShort(blp);
            pidBlocks++;
            i = j;
        }

        if (!fci.isReadable()) {
            return null;
        }

        PsFbRtcpPacket nackPacket = new PsFbRtcpPacket();
        nackPacket.setVersion((byte) 2);
        nackPacket.setPadding((byte) 0);
        nackPacket.setFmt(RTCP_RTPFB_NACK_FMT);
        nackPacket.setPayloadType(RtcpPayloadType.RTPFB.value);
        nackPacket.setLength(2 + (fci.readableBytes() / 4));
        nackPacket.setSenderSsrc(1);
        nackPacket.setMediaSsrc(mainSsrc);
        nackPacket.setFci(fci);
        return nackPacket;
    }
}
