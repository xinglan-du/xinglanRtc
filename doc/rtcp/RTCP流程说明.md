# RTCP流程说明

这些说明目前是基于chrome说明的。我还发现如果sdp中没有支持的数据chrome也会发送，可能是实现上的问题

## 支持的RTCP类型

目前星澜RTC已经实现的类型：

- SR (200): Sender Report，发送端统计信息（发送包/字节、NTP/RTP时间戳）。
- SDES (202): Source Description，源描述项（通常包含 CNAME）。
- RR (201): Receiver Report，接收端统计信息（丢包率、抖动、延迟估计等）。
- PSFB (206): Payload-specific Feedback，负载相关反馈（重点说明 PLI）。

## RTCP协议头与复合包

单个 RTCP 包的头部由 4 字节组成，字段含义如下：

- V (2 bits): 版本号，固定为 2。
- P (1 bit): Padding 标志，末尾是否有填充字节。
- RC (5 bits): Reception Report Count；在 SR/RR 中表示报告块数量；在 SDES 中表示源描述chunk数量。
- PT (8 bits): Packet Type，对应 `cn.duxinglan.media.protocol.rtcp.RtcpPayloadType` 的取值。
- Length (16 bits): 本 RTCP 包长度，单位为 32-bit words，且不包含头部自身的 1 个 word。计算公式：Length = (RTCP包总字节数 / 4) - 1。

RTCP 通常以 compound RTCP packet 发送，即将多个 RTCP 包按顺序拼接为一个 UDP 负载
Compound RTCP 必须以 SR 或 RR 开头（RFC 3550 强制要求）

- 常见以 SR+SDES 或 RR+PSFB 组合发送（本文只说明这两种组合）。
- Length 字段对每个包独立计算，接收端需要按顺序解析每个子包。



### [SR] + [SDES]

在 SRTCP 场景下，为了让接收端能识别 RTCP 包类型并做路由，
通常会保持 RTCP 的公共头部可见，其余部分可能被加密/保护；
具体哪些字节明文取决于实现与配置。

下面是一个未加密的示例
```
80 C8 00 06 11 22 33 44 E2 E3 E4 E5 E6 E7 E8 E9
00 11 22 33 00 00 30 39 00 0F 12 06 81 CA 00 09 
11 22 33 44 01 10 63 68 72 6F 6D 65 40 68 6F 73 
74 2D 31 32 33 34 02 0C 43 68 72 6F 6D 65 53 65
6E 64 65 72 00 00 00
```


复合包切分与类型解析入口在 `src/main/java/cn/duxinglan/media/protocol/srtcp/SRtcpFactory.java` 的 `packetsSRtcpToRtcp`。

## 发送与接收的RTCP组合（Chrome）

### 发送端 -> 接收端（SR + SDES）

发送端周期性发送 SR 与 SDES 组成的复合包，SR 在前，SDES 在后


### 接收端 -> 发送端（RR + PSFB）

接收端周期性发送 RR 与 PSFB 组成的复合包，RR 在前，PSFB 在后


## Chrome下的典型流程

1. 媒体会话建立后，双方开始互发 RTP。
2. 发送端周期性发送 SR+SDES 复合包。
3. 接收端周期性发送 RR+PSFB 复合包。
4. 关键帧请求以 PLI 为主；

## 数据示例（16进制）

以下为模拟数据的 16 进制示例，用于理解

### SR (200) 示例

```
80 C8 00 06 11 22 33 44 E2 E3 E4 E5 E6 E7 E8 E9
00 11 22 33 00 00 30 39 00 0F 12 06
```

字段提示：V=2 P=0 RC=0 PT=200 Len=6，SSRC=0x11223344，NTP=0xE2E3E4E5E6E7E8E9，RTP=0x00112233。

### SDES (202) 示例

```
81 CA 00 09 11 22 33 44 01 10 63 68 72 6F 6D 65
40 68 6F 73 74 2D 31 32 33 34 02 0C 43 68 72 6F
6D 65 53 65 6E 64 65 72 00 00 00
```

字段提示：V=2 P=0 RC=1 PT=202 Len=9，SSRC=0x11223344，CNAME="chrome@host-1234"，NAME="ChromeSender"。

SDES Item 列表结束后需补 0x00，整个 SDES 包需 32-bit（4字节） 对齐，不足部分用 0 填充，并计入 Length。

### RR (201) 示例

```
81 C9 00 07 55 66 77 88 11 22 33 44 02 00 00 05
00 02 A3 B1 00 00 00 12 7A 9B 3C 12 00 00 12 34
```

字段提示：V=2 P=0 RC=1 PT=201 Len=7，接收端SSRC=0x55667788，源SSRC=0x11223344，FractionLost=2，Lost=5，Jitter=18。

### PSFB (206) 示例（重点：PLI）

```
81 CE 00 02 55 66 77 88 11 22 33 44
```

字段提示：
* V=2 
* P=0 
* FMT=1(PLI) 其中fmt的子类型有很多 这里重点说明pli 其他的还没有实现
* PT=206 
* Len=2 
* 发送端SSRC=0x55667788 (Chrome通常是1)
* 媒体SSRC=0x11223344 (通常是你希望尽快发送关键帧的ssrc)

PLI 用于请求关键帧，建议优先实现与解析 PLI；对端收到 PLI 后应尽快发送 IDR 关键帧以恢复画面。



### PSFB 其他类型（补充说明）

PSFB (PT=206) 是一个大类，PLI/FIR/REMB 都属于它的不同 FMT/子类型

FIR FMT = 4
REMB FMT = 15

```
FIR (FMT=4): 强制请求关键帧，语义更强但当前 Chrome 不需要。
REMB (App-based): 码率上限建议，当前未实现。
```
