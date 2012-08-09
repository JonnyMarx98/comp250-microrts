/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ai.rtminimax;

import ai.evaluation.EvaluationFunction;
import ai.AI;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import rts.GameState;
import rts.PlayerAction;
import rts.PlayerActionGenerator;
import util.Pair;

/**
 *
 * @author santi
 */
public class IDRTMinimax extends RTMinimax {
    public static int MAX_DEPTH = 0;
    public static long MAX_POTENTIAL_BRANCHING = 0;
    
    int TIME_PER_CYCLE = 100;
    
    public IDRTMinimax(int tpc) {
        TIME_PER_CYCLE = tpc;
    }
    

    public void reset() {
    }

    
    public AI clone() {
        return new IDRTMinimax(TIME_PER_CYCLE);
    }     
    
    
    public PlayerAction getAction(int player, GameState gs) throws Exception {
        if (gs.canExecuteAnyAction(player) && gs.winner()==-1) {
            PlayerAction pa = realTimeMinimaxABIterativeDeepening(player, gs, TIME_PER_CYCLE); 
            pa.fillWithNones(gs, player);
            return pa;
        } else {
            return new PlayerAction();
        }

    }
    
    
    public PlayerAction realTimeMinimaxABIterativeDeepening(int player, GameState gs, int availableTime) throws Exception {
        int maxplayer = player;
        int minplayer = 1 - player;
        int lookAhead = 1;
        long startTime = System.currentTimeMillis();
        PlayerAction bestMove = null;
        System.out.println("Starting realTimeMinimaxABIterativeDeepening... ");
        do {
//            System.out.println("next lookahead: " + lookAhead);
            if (nLeaves>MAX_LEAVES) MAX_LEAVES = nLeaves;
            minCT = -1;
            maxCT = -1;
            nLeaves = 0;
            long runStartTime = System.currentTimeMillis();
            PlayerAction tmp = timeBoundedRealTimeMinimaxAB(gs, maxplayer, minplayer, gs.getTime() + lookAhead, startTime + availableTime, bestMove==null);
            if (tmp!=null) {
                bestMove = tmp;
                if (lookAhead>MAX_DEPTH) MAX_DEPTH = lookAhead;
            }
            System.out.println("realTimeMinimaxABIterativeDeepening (lookahead = " + lookAhead + "): " + bestMove + " in " + (System.currentTimeMillis()-runStartTime) + " (" + nLeaves + " leaves)");
            int nextLookAhead = Math.max((minCT+1) - gs.getTime(), lookAhead+4);
            if (nextLookAhead<=lookAhead) {
                return bestMove;
            } else {
                lookAhead = nextLookAhead;
            }
        }while(System.currentTimeMillis() - startTime < availableTime);
        return bestMove;
    }
    
    
    public PlayerAction timeBoundedRealTimeMinimaxAB(GameState initial_gs, int maxplayer, int minplayer, int lookAhead, long cutOffTime, boolean needAResult) throws Exception {
        List<RTMiniMaxNode> stack = new LinkedList<RTMiniMaxNode>();      
        RTMiniMaxNode head = new RTMiniMaxNode(0,initial_gs,-EvaluationFunction.VICTORY, EvaluationFunction.VICTORY);
        stack.add(head);
        Pair<PlayerAction,Float> lastResult = null;
        while(!stack.isEmpty() && System.currentTimeMillis()<cutOffTime){
            
//            System.out.print("Stack: [ ");
//            for(RTMiniMaxNode n:stack) System.out.print(" " + n.type + "(" + n.gs.getTime() + ") ");
//            System.out.println("]");
            
            RTMiniMaxNode current = stack.get(0);
            switch(current.type) {
                case -1: // unknown node:
                        {
                            int winner = current.gs.winner();
                            if (current.gs.getTime()>=lookAhead || winner!=-1) {
                                if (winner==-1) {
                                    int CT = current.gs.getNextChangeTime();
                                    if (minCT==-1 || CT<minCT) minCT = CT;
                                    if (maxCT==-1 || CT>maxCT) maxCT = CT;
                                }
                                nLeaves++;
                                lastResult = new Pair<PlayerAction,Float>(null,EvaluationFunction.evaluate(maxplayer, minplayer, current.gs));
                                stack.remove(0);    
                            } else if (current.gs.canExecuteAnyAction(maxplayer)) {
                                current.type = 0;
                            } else if (current.gs.canExecuteAnyAction(minplayer)) {
                                current.type = 1;
                            } else {
                                current.type = 2;
                            }     
                        }
                        break;
                case 0: // max node:
                        if (current.actions==null) {
                            current.actions = new PlayerActionGenerator(current.gs,maxplayer);
                            long l = current.actions.getSize();
                            if (l>MAX_POTENTIAL_BRANCHING) MAX_POTENTIAL_BRANCHING = l;
//                            while(current.actions.size()>MAX_BRANCHING_FACTOR) current.actions.remove(r.nextInt(current.actions.size()));
                            current.best = null;
                            GameState gs2 = current.gs.cloneIssue(current.actions.getNextAction(cutOffTime));
                            stack.add(0, new RTMiniMaxNode(-1,gs2,current.alpha, current.beta));
                        } else {                            
                            current.alpha = Math.max(current.alpha,lastResult.m_b);
                            if (current.best==null || lastResult.m_b>current.best.m_b) {
                                current.best = lastResult;
                                current.best.m_a = current.actions.getLastAction();
                            }
                            PlayerAction next = current.actions.getNextAction(cutOffTime);
                            if (current.beta<=current.alpha || next == null) {
                                lastResult = current.best;
                                stack.remove(0);
                                if (current.actions.getGenerated()>MAX_BRANCHING) MAX_BRANCHING = current.actions.getGenerated();
                            } else {
                                GameState gs2 = current.gs.cloneIssue(next);
                                stack.add(0, new RTMiniMaxNode(-1,gs2,current.alpha, current.beta));
                            }
                        }                    
                        break;
                case 1: // min node:
                        if (current.actions==null) {
                            current.actions = new PlayerActionGenerator(current.gs,minplayer);
                            long l = current.actions.getSize();
                            if (l>MAX_POTENTIAL_BRANCHING) MAX_POTENTIAL_BRANCHING = l;
//                            while(current.actions.size()>MAX_BRANCHING_FACTOR) current.actions.remove(r.nextInt(current.actions.size()));
                            current.best = null;
                            GameState gs2 = current.gs.cloneIssue(current.actions.getNextAction(cutOffTime));
                            stack.add(0, new RTMiniMaxNode(-1,gs2,current.alpha, current.beta));
                        } else {                            
                            current.beta = Math.min(current.beta,lastResult.m_b);
                            if (current.best==null || lastResult.m_b<current.best.m_b) {
                                current.best = lastResult;
                                current.best.m_a = current.actions.getLastAction();
                            }
                            PlayerAction next = current.actions.getNextAction(cutOffTime);
                            if (current.beta<=current.alpha || next == null) {
                                lastResult = current.best;
                                stack.remove(0);
                                if (current.actions.getGenerated()>MAX_BRANCHING) MAX_BRANCHING = current.actions.getGenerated();
                            } else {
                                GameState gs2 = current.gs.cloneIssue(next);
                                stack.add(0, new RTMiniMaxNode(-1,gs2,current.alpha, current.beta));
                            }
                        }
                        break;
                case 2: // simulation node:
                        current.gs = current.gs.clone();
                        while(current.gs.winner()==-1 && 
//                            current.gs.getTime()<lookAhead && 
                            !current.gs.canExecuteAnyAction(maxplayer) && 
                            !current.gs.canExecuteAnyAction(minplayer)) current.gs.cycle();
                        current.type = -1;
                        break;
            }
        }
        
        if (stack.isEmpty()) return lastResult.m_a;
        if (needAResult) {
            if (head.best!=null) return head.best.m_a;
            Random r = new Random();
            return head.actions.getRandom();
        }
        return null;
    }
}
