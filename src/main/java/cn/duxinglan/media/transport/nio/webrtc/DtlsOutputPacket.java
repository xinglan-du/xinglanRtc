package cn.duxinglan.media.transport.nio.webrtc;

import cn.duxinglan.media.core.INetworkPacket;
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
public class DtlsOutputPacket implements INetworkPacket {

    private final byte[] buf;
    private final int off;
    private final int len;

    public DtlsOutputPacket(byte[] buf, int off, int len) {
        this.buf = buf;
        this.off = off;
        this.len = len;
    }

    @Override
    public int getTotalLength() {
        return len;
    }

    @Override
    public void writeTo(ByteBuf out) {
        out.writeBytes(buf, off, len);
    }


}
