package com.swebench;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swebench.model.PRData;
import com.swebench.model.AgentResult;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class JavaSWEBenchTrajectory {
    private static final Logger logger = LogManager.getLogger(JavaSWEBenchTrajectory.class);

    private final String repoUrl;
    private final String prUrl;
    private final String trainerEmail;
    private final Path baseDir;
    private final Path artifactsDir;
    private PRData prData;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JavaSWEBenchTrajectory(String repoUrl, String prUrl, String trainerEmail) {
        this.repoUrl = repoUrl;
        this.prUrl = prUrl;
        this.trainerEmail = trainerEmail;
        this.baseDir = Paths.get(System.getProperty("user.home"), "swe_bench",
                repoUrl.substring(repoUrl.lastIndexOf('/') + 1));
        this.artifactsDir = baseDir.resolve("artifacts");

        try {
            Files.createDirectories(artifactsDir);
            logger.info("Created artifacts directory at {}", artifactsDir);
        } catch (IOException e) {
            logger.error("Failed to create artifacts directory", e);
        }
    }

    public boolean setupEnvironment() throws Exception {
        logger.info("Setting up environment for {}", repoUrl);
        this.prData = fetchPRData();

        Git git = setupRepository();
        if (git == null) return false;

        checkoutBaseCommit(git);
        return runTests();
    }

    private PRData fetchPRData() throws IOException {
        String apiUrl = prUrl.replace("github.com", "api.github.com/repos")
                .replace("pull", "pulls");

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(apiUrl);
            request.addHeader("Accept", "application/vnd.github.v3+json");

            return client.execute(request, response -> {
                String json = EntityUtils.toString(response.getEntity());
                return objectMapper.readValue(json, PRData.class);
            });
        }
    }

    private Git setupRepository() {
        try {
            if (!Files.exists(baseDir.resolve(".git"))) {
                logger.info("Cloning repository from {}", repoUrl);
                return Git.cloneRepository()
                        .setURI(repoUrl)
                        .setDirectory(baseDir.toFile())
                        .call();
            }
            return Git.open(baseDir.toFile());
        } catch (GitAPIException | IOException e) {
            logger.error("Repository setup failed", e);
            return null;
        }
    }

    private void checkoutBaseCommit(Git git) throws GitAPIException {
        logger.info("Checking out base commit: {}", prData.getBase().getSha());
        git.checkout()
                .setName(prData.getBase().getSha())
                .call();
    }

    private boolean runTests() {
        // Implementation from previous example
        return true;
    }

    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("Usage: java -jar java-swe-bench.jar <repo_url> <pr_url> <trainer_email>");
            System.exit(1);
        }

        try {
            JavaSWEBenchTrajectory runner = new JavaSWEBenchTrajectory(args[0], args[1], args[2]);
            if (runner.setupEnvironment()) {
                AgentRunner agent = new AgentRunner();
                AgentResult result = agent.run(runner.generateTaskPrompt());
                runner.collectArtifacts(result);
            }
        } catch (Exception e) {
            logger.error("Execution failed", e);
            System.exit(1);
        }
    }
}