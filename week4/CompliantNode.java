import java.util.*;

import static java.util.stream.Collectors.toSet;


/* CompliantNode refers to a node that follows the rules (not malicious)*/
public class CompliantNode implements Node {
    private int numRounds;
    private int currentRound;

    private boolean[] followees;
    private boolean[] malicious;

    private Set<Transaction> pendingTransactions;

    private int[] lastRoundFolloweeActivity;

    public CompliantNode(double p_graph, double p_malicious, double p_txDistribution, int numRounds) {
        this.numRounds = numRounds;
        this.currentRound = 0;
    }

    public void setFollowees(boolean[] followees) {
        this.followees = followees;
        this.malicious = new boolean[followees.length];
    }

    public void setPendingTransaction(Set<Transaction> pendingTransactions) {
        this.pendingTransactions = new HashSet<>(pendingTransactions);
    }

    public Set<Transaction> sendToFollowers() {
        return new HashSet<>(pendingTransactions);
    }

    private void updateMalicious(Set<Candidate> candidates){
        Set<Integer> proposingNodes = candidates.stream().map(c -> c.sender).collect(toSet());
        for (int i = 0; i < followees.length; i++) {
            if (!proposingNodes.contains(i) && followees[i])
                malicious[i] = true;
        }
    }

    public void receiveFromFollowees(Set<Candidate> candidates) {
        if (currentRound == numRounds) {
            return;
        }
        this.pendingTransactions.clear();
        updateMalicious(candidates);
        if(currentRound >= 1)
            pendingTransactions.clear();
        for (Candidate c : candidates) {
            if (!pendingTransactions.contains(c.tx) && !malicious[c.sender]) {
                pendingTransactions.add(c.tx);
            }
        }
        this.currentRound++;
    }

}