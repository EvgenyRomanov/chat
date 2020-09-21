package com.javarush.task.task30.task3008.client;

import com.javarush.task.task30.task3008.*;
import java.io.IOException;
import java.net.Socket;

public class Client{

    protected Connection connection;

    private volatile boolean clientConnected = false;

    public static void main(String[] args){
        Client client = new Client();
        client.run();
    }

    public class SocketThread extends Thread{
        protected void processIncomingMessage(String message){
            ConsoleHelper.writeMessage(message);
        }

        protected void informAboutAddingNewUser(String userName){
            ConsoleHelper.writeMessage(String.format("Пользователь %s подключился к чату.", userName));
        }

        protected void informAboutDeletingNewUser(String userName){
            ConsoleHelper.writeMessage(String.format("Пользователь %s покинул чат.", userName));
        }

        protected void notifyConnectionStatusChanged(boolean clientConnected){
            Client.this.clientConnected = clientConnected;
            synchronized (Client.this){
                Client.this.notify();
            }
        }

        protected void clientHandshake() throws IOException, ClassNotFoundException{
            while (true){
                Message message = connection.receive();
                if (message.getType() == MessageType.NAME_REQUEST){
                    String name = getUserName();
                    connection.send(new Message(MessageType.USER_NAME, name));
                } else if (message.getType() == MessageType.NAME_ACCEPTED){
                    notifyConnectionStatusChanged(true);
                    return;
                } else {
                    throw new IOException("Unexpected MessageType");
                }
            }
        }

        protected void clientMainLoop() throws IOException, ClassNotFoundException{
            while (true){
                Message message = connection.receive();
                if (message.getType() == MessageType.TEXT){
                    processIncomingMessage(message.getData());
                } else if (message.getType() == MessageType.USER_ADDED){
                    informAboutAddingNewUser(message.getData());
                } else if (message.getType() == MessageType.USER_REMOVED){
                    informAboutDeletingNewUser(message.getData());
                } else {
                    throw new IOException("Unexpected MessageType");
                }
            }
        }

        @Override
        public void run(){
            String address = getServerAddress();
            int port = getServerPort();
            Socket socket = null;
            try {
                socket = new Socket(address, port);
                connection = new Connection(socket);
                clientHandshake();
                clientMainLoop();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                notifyConnectionStatusChanged(false);
            }
        }
    }

    public void run(){
        SocketThread socketThread = getSocketThread();
        socketThread.setDaemon(true);
        socketThread.start();
        synchronized (this){
            try {
                wait();
            } catch (InterruptedException e) {
                ConsoleHelper.writeMessage("Ошибка!");
            }
        }
        if (clientConnected){
            ConsoleHelper.writeMessage("Соединение установлено.\n" +
                    "Для выхода наберите команду 'exit'.");
        } else {
            ConsoleHelper.writeMessage("Произошла ошибка во время работы клиента.");
        }
        String text;
        while (clientConnected && !(text = ConsoleHelper.readString()).equals("exit")){
            if (shouldSendTextFromConsole()){
                sendTextMessage(text);
            }
        }
    }

    protected String getServerAddress(){
        return ConsoleHelper.readString();
    }

    protected int getServerPort(){
        return ConsoleHelper.readInt();
    }

    protected String getUserName(){
        return ConsoleHelper.readString();
    }

    protected boolean shouldSendTextFromConsole(){
        return true;
    }

    protected SocketThread getSocketThread(){
        return new SocketThread();
    }

    protected void sendTextMessage(String text){
        try {
            connection.send(new Message(MessageType.TEXT, text));
        } catch (IOException e){
            ConsoleHelper.writeMessage("Ошибка отправки сообщения!");
            clientConnected = false;
        }

    }

}
