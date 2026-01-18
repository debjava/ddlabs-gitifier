package com.ddlab.rnd.util;

import com.ddlab.rnd.git.model.UserAccount;
import com.ddlab.rnd.handler.IGitHandler;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
public class GitUtil {

    public static void updateOnlineRepo(String projDirPath,IGitHandler gitHandler, String commitMsg) throws Exception {
        String gitUserName = gitHandler.getUserName();
        String gitTypeUserName = gitHandler.getGitType() + "_" + gitUserName;
        UserAccount userAct = gitHandler.getUserAccount();

        if (gitDirExists(projDirPath)) {
            // Add Multi remote origin
            // get the clone URl and add to it
            Map<String, String> gitUserRemoteMap = getUserAndCloneUrlMap(projDirPath);
            String cloneToUrl = null;
            if(!gitUserRemoteMap.containsKey(gitTypeUserName)) {
                cloneToUrl = gitHandler.getUrlToClone(new File(projDirPath).getName());
                log.debug("Git Remote clone URL: {}", cloneToUrl);
            }  else if (gitUserRemoteMap.containsKey(gitTypeUserName)) {
                // First get clone url by Git user name
                cloneToUrl = gitUserRemoteMap.get(gitTypeUserName);
                if(cloneToUrl == null) {
                    cloneToUrl = gitUserRemoteMap.get("origin");
                }
                log.debug("Git clone URL from local: {}", cloneToUrl);
            }
            log.debug("Git user final remote URL: {}", cloneToUrl);
//            addMultiRemoteOrigin(projDirPath, gitTypeUserName, cloneToUrl);
            addMultiRemote(projDirPath, gitTypeUserName, cloneToUrl);

            // Add all files
            addAllFiles(projDirPath);

            // Commit
            PersonIdent author = new PersonIdent(gitUserName, userAct.getUserName());
            commit(projDirPath, author, commitMsg);

            // Push
            String userToken = userAct.getToken();
            UsernamePasswordCredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider(gitUserName,
                    userToken);
            multiPushRepo(projDirPath, gitTypeUserName, credentialsProvider);
        }
    }

    public static void createOnlineRepo(String projDirPath, IGitHandler gitHandler, String repoDescription, String branchName) throws Exception {
        try {
            UserAccount userAct = gitHandler.getUserAccount();
            File projDirFile = new File(projDirPath);
            String repoName = projDirFile.getName();
//			// Create a Hosted Repository
            String remoteCloneUrl = gitHandler.getCloneUrlAfterRepoCreation(repoName, repoDescription);
            boolean repoExistFlag = GitUtil.gitDirExists(projDirPath);
            if (!repoExistFlag) {
                GitUtil.initializeRepo(projDirFile);
            }
            String gitUserName = gitHandler.getUserName();
            String gitTypeUserName = gitHandler.getGitType() + "_" + gitUserName;

            addMultiRemote(projDirPath, gitTypeUserName, remoteCloneUrl);
			// Add all files
            addAllFiles(projDirPath);
			// Commit
            PersonIdent author = new PersonIdent(gitUserName, userAct.getUserName());
            commit(projDirPath, author, "first commit");
            // Add a branch details
            addBranch(projDirPath, branchName);
            // Checkout main branch safely
            checkoutRepo(projDirPath);
            // Push
            UsernamePasswordCredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider(gitUserName,
					userAct.getToken());
            multiPushRepo(projDirPath, gitTypeUserName, credentialsProvider);
        } catch (Exception ex) {
            log.error("Exception while sharing the code in online repository: {\n}", ex);
			throw ex;
        }
    }

    public static void initializeRepo(File dir) throws GitAPIException {
        Git.init().setDirectory(dir).call();
        log.debug("Repository initialized ...");
    }

    @Deprecated
    public static void addRemoteOrigin(String repoDir, String remoteUrl) throws IOException {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        Repository repository = builder.setGitDir(new File(repoDir, ".git")).readEnvironment().findGitDir().build();
        StoredConfig config = repository.getConfig();
        config.setBoolean("http", null, "sslVerify", false);

        config.setString("remote", "origin", "url", remoteUrl);
        config.setString("remote", "origin", "fetch", "+refs/heads/*:refs/remotes/origin/*");
        config.save();
        repository.close();
        System.out.println("Remote origin added!");
    }

    public static void addMultiRemote(String repoDir, String gitTypeUserName, String remoteUrl)
            throws Exception {
        try (Repository repository = new FileRepositoryBuilder()
                .setGitDir(new File(repoDir, ".git"))
                .build()) {

            if (repository.getConfig().getSubsections("remote").contains(gitTypeUserName)) {
                log.debug("Remote already exists");
                // Do not add
            } else {
                RemoteConfig remoteConfig =
                        new RemoteConfig(repository.getConfig(), gitTypeUserName);

                remoteConfig.addURI(new URIish(remoteUrl));

                // Save to .git/config
                remoteConfig.update(repository.getConfig());
                repository.getConfig().save();
            }
        }
        log.debug("Multi Remote origin added ... ");

//        Repository repository = new FileRepositoryBuilder()
//                .setGitDir(new File(repoDir, ".git"))
//                .build();
//        if (repository.getConfig().getSubsections("remote").contains(gitTypeUserName)) {
//            log.debug("Remote already exists");
//            // Do not add
//        } else {
//            RemoteConfig remoteConfig =
//                    new RemoteConfig(repository.getConfig(), gitTypeUserName);
//
//            remoteConfig.addURI(new URIish(remoteUrl));
//
//            // Save to .git/config
//            remoteConfig.update(repository.getConfig());
//            repository.getConfig().save();
//        }
//        repository.close();
//        log.debug("Multi Remote origin added ... ");
    }

    @Deprecated
    public static void addMultiRemoteOrigin(String repoDir, String gitTypeUserName, String remoteUrl)
            throws IOException {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        Repository repository = builder.setGitDir(new File(repoDir, ".git")).readEnvironment().findGitDir().build();
        StoredConfig config = repository.getConfig();
        config.setBoolean("http", null, "sslVerify", false);

        config.setString("remote", "origin", "url", remoteUrl);

        config.setString("remote", gitTypeUserName, "url", remoteUrl);
//        config.setString("remote", gitTypeUserName, "fetch", "+refs/heads/*:refs/remotes/github/*");

//		config.setString("remote", "origin", "url", remoteUrl);
//		config.setString("remote", "origin", "fetch", "+refs/heads/*:refs/remotes/origin/*");

        config.save();
        repository.close();
        System.out.println("Remote origin added!");
    }

    public static void addAllFiles(String repoDir) throws IOException, GitAPIException {
        try (Git git = Git.open(new File(repoDir))) {
            git.add().addFilepattern(".").call();
            System.out.println("All files staged!");
        }
    }

    public static void commit(String repoDir, PersonIdent author, String commitMessage)
            throws IOException, GitAPIException {
        try (Git git = Git.open(new File(repoDir))) {
            git.commit().setMessage(commitMessage).setAuthor(author).setCommitter(author).call();
            System.out.println("Committed successfully!");
        }
    }

    public static void checkoutRepo(String repoDir) throws IOException, GitAPIException {
        try (Git git = Git.open(new File(repoDir))) {
            boolean mainExists = git.branchList().call().stream()
                    .anyMatch(ref -> ref.getName().equals("refs/heads/master"));

            if (mainExists) {
                git.checkout().setName("master").call();
            } else {
                git.checkout().setCreateBranch(true).setName("master")
                        .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK).call();
            }
            System.out.println("Checked out 'main' branch!");
        }
    }

//    public static void pushRepo(String repoDir, UsernamePasswordCredentialsProvider credentialsProvider)
//            throws IOException, GitAPIException {
//        try (Git git = Git.open(new File(repoDir))) {
//            git.push().setRemote("origin").add("main") // push local main branch
//                    .setCredentialsProvider(credentialsProvider).call();
//            System.out.println("Pushed successfully!");
//        }
//    }

    public static void multiPushRepo(String repoDir, String gitTypeUserName,
                                     UsernamePasswordCredentialsProvider credentialsProvider) throws IOException, GitAPIException {
        try (Git git = Git.open(new File(repoDir))) {
            git.push().setRemote(gitTypeUserName)
                    .setCredentialsProvider(credentialsProvider).call();
            log.debug("Code Pushed successfully!");
        }
    }

    @Deprecated
    public static void readGitConfig(String projDirPath) throws Exception {
        Repository repository = null;
        try {
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            repository = builder.setGitDir(new File(projDirPath + File.separator + ".git")).readEnvironment()
                    .findGitDir().build();
            StoredConfig config = repository.getConfig();

            // ---- User info ----
            String userName = config.getString("user", null, "name");
            String userEmail = config.getString("user", null, "email");

            System.out.println("User Name : " + userName);
            System.out.println("User Email: " + userEmail);

            // ---- Remote URLs ----
            Set<String> remotes = config.getSubsections("remote");
            for (String remote : remotes) {
                String url = config.getString("remote", remote, "url");
                System.out.println("Remote [" + remote + "] URL: " + url);
            }

        } catch (Exception ex) {
            log.error("Unable to read .git directory ...{}", ex);

        } finally {
            if (repository != null)
                repository.close();
        }
//        repository.close();
    }

    public static Map<String, String> getUserAndCloneUrlMap(String projDirPath) throws Exception {
        Map<String, String> userCloneUrlMap = new HashMap<>();
        File gitDir = new FileRepositoryBuilder().findGitDir(new File(projDirPath)).getGitDir();
        if (gitDir != null) {

        }
        try(Repository repository = new FileRepositoryBuilder().setGitDir(new File(projDirPath + File.separator + ".git")).readEnvironment()
                .findGitDir().build()) {
            StoredConfig config = repository.getConfig();

            Set<String> remotes = config.getSubsections("remote");
            for (String remote : remotes) {
                String url = config.getString("remote", remote, "url");
//					System.out.println("Remote [" + remote + "] URL: " + url);

                userCloneUrlMap.put(remote, url);
            }
        }





//        Repository repository = null;
//        try {
//            File gitDir = new FileRepositoryBuilder().findGitDir(new File(projDirPath)).getGitDir();
//            if (gitDir != null) {
//                FileRepositoryBuilder builder = new FileRepositoryBuilder();
//                repository = builder.setGitDir(new File(projDirPath + File.separator + ".git")).readEnvironment()
//                        .findGitDir().build();
//
//                StoredConfig config = repository.getConfig();
//
//                Set<String> remotes = config.getSubsections("remote");
//                for (String remote : remotes) {
//                    String url = config.getString("remote", remote, "url");
////					System.out.println("Remote [" + remote + "] URL: " + url);
//
//                    userCloneUrlMap.put(remote, url);
//                }
//            }
//        } catch (Exception ex) {
//            log.error("Unable to read .git directory ...{}", ex);
//        } finally {
//            if (repository != null)
//                repository.close();
//        }
        return userCloneUrlMap;
    }

    @Deprecated
    public static Map<String,String> getAllRemoteUrl(String projDirPath) throws Exception {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        Repository repository = builder.setGitDir(new File(projDirPath, ".git")).readEnvironment().findGitDir().build();

//        Repository repository = new FileRepositoryBuilder()
//                .setWorkTree(new File(projDirPath))
//                .findGitDir(projDirPath)   // searches for .git
//                .build();

        StoredConfig config = repository.getConfig();

        Set<String> remotes = config.getSubsections("remote");
        Map<String,String> gitUserRemoteMap = new HashMap<String, String>();
        for (String remote : remotes) {
            String url = config.getString("remote", remote, "url");
//            log.debug(remote + " -----> " + url);
            gitUserRemoteMap.put(remote, url);
        }
        log.debug("Git User Map: \n"+gitUserRemoteMap);

        return gitUserRemoteMap;
    }

    public static boolean gitDirExists(String projDirPath) {
        File gitDir = new FileRepositoryBuilder().findGitDir(new File(projDirPath)).getGitDir();
        return gitDir != null;
    }

    @Deprecated
    public static void addBranch(String projDirPath) throws IOException {
        Repository repository = new FileRepositoryBuilder()
                .findGitDir(new File(projDirPath + File.separator + ".git"))
                .build();

        StoredConfig config = repository.getConfig();

        // branch.master.remote = origin
        config.setString("branch", "master", "remote", "origin");

        // branch.master.merge = refs/heads/master
        config.setString("branch", "master", "merge", "refs/heads/master");

        config.save();

        repository.close();
    }

    public static void addBranch(String projDirPath, String branchName) throws Exception {

        try(Repository repository = new FileRepositoryBuilder()
                .findGitDir(new File(projDirPath + File.separator + ".git"))
                .build()) {
            StoredConfig config = repository.getConfig();

            // branch.master.remote = origin
            config.setString("branch", branchName, "remote", "origin");

            // branch.master.merge = refs/heads/master
            config.setString("branch", branchName, "merge", "refs/heads/" + branchName);

            config.save();
            log.debug("Branch added successfully ...");
        }


//        Repository repository = new FileRepositoryBuilder()
//                .findGitDir(new File(projDirPath + File.separator + ".git"))
//                .build();
//
//        StoredConfig config = repository.getConfig();
//
//        // branch.master.remote = origin
//        config.setString("branch", branchName, "remote", "origin");
//
//        // branch.master.merge = refs/heads/master
//        config.setString("branch", branchName, "merge", "refs/heads/" + branchName);
//
//        config.save();
//
//        repository.close();
    }
}
