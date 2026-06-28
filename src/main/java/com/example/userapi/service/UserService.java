package com.example.userapi.service;

import com.example.userapi.exception.UserNotFoundException;
import com.example.userapi.model.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private static final String SEED_RESOURCE = "users-seed.json";

    private final ObjectMapper objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private final Path dataFile;

    public UserService(@Value("${app.data-file:data/users.json}") String dataFilePath) {
        this.dataFile = Path.of(dataFilePath);
    }

    @PostConstruct
    void init() {
        lock.writeLock().lock();
        try {
            if (Files.notExists(dataFile)) {
                if (dataFile.getParent() != null) {
                    Files.createDirectories(dataFile.getParent());
                }
                List<User> seed = readSeed();
                writeToFile(seed);
                log.info("Seeded {} users into {}", seed.size(), dataFile.toAbsolutePath());
            } else {
                log.info("Using existing data file {}", dataFile.toAbsolutePath());
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to initialise user data file", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<User> getAllUsers() {
        lock.readLock().lock();
        try {
            return readFromFile();
        } finally {
            lock.readLock().unlock();
        }
    }

    public User getUserById(Long id) {
        lock.readLock().lock();
        try {
            return findById(readFromFile(), id)
                    .orElseThrow(() -> new UserNotFoundException(id));
        } finally {
            lock.readLock().unlock();
        }
    }

    public User createUser(User user) {
        lock.writeLock().lock();
        try {
            List<User> users = readFromFile();
            user.setId(nextId(users));
            users.add(user);
            writeToFile(users);
            return user;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public User updateUser(Long id, User updated) {
        lock.writeLock().lock();
        try {
            List<User> users = readFromFile();
            User existing = findById(users, id)
                    .orElseThrow(() -> new UserNotFoundException(id));

            existing.setName(updated.getName());
            existing.setUsername(updated.getUsername());
            existing.setEmail(updated.getEmail());
            existing.setPhone(updated.getPhone());
            existing.setWebsite(updated.getWebsite());

            writeToFile(users);
            return existing;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void deleteUser(Long id) {
        lock.writeLock().lock();
        try {
            List<User> users = readFromFile();
            boolean removed = users.removeIf(u -> u.getId().equals(id));
            if (!removed) {
                throw new UserNotFoundException(id);
            }
            writeToFile(users);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private Optional<User> findById(List<User> users, Long id) {
        return users.stream().filter(u -> u.getId().equals(id)).findFirst();
    }

    private long nextId(List<User> users) {
        return users.stream()
                .map(User::getId)
                .filter(java.util.Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(0L) + 1;
    }

    private List<User> readFromFile() {
        try {
            if (Files.notExists(dataFile)) {
                return new ArrayList<>();
            }
            byte[] bytes = Files.readAllBytes(dataFile);
            if (bytes.length == 0) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(bytes, new TypeReference<List<User>>() {});
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read user data file", e);
        }
    }

    private void writeToFile(List<User> users) {
        try {
            objectMapper.writeValue(dataFile.toFile(), users);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write user data file", e);
        }
    }

    private List<User> readSeed() throws IOException {
        ClassPathResource resource = new ClassPathResource(SEED_RESOURCE);
        try (InputStream in = resource.getInputStream()) {
            return objectMapper.readValue(in, new TypeReference<List<User>>() {});
        }
    }
}
