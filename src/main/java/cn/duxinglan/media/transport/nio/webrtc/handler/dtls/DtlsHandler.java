package cn.duxinglan.media.transport.nio.webrtc.handler.dtls;

import cn.duxinglan.media.core.IMediaTransport;
import cn.duxinglan.media.impl.webrtc.WebRTCCertificateGenerator;
import cn.duxinglan.media.transport.nio.webrtc.DtlsContext;
import io.netty.buffer.ByteBuf;

import java.io.IOException;

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
public class DtlsHandler {


    private final DtlsContext dtlsContext;

    public DtlsHandler(IMediaTransport mediaTransport, WebRTCCertificateGenerator.DTLSKeyMaterial keyMaterial, DtlsContext.DtlsShakeHandsCallback dtlsShakeHandsCallback) {
        this.dtlsContext = new DtlsContext(mediaTransport, keyMaterial, dtlsShakeHandsCallback);
    }

    public void processDtls(ByteBuf buf) throws IOException {
        int readable = buf.readableBytes();
        byte[] bytes = new byte[readable];
        buf.getBytes(buf.readerIndex(), bytes);
        //DTLS直接使用bc库
        dtlsContext.processData(bytes);
    }
}
