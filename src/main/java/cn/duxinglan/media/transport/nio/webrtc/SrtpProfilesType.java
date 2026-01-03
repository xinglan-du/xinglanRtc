package cn.duxinglan.media.transport.nio.webrtc;

import org.bouncycastle.tls.SRTPProtectionProfile;

import java.util.Optional;

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
public enum SrtpProfilesType {

    /**
     * SRTP AES-128 Counter Mode with HMAC-SHA1 80-bit authentication tag.
     * 该枚举实例表示一个 SRTP (安全实时传输协议) 配置，使用 AES-128-CM 加密算法和 HMAC-SHA1
     * 验证算法，验证标签长度为 80 位。
     * <p>
     * 参数说明:
     * - SRTPProtectionProfile.SRTP_AES128_CM_HMAC_SHA1_80: 表示该配置的 SRTP 保护配置文件类型。
     * - cipherKeyLength: 加密密钥长度，单位为字节，此处为 16 字节。
     * - cipherSaltLength: 加密盐值长度，单位为字节，此处为 14 字节。
     * - cipherName: 加密算法的名称标识。
     * - authFunctionName: 验证算法的名称标识。
     * - authKeyLength: 验证密钥长度，单位为字节，此处为 20 字节。
     * - rtcpAuthTagLength: RTCP 验证明标签长度，单位为字节。
     * - rtpAuthTagLength: RTP 验证明标签长度，单位为字节。
     */
    SRTP_AES128_CM_HMAC_SHA1_80(SRTPProtectionProfile.SRTP_AES128_CM_HMAC_SHA1_80, 128/8, 112/8, 1, 1, 20, 10, 10),
    ;

    /**
     * 表示 SRTP (安全实时传输协议) 保护配置的标识符。
     * 该变量通常用于定义 SRTP 配置文件类型，与对应的加密和认证算法绑定。
     * <p>
     * 例如:
     * - SRTP_AES128_CM_HMAC_SHA1_80: 表示使用 AES-128-CM 加密算法和 HMAC-SHA1 验证算法，验证标签长度为 80 位。
     * <p>
     * 可用于协议实现中选择相应的 SRTP 配置文件，为 RTP 和 RTCP 流提供加密与认证保护。
     */
    public final int srtpProtectionProfile;

    /**
     * 表示 SRTP (安全实时传输协议) 配置中加密密钥的长度。
     * 该变量用于定义 SRTP 配置中的加密密钥长度，单位为字节。
     * <p>
     * 在 SRTP 配置中，加密密钥的长度直接影响传输数据的保密性。
     * 常见的密钥长度如 16 字节（AES-128）等，通过不同长度的密钥可以支持不同级别的安全需求。
     */
    public final int encKeyLength;

    /**
     * 表示 SRTP (安全实时传输协议) 配置中加密盐值的长度。
     * 该变量用于定义加密盐值的长度，单位为字节。
     * <p>
     * 在 SRTP 配置中，加密盐值主要用于增强加密密钥的随机性和唯一性，
     * 有助于进一步提高传输数据的安全性。
     */
    public final int saltKeyLength;

    /**
     * 表示 SRTP (安全实时传输协议) 配置中加密算法的名称标识。
     * 该变量主要用于标识 SRTP 配置中所使用的加密算法类型。
     * <p>
     * 不同的值对应不同的加密算法，例如 AES 或其他加密方法，用于指定 RTP/RTCP 数据传输中的加密机制。
     * 此标识有助于协议实现中明确配置使用的加密算法。
     *
     * TODO 这里后期要改成枚举 代表不同算法的名字
     */
    public final int cipherName;

    /**
     * 表示 SRTP (安全实时传输协议) 配置中认证算法的名称标识。
     * 该变量用于定义 SRTP 配置中使用的认证算法类型。
     * <p>
     * 不同的值对应不同的认证算法，例如 HMAC-SHA1 或其他算法，用于指定 RTP/RTCP 数据
     * 传输的认证机制。
     * 此标识有助于协议实现中明确配置使用的认证功能。
     *
     * TODO 这里后期要改成使用的认证算法枚举 有HMAC-SHA1、AEAD_GCM、NULL
     */
    public final int authFunctionName;

    /**
     * 表示 SRTP (安全实时传输协议) 配置中认证密钥的长度。
     * 该变量定义了用于 SRTP 验证过程的密钥长度，单位为字节。
     * <p>
     * 验证密钥长度直接影响认证过程的安全性，其值通常根据认证算法的要求进行设定。
     * 例如，对于 HMAC-SHA1 算法，认证密钥通常为 20 字节。
     * 此变量在协议实现中用于确保认证过程符合配置的安全需求。
     */
    public final int authKeyLength;

    /**
     * 表示 SRTP (安全实时传输协议) 在 RTCP (实时传输控制协议) 流中使用的认证标签的长度。
     * 该变量定义了 RTCP 验证明标签的长度，单位为字节。
     * <p>
     * 验证明标签用于验证 RTCP 数据完整性和真实性，确保传输过程中数据未被篡改。
     * 该值通常由具体的 SRTP 配置决定，例如特定认证算法所需的标签长度。
     */
    public final int rtcpAuthTagLength;

    /**
     * RTP（实时传输协议）的认证标签长度。
     * 用于指定SRTP（安全实时传输协议）认证标签的长度，以确保数据包的认证和完整性。
     */
    public final int rtpAuthTagLength;

    SrtpProfilesType(int srtpProtectionProfile, int encKeyLength, int saltKeyLength, int cipherName, int authFunctionName, int authKeyLength, int rtcpAuthTagLength, int rtpAuthTagLength) {
        this.srtpProtectionProfile = srtpProtectionProfile;
        this.encKeyLength = encKeyLength;
        this.saltKeyLength = saltKeyLength;
        this.cipherName = cipherName;
        this.authFunctionName = authFunctionName;
        this.authKeyLength = authKeyLength;
        this.rtcpAuthTagLength = rtcpAuthTagLength;
        this.rtpAuthTagLength = rtpAuthTagLength;
    }

    public static SrtpProfilesType fromSrtpProtectionProfile(int srtpProtectionProfile) {
        for (SrtpProfilesType value : SrtpProfilesType.values()) {
            if (value.srtpProtectionProfile == srtpProtectionProfile) {
                return value;
            }
        }
        return null;
    }

    public static Optional<SrtpProfilesType> fromSrtpProtectionProfileOpt(int srtpProtectionProfile) {
        return Optional.ofNullable(fromSrtpProtectionProfile(srtpProtectionProfile));
    }
}
