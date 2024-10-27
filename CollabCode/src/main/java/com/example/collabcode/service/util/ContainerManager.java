package com.example.collabcode.service.util;

import com.example.collabcode.model.ExecResult;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ContainerManager {
    private static final Lock containerLock = new ReentrantLock();

    public static String readOut(Process process, String containerName) {
        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            int maxOutputSize = 100000;// Max 100 KB
            while ((line = reader.readLine()) != null) {
                if (sb.length() + line.length() > maxOutputSize) {
                    terminateContainer(containerName);
                    sb.append("\nYour code generates output longer than allowed limit.");
                    break;
                }
                sb.append(line);
                sb.append("\n");
            }
            terminateContainer(containerName);
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error encountered during exection";
        }
    }

    public static void terminateContainer(String containerName) {

        try {
            ProcessBuilder pb = new ProcessBuilder()
                    .command("docker", "rm", "-f", containerName);
            Process p = pb.start();
            p.destroy();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static ExecResult initContainer(ProcessBuilder pb, String containerName, ExecResult result) {
        containerLock.lock();
        try {
            Process p = pb.start();
            long startTime = System.currentTimeMillis();
            String output = readOut(p, containerName);
            long endTime = System.currentTimeMillis();
            float time = (float) (endTime - startTime) / 1000;
            System.out.println("Executing command: " + String.join(" ", pb.command()));

            if (output.contains("Killed")) {
                result.setOut("Your code took too long to execute!");
                result.setTte(time);
            } else {
                result.setOut(output.trim());
                result.setTte(time);
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            result.setOut("Error encountered during execution");
            result.setTte(0.00F);
            return result;
        }finally {
            containerLock.unlock();
        }
    }
}
