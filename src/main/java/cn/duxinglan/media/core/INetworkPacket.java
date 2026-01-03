package cn.duxinglan.media.core;

import io.netty.buffer.ByteBuf;

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
public interface INetworkPacket {

    /**
     * 获取网络数据包的总长度。
     *
     * @return 网络数据包的总长度，以字节为单位。
     */
    int getTotalLength();

    /**
     * 将当前对象的内容写入指定的 {@link ByteBuf} 中。
     *
     * @param out 目标 {@code ByteBuf}，用于接收写入的数据。调用方需确保该缓冲区有足够的空间以存储待写入的数据。
     */
    void writeTo(ByteBuf out);

}
