package ru.violence.currenttime.core.network;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import ru.violence.coreapi.common.messaging.ChannelActiveListener;
import ru.violence.coreapi.common.messaging.ReceivedMessage;
import ru.violence.currenttime.common.network.PacketType;
import ru.violence.currenttime.core.database.SQLite;
import ru.violence.currenttime.core.model.CapturedTime;

public class ChannelListener implements ChannelActiveListener {
    @Override
    public byte[] onMessageReceived(ReceivedMessage message) throws Exception {
        ByteArrayDataInput input = ByteStreams.newDataInput(message.getData());
        ByteArrayDataOutput output = ByteStreams.newDataOutput();

        int packetId = input.readInt();
        PacketType packet = PacketType.fromId(packetId);

        switch (packet) {
            case CREATE: {
                int userId = input.readInt();
                long time = input.readLong();

                String code = SQLite.insertTime(userId, time);

                output.writeUTF(code);
                break;
            }
            case GET: {
                String code = input.readUTF();

                CapturedTime time = SQLite.getTimeByCode(code);

                output.writeBoolean(time != null);
                if (time != null) {
                    output.writeInt(time.getCreatorId());
                    output.writeLong(time.getTimeSeconds());
                }
                break;
            }
            default:
                throw new IllegalStateException("Unexpected value: " + packet);
        }

        return output.toByteArray();
    }
}
