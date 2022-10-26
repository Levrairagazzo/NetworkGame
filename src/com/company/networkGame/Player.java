package com.company.networkGame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Player extends JFrame {
    private int width;
    private int height;
    private Container contentPane;
    private JTextArea message;
    private JButton b1, b2, b3, b4;
    private ClientSideConnection csc;
    private int playerID, otherPLayerID;
    private int[] values;
    private int maxTurns;
    private int turnsMade;
    private int myPoints;
    private int enemyPoints;
    private boolean buttonsEnabled;


    public Player(int width, int height) {
        this.width = width;
        this.height = height;
        contentPane = this.getContentPane();
        message = new JTextArea();
        b1 = new JButton("1");
        b2 = new JButton("2");
        b3 = new JButton("3");
        b4 = new JButton("4");
        turnsMade = 0;
        myPoints = 0;
        enemyPoints = 0;
        values = new int[4];
    }

    public void setUpGui() {
        this.setSize(width, height);
        this.setTitle("Player #" + playerID);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        contentPane.setLayout(new GridLayout(1, 5));
        contentPane.add(message);
        message.setText("Creating a simple turn based game in Java.");
        message.setEditable(false);
        message.setWrapStyleWord(true);
        message.setLineWrap(true);
        contentPane.add(b1);
        contentPane.add(b2);
        contentPane.add(b3);
        contentPane.add(b4);

        if (playerID == 1) {
            message.setText("You are player #1, you go first.");
            otherPLayerID = 2;
            buttonsEnabled = true;
        } else {
            message.setText("You are player #2, wait for your turn.");
            otherPLayerID = 1;
            buttonsEnabled = false;
            Thread t = new Thread(() -> {
                updateTurn();
            });
            t.start();
        }
        toggleButtons();
        setVisible(true);

    }

    public void connectToServer() {
        csc = new ClientSideConnection();
    }

    public void setUpButtons() {
        ActionListener al = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JButton b = (JButton) e.getSource();
                int bNum = Integer.parseInt(b.getText());
                message.setText("You clicked button #" + bNum + ". Now wait for player#" + otherPLayerID);
                turnsMade++;
                System.out.println("Turns made: " + turnsMade);

                buttonsEnabled = false;
                toggleButtons();

                myPoints += values[bNum - 1];
                System.out.println("My points: " + myPoints);
                csc.sendButtonNumFromClient(bNum);

                if (playerID == 2 && turnsMade == maxTurns) {
                    checkWinner();
                } else {
                    //Goes into waiting mode
                    Thread T = new Thread(() -> updateTurn());
                    T.start();
                }


            }
        };

        b1.addActionListener(al);
        b2.addActionListener(al);
        b3.addActionListener(al);
        b4.addActionListener(al);
    }

    public void toggleButtons() {
        b1.setEnabled(buttonsEnabled);
        b2.setEnabled(buttonsEnabled);
        b3.setEnabled(buttonsEnabled);
        b4.setEnabled(buttonsEnabled);

    }


    public void updateTurn() {
        int n = csc.receiveButtonNum();
        System.out.println("The other player clicked button #" + n + ".It's now your turn. ");
        enemyPoints += values[n - 1];
        System.out.println("Your ennemy has " + enemyPoints + " points.");

        if (playerID == 1 && turnsMade == maxTurns) {
            checkWinner();
        } else {
            buttonsEnabled = true;
        }
        toggleButtons();
    }

    private void checkWinner() {
        buttonsEnabled = false;
        if (myPoints > enemyPoints) {
            message.setText("You won!\n" + "You: " + myPoints + "Enemy: " + enemyPoints);
        } else if (myPoints < enemyPoints) {
            message.setText("You lost!\n" + "You: " + myPoints + "Enemy: " + enemyPoints);
        } else {
            message.setText("It's a tie! You both got " + myPoints + " points.");
        }
        csc.closeConnection();
    }

    //Client connection
    private class ClientSideConnection {
        private Socket socket;
        private DataInputStream dataIn;
        private DataOutputStream dataOut;

        public ClientSideConnection() {
            System.out.println("------Client-----");
            try {
                socket = new Socket("localhost", 5001);
                dataIn = new DataInputStream(socket.getInputStream());
                dataOut = new DataOutputStream(socket.getOutputStream());
                playerID = dataIn.readInt();

                System.out.println("Connected to server as " + playerID + ".");

                maxTurns = dataIn.readInt() / 2;
                values[0] = dataIn.readInt();
                values[1] = dataIn.readInt();
                values[2] = dataIn.readInt();
                values[3] = dataIn.readInt();
                System.out.println("Max turn: " + maxTurns);
                System.out.println("Value #1 = " + values[0]);
                System.out.println("Value #2 = " + values[1]);
                System.out.println("Value #3 = " + values[2]);
                System.out.println("Value #4 = " + values[3]);


            } catch (IOException e) {
                System.out.println("Error from ClientSide Connection constructor");
            }
        }

        public void sendButtonNumFromClient(int n) {
            try {
                dataOut.writeInt(n);
                dataOut.flush();
            } catch (IOException e) {
                System.out.println("Error in sendButtonNum from player#" + playerID);
            }
        }

        public int receiveButtonNum() {
            int n = -1;
            try {
                n = dataIn.readInt();
                System.out.println("Player #" + otherPLayerID + " clicked button #" + n);
            } catch (IOException e) {
                System.out.println("Error from receiveButtonNum() csc");
            }
            return n;
        }

        public void closeConnection(){
            try {
                socket.close();
                System.out.println("-----CONNECTION CLOSED------");
            } catch (IOException e) {
                System.out.println("IOException on closeConnection csc");
            }
        }


    }





    public static void main(String[] args) {
        Player p = new Player(500, 100);
        p.connectToServer();
        p.setUpGui();
        p.setUpButtons();

    }

}
