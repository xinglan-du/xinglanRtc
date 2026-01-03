package cn.duxinglan.media.protocol.rtp;

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

/**
 * 用于表示绑定源时间信息和 RTP 数据包的时间戳记录。
 * <p>
 * 此类是一个不可变的记录类型，结合了一个源时间戳 (sourceTimeNs) 和一个 RTP 数据包 (RtpPacket) 实例。
 * 主要用于维护 RTP 数据包与其生成或接收的时间信息之间的关系。
 */
public record TimerRtpPacket(Long sourceTimeNs, RtpPacket rtpPacket) {


}
