package cn.duxinglan.media.transport.nio.webrtc;

import cn.duxinglan.media.core.IMediaTransport;
import cn.duxinglan.media.impl.webrtc.WebRTCCertificateGenerator;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.tls.DTLSServerProtocol;
import org.bouncycastle.tls.DTLSTransport;

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
@Slf4j
public class DtlsContext {

    private final WebRTCCertificateGenerator.DTLSKeyMaterial keyMaterial;

    private DTLSServerProtocol protocol;

    private WebRtcTlsServer tlsServer;

    private DatagramTransportImpl datagramTransport;

    private DtlsState state = DtlsState.INIT;

    private IMediaTransport mediaTransport;

    private DTLSTransport dtlsTransport;


    private final DtlsShakeHandsCallback dtlsShakeHandsCallback;

    public DtlsContext(IMediaTransport mediaTransport, WebRTCCertificateGenerator.DTLSKeyMaterial keyMaterial, DtlsShakeHandsCallback dtlsShakeHandsCallback) {
        this.mediaTransport = mediaTransport;
        this.keyMaterial = keyMaterial;
        this.dtlsShakeHandsCallback = dtlsShakeHandsCallback;
        this.protocol = new DTLSServerProtocol();
        this.tlsServer = new WebRtcTlsServer(keyMaterial, dtlsShakeHandsCallback);
        this.datagramTransport = new DatagramTransportImpl(mediaTransport);
    }

    public void processData(byte[] msg) throws IOException {
        if (state == DtlsState.CLOSED) {
            throw new IOException("DTLS会话已关闭");
        }
        datagramTransport.offerPacket(msg);
        // 如果还没握手，则尝试启动握手
        if (state == DtlsState.INIT) {
            state = DtlsState.HANDSHAKING;
            new Thread(() -> {
                try {
                    log.debug("开始 DTLS 握手");
                    dtlsTransport = protocol.accept(tlsServer, datagramTransport);
                    state = DtlsState.CONNECTED;
                    log.debug("DTLS 握手完成");
                } catch (Exception e) {
                    state = DtlsState.FAILED;
                    log.error("握手失败", e);
                    try {
                        datagramTransport.close();
                    } catch (IOException ioException) {
                        log.warn("关闭 DatagramTransport 失败", ioException);
                    }
                } finally {
                    dtlsShakeHandsCallback.onConnected(state);
                }
            }, "dtls-accept-thread").start();
        }
    }


    public enum DtlsState {
        INIT, HANDSHAKING, CONNECTED, FAILED, CLOSED
    }

    public interface DtlsShakeHandsCallback {

        void notifyHandshakeComplete(SrtpProfilesType srtpProfilesType, byte[] keyingMaterial);

        void onConnected(DtlsState state);
    }
}
