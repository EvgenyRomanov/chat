package com.javarush.task.task30.task3008;

import java.io.IOException;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server {

    private static Map<String, Connection> connectionMap = new ConcurrentHashMap<>();

    public static void sendBroadcastMessage(Message message){
        for (Map.Entry<String, Connection> x : connectionMap.entrySet()){
            try {
                x.getValue().send(message);
            } catch (IOException e){
                ConsoleHelper.writeMessage("Ошибка!");
            }

        }
    }

    public static void main(String[] args) throws IOException {
        int serverPort = ConsoleHelper.readInt();
        ServerSocket serverSocket = new ServerSocket(serverPort); // серверсокет прослушивает определенный порт
        Socket clientSocket;
        System.out.println("Сервер запущен");
        try{
            while (true){
                clientSocket = serverSocket.accept(); // accept() будет ждать, пока кто-нибудь не захочет подключиться
                Handler handler = new Handler(clientSocket);
                handler.start();
            }
        } catch (Exception e){
            serverSocket.close();
            e.printStackTrace();
        }
    }


    private static class Handler extends Thread{
        private Socket socket;

        public Handler(Socket socket) {
            this.socket = socket;
        }

        private String serverHandshake(Connection connection) throws IOException, ClassNotFoundException{
            while (true){
                connection.send(new Message(MessageType.NAME_REQUEST));
                Message nameReceive = connection.receive();
                if (nameReceive.getType() == MessageType.USER_NAME){
                    String name = nameReceive.getData();
                    if (name != null && !name.isEmpty()){
                        if (!connectionMap.containsKey(name)){
                            connectionMap.put(name, connection);
                            connection.send(new Message(MessageType.NAME_ACCEPTED));
                            return name;
                        }
                    }
                }
            }
        }

        private void notifyUsers(Connection connection, String userName) throws IOException{
            for (Map.Entry<String, Connection> x : connectionMap.entrySet()){
                String name = x.getKey();
                if (!name.equals(userName)){
                    connection.send(new Message(MessageType.USER_ADDED, name));
                }
            }
        }

        private void serverMainLoop(Connection connection, String userName) throws IOException, ClassNotFoundException{
            while (true){
                Message message = connection.receive();
                String data;
                Message newMessage;
                if (message.getType() == MessageType.TEXT){
                    data = String.format("%s: %s", userName, message.getData());
                    newMessage = new Message(MessageType.TEXT, data);
                    sendBroadcastMessage(newMessage);
                } else {
                    ConsoleHelper.writeMessage("Ошибка!");
                }
            }
        }

        @Override
        public void run(){
            ConsoleHelper.writeMessage(String.format("Установлено новое соединение с удаленным адресом: %s",
                    socket.getRemoteSocketAddress()));
            String name = "";
            try (Connection connection = new Connection(socket)){
                name = serverHandshake(connection);
                sendBroadcastMessage(new Message(MessageType.USER_ADDED, name));
                notifyUsers(connection, name);
                serverMainLoop(connection, name);
            } catch (IOException | ClassNotFoundException e){
                ConsoleHelper.writeMessage(String.format("Произошла ошибка при обмене данными с удаленным адресом: %s",
                        socket.getRemoteSocketAddress()));

            }
            connectionMap.remove(name);
            sendBroadcastMessage(new Message(MessageType.USER_REMOVED, name));
            ConsoleHelper.writeMessage(String.format("Соединение с удаленным адресом %s закрыто",
                    socket.getRemoteSocketAddress()));

        }
    }
}
