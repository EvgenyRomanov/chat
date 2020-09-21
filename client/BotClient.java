package com.javarush.task.task30.task3008.client;

import com.javarush.task.task30.task3008.ConsoleHelper;
import com.javarush.task.task30.task3008.Message;
import com.javarush.task.task30.task3008.MessageType;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class BotClient extends Client {

    public class BotSocketThread extends SocketThread{
        @Override
        protected void clientMainLoop() throws IOException, ClassNotFoundException {
            String text = "Привет чатику. Я бот. Понимаю команды: дата, день, месяц, год, время, час, минуты, секунды.";
            sendTextMessage(text);
            super.clientMainLoop();
        }

        @Override
        protected void processIncomingMessage(String message) {
            ConsoleHelper.writeMessage(message);
            if (message.contains(":")){
                String name = message.split(":")[0].trim();
                String text = message.split(":")[1].trim();
                Calendar calendar = Calendar.getInstance();
                SimpleDateFormat simpleDateFormat = null;
                switch (text){
                    case "дата":
                        simpleDateFormat = new SimpleDateFormat("d.MM.YYYY");
                        break;
                    case "день":
                        simpleDateFormat = new SimpleDateFormat("d");
                        break;
                    case "месяц":
                        simpleDateFormat = new SimpleDateFormat("MMMM");
                        break;
                    case "год":
                        simpleDateFormat = new SimpleDateFormat("YYYY");
                        break;
                    case "время":
                        simpleDateFormat = new SimpleDateFormat("H:mm:ss");
                        break;
                    case "час":
                        simpleDateFormat = new SimpleDateFormat("H");
                        break;
                    case "минуты":
                        simpleDateFormat = new SimpleDateFormat("m");
                        break;
                    case "секунды":
                        simpleDateFormat = new SimpleDateFormat("s");
                        break;
                    default:
                        return;
                }
                calendar.setTimeZone(calendar.getTimeZone());
                String response = String.format("Информация для %s: %s", name, simpleDateFormat.format(calendar.getTime()));
                sendTextMessage(response);
            }
        }
    }

    @Override
    protected boolean shouldSendTextFromConsole() {
        return false;
    }

    @Override
    protected SocketThread getSocketThread() {
        return new BotSocketThread();
    }

    @Override
    protected String getUserName() {
        return String.format("date_bot_%s", (int) (Math.random() * 100));
    }

    public static void main(String[] args) {
        BotClient botClient = new BotClient();
        botClient.run();
    }
}
