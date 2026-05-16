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
 package cn.duxinglan.media.impl;


 import cn.duxinglan.ice.IceEndpoint;
 import cn.duxinglan.media.core.MediaSession;
 import cn.duxinglan.media.impl.dtls.DTLSKeyMaterial;
 import cn.duxinglan.media.impl.dtls.DtlsCertificateGenerator;
 import lombok.Data;
 import lombok.extern.slf4j.Slf4j;

 @Slf4j
 @Data
 public class TransportContext {

     private final MediaSession session;

     private IceEndpoint local;

     private IceEndpoint remote;

     private DTLSKeyMaterial dtlsKeyMaterial;

     public TransportContext(MediaSession session) throws Exception {
         this.session = session;
         this.dtlsKeyMaterial = DtlsCertificateGenerator.generateDTLSKeyMaterial();
     }
 }
