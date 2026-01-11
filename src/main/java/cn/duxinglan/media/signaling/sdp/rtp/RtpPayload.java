package cn.duxinglan.media.signaling.sdp.rtp;

import lombok.Data;

import java.util.LinkedHashSet;
import java.util.Set;

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
public class RtpPayload {

    /**
     * 描述 RTP 负载类型的标识符。
     * <p>
     * payloadType 是一个整型常量，表示 RTP 数据包的负载类型，
     * 根据 RFC 3551 和相关标准，该值定义了 RTP 会话中可支持的编码类型。
     * <p>
     * 典型应用场景包括：
     * 1. 用于区分音视频编码格式，如 VP8、H264 或 OPUS 等。
     * 2. 用于与远端设备协商支持的解码器及参数。
     * <p>
     * 值域为动态负载（96-127）或静态负载范围（0-95）（例如 PCMU 的静态负载值为 0）。
     */
    private int payloadType;


    /**
     * 描述 RTP 负载的媒体类型。
     * <p>
     * mediaType 表示此 RTP 负载所属的媒体类型，包括音频 (AUDIO) 和视频 (VIDEO)。
     * 该值用于区分和标识 RTP 会话中不同的媒体流类型。
     * <p>
     * 应用场景包括：
     * 1. 在 SDP 协议中声明流的媒体类型，以满足媒体协商需求。
     * 2. 在多路复用场景下，用于解码器或其他组件正确处理媒体数据。
     * <p>
     * mediaType 的值来自于枚举类型 {@link MediaType}，确保了媒体类型的定义明确且规范。
     */
    private MediaType mediaType;

    /**
     * 表示 RTP 负载的编码名称。
     * <p>
     * encodingName 是一个字符串，描述 RTP 数据包中负载使用的具体编码格式。
     * 此字段通常用于标识视频或音频的编解码器类型，如 VP8、VP9、H264、OPUS 或 RTX。
     * <p>
     * 应用场景包括：
     * 1. 在 SDP 协议中，encodingName 定义了媒体编码类型以支持协商。
     * 2. 在流媒体解码器设定时，用于匹配和选择正确的解码逻辑。
     * <p>
     * 以下是常见的编码名称：
     * - 视频编码：VP8、VP9、H264
     * - 音频编码：OPUS
     * - 转发冗余：RTX
     */
    private String encodingName;       // VP8 / VP9 / H264 / OPUS / rtx
    /**
     * 表示 RTP 负载的时钟速率（clock rate）。
     * <p>
     * clockRate 定义了 RTP 数据包的时间刻度单位（单位：Hz），
     * 用于解释 RTP 时间戳的含义，确保接收和解码数据时的同步。
     * 不同的媒体类型和编码格式通常使用不同的时钟速率。
     * <p>
     * 典型值：
     * - 音频：48000（用于 OPUS 等音频编码）
     * - 视频：90000（用于 H264、VP8 等视频编码）
     * <p>
     * 应用场景：
     * 1. 用于计算 RTP 时间戳到实际时间的映射关系。
     * 2. 在 SDP 会话协商中指定媒体流的时间基准。
     */
    private int clockRate;             // 90000 / 48000

    private PayloadRole role;           // PRIMARY / RTX / RED / FEC


    /* 音频数据*/
    private Integer channels;          // audio only (e.g. OPUS/48000/2)

    /* ========= 能力声明 ========= */

    private FmtpAttributes fmtp;        // 强类型 + 扩展
    private Set<RtcpFeedback> rtcpFeedbacks;


    public void addRtcpFb(RtcpFeedback rtcpFeedback) {
        if (rtcpFeedbacks == null) {
            rtcpFeedbacks = new LinkedHashSet<>();
        }
        rtcpFeedbacks.add(rtcpFeedback);
    }
}
