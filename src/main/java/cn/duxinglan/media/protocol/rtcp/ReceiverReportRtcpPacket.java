package cn.duxinglan.media.protocol.rtcp;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

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
@EqualsAndHashCode(callSuper = true)
@Data
public class ReceiverReportRtcpPacket extends RtcpPacket {

    /**
     * 表示 RTCP 数据包中的接收者报告块计数字段 (Reception Report Count)。
     * 该字段用于标识当前 RTCP 数据包中包含的接收者报告块的数量。
     * 范围为 0 到 31，由发送端根据需要设置。
     */
    private int rc;


    /**
     * 表示同步源标识符 (SSRC, Synchronization Source Identifier)。
     * 用于唯一标识 RTCP 数据包的源节点。
     * 在同一个 RTP/RTCP 会话中，SSRC 应该是唯一的，以确保能够正确地区分来自不同源的数据流。
     */
    private long ssrc;


    List<ReceiverReportBlock> receiverReportBlocks = new ArrayList<>();

    public void addReceiverReportBlock(ReceiverReportBlock receiverReportBlock) {
        receiverReportBlocks.add(receiverReportBlock);
    }
}
