package echo.music.iad1tya.listentogether

import com.google.protobuf.ByteString
import echo.music.iad1tya.listentogether.proto.Listentogether
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.Assert.*
import org.junit.Test

class ListenTogetherCodecTest {
  @Test
  fun pingAndCaptureUseExistingMillisecondWireFields() {
    val timestamp = 1_800_000_000_001L
    for (format in MessageFormat.entries) {
      val codec = MessageCodec(format)
      val (_, ping) = codec.decode(codec.encode(MessageTypes.PING, PingPayload(timestamp, 38)))
      val action =
        PlaybackActionPayload(PlaybackActions.PLAY, position = 0, capturedAtServerTime = timestamp)
      val (_, play) = codec.decode(codec.encode(MessageTypes.PLAYBACK_ACTION, action))
      if (format == MessageFormat.PROTOBUF) {
        assertEquals(timestamp, Listentogether.PingPayload.parseFrom(ping).clientTime)
        assertEquals(38L, Listentogether.PingPayload.parseFrom(ping).sequence)
        assertEquals(
          timestamp,
          Listentogether.PlaybackActionPayload.parseFrom(play).capturedAtServerTime,
        )
      } else {
        val probe = Json.parseToJsonElement(ping.decodeToString()).jsonObject
        assertEquals(timestamp, probe.getValue("client_time").jsonPrimitive.long)
        assertEquals(38L, probe.getValue("sequence").jsonPrimitive.long)
        val payload = Json.parseToJsonElement(play.decodeToString()).jsonObject
        assertEquals(timestamp, payload.getValue("captured_at_server_time").jsonPrimitive.long)
      }
    }
  }

  @Test
  fun decodesTimestampedAndEmptyPongs() {
    val protobuf =
      Listentogether.PongPayload.newBuilder()
        .setSequence(8)
        .setClientTime(100)
        .setServerReceiveTime(900)
        .setServerSendTime(901)
        .build()
    val envelope =
      Listentogether.Envelope.newBuilder()
        .setType("pong")
        .setPayload(ByteString.copyFrom(protobuf.toByteArray()))
        .build()
        .toByteArray()
    val json =
      """{"type":"pong","payload":{"client_time":100,"sequence":8,"server_receive_time":900,"server_send_time":901}}"""
        .toByteArray()
    for (frame in listOf(json, envelope)) {
      val codec = MessageCodec()
      val (type, payload) = codec.decode(frame)
      assertEquals(
        PongPayload(8, 900, 901),
        codec.decodePayload(type, payload, MessageCodec.detectMessageFormat(frame)),
      )
    }
    for (format in MessageFormat.entries) {
      val codec = MessageCodec(format)
      val (type, payload) = codec.decode(codec.encode(MessageTypes.PONG, null))
      assertNull(codec.decodePayload(type, payload, format))
    }
  }
}
