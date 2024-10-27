
package com.example.collabcode.service.strategy;

import com.example.collabcode.model.Code;
import com.example.collabcode.model.ExecResult;
import com.example.collabcode.service.util.ContainerManager;
import org.springframework.stereotype.Component;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Component("java")
public class JavaExecutor implements CodeExecutionStrategy {
    @Override
    public ExecResult execCode(Code code) {

        String containerName = code.getLang() + UUID.randomUUID();
        String dockerCommand = String.format("echo \"%s\" > Main.java && javac Main.java && timeout -s SIGKILL 10 java Main ; exit", code.getCode().replace("\"", "\\\""));
        ProcessBuilder pb = new ProcessBuilder()
                .command("docker", "run", "--rm", "--network", "default",
                        "--memory", "150m",  "--cpus", "0.5","java-container:dev", "sh", "-c", dockerCommand)
                .redirectErrorStream(true);
        ExecResult result = new ExecResult();
        return ContainerManager.initContainer(pb, containerName, result);
    }
}