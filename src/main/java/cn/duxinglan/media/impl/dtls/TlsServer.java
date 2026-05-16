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
package cn.duxinglan.media.impl.dtls;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.tls.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

/**
 * TlsServer 是一个用于实现 WebRTC 中基于 DTLS 1.2 的安全传输协议的服务端类。
 * 它继承自 {@link DefaultTlsServer}，并扩展了以下功能：
 * <p>
 * 1. 支持 WebRTC 推荐的 Cipher Suites 和 DTLS 版本。
 * 2. 集成 SRTP（Secure Real-Time Transport Protocol）扩展以支持加密的实时媒体流传输。
 * 3. 使用自定义的密钥和证书管理。
 * 4. 握手成功后，通过回调通知上层，并提供加密密钥材料以实现媒体加密。
 * <p>
 * 特性和细节：
 * - 支持 DTLS 中的 ECDSA 签名算法，用于身份验证。
 * - 不支持 RSA 加密和 DSA 签名算法，这符合 WebRTC 的设计规范。
 * - 实现了 RFC5764 中定义的 SRTP 扩展，并根据客户端的 SRTP 配置协商合适的 SRTP 属性。
 * <p>
 * 构造方法：
 * - 需要通过 {@link DTLSKeyMaterial} 提供密钥材料（包括私钥、证书和加密对象）。
 * - 需要通过 {@link DtlsContext.DtlsShakeHandsCallback} 提供握手完成时的回调，用于暴露密钥材料给外部。
 * <p>
 * 重写的方法：
 * 1. {@link #getSupportedCipherSuites()}
 * 返回支持的 Cipher Suites 列表，目前支持 `TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256` 和
 * `TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384`。
 * <p>
 * 2. {@link #getSupportedVersions()}
 * 返回支持的 DTLS 版本，目前支持 DTLS 1.2。
 * <p>
 * 3. {@link #getServerExtensions()}
 * 配置并添加 SRTP 扩展，用于对实时传输保护的支持。
 * <p>
 * 4. {@link #processClientExtensions(Hashtable)}
 * 处理客户端的 SRTP 扩展信息，并协商合适的 SRTP 配置。
 * <p>
 * 5. {@link #notifyHandshakeComplete()}
 * 通知 DTLS 握手完成，并通过回调返回加密密钥材料。
 */
@Slf4j
public class TlsServer extends DefaultTlsServer {


    /**
     * 表示用于DTLS（Datagram Transport Layer Security）操作的密钥材料。
     * 该对象包含服务端用于加密通信和进行身份验证的必要密钥信息。
     * 被 {@link TlsServer} 类用于初始化和运行TLS会话。
     */
    private final DTLSKeyMaterial keyMaterial;

    /**
     * 表示 TLS 服务器中支持的安全实时传输协议 (SRTP) 配置文件类型。
     * 此字段用于确定服务器在 DTLS 握手期间支持的 SRTP 配置，
     * 并影响 RTP/RTCP 数据流的加密和认证行为。
     * <p>
     * 具体内容取决于 SrtpProfilesType 枚举的定义，不同的枚举实例
     * 表示不同的加密算法、认证算法以及对应的参数配置。
     * <p>
     * 该字段在 TLS 握手的过程中被使用，用于协商服务器和客户端之间支持的
     * SRTP 配置，从而决定 RTP/RTCP 数据的安全传输方式。
     */
    private SrtpProfilesType srtpProfilesType;

    private final DtlsContext.DtlsShakeHandsCallback dtlsShakeHandsCallback;

    public TlsServer(DTLSKeyMaterial keyMaterial, DtlsContext.DtlsShakeHandsCallback dtlsShakeHandsCallback) {
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
