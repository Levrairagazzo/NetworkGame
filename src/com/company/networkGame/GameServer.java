package com.company.networkGame;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class GameServer {
    private ServerSocket ss;
    private int numPlayers;
    private ServerSideConnection player1;
    private ServerSideConnection player2;
    private int turnsMade, maxTurns;
    private int[] values;
    private int player1ButtonNum, player2ButtonNum;

    public GameServer() {
        System.out.println("--------Game Server-------");
        numPlayers = 0;
        turnsMade = 0;
        maxTurns = 4;
        values = new int[4];

        for (int i = 0; i < values.length; i++) {
            values[i] = (int)Math.ceil(Math.random()*100);
            System.out.println("Value #" + (i+1) + " is " + values[i]);
        }
        try {
            ss = new ServerSocket(5001);
        } catch (IOException e) {
            System.out.println("IOExcepption from GameServer Constructor");
        }
    }

    public void acceptConnections() {
        try {
            System.out.println("Waiting for connections");
            while (numPlayers < 2) {
                Socket s = ss.accept();
                numPlayers++;
                System.out.println("Player#" + numPlayers + " has connected.");
                ServerSideConnection ssc = new ServerSideConnection(s, numPlayers);
                if (numPlayers == 1 ) {
                    player1 = ssc;
                }else {
                    player2 = ssc;
                }
                Thread t = new Thread(ssc);
                t.start();
            }
            System.out.println("2 players now. No longer accepting connections");
        } catch (IOException e) {
            System.out.println("Error from acceptConnections()");
        }
    }

    private class ServerSideConnection implements Runnable {
        private Socket socket;
        private DataOutputStream dataOut;
        private DataInputStream dataIn;
        private int playerID;

        public ServerSideConnection(Socket s, int id){
            socket = s;
            playerID = id;
            try {
                dataIn = new DataInputStream(socket.getInputStream());
                dataOut = new DataOutputStream(socket.getOutputStream());


            } catch (IOException e) {
                System.out.println("Error from ServerSideConnection constructor.");
            }
        }

        @Override
        public void run() {
            try{
                dataOut.writeInt(playerID);
                dataOut.writeInt(maxTurns);
                dataOut.writeInt(values[0]);
                dataOut.writeInt(values[1]);
                dataOut.writeInt(values[2]);
                dataOut.writeInt(values[3]);

                dataOut.flush();
                while (true){
                    if (playerID == 1){
                        player1ButtonNum = dataIn.readInt();
                        System.out.println("Player 1 clicked button #" + player1ButtonNum);
                        player2.sendButtonNumFromServer(player1ButtonNum);
                    }else{
                        player2ButtonNum = dataIn.readInt();
                        System.out.println("Player 2 clicked button#" + player2ButtonNum);
                        player1.sendButtonNumFromServer(player2ButtonNum);
                    }
                    turnsMade++;
                    if(turnsMade == maxTurns){
                        System.out.println("Max turns has been reached.");
                        break;
                    }
                }
                player1.closeServerConnection();
                player2.closeServerConnection();
            } catch (IOException e) {
                System.out.println("Error from run method in ServerSideConnection.");
            }
        }

        public void sendButtonNumFromServer(int n){
            try{
                dataOut.writeInt(n);
                dataOut.flush();
            } catch (IOException e) {
                System.out.println("Error in sendButtonNum on serverside");
            }
        }

        public void closeServerConnection(){
            try {
                socket.close();
                System.out.println("------Connection closed ----- ");

            }catch (IOException e){
                System.out.println("IOException from closeServerConnection() SSC");
            }
        }

    }

    public static void main(String[] args) {
        GameServer gs = new GameServer();
        gs.acceptConnections();
    }
}
