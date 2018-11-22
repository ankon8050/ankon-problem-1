package com.ankon.problem1;

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
import java.util.Date;
import java.util.List;
import java.util.Map;

public class FetchCommit {

    static Repository repo;

    public static void main(String[] args) throws IOException, GitAPIException {
        String REMOTE_URL;

        REMOTE_URL = "https://gitlab.com/AaviJit/muslimei.git";
        REMOTE_URL = "https://gitlab.com/ankon/problem-1.git";

        File localPath = File.createTempFile("TestGitRepository", "");
        if (!localPath.delete()) {
            throw new IOException("Could not delete temporary file " + localPath);
        }
        // then clone
        System.out.println("Cloning from " + REMOTE_URL + " to " + localPath);

        // Git git = new Git(repo);
        Git git = Git.cloneRepository().setURI(REMOTE_URL).setDirectory(localPath).call();

        repo = git.getRepository();
        RevWalk walk = new RevWalk(repo);

        System.out.println("Having repository: " + git.getRepository().getDirectory());
        System.out.println("Starting fetch");
        FetchResult result = git.fetch().setCheckFetchedObjects(true).call();
        System.out.println("Messages: " + result.getMessages());

        List<Ref> call = git.branchList().call();
        for (Ref ref : call) {
            System.out.println("Branch: " + ref + " " + ref.getName() + " " + ref.getObjectId().getName());
        }

        System.out.println("-------------------------------------");
        System.out.println("Fetching list of commits and changes");
        System.out.println("-------------------------------------");

        for (Ref branch : call) {
            String branchName = branch.getName();

            System.out.println("Commits of branch: " + branch.getName());
            System.out.println("-------------------------------------");

            Iterable<RevCommit> commits = git.log().all().call();

            for (RevCommit commit: commits) {
            // while (commits.iterator().hasNext()) {
                // RevCommit commit = commits.iterator().next();
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

//                    if (commits.iterator().hasNext())
//                        listDiff(repo, git,
//                            commit.getName() + "^",
//                            commit.getName());

                    //Get the commit you are looking for.
                    RevCommit newCommit;
                    try (RevWalk walk1 = new RevWalk(repo)) {
                        newCommit = walk1.parseCommit(repo.resolve(commit.getName()));
                    }

                    System.out.println("LogCommit: " + newCommit);
                    String logMessage = newCommit.getFullMessage();
                    System.out.println("LogMessage: " + logMessage);
                    //Print diff of the commit with the previous one.
                    // System.out.println(getDiffOfCommit(newCommit));
                    processResult(getDiffOfCommit(newCommit));

                    System.out.println("----------------End------------------");
                    System.out.println();
                }
            }
        }

//        listDiff(repo, git,
//                "8e4248d4edac130423ed935f76e2acd94d0ddc48^",
//                "8e4248d4edac130423ed935f76e2acd94d0ddc48");

        System.out.println("Done");
    }

    private static void processResult(String result) throws IOException {
        String[] lines = result.split("\n");

        locateTargetChanges(lines, "public void");
        locateTargetChanges(lines, "public int");
        locateTargetChanges(lines, "public String");
    }

    private static void locateTargetChanges(String[] lines, String declaration) {
        for (String line: lines) {
            if (line.charAt(0) == '+'
                    && line.contains(declaration)) {
                System.out.println(line);

                String temp = line.replaceAll("\\s", "");
                String method1 = temp.substring(temp.indexOf("+") + 1, temp.indexOf("("));

                for (int i = 0; i < lines.length; i++) {
                    String temp1 = lines[i];
                    if (temp1.charAt(0) == '-'
                            && temp1.contains("(") && temp1.contains(")")) {
                        String method2 = temp1.replaceAll("\\s", "");

                        if (method2.substring(method2.indexOf("-") + 1, method2.indexOf("(")).equals(method1))
                            System.out.println(lines[i]);
                    }
                }
            }
        }
    }

    //Helper gets the diff as a string.
    private static String getDiffOfCommit(RevCommit newCommit) throws IOException {

        //Get commit that is previous to the current one.
        RevCommit oldCommit = getPrevHash(newCommit);
        if(oldCommit == null){
            return "Start of repo";
        }
        //Use treeIterator to diff.
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
    //Helper function to get the previous commit.
    public static RevCommit getPrevHash(RevCommit commit)  throws  IOException {

        try (RevWalk walk = new RevWalk(repo)) {
            // Starting point
            walk.markStart(commit);
            int count = 0;
            for (RevCommit rev : walk) {
                // got the previous commit.
                if (count == 1) {
                    return rev;
                }
                count++;
            }
            walk.dispose();
        }
        //Reached end and no previous commits.
        return null;
    }
    //Helper function to get the tree of the changes in a commit. Written by Rüdiger Herrmann
    private static AbstractTreeIterator getCanonicalTreeParser(ObjectId commitId) throws IOException {
        try (RevWalk walk = new RevWalk(repo)) {
            RevCommit commit = walk.parseCommit(commitId);
            ObjectId treeId = commit.getTree().getId();
            try (ObjectReader reader = repo.newObjectReader()) {
                return new CanonicalTreeParser(null, reader, treeId);
            }
        }
    }

    private static void listDiff(Repository repository, Git git, String oldCommit, String newCommit) throws GitAPIException, IOException {
        final List<DiffEntry> diffs = git.diff()
                .setOldTree(prepareTreeParser(repository, oldCommit))
                .setNewTree(prepareTreeParser(repository, newCommit))
                .call();

        System.out.println("Found: " + diffs.size() + " differences");
        for (DiffEntry diff : diffs) {
            String output = "Diff: " + diff.getChangeType() + ": " +
                    (diff.getOldPath().equals(diff.getNewPath()) ? diff.getNewPath() : diff.getOldPath() + " -> " + diff.getNewPath());
            System.out.println(output);

            if (output.contains("MODIFY")) {
                String filename1, filename2;
                filename1 = generateFiles((diff.getOldPath().equals(diff.getNewPath()) ? diff.getNewPath() : diff.getOldPath() + " -> " + diff.getNewPath()), newCommit, true);
                filename2 = generateFiles(diff.getOldPath(), oldCommit, false);

                if (filename1 != null && filename2 != null) {
                    System.out.println("-------------Compare Start-------------");
                    // compareFiles1(filename1, filename2);
                    //Get the commit you are looking for.
                    RevCommit commit;
                    try (RevWalk walk1 = new RevWalk(repo)) {
                        commit = walk1.parseCommit(repo.resolve(newCommit));
                    }

                    System.out.println("LogCommit: " + newCommit);
                    String logMessage = commit.getFullMessage();
                    System.out.println("LogMessage: " + logMessage);
                    //Print diff of the commit with the previous one.
                    System.out.println(getDiffOfCommit(commit));
                    System.out.println("--------------Compare End-------------");
                }
            }
        }
    }

    private static void compareFiles(String filename1, String filename2) throws IOException {
        long start = System.nanoTime();
        FileChannel ch1 = new RandomAccessFile(filename1, "rw").getChannel();
        FileChannel ch2 = new RandomAccessFile(filename2, "rw").getChannel();
        if (ch1.size() != ch2.size()) {
            System.out.println("Files have different length");
        }
        long size = ch1.size();
        ByteBuffer m1 = ch1.map(FileChannel.MapMode.READ_WRITE, 0L, size);
        ByteBuffer m2 = ch2.map(FileChannel.MapMode.READ_WRITE, 0L, size);
        for (int pos = 0; pos < size; pos++) {
            if (m1.get(pos) != m2.get(pos)) {
                System.out.println(m1.get(pos));
            }
        }
        System.out.println("Files are identical, you can delete one of them.");
        long end = System.nanoTime();
        System.out.print("Execution time: " + (end - start) / 1000000 + "ms");
    }

    private static void compareFiles1(String filename1, String filename2) throws IOException {
        BufferedReader reader1 = new BufferedReader(new FileReader(filename1));
        BufferedReader reader2 = new BufferedReader(new FileReader(filename2));

        String line1 = reader1.readLine();
        String line2 = reader2.readLine();

        boolean areEqual = true;
        int lineNum = 1;

        while (line1 != null || line2 != null)
        {
            areEqual = true;

            if(line1 == null || line2 == null) {
                areEqual = false;
            }
            else if(! line1.equalsIgnoreCase(line2)) {
                areEqual = false;
            }

            if(areEqual) {
                // System.out.println("Two files have same content.");
            }
            else {
                // System.out.println("Two files have different content. They differ at line "+lineNum);
                System.out.println("File1 has "+line1+" and File2 has "+line2+" at line "+lineNum);
            }

            line1 = reader1.readLine();
            line2 = reader2.readLine();

            lineNum++;
        }

        reader1.close();

        reader2.close();
    }

    private static String generateFiles(String path, String commitId, boolean flag) throws IOException {
        ObjectId lastCommitId = repo.resolve(commitId);

        if (!path.contains(".java"))
            return null;

        String name = "";

        // a RevWalk allows to walk over commits based on some filtering that is defined
        try (RevWalk revWalk = new RevWalk(repo)) {
            RevCommit commit = revWalk.parseCommit(lastCommitId);
            // and using commit's tree find the path
            RevTree tree = commit.getTree();
            System.out.println("Having tree: " + tree);

            // now try to find a specific file
            try (TreeWalk treeWalk = new TreeWalk(repo)) {
                treeWalk.addTree(tree);
                treeWalk.setRecursive(true);
                treeWalk.setFilter(PathFilter.create(path));
                if (!treeWalk.next()) {
                    throw new IllegalStateException("Did not find expected file");
                }

                ObjectId objectId = treeWalk.getObjectId(0);
                ObjectLoader loader = repo.open(objectId);

                System.out.println("-------------------------------------");
                // and then one can the loader to read the file
                // loader.copyTo(System.out);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectStream objectStream = loader.openStream();

                loader.copyTo(baos);

                String[] fileName = path.split("/");

                if (flag) {
                    name = fileName[fileName.length - 1] + "_new_" + commitId + ".java";
                } else {
                    name = fileName[fileName.length - 1] + "_old_" + commitId + ".java";
                }

                try (OutputStream outputStream = new FileOutputStream(name)) {
                    baos.writeTo(outputStream);
                    baos.close();
                    outputStream.close();
                }
                System.out.println("-------------------------------------");
                System.out.println(path);
                System.out.println("-------------------------------------");
                System.out.println();
                System.out.println();
            }

            revWalk.dispose();
        }

        return name;
    }

    private static AbstractTreeIterator prepareTreeParser(Repository repository, String objectId) throws IOException {
        // from the commit we can build the tree which allows us to construct the TreeParser
        //noinspection Duplicates
        try (RevWalk walk = new RevWalk(repository)) {
            RevCommit commit = walk.parseCommit(repository.resolve(objectId));
            RevTree tree = walk.parseTree(commit.getTree().getId());

            CanonicalTreeParser treeParser = new CanonicalTreeParser();
            try (ObjectReader reader = repository.newObjectReader()) {
                treeParser.reset(reader, tree.getId());
            }

            walk.dispose();

            return treeParser;
        }
    }

}
