package org.keycloak.services.resources;

import org.keycloak.Config.Scope;
import org.keycloak.services.util.JsonConfigProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CommandPoller {

    private static ObjectMapper MAPPER = new ObjectMapper();

    private WatchService watchService;
    private AtomicBoolean closed = new AtomicBoolean();

    private Path dir;

    public CommandPoller(Path dir) throws IOException {
        this.dir = dir;
        watchService = FileSystems.getDefault().newWatchService();
        dir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
    }

    public void poll(KeycloakApplication application) throws InterruptedException, IOException {
        WatchKey key;
        Files.list(dir).forEach(path -> runCommand(application, path));
        try {
            while ((key = watchService.take()) != null) {
                for (WatchEvent<?> event : key.pollEvents()) {
                    Path path = (Path) event.context();
                    runCommand(application, path);
                }
                key.reset();
            }
        } catch (ClosedWatchServiceException e) {
            if (!closed.get()) {
                throw e;
            }
        }
    }

    private void runCommand(KeycloakApplication application, Path path) {
        if (!Files.exists(path) || Files.isDirectory(path) || !path.getFileName().toString().endsWith(".in")) {
            return;
        }
        Exception ex = null;
        try {
            JsonNode command = MAPPER.readTree(new FileInputStream(path.toFile()));
            String type = Optional.ofNullable(command.get("command")).map(JsonNode::asText).orElse(null);
            Scope scope = new JsonConfigProvider(command, new Properties()).scope("arguments");
            // could also map to typed command objects to avoid if/else
            if (type.equals("export")) {
                application.export(scope);
            }
        } catch (Exception e) {
            ex = e;
        }
        try {
            Files.delete(path);
        } catch (IOException e) {
            // ignore
        }
        String fileName = path.getFileName().toString();
        Path out = path.resolveSibling(fileName.substring(0, fileName.length() - 3) + ".out");
        // write a result file
    }

    public void close() {
        closed.set(true);
        try {
            this.watchService.close();
        } catch (IOException e) {
        }
    }

}
