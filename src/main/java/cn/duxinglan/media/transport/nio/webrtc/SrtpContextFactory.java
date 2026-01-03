package cn.duxinglan.media.transport.nio.webrtc;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
@Data
public class SrtpContextFactory {

    /**
     * 存储和管理 SRTP (Secure Real-time Transport Protocol) 数据上下文的映射关系。
     *
     * 该映射表以 `ssrc` (synchronization source identifier) 作为键，
     * 将其与对应的 {@link SRtpContext} 实例关联，用于在处理安全传输数据时快速检索上下文。
     *
     * 特性：
     * - 线程安全，支持多线程环境的并发访问。
     * - 利用 `ConcurrentHashMap` 实现高效的键值对存储与检索。
     *
     * 用途：
     * - 在 SRTP 数据流中，根据 `ssrc` 动态创建和分配密钥材料和加密上下文。
     * - 存储和重用已经创建的 `SRtpContext`，以提升性能并确保一致性。
     *
     * 注意：
     * - 必须由外部逻辑确保上下文的正确初始化和清理，以避免内存泄漏或冗余存储。
     */
    private Map<Long, SRtpContext> srtpContextMap = new ConcurrentHashMap<>();

    /**
     * 保存每个 SSRC（同步信源标识）对应的 SRtcpContext 映射表。
     *
     * 该映射表用于管理和存储与 SRTP 协议相关的 RTCP 加密上下文，确保不同 SSRC 的数据流
     * 均采用独立的加密和认证配置。
     *
     * 线程安全：使用 ConcurrentHashMap 实现，支持多线程环境的读写操作。
     *
     * 键：Long 类型，表示 SSRC 值（同步信源标识）。
     * 值：SRtcpContext 类型，包含与该 SSRC 相关的加密、认证等上下文信息。
     *
     * 典型场景：
     * - 客户端或服务器分别根据 SSRC 查找已有的 SRtcpContext，或为新的 SSRC 创建并存储相关上下文。
     * - 支持动态的上下文管理以适应实时音视频传输的需求。
     */
    private Map<Long, SRtcpContext> srtcpContextMap = new ConcurrentHashMap<>();

    /**
     * 表示 SRTP (安全实时传输协议) 配置类型的变量。
     *
     * 用于定义 SRTP 加密和验证过程中的各种参数设置，包括加密算法、密钥长度、盐值长度等。
     * 在类 {@code SrtpContextFactory} 中，该变量被用来初始化和指定 SRTP 的配置选项。
     *
     * 不同的配置类型通过枚举 {@code SrtpProfilesType} 来描述，用于匹配不同的安全需求。
     * SRTP 的配置选项会直接影响对 RTP 和 RTCP 流的保护水平。
     */
    private SrtpProfilesType srtpProfilesType;

    /**
     * 客户端的主密钥，用于 SRTP（安全实时传输协议）的加密操作。
     *
     * 该密钥在握手协议完成后生成，并分配给客户端，用于 SRTP 数据包的加密和解密过程。
     * 长度由所选的 SRTP 配置类型（SrtpProfilesType）决定，通常是 AES 加密算法所要求的密钥长度。
     *
     * 配合客户端的盐值（clientMasterSalt）使用，以确保密钥的安全性和唯一性。
     */
    private byte[] clientMasterKey;
    /**
     * 客户端主盐值（Master Salt）的字节数组表示，用于 SRTP (安全实时传输协议) 密钥派生。
     *
     * 此字段保存了客户端通信所需的主盐值，它与客户端密钥 (Master Key) 配合使用，用于生成加密密钥和认证密钥。
     * 主盐值是 SRTP 密钥管理的一部分，决定了 SRTP 数据流的加密和认证行为。
     *
     * 在 SRTP 协议中，主盐值通过密钥派生函数 (Key Derivation Function)
     * 来生成最终加密以及认证的参数，以保护数据的机密性和完整性。
     */
    private byte[] clientMasterSalt;
    /**
     * 表示服务器端用于 SRTP (安全实时传输协议) 的主加密密钥。
     *
     * serverMasterKey 是在 SRTP 会话中为服务端提供的对称密钥，通常通过 DTLS 握手阶段生成。
     * 该密钥与 serverMasterSalt 一起用于初始化服务器端的加密算法，用以保障 SRTP 数据包的保密性。
     *
     * 注意：serverMasterKey 的长度由所选的 SRTP 配置类型 (SrtpProfilesType) 决定，
     * 不同配置类型可能要求不同的密钥长度。
     *
     * 主要用途包括：
     * 1. 在 SRTP 数据加密上下文 (SRtpContext) 中配置服务端加密算法。
     * 2. 提供密钥材料来加密和解密服务器端的音视频媒体数据。
     *
     * 该字段在会话期间需谨慎保护，防止泄露以保障传输安全。
     */
    private byte[] serverMasterKey;
    /**
     * 表示服务器用于 SRTP（安全实时传输协议）的主盐值。
     * <p>该字段用于为服务器端的 SRTP 会话生成加密数据包的唯一标识符或保护密钥。
     * SRTP 将主盐值与其他安全参数结合使用，以增强整体数据安全性，并防止重放攻击等潜在威胁。</p>
     *
     * 声明：
     * - 数据类型为字节数组，存储原始的二进制盐值。
     * - 盐值的长度通常与 SRTP 配置类型（如 SrtpProfilesType）相关联。
     *
     * 使用场景：
     * - 与 {@code serverMasterKey} 一起作为服务器端 SRTP 加密上下文的一部分。
     * - 在 SRTP 初始化或密钥派生流程中被设定和引用。
     * - 通过 {@link #setServerCipher(byte[], byte[])} 方法设定此字段的值。
     */
    private byte[] serverMasterSalt;

    public SrtpContextFactory() {
    }

    public SrtpContextFactory(SrtpProfilesType srtpProfilesType, byte[] keyingMaterial) throws NoSuchPaddingException, NoSuchAlgorithmException {
        setKeyingMaterial(srtpProfilesType, keyingMaterial);
    }


    /**
     * 配置用于 SRTP (安全实时传输协议) 的加密密钥和盐值。
     * 该方法根据 SRTP 配置类型提取密钥材料，将其分配为客户端和服务器的 AES 密钥及对应的盐值。
     *
     * @param srtpProfilesType SRTP 配置类型，指定加密密钥长度和盐值长度等信息。
     * @param keyingMaterial   原始密钥材料，包含密钥和盐值，使用此材料划分客户端密钥、服务器密钥、
     *                         客户端盐值、服务器盐值。
     *                         长度根据 SRTP 配置类型动态确定。
     */
    public void setKeyingMaterial(SrtpProfilesType srtpProfilesType, byte[] keyingMaterial) throws NoSuchPaddingException, NoSuchAlgorithmException {
        this.srtpProfilesType = srtpProfilesType;
        int keyLen = this.srtpProfilesType.encKeyLength;
        int saltLen = this.srtpProfilesType.saltKeyLength;
        byte[] clientKey = Arrays.copyOfRange(keyingMaterial, 0, keyLen);
        byte[] serverKey = Arrays.copyOfRange(keyingMaterial, keyLen, 2 * keyLen);
        byte[] clientSalt = Arrays.copyOfRange(keyingMaterial, 2 * keyLen, 2 * keyLen + saltLen);
        byte[] serverSalt = Arrays.copyOfRange(keyingMaterial, 2 * keyLen + saltLen, 2 * (keyLen + saltLen));
        setClientCipher(clientKey, clientSalt);
        setServerCipher(serverKey, serverSalt);
    }

    public void setClientCipher(byte[] masterKey, byte[] masterSalt) throws NoSuchPaddingException, NoSuchAlgorithmException {
        this.clientMasterKey = masterKey;
        this.clientMasterSalt = masterSalt;
    }

    public void setServerCipher(byte[] masterKey, byte[] masterSalt) throws NoSuchPaddingException, NoSuchAlgorithmException {
        this.serverMasterKey = masterKey;
        this.serverMasterSalt = masterSalt;
    }


    public SRtcpContext getClientSRtcpContext(long ssrc) {
        return srtcpContextMap.computeIfAbsent(
                ssrc,
                k -> {
                    try {
                        return new SRtcpContext(k, new SrtpKeyDerivationFunction(clientMasterKey, clientMasterSalt, this.srtpProfilesType), Cipher.DECRYPT_MODE);
                    } catch (NoSuchPaddingException | NoSuchAlgorithmException e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }

    public SRtcpContext getServerSRtcpContext(long ssrc)  {
        return srtcpContextMap.computeIfAbsent(
                ssrc,
                k -> {
                    try {
                        return new SRtcpContext(k, new SrtpKeyDerivationFunction(serverMasterKey, serverMasterSalt, this.srtpProfilesType), Cipher.ENCRYPT_MODE);
                    } catch (NoSuchPaddingException | NoSuchAlgorithmException e) {
                        throw new RuntimeException(e);
                    }
                }
        );

    }


    public SRtpContext getClientSrtpContext(long ssrc) {
        return srtpContextMap.computeIfAbsent(
                ssrc,
                k -> {
                    try {
                        return new SRtpContext(k, new SrtpKeyDerivationFunction(clientMasterKey, clientMasterSalt, this.srtpProfilesType), Cipher.DECRYPT_MODE);
                    } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException e) {
                        throw new RuntimeException(e);
                    }
                }
        );

    }

    public SRtpContext getServerSrtpContext(long ssrc) {
        return srtpContextMap.computeIfAbsent(
                ssrc,
                k -> {
                    try {
                        return new SRtpContext(ssrc, new SrtpKeyDerivationFunction(serverMasterKey, serverMasterSalt, this.srtpProfilesType), Cipher.ENCRYPT_MODE);
                    } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }
}
