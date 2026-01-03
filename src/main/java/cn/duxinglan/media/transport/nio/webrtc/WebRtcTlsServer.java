package cn.duxinglan.media.transport.nio.webrtc;

import cn.duxinglan.media.impl.webrtc.WebRTCCertificateGenerator;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.tls.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
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
@Slf4j
public class WebRtcTlsServer extends DefaultTlsServer {


    /**
     * 表示用于DTLS（Datagram Transport Layer Security）操作的密钥材料。
     * 该对象包含服务端用于加密通信和进行身份验证的必要密钥信息。
     * 被 {@link WebRtcTlsServer} 类用于初始化和运行TLS会话。
     */
    private final WebRTCCertificateGenerator.DTLSKeyMaterial keyMaterial;

    private SrtpProfilesType srtpProfilesType;

    private final DtlsContext.DtlsShakeHandsCallback dtlsShakeHandsCallback;

    public WebRtcTlsServer(WebRTCCertificateGenerator.DTLSKeyMaterial keyMaterial, DtlsContext.DtlsShakeHandsCallback dtlsShakeHandsCallback) {
        super(keyMaterial.getCrypto());
        this.keyMaterial = keyMaterial;
        this.dtlsShakeHandsCallback = dtlsShakeHandsCallback;
    }

    @Override
    protected TlsCredentialedSigner getDSASignerCredentials() throws IOException {
        // WebRTC 不使用 DSA
        return null;
    }

    @Override
    protected TlsCredentialedSigner getECDSASignerCredentials() throws IOException {
        try {
            return keyMaterial.getSignerCredentials(context);
        } catch (Exception e) {
            throw new IOException("获取 ECDSA 签名凭证失败", e);
        }
    }

    @Override
    protected TlsCredentialedDecryptor getRSAEncryptionCredentials() throws IOException {
        // WebRTC DTLS 通常不使用 RSA 加密
        return null;
    }

    @Override
    public Hashtable<Integer, Object> getServerExtensions() throws IOException {
        Hashtable<Integer, Object> extensions = super.getServerExtensions();
        if (extensions == null) {
            extensions = new Hashtable<>();
        }

        // 添加 SRTP 扩展（RFC 5764）
        UseSRTPData srtpData = new UseSRTPData(new int[]{this.srtpProfilesType.srtpProtectionProfile}, new byte[0]);
        TlsSRTPUtils.addUseSRTPExtension(extensions, srtpData);
        return extensions;
    }

    @Override
    public void notifyHandshakeComplete() throws IOException {
        super.notifyHandshakeComplete();
        log.debug("✅ DTLS 握手完成");
        byte[] keyingMaterial = context.exportKeyingMaterial(ExporterLabel.dtls_srtp, null, 2 * (this.srtpProfilesType.encKeyLength + this.srtpProfilesType.saltKeyLength));
        dtlsShakeHandsCallback.notifyHandshakeComplete(this.srtpProfilesType, keyingMaterial);
    }

    @Override
    protected int[] getSupportedCipherSuites() {
        // WebRTC 推荐的 DTLS cipher suites
        return new int[]{
                CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
                CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384
        };
    }

    @Override
    protected ProtocolVersion[] getSupportedVersions() {
        // WebRTC 通常使用 DTLS 1.2（部分浏览器开始支持 DTLS 1.3）
        return ProtocolVersion.DTLSv12.downTo(ProtocolVersion.DTLSv12);
    }

    @Override
    public void processClientExtensions(Hashtable hashtable) throws IOException {
        super.processClientExtensions(hashtable);

        if (clientExtensions == null) {
            return;
        }

        // 从客户端扩展中获取 UseSRTPData
        UseSRTPData useSRTPData = TlsSRTPUtils.getUseSRTPExtension(clientExtensions);
        if (useSRTPData == null) {
            log.warn("客户端未提供 SRTP 扩展");
            return;
        }

        int[] protectionProfiles = useSRTPData.getProtectionProfiles();
        if (protectionProfiles == null || protectionProfiles.length == 0) {
            log.warn("客户端 SRTP 扩展中未提供 protectionProfiles");
            return;
        }
        List<SrtpProfilesType> srtpProfilesTypeList = new ArrayList<>();
        for (int protectionProfile : protectionProfiles) {
            SrtpProfilesType.fromSrtpProtectionProfileOpt(protectionProfile).ifPresent(srtpProfilesTypeList::add);
        }

        if (srtpProfilesTypeList.isEmpty()) {
            throw new IOException("未找支持的 SRTP profile 客户端: " + Arrays.toString(protectionProfiles));
        }

        if (srtpProfilesTypeList.contains(SrtpProfilesType.SRTP_AES128_CM_HMAC_SHA1_80)) {
            this.srtpProfilesType = SrtpProfilesType.SRTP_AES128_CM_HMAC_SHA1_80;
            return;
        }

        throw new IOException("服务器不支持当前客户端的SRTP profile: " + Arrays.toString(protectionProfiles));
    }
}
