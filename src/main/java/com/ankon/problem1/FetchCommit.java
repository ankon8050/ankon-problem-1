package com.ankon.problem1;

import com.ankon.problem1.helper.CookbookHelper;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class FetchCommit {

    public static void main(String[] args) throws IOException, GitAPIException {
        // try (Repository repository = CookbookHelper.openJGitCookbookRepository()) {
        try (Repository repository = new FileRepository("git@gitlab.com:ankon/populate-from-rest-api.git")) {
            // The {tree} will return the underlying tree-id instead of the commit-id itself!
            // For a description of what the carets do see e.g. http://www.paulboxley.com/blog/2011/06/git-caret-and-tilde
            // This means we are selecting the parent of the parent of the parent of the parent of current HEAD and

            // Repository repo = new FileRepository("pathToRepo/.git");
            Git git = new Git(repository);
            RevWalk walk = new RevWalk(repository);

            System.out.println(repository.toString());
            System.out.println(git.toString());

            List<Ref> branches = git.branchList().call();

            for (Ref branch : branches) {
                String branchName = branch.getName();

                System.out.println("Commits of branch: " + branch.getName());
                System.out.println("-------------------------------------");

                Iterable<RevCommit> commits = git.log().all().call();

                for (RevCommit commit : commits) {
                    boolean foundInThisBranch = false;

                    RevCommit targetCommit = walk.parseCommit(repository.resolve(
                            commit.getName()));
                    for (Map.Entry<String, Ref> e : repository.getAllRefs().entrySet()) {
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
                    }
                }
            }
            // take the tree-ish of it
//            ObjectId oldHead = repository.resolve("HEAD^^^^{tree}");
//            ObjectId head = repository.resolve("HEAD^{tree}");
//
//            System.out.println("Printing diff between tree: " + oldHead + " and " + head);
//
//            // prepare the two iterators to compute the diff between
//            try (ObjectReader reader = repository.newObjectReader()) {
//                CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
//                oldTreeIter.reset(reader, oldHead);
//                CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
//                newTreeIter.reset(reader, head);
//
//                // finally get the list of changed files
//                try (Git git1 = new Git(repository)) {
//                    List<DiffEntry> diffs= git1.diff()
//                            .setNewTree(newTreeIter)
//                            .setOldTree(oldTreeIter)
//                            .call();
//                    for (DiffEntry entry : diffs) {
//                        System.out.println("Entry: " + entry);
//                    }
//                }
//            }
        }

        System.out.println("Done");
    }

}
