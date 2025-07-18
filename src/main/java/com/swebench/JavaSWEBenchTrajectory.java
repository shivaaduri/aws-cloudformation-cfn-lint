package com.swebench;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swebench.model.PRData;
import com.swebench.model.AgentResult;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class JavaSWEBenchTrajectory {
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
            System.out.println("Created artifacts directory at " + artifactsDir);
        } catch (IOException e) {
            System.err.println("Failed to create artifacts directory: " + e.getMessage());
        }
    }

    public boolean setupEnvironment() {
        System.out.println("Setting up environment for " + repoUrl);

        try {
            this.prData = fetchPRData();
            String baseCommit = prData.getBase().getSha();

            Git git = setupRepository();
            if (git == null) return false;

            checkoutBaseCommit(git, baseCommit);
            return runTests();
        } catch (Exception e) {
            System.err.println("Environment setup failed: " + e.getMessage());
            return false;
        }
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

    private Git setupRepository() throws GitAPIException, IOException {
        if (!Files.exists(baseDir.resolve(".git"))) {
            System.out.println("Cloning repository from " + repoUrl);
            return Git.cloneRepository()
                    .setURI(repoUrl)
                    .setDirectory(baseDir.toFile())
                    .call();
        }
        return Git.open(baseDir.toFile());
    }

    private void checkoutBaseCommit(Git git, String commitHash) throws GitAPIException {
        System.out.println("Checking out base commit: " + commitHash);
        git.checkout()
                .setName(commitHash)
                .call();
    }

    private boolean runTests() {
        System.out.println("Running tests...");
        // Implement actual test execution based on build system
        return true; // Simulated success
    }

    public String generateTaskPrompt() {
        if (prData == null) {
            throw new IllegalStateException("PR data not initialized");
        }

        return String.format("""
            Fix the following issue in %s:
            
            %s
            
            Requirements:
            1. Make minimal necessary changes to Java files
            2. Maintain backward compatibility
            3. Include appropriate unit tests
            4. Follow existing code style
            """,
                repoUrl.substring(repoUrl.lastIndexOf('/') + 1),
                prData.getBody() != null ? prData.getBody() : "No description available");
    }

    public void collectArtifacts(AgentResult result) {
        try {
            // Save metadata
            String metadata = String.format("""
                {
                    "repository": "%s",
                    "pr": "%s",
                    "trainer": "%s",
                    "status": "%s",
                    "timestamp": "%s"
                }
                """,
                    repoUrl, prUrl, trainerEmail,
                    result.isSolved() ? "PASS" : "FAIL",
                    System.currentTimeMillis());

            Files.writeString(artifactsDir.resolve("metadata.json"), metadata);

            // Save patches
         /*   if (result.getPatches() != null && !result.getPatches().isEmpty()) {
                Files.write(artifactsDir.resolve("changes.patch"),
                        result.getPatches().getBytes());
            }
*/
            // Save conversation log
            if (result.getConversationLog() != null) {
                Files.write(artifactsDir.resolve("conversation.log"),
                        result.getConversationLog());
            }

            System.out.println("Artifacts saved to " + artifactsDir);
        } catch (IOException e) {
            System.err.println("Failed to save artifacts: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("Usage: <repo_url> <pr_url> <trainer_email>");
            System.exit(1);
        }

        try {
            JavaSWEBenchTrajectory runner = new JavaSWEBenchTrajectory(
                    args[0], args[1], args[2]);

            if (runner.setupEnvironment()) {
                AgentRunner agent = new AgentRunner();
                String prompt = runner.generateTaskPrompt();
                System.out.println("Generated prompt:\n" + prompt);

                AgentResult result = agent.run(prompt);
                runner.collectArtifacts(result);

                System.out.println("Execution completed with status: " +
                        (result.isSolved() ? "SUCCESS" : "FAILURE"));
            }
        } catch (Exception e) {
            System.err.println("Execution failed: ");
            e.printStackTrace();
            System.exit(1);
        }
    }
}