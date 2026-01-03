package cn.duxinglan.media.transport.nio.webrtc;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.util.Arrays;

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
public class SrtpKeyDerivationFunction {

    /**
     * 表示 SRTP (Secure Real-time Transport Protocol) 加密密钥派生过程中的标签字段，用于指示派生的密钥类型。
     * <p>
     * 此常量 LABEL_RTP_ENCRYPTION 的值为 0x00，与 SRTP RFC 文档中定义的 RTP 加密标签一致。
     * 在密钥派生过程中，当计算对应的加密密钥时，可将此标签作为输入，以确保生成的密钥适用于 RTP 数据流的加密。
     * <p>
     * 值: 0x00
     * <p>
     * 用途:
     * - 在 SRTP 密钥派生函数中，标识当前计算的是 RTP 数据流的加密密钥。
     * - 此标签确保在生成密钥时符合 SRTP 规范中的定义。
     */
    public static final byte LABEL_RTP_ENCRYPTION = 0x00;
    public static final byte LABEL_RTP_MSG_AUTH = 0x01;
    public static final byte LABEL_RTP_SALT = 0x02;
    public static final byte LABEL_RTCP_ENCRYPTION = 0x03;
    public static final byte LABEL_RTCP_MSG_AUTH = 0x04;
    public static final byte LABEL_RTCP_SALT = 0x05;


    private final byte[] masterKey;

    private final byte[] masterSalt;

    /**
     * 存储用于加密操作的初始向量（IV）。
     * IV 是在加密过程中用于保障数据唯一性的重要参数，其长度为 16 字节。
     * 该变量为不可变属性，主要用于 SRTP（安全实时传输协议）密钥派生操作。
     */
    private final byte[] ivStore = new byte[16];

    /**
     * 表示 SRTP (安全实时传输协议) 的配置类型变量。
     * 该变量保存了一个 {@link SrtpProfilesType} 枚举实例，用于定义当前 SRTP 配置的详细信息。
     * 通过此变量，SRTP 配置可以实现对加密、认证和关联参数的统一管理。
     * <p>
     * 在 SRTP 密钥派生功能中，{@link SrtpProfilesType} 的信息决定了密钥的长度、
     * 加密算法和认证算法的选择等核心配置。
     */
    private final SrtpProfilesType srtpProfilesType;

    /**
     * 表示一个用于加密操作的不可变 Cipher 实例。
     * 该变量在 SRTP 密钥派生过程中用于实现加解密功能。
     * <p>
     * 特性：
     * - 使用 javax.crypto.Cipher 提供的加密实现。
     * - 配合密钥和初始化向量，执行对称加密操作。
     * <p>
     * 注意：
     * 该变量被声明为 final，因此一旦初始化，无法被重新赋值。
     * 它与 SRTP 密钥派生函数的具体实现高度相关。
     */
    private final Cipher cipher;
    /**
     * 表示一个用于加密操作的密钥规范对象。
     * <p>
     * secretKeySpec 是基于提供的主密钥和密钥派生函数所生成的密钥规范。
     * 该对象用于支持 SRTP (Secure Real-time Transport Protocol) 中的密钥派生和加密操作。
     * 它结合了加密算法的信息以及生成的密钥数据，用于初始化加密组件（如 Cipher）。
     * <p>
     * 类 SrtpKeyDerivationFunction 中的主要方法通过 secretKeySpec 来生成不同类型的会话密钥（如加密密钥、认证密钥和盐值密钥）。
     */
    private final SecretKeySpec secretKeySpec;

    /**
     * 表示用于 SRTP RTP 加密会话的加密密钥。
     * <p>
     * 该密钥从主密钥和主盐值通过密钥派生函数生成，并在 SRTP 的加密过程中使用。
     * 其长度和格式取决于所选的 SRTP 配置文件类型。
     * <p>
     * 主要用途包括：
     * 1. 派生会话加密密钥。
     * 2. 在加密 RTP 时，作为输入密钥用于加密算法（如 AES-CTR）。
     * <p>
     * 注意：在生成后，密钥通常不会直接暴露或修改。
     */
    private byte[] encKey;

    /**
     * 用于存储认证密钥的字节数组。
     * 该密钥在 SRTP 中用于 HMAC-SHA1 的消息认证码计算，保障数据的完整性和认证。
     * 在密钥派生期间，根据指定的 SRTP 配置和标签生成。
     */
    private byte[] authKey;

    /**
     * 用于存储 SRTP 会话中的加盐密钥（Salt Key）。
     * 加盐密钥是通过 SRTP 密钥派生函数从主密钥和主盐生成的。
     * 在 SRTP 协议中，加盐密钥用于生成会话标识符（Session Identifier）并确保消息的唯一性。
     * <p>
     * 该变量作为字节数组存储，长度取决于 SRTP 配置的加密算法和密钥长度。
     */
    private byte[] saltKey;


    public SrtpKeyDerivationFunction(byte[] masterKey, byte[] masterSalt, SrtpProfilesType srtpProfilesType) throws NoSuchPaddingException, NoSuchAlgorithmException {
        this.masterKey = masterKey;
        this.masterSalt = masterSalt;
        this.srtpProfilesType = srtpProfilesType;
        Provider provider = Security.getProvider("SunJCE");
        switch (srtpProfilesType) {
            case SRTP_AES128_CM_HMAC_SHA1_80 -> {
                secretKeySpec = new SecretKeySpec(masterKey, "AES");
                cipher = Cipher.getInstance("AES/CTR/NoPadding", provider);
            }
            default -> {
                secretKeySpec = null;
                cipher = null;
            }
        }

    }

    /**
     * 计算并生成 SRTP 加密密钥。
     *
     * @param label 用于密钥派生的标签，标识特定 SRTP 使用场景（如 RTP 或 RTCP 加密）。
     */
    public void calculationEncKey(byte label) {
        try {
            encKey = deriveSessionKey(srtpProfilesType.encKeyLength, label);
        } catch (GeneralSecurityException e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 计算并生成 SRTP 认证密钥（Auth Key）。
     *
     * @param label 用于密钥派生的标签，标识特定 SRTP 使用场景（如 RTP 或 RTCP 认证）。
     */
    public void calculationAuthKey(byte label) {
        try {
            authKey = deriveSessionKey(srtpProfilesType.authKeyLength, label);
        } catch (GeneralSecurityException e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 计算并生成 SRTP 的盐键（Salt Key）。
     *
     * @param label 用于密钥派生的标签，标识特定 SRTP 使用场景（如 RTP 或 RTCP）。
     */
    public void calculationSaltKey(byte label) {
        try {
            saltKey = deriveSessionKey(srtpProfilesType.saltKeyLength, label);
        } catch (GeneralSecurityException e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 根据指定的长度和标签派生会话密钥。
     *
     * @param length 会话密钥的长度，以字节为单位。
     * @param label 用于派生密钥的标签，标识特定 SRTP 使用场景（例如用于 RTP 或 RTCP）。
     * @return 派生的会话密钥。
     * @throws GeneralSecurityException 当密钥派生过程中出现加密操作错误时抛出。
     */
    private byte[] deriveSessionKey(int length, byte label) throws GeneralSecurityException {
        byte[] sessKey = new byte[length];
        System.arraycopy(masterSalt, 0, ivStore, 0, masterSalt.length);
        ivStore[7] ^= label;
        for (int i = masterSalt.length; i < ivStore.length; i++) {
            ivStore[i] = 0;
        }
        Arrays.fill(sessKey, (byte) 0);
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new IvParameterSpec(ivStore));
        cipher.doFinal(sessKey, 0, length, sessKey, 0);
        return sessKey;
    }


}
