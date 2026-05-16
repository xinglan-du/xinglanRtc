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
 package cn.duxinglan.media.transport.nio.webrtc;

 import cn.duxinglan.media.impl.dtls.SrtpProfilesType;
 import lombok.Data;
 import lombok.extern.slf4j.Slf4j;

 import javax.crypto.Cipher;
 import javax.crypto.NoSuchPaddingException;
 import java.security.InvalidKeyException;
 import java.security.NoSuchAlgorithmException;
 import java.util.Arrays;
 import java.util.Map;
 import java.util.concurrent.ConcurrentHashMap;

 @Slf4j
 @Data
 public class SrtpContextFactory {

     /**
      * 存储 SRTP 客户端上下文 (SRtpContext) 的映射关系。
      * 键为 SSRC（同步源标识符），值为对应的 SRtpContext 实例。
      * <p>
      * 该映射用于管理多个 SRTP 客户端的会话上下文，通过 SSRC 快速检索对应的上下文实例。
      * 在处理加密和解密过程时，基于 SSRC 确定所需的加密配置和状态。
      */
     private Map<Long, SRtpContext> srtpClientContextMap = new ConcurrentHashMap<>();
     /**
      * 存储服务器端 SRTP 上下文的映射表。
      * 键为 SSRC（同步源标识符），值为对应的 {@link SRtpContext} 实例，提供基于 SRTP 协议的加密与认证功能。
      * <p>
      * 此映射表用于管理服务器端的 SRTP 加密上下文，每个 SSRC 对应一个独立的加密会话。
      * 使用线程安全的 {@link ConcurrentHashMap} 实现，确保在多线程环境中对 SRTP 上下文的安全访问与操作。
      */
     private Map<Long, SRtpContext> srtpServerContextMap = new ConcurrentHashMap<>();


     /**
      * 用于存储与 SRTP（安全实时传输协议）的 SRtcpContext（安全 RTP 控制协议上下文）对象的映射关系。
      * 键为 SSRC（同步信源标识符，标识特定媒体流的唯一标记），值为 SRtcpContext 实例。
      * <p>
      * 该映射用于管理所有客户端相关的 SRtcpContext 实例，支持并发访问以保证线程安全。
      * SRtcpContext 提供了 SRTP 中 RTCP 包的加密、解密操作以及相关的状态管理功能。
      */
     private Map<Long, SRtcpContext> srtcpClientContextMap = new ConcurrentHashMap<>();

     /**
      * 用于存储和管理 SRTP 协议中服务器端的 SRtcpContext 映射关系。
      * 该映射关系将 SSRC（同步源标识符）与配套的 SRtcpContext 进行关联。
      * <p>
      * srtcpServerContextMap 的核心作用包括：
      * - 通过 SSRC 高效地查找对应的 SRtcpContext 实例，以支持 SRTP 中 RTCP 数据包的加密和解密操作。
      * - 提供线程安全的访问控制，确保并发环境下的正确性。
      * - 支持动态更新和管理，允许在运行时添加或移除特定 SSRC 及其上下文。
      */
     private Map<Long, SRtcpContext> srtcpServerContextMap = new ConcurrentHashMap<>();

     /**
      * 表示 SRTP (安全实时传输协议) 配置类型的变量。
      * <p>
      * 用于定义 SRTP 加密和验证过程中的各种参数设置，包括加密算法、密钥长度、盐值长度等。
      * 在类 {@code SrtpContextFactory} 中，该变量被用来初始化和指定 SRTP 的配置选项。
      * <p>
      * 不同的配置类型通过枚举 {@code SrtpProfilesType} 来描述，用于匹配不同的安全需求。
      * SRTP 的配置选项会直接影响对 RTP 和 RTCP 流的保护水平。
      */
     private SrtpProfilesType srtpProfilesType;

     /**
      * 客户端的主密钥，用于 SRTP（安全实时传输协议）的加密操作。
      * <p>
      * 该密钥在握手协议完成后生成，并分配给客户端，用于 SRTP 数据包的加密和解密过程。
      * 长度由所选的 SRTP 配置类型（SrtpProfilesType）决定，通常是 AES 加密算法所要求的密钥长度。
      * <p>
      * 配合客户端的盐值（clientMasterSalt）使用，以确保密钥的安全性和唯一性。
      */
     private byte[] clientMasterKey;
     /**
      * 客户端主盐值（Master Salt）的字节数组表示，用于 SRTP (安全实时传输协议) 密钥派生。
      * <p>
      * 此字段保存了客户端通信所需的主盐值，它与客户端密钥 (Master Key) 配合使用，用于生成加密密钥和认证密钥。
      * 主盐值是 SRTP 密钥管理的一部分，决定了 SRTP 数据流的加密和认证行为。
      * <p>
      * 在 SRTP 协议中，主盐值通过密钥派生函数 (Key Derivation Function)
      * 来生成最终加密以及认证的参数，以保护数据的机密性和完整性。
      */
     private byte[] clientMasterSalt;
     /**
      * 表示服务器端用于 SRTP (安全实时传输协议) 的主加密密钥。
      * <p>
      * serverMasterKey 是在 SRTP 会话中为服务端提供的对称密钥，通常通过 DTLS 握手阶段生成。
      * 该密钥与 serverMasterSalt 一起用于初始化服务器端的加密算法，用以保障 SRTP 数据包的保密性。
      * <p>
      * 注意：serverMasterKey 的长度由所选的 SRTP 配置类型 (SrtpProfilesType) 决定，
      * 不同配置类型可能要求不同的密钥长度。
      * <p>
      * 主要用途包括：
      * 1. 在 SRTP 数据加密上下文 (SRtpContext) 中配置服务端加密算法。
      * 2. 提供密钥材料来加密和解密服务器端的音视频媒体数据。
      * <p>
      * 该字段在会话期间需谨慎保护，防止泄露以保障传输安全。
      */
     private byte[] serverMasterKey;
     /**
      * 表示服务器用于 SRTP（安全实时传输协议）的主盐值。
      * <p>该字段用于为服务器端的 SRTP 会话生成加密数据包的唯一标识符或保护密钥。
      * SRTP 将主盐值与其他安全参数结合使用，以增强整体数据安全性，并防止重放攻击等潜在威胁。</p>
      * <p>
      * 声明：
      * - 数据类型为字节数组，存储原始的二进制盐值。
      * - 盐值的长度通常与 SRTP 配置类型（如 SrtpProfilesType）相关联。
      * <p>
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

     /**
      * 设置客户端加密密钥和盐值。
      * <p>
      * 该方法用于配置用于客户端 SRTP（安全实时传输协议）加密的主密钥和主盐值。
      *
      * @param masterKey  客户端加密的主密钥，作为对称加密的核心密钥。
      * @param masterSalt 客户端加密的主盐值，用于密钥派生和增加随机性。
      * @throws NoSuchPaddingException   如果初始化加密时指定的填充机制不可用。
      * @throws NoSuchAlgorithmException 如果指定的加密算法在环境中不可用。
      */
     public void setClientCipher(byte[] masterKey, byte[] masterSalt) throws NoSuchPaddingException, NoSuchAlgorithmException {
         this.clientMasterKey = masterKey;
         this.clientMasterSalt = masterSalt;
     }

     /**
      * 配置服务器端的加密密钥和盐值。
      * <p>
      * 该方法用于设置服务器 SRTP（安全实时传输协议）加密的主密钥和主盐值。
      *
      * @param masterKey  服务器加密的主密钥，作为对称加密的核心密钥。
      * @param masterSalt 服务器加密的主盐值，用于密钥派生和增加随机性。
      * @throws NoSuchPaddingException   如果初始化加密时指定的填充机制不可用。
      * @throws NoSuchAlgorithmException 如果指定的加密算法在环境中不可用。
      */
     public void setServerCipher(byte[] masterKey, byte[] masterSalt) throws NoSuchPaddingException, NoSuchAlgorithmException {
         this.serverMasterKey = masterKey;
         this.serverMasterSalt = masterSalt;
     }

     /**
      * 获取指定 SSRC 的客户端 SRtcpContext 对象。
      * 如果指定的 SSRC 对应的上下文不存在，将创建一个新的 SRtcpContext 并存储。
      *
      * @param ssrc 指定的同步源标识符（SSRC），用于标识实时通信会话中的数据流。
      * @return 对应于指定 SSRC 的客户端 SRtcpContext 对象，包含处理安全 RTP 流的上下文。
      */
     public SRtcpContext getClientSrtcpContext(long ssrc) {
         return srtcpClientContextMap.computeIfAbsent(
                 ssrc,
                 k -> {
                     try {
                         return new SRtcpContext(k, new SrtpKeyDerivationFunction(clientMasterKey, clientMasterSalt, this.srtpProfilesType), Cipher.DECRYPT_MODE, srtpProfilesType.rtcpAuthTagLength);
                     } catch (NoSuchPaddingException | NoSuchAlgorithmException e) {
                         throw new RuntimeException(e);
                     }
                 }
         );
     }

     /**
      * 获取指定 SSRC 的服务器 SRtcpContext 对象。
      * 如果指定的 SSRC 对应的上下文不存在，将创建一个新的 SRtcpContext 并存储。
      *
      * @param ssrc 指定的同步源标识符（SSRC），用于标识实时通信会话中的数据流。
      * @return 对应于指定 SSRC 的服务器 SRtcpContext 对象，包含处理安全 RTP 流的上下文。
      */
     public SRtcpContext getServerSRtcpContext(long ssrc) {
         return srtcpServerContextMap.computeIfAbsent(
                 ssrc,
                 k -> {
                     try {
                         return new SRtcpContext(k, new SrtpKeyDerivationFunction(serverMasterKey, serverMasterSalt, this.srtpProfilesType), Cipher.ENCRYPT_MODE, srtpProfilesType.rtcpAuthTagLength);
                     } catch (NoSuchPaddingException | NoSuchAlgorithmException e) {
                         throw new RuntimeException(e);
                     }
                 }
         );

     }

     /**
      * 获取指定 SSRC 的客户端 SRTP 上下文对象。
      * 如果指定的 SSRC 对应的上下文不存在，将创建一个新的 SRTP 上下文并存储。
      *
      * @param ssrc 指定的同步源标识符（SSRC），用于标识实时通信会话中的数据流。
      * @return 对应于指定 SSRC 的客户端 SRTP 上下文对象，包含处理安全 RTP 流的上下文。
      */
     public SRtpContext getClientSrtpContext(long ssrc) {
         return srtpClientContextMap.computeIfAbsent(
                 ssrc,
                 k -> {
                     try {
                         return new SRtpContext(k, new SrtpKeyDerivationFunction(clientMasterKey, clientMasterSalt, this.srtpProfilesType), Cipher.DECRYPT_MODE, srtpProfilesType.rtpAuthTagLength);
                     } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException e) {
                         throw new RuntimeException(e);
                     }
                 }
         );

     }

     /**
      * 获取指定 SSRC 的服务器 SRTP 上下文对象。
      * 如果指定的 SSRC 对应的上下文不存在，将创建一个新的 SRTP 上下文并存储。
      *
      * @param ssrc 指定的同步源标识符（SSRC），用于标识实时通信会话中的数据流。
      * @return 对应于指定 SSRC 的服务器 SRTP 上下文对象，包含处理安全 RTP 流的上下文。
      */
     public SRtpContext getServerSrtpContext(long ssrc) {
         return srtpServerContextMap.computeIfAbsent(
                 ssrc,
                 k -> {
                     try {
                         return new SRtpContext(ssrc, new SrtpKeyDerivationFunction(serverMasterKey, serverMasterSalt, this.srtpProfilesType), Cipher.ENCRYPT_MODE, srtpProfilesType.rtpAuthTagLength);
                     } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException e) {
                         throw new RuntimeException(e);
                     }
                 }
         );
     }


 }
