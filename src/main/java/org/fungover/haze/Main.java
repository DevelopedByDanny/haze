package org.fungover.haze;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.*;

public class Main {

    private static Logger logger = LogManager.getLogger(Main.class);

    public static void main(String[] args) throws IOException {
        Initialize initialize = new Initialize();
        initialize.importCliOptions(args);

        HazeDatabase hazeDatabase = new HazeDatabase();

        try (ServerSocket serverSocket = new ServerSocket()) {
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(initialize.getPort()));
            while (true) {
                var client = serverSocket.accept();
                logger.debug(String.valueOf(client));
                logger.info("Application started: serverSocket.accept()");

                Runnable newThread = () -> {
                    try {
                        BufferedReader input = new BufferedReader(new InputStreamReader(client.getInputStream()));

                        List<String> inputList = new ArrayList<>();

                        String firstReading = input.readLine();
                        readInputStream(input, inputList, firstReading);

                        client.getOutputStream().write(executeCommand(hazeDatabase, inputList).getBytes());

                        inputList.forEach(System.out::println); // For checking incoming message

                        printThreadDebug();

                        client.close();
                        logger.info("Client closed");

                    } catch (IOException e) {
                        logger.error(String.valueOf(e));
                    }
                };
                Thread.startVirtualThread(newThread);
            }
        } catch (IOException e) {
            logger.error(String.valueOf(e));
        }
    }

    private static void printThreadDebug() {
        logger.debug("ThreadID " + Thread.currentThread().threadId());  // Only for Debug
        logger.debug("Is virtual Thread " + Thread.currentThread().isVirtual()); // Only for Debug
    }

    public static String executeCommand(HazeDatabase hazeDatabase, List<String> inputList) {
        logger.debug("executeCommand: " + hazeDatabase + " " + inputList);
        String command = inputList.get(0).toUpperCase();

        return switch (command) {
            case "SETNX" -> hazeDatabase.setNX(inputList);
            case "DEL" -> hazeDatabase.delete(inputList.subList(1, inputList.size()));
            default -> "-ERR unknown command\r\n";
        };
    }


    private static void readInputStream(BufferedReader input, List<String> inputList, String firstReading) throws
            IOException {
        logger.debug("readInputStream: " + input + " " + inputList + " " + firstReading);
        int size;
        if (firstReading.startsWith("*")) {
            size = Integer.parseInt(firstReading.substring(1)) * 2;
            for (int i = 0; i < size; i++) {
                String temp = input.readLine();
                if (!temp.contains("$"))
                    inputList.add(temp);
            }
        } else {
            String[] seperated = firstReading.split("\\s");
            inputList.addAll(Arrays.asList(seperated));
        }
    }
}
