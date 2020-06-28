package messages;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.logging.Logger;

public class MQSender extends MQConnector {
    private static final Logger LOG = Logger.getLogger(MQSender.class.getName());

    public MQSender(String queueName) {
        super(queueName);

        // Only start the MQ producer if there is a consumer waiting.

        if (!getChannel().isOpen()) {
            LOG.warning("Channel is not open.");
        } else {


            // Reset message should be the first message to synchronize any consumers
            // Reset messages do not need a body
            Message.createAndSendResetMessage(this);
        }
    }


    public synchronized void sendMessage(Message message) {
        // Fail Silently if the connection is closed.
        if (!getChannel().isOpen())
            return;

        try {
            AMQP.BasicProperties locationProps = new AMQP.BasicProperties.Builder().contentType(message.getContentType()).build();
            getChannel().basicPublish("", getQueueName(), locationProps, message.getMessageBody().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        // Delete the queue after we are done
        try {
            getChannel().queueDelete(getQueueName());
        } catch (IOException e) {
            LOG.warning("Failed to delete queue.");
            e.printStackTrace();
        }
        super.close();
    }

}
