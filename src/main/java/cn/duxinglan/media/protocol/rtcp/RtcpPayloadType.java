package cn.duxinglan.media.protocol.rtcp;

import lombok.extern.slf4j.Slf4j;

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
public enum RtcpPayloadType {

    /**
     * 表示 RTCP 协议中的发送者报告 (Sender Report) 类型。
     * <p>
     * 发送者报告是 RTCP 协议中一种数据包类型，
     * 用于提供发送者相关的统计信息，包括时间戳、同步源标识符 (SSRC)、发送的数据包数、字节数等。
     * 此报文类型通常用于同步接收端与发送端之间的时钟以及监控网络性能。
     * <p>
     * 该类型的枚举值为 200，对应于 RTCP 协议中定义的发送者报告 (Sender Report) 类型标识符。
     */
    SENDER_REPORT(200),
    /**
     * 表示 RTCP 协议中的接收者报告 (Receiver Report) 类型。
     * <p>
     * 接收者报告是 RTCP 协议中一种数据包类型，
     * 用于由未发送 RTP 数据包的会话参与者提供的统计信息。
     * 接收者报告包含发送方 RTP 数据流的质量信息，例如丢包率、抖动等。
     * <p>
     * 接收者报告被发送给发送者，用于监控网络的传输质量，
     * 并为传输优化提供反馈。
     * <p>
     * 该类型的枚举值为 201，对应于 RTCP 协议中定义的接收者报告 (Receiver Report) 类型标识符。
     */
    RECEIVER_REPORT(201),
    /**
     * 表示 RTCP 协议中的源描述 (Source Description, SDES) 类型。
     * <p>
     * 源描述是一种 RTCP 数据包类型，用于提供会话参与者的元信息，
     * 包括标识信息（如 CNAME）、会话相关描述（如名称、电子邮件地址、备注等）。
     * <p>
     * SDES 数据包通常用于标识和描述数据流的来源，帮助接收端理解和分类接收到的 RTP 数据流。
     * 它包含一系列的 SDES 项目，如 CNAME (Canonical Name)，确保会话中不同 SSRC 的唯一标识。
     * <p>
     * 该类型的枚举值为 202，对应于 RTCP 协议中定义的 SDES 类型标识符。
     */
    SDES(202),
    /**
     * 表示 RTCP 协议中的再见 (Goodbye) 类型。
     * <p>
     * 再见是一种 RTCP 数据包类型，通常用于会话参与者通知其他参与者自己即将离开 RTP 会话。
     * 它可以携带会话参与者的同步源标识符 (SSRC) 及可选的离开原因描述。
     * <p>
     * 再见数据包能够帮助会话管理，减少无用的状态信息，提示其他参与者可以停止接收相关的 RTP 数据流。
     * <p>
     * 该类型的枚举值为 203，对应于 RTCP 协议中定义的再见 (Goodbye) 类型标识符。
     */
    BYE(203),
    /**
     * 表示 RTCP 协议中的应用程序定义类型 (APP)。
     * <p>
     * APP 数据包是 RTCP 中扩展性极强的一种数据包类型，用于支持特定应用场景自定义的信息传输。
     * 应用程序定义的数据包通常包含应用程序自身的标识符和应用数据。
     * 这种类型的数据包使得 RTCP 可以适应一些特定需求的扩展，而无需破坏原有协议。
     * <p>
     * 该类型的枚举值为 204，对应于 RTCP 协议中定义的 APP 类型标识符。
     */
    APP_PT(204),
    /**
     * 表示 RTCP 协议中的 RTP 传输反馈 (Transport Feedback) 类型。
     * <p>
     * RTP 传输反馈数据包用于为实时媒体流的发送方提供与传输相关的精细反馈信息。
     * 它允许接收方报告 RTP 数据包的接收状态，包括数据包的接收与丢失情况、
     * 传输时延与网络性能等。这些信息对传输参数的优化和链路质量的评估至关重要。
     * <p>
     * 此类型主要用于增强传输可靠性，特别是在需要高效错误恢复和流量控制的实时通信系统中。
     * <p>
     * 该类型的枚举值为 205，对应于 RTCP 协议中定义的 RTP 传输反馈类型标识符。
     */
    RTPFB(205),

    /**
     * 表示 RTCP 协议中的有效负载特定反馈类型 (Payload-Specific Feedback, PSFB)。
     * <p>
     * PSFB 数据包用于为多媒体数据流提供特定于负载类型的反馈，通常涉及对某些特定编解码技术或协议的支持。
     * 它允许接收方上报如关键帧请求、解码错误、数据丢失等与特定负载类型相关的信息，
     * 从而便于发送方动态调整传输策略以优化数据流。
     * <p>
     * 该类型常应用于对支持特定功能或需求的传输反馈，例如视频关键帧请求 (Picture Loss Indication, PLI)
     * 或解码问题报告等，是 RTCP 协议扩展中非常重要的一个类型。
     * <p>
     * 此枚举值的常量为 206，对应 RTCP 标准规定的 PSFB 类型标识符。
     */
    PSFB(206),

    ;


    public final int value;

    RtcpPayloadType(int value) {
        this.value = value;
    }


    public static RtcpPayloadType fromValue(int value) {
        for (RtcpPayloadType type : RtcpPayloadType.values()) {
            if (type.value == value) {
                return type;
            }
        }
//        log.warn("未实现的payloadType:{}", value);
        return null;
    }
}
