package com.ankon.problem1;

import com.ankon.problem1.helper.CookbookHelper;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class FetchCommit {

    public static void main(String[] args) throws IOException, GitAPIException {
        // try (Repository repository = CookbookHelper.openJGitCookbookRepository()) {
//        try {
//            // The {tree} will return the underlying tree-id instead of the commit-id itself!
//            // For a description of what the carets do see e.g. http://www.paulboxley.com/blog/2011/06/git-caret-and-tilde
//            // This means we are selecting the parent of the parent of the parent of the parent of current HEAD and
//
//            // Repository repo = new FileRepository("pathToRepo/.git");
//            Repository repository = new FileRepository("git@gitlab.com:ankon/hr-module-rest.git");
//            Git git = new Git(repository);
//            RevWalk walk = new RevWalk(repository);
//
//            System.out.println(repository.toString());
//            System.out.println(git.toString());
//
//            List<Ref> branches = git.branchList().call();
//
//            for (Ref branch : branches) {
//                String branchName = branch.getName();
//
//                System.out.println("Commits of branch: " + branch.getName());
//                System.out.println("-------------------------------------");
//
//                Iterable<RevCommit> commits = git.log().all().call();
//
//                for (RevCommit commit : commits) {
//                    boolean foundInThisBranch = false;
//
//                    RevCommit targetCommit = walk.parseCommit(repository.resolve(
//                            commit.getName()));
//                    for (Map.Entry<String, Ref> e : repository.getAllRefs().entrySet()) {
//                        if (e.getKey().startsWith(Constants.R_HEADS)) {
//                            if (walk.isMergedInto(targetCommit, walk.parseCommit(
//                                    e.getValue().getObjectId()))) {
//                                String foundInBranch = e.getValue().getName();
//                                if (branchName.equals(foundInBranch)) {
//                                    foundInThisBranch = true;
//                                    break;
//                                }
//                            }
//                        }
//                    }
//
//                    if (foundInThisBranch) {
//                        System.out.println(commit.getName());
//                        System.out.println(commit.getAuthorIdent().getName());
//                        System.out.println(new Date(commit.getCommitTime() * 1000L));
//                        System.out.println(commit.getFullMessage());
//                    }
//                }
//            }
//
////            String treeName = "master"; // tag or branch
////            for (RevCommit commit : git.log().add(repository.resolve(treeName)).call()) {
////                System.out.println(commit.getName());
////            }
//
//            // take the tree-ish of it
////            ObjectId oldHead = repository.resolve("HEAD^^^^{tree}");
////            ObjectId head = repository.resolve("HEAD^{tree}");
////
////            System.out.println("Printing diff between tree: " + oldHead + " and " + head);
////
////            // prepare the two iterators to compute the diff between
////            try (ObjectReader reader = repository.newObjectReader()) {
////                CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
////                oldTreeIter.reset(reader, oldHead);
////                CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
////                newTreeIter.reset(reader, head);
////
////                // finally get the list of changed files
////                try (Git git1 = new Git(repository)) {
////                    List<DiffEntry> diffs= git1.diff()
////                            .setNewTree(newTreeIter)
////                            .setOldTree(oldTreeIter)
////                            .call();
////                    for (DiffEntry entry : diffs) {
////                        System.out.println("Entry: " + entry);
////                    }
////                }
////            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        String REMOTE_URL = "git@github.com:centic9/jgit-cookbook.git";
        // String REMOTE_URL = "git@gitlab.com:ankon/hr-module-rest.git";

        // Repository repo = CookbookHelper.createNewRepository();

        File localPath = File.createTempFile("TestGitRepository", "");
        if (!localPath.delete()) {
            throw new IOException("Could not delete temporary file " + localPath);
        }
        // then clone
        System.out.println("Cloning from " + REMOTE_URL + " to " + localPath);

        // Git git = new Git(repo);
        Git git = Git.cloneRepository().setURI(REMOTE_URL).setDirectory(localPath).call();

        Repository repo = git.getRepository();
        RevWalk walk = new RevWalk(repo);

        // Note: the call() returns an opened repository already which needs to be closed to avoid file handle leaks!
        System.out.println("Having repository: " + git.getRepository().getDirectory());
        System.out.println("Starting fetch");
        FetchResult result = git.fetch().setCheckFetchedObjects(true).call();
        System.out.println("Messages: " + result.getMessages());

        List<Ref> call = git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call();
        for (Ref ref : call) {
            System.out.println("Branch: " + ref + " " + ref.getName() + " " + ref.getObjectId().getName());
        }

        System.out.println("-------------------------------------");
        System.out.println();
        System.out.println();

//        List<Ref> branches = git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call();
//
//        System.out.println(branches.size());
//
//        Map<String, Ref> allRefs = git.getRepository().getAllRefs();
//
//        System.out.println(allRefs.size());
//
        for (Ref branch : call) {
            String branchName = branch.getName();

            System.out.println("Commits of branch: " + branch.getName());
            System.out.println("-------------------------------------");

            Iterable<RevCommit> commits = git.log().all().call();

            boolean flag = false;
            for (RevCommit commit : commits) {
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
                    System.out.println(commit.getName());
                    System.out.println(commit.getAuthorIdent().getName());
                    System.out.println(new Date(commit.getCommitTime() * 1000L));
                    System.out.println(commit.getFullMessage());

                    if (!flag)
                        flag = true;
                    else listDiff(repo, git,
                            commit.getName() + "^",
                            commit.getName());
                }
            }
        }

        System.out.println("Done");
    }

    private static void listDiff(Repository repository, Git git, String oldCommit, String newCommit) throws GitAPIException, IOException {
        final List<DiffEntry> diffs = git.diff()
                .setOldTree(prepareTreeParser(repository, oldCommit))
                .setNewTree(prepareTreeParser(repository, newCommit))
                .call();

        System.out.println("Found: " + diffs.size() + " differences");
        for (DiffEntry diff : diffs) {
            System.out.println("Diff: " + diff.getChangeType() + ": " +
                    (diff.getOldPath().equals(diff.getNewPath()) ? diff.getNewPath() : diff.getOldPath() + " -> " + diff.getNewPath()));
        }
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
