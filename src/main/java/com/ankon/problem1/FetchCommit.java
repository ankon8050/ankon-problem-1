package com.ankon.problem1;

import com.ankon.problem1.domain.Result;
import com.ankon.problem1.helper.CookbookHelper;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class FetchCommit {

    static Repository repo;
    static List<Result> results = new ArrayList<>();

    public static void main(String[] args) throws IOException, GitAPIException {
        String REMOTE_URL;

        // REMOTE_URL = "https://github.com/dfleta/Java.git"; // result_set-1
        REMOTE_URL = "https://github.com/EzTexting/java-code-samples.git"; // result_set_2

        File localPath = File.createTempFile("TestGitRepository", "");
        if (!localPath.delete()) {
            throw new IOException("Could not delete temporary file " + localPath);
        }

        Git git = Git.cloneRepository().setURI(REMOTE_URL).setDirectory(localPath).call();

        repo = git.getRepository();
        RevWalk walk = new RevWalk(repo);

        FetchResult result = git.fetch().setCheckFetchedObjects(true).call();

        List<Ref> call = git.branchList().call();

        for (Ref branch : call) {
            String branchName = branch.getName();

            Iterable<RevCommit> commits = git.log().all().call();

            for (RevCommit commit: commits) {
                boolean foundInThisBranch = false;

                RevCommit targetCommit = walk.parseCommit(repo.resolve(
                        commit.getName()));
                for (Map.Entry<String, Ref> e : repo.getAllRefs().entrySet()) {
                    if (e.getKey().startsWith(Constants.R_HEADS)) {
                        if (walk.isMergedInto(targetCommit, walk.parseCommit(
                                e.getValue().getObjectId()))) {
                            String foundInBranch = e.getValue().getName();
                            if (branchName.equals(foundInBranch)) {
                                foundInThisBranch = true;
                                break;
                            }
                        }
                    }
                }

                if (foundInThisBranch) {
                    System.out.println("---------------Start-----------------");
                    System.out.println(commit.getName());
                    System.out.println(commit.getAuthorIdent().getName());
                    System.out.println(new Date(commit.getCommitTime() * 1000L));
                    System.out.println(commit.getFullMessage());

                    RevCommit newCommit;
                    try (RevWalk walk1 = new RevWalk(repo)) {
                        newCommit = walk1.parseCommit(repo.resolve(commit.getName()));
                    }

                    String logMessage = newCommit.getFullMessage();
                    // System.out.println(getDiffOfCommit(newCommit));
                    processResult(getDiffOfCommit(newCommit), commit.getName(), "");

                    System.out.println("----------------End------------------");
                    System.out.println();
                }
            }
        }

        PrintWriter pw = new PrintWriter(new File("result_set_2.csv"));
        StringBuilder sb = new StringBuilder();

        sb.append("Commit SHA");
        sb.append(",");
        sb.append("Java File");
        sb.append(",");
        sb.append("Old function signature");
        sb.append(",");
        sb.append("New function signature");
        sb.append("\n");

        for (Result res: results) {
            sb.append(res.getSHA());
            sb.append(",");
            sb.append(res.getFilename());
            sb.append(",");
            sb.append(res.getOldSignature().replaceAll(",", " "));
            sb.append(",");
            sb.append(res.getNewSignature().replaceAll(",", " "));
            sb.append("\n");
        }

        pw.write(sb.toString());
        pw.close();

        System.out.println("Done");
    }

    private static void processResult(String result, String SHA, String filename) throws IOException {
        String[] lines = result.split("\n");

        locateTargetChanges(lines, "public void", SHA, filename);
        locateTargetChanges(lines, "public int", SHA, filename);
        locateTargetChanges(lines, "public String", SHA, filename);
        locateTargetChanges(lines, "public double", SHA, filename);
        locateTargetChanges(lines, "private void", SHA, filename);
        locateTargetChanges(lines, "private int", SHA, filename);
        locateTargetChanges(lines, "private String", SHA, filename);
        locateTargetChanges(lines, "private double", SHA, filename);
    }

    private static void locateTargetChanges(String[] lines, String declaration, String SHA, String filename) {
        for (int j = 0; j < lines.length; j++) {
            String line = lines[j];

            if (line.length() > 0 && line.charAt(0) == '+'
                    && line.contains(declaration)
                    && line.contains("(") && line.contains(")")) {
                String temp = line.replaceAll("\\s", "");
                String method1 = temp.substring(temp.indexOf("+") + 1, temp.indexOf("("));

                for (int i = 0; i < lines.length; i++) {
                    String temp1 = lines[i];
                    if (temp1.charAt(0) == '-'
                            && temp1.contains("(") && temp1.contains(")")) {
                        String method2 = temp1.replaceAll("\\s", "");

                        if (method2.substring(method2.indexOf("-") + 1, method2.indexOf("(")).equals(method1)
                                && temp.substring(temp.indexOf("("), temp.indexOf(")")).length() !=
                                method2.substring(method2.indexOf("("), method2.indexOf(")")).length()) {
                            String javaFile = filename;
                            for (int k = j; k > 0; k--) {
                                if (lines[k].contains("index")) {
                                    if ((k - 1) >= 0 && lines[k-1].contains("diff")) {
                                        String[] tokenize = lines[k+2].split("/");
                                        javaFile = tokenize[tokenize.length - 1];
                                        break;
                                    }
                                }
                            }

                            Result result = new Result(SHA, javaFile,
                                    lines[i].substring(lines[i].indexOf("p"), lines[i].length() - 1),
                                    line.substring(line.indexOf("p"), line.length() - 1));
                            System.out.println(result.toString());
                            results.add(result);
                        }
                    }
                }
            }
        }
    }

    private static String getDiffOfCommit(RevCommit newCommit) throws IOException {
        RevCommit oldCommit = getPrevHash(newCommit);
        if(oldCommit == null){
            return "Start of repo";
        }

        AbstractTreeIterator oldTreeIterator = getCanonicalTreeParser(oldCommit);
        AbstractTreeIterator newTreeIterator = getCanonicalTreeParser(newCommit);
        OutputStream outputStream = new ByteArrayOutputStream();
        try (DiffFormatter formatter = new DiffFormatter(outputStream)) {
            formatter.setRepository(repo);
            formatter.format(oldTreeIterator, newTreeIterator);
        }
        String diff = outputStream.toString();
        return diff;
    }

    public static RevCommit getPrevHash(RevCommit commit)  throws  IOException {

        try (RevWalk walk = new RevWalk(repo)) {
            walk.markStart(commit);
            int count = 0;
            for (RevCommit rev : walk) {
                if (count == 1) {
                    return rev;
                }
                count++;
            }
            walk.dispose();
        }
        return null;
    }

    private static AbstractTreeIterator getCanonicalTreeParser(ObjectId commitId) throws IOException {
        try (RevWalk walk = new RevWalk(repo)) {
            RevCommit commit = walk.parseCommit(commitId);
            ObjectId treeId = commit.getTree().getId();
            try (ObjectReader reader = repo.newObjectReader()) {
                return new CanonicalTreeParser(null, reader, treeId);
            }
        }
    }

}
