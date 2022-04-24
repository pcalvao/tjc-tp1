package lp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import scpsolver.constraints.*;
import scpsolver.lpsolver.*;
import scpsolver.problems.*;
import gametree.*;
import play.*;
import play.exception.InvalidStrategyException;

public class IteratedRemoval extends Strategy {

    @Override
    public void execute() throws InterruptedException {
        while (!this.isTreeKnown()) {
            System.err.println("Waiting for game tree to become available.");
            Thread.sleep(1000);
        }
        while (true) {
            PlayStrategy myStrategy = this.getStrategyRequest();
            if (myStrategy == null) // Game was terminated by an outside event
                break;
            boolean playComplete = false;

            while (!playComplete) {
                System.out.println("*******************************************************");
                if (myStrategy.getFinalP1Node() != -1) {
                    GameNode finalP1 = this.tree.getNodeByIndex(myStrategy.getFinalP1Node());
                    GameNode fatherP1 = null;
                    if (finalP1 != null) {
                        try {
                            fatherP1 = finalP1.getAncestor();
                        } catch (GameNodeDoesNotExistException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        System.out.print("Last round as P1: " + showLabel(fatherP1.getLabel()) + "|"
                                + showLabel(finalP1.getLabel()));
                        System.out.println(" -> (Me) " + finalP1.getPayoffP1() + " : (Opp) " + finalP1.getPayoffP2());
                    }
                }
                if (myStrategy.getFinalP2Node() != -1) {
                    GameNode finalP2 = this.tree.getNodeByIndex(myStrategy.getFinalP2Node());
                    GameNode fatherP2 = null;
                    if (finalP2 != null) {
                        try {
                            fatherP2 = finalP2.getAncestor();
                        } catch (GameNodeDoesNotExistException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        System.out.print("Last round as P2: " + showLabel(fatherP2.getLabel()) + "|"
                                + showLabel(finalP2.getLabel()));
                        System.out.println(" -> (Opp) " + finalP2.getPayoffP1() + " : (Me) " + finalP2.getPayoffP2());
                    }
                }
                // Normal Form Games only!
                GameNode rootNode = tree.getRootNode();
                int n1 = rootNode.numberOfChildren();
                int n2 = rootNode.getChildren().next().numberOfChildren();
                String[] labelsP1 = new String[n1];
                String[] labelsP2 = new String[n2];
                int[][] U1 = new int[n1][n2];
                int[][] U2 = new int[n1][n2];
                Iterator<GameNode> childrenNodes1 = rootNode.getChildren();
                GameNode childNode1;
                GameNode childNode2;
                int i = 0;
                int j = 0;
                while (childrenNodes1.hasNext()) {
                    childNode1 = childrenNodes1.next();
                    labelsP1[i] = childNode1.getLabel();
                    j = 0;
                    Iterator<GameNode> childrenNodes2 = childNode1.getChildren();
                    while (childrenNodes2.hasNext()) {
                        childNode2 = childrenNodes2.next();
                        if (i == 0)
                            labelsP2[j] = childNode2.getLabel();
                        U1[i][j] = childNode2.getPayoffP1();
                        U2[i][j] = childNode2.getPayoffP2();
                        j++;
                    }
                    i++;
                }
                showActions(1, labelsP1);
                showActions(2, labelsP2);
                showUtility(1, U1);
                showUtility(2, U2);
                NormalFormGame game = new NormalFormGame(U1, U2, labelsP1, labelsP2);
                game.showGame();
                this.iteratedRemoval(U1, U2, labelsP1, labelsP2, myStrategy);
                try {
                    this.provideStrategy(myStrategy);
                    playComplete = true;
                } catch (InvalidStrategyException e) {
                    System.err.println("Invalid strategy: " + e.getMessage());
                    e.printStackTrace(System.err);
                }
            }
        }
    }

    public String showLabel(String label) {
        return label.substring(label.lastIndexOf(':') + 1);
    }

    public void showActions(int P, String[] labels) {
        System.out.println("Actions Player " + P + ":");
        for (int i = 0; i < labels.length; i++)
            System.out.println("   " + showLabel(labels[i]));
    }

    public void showUtility(int P, int[][] M) {
        int nLin = M.length;
        int nCol = M[0].length;
        System.out.println("Utility Player " + P + ":");
        for (int i = 0; i < nLin; i++) {
            for (int j = 0; j < nCol; j++)
                System.out.print("| " + M[i][j] + " ");
            System.out.println("|");
        }
    }

    public int[][] makeAllPositive(int[][] M, boolean row, int index) {
        int rows = M.length;
        int cols = M[0].length;
        int min = 0;

        int[][] positiveM = new int[rows][cols];

        if (row) {
            for (int i = 0; i < rows; i++) {
                if (i == index) continue;
                for (int j = 0; j < cols; j++) {
                    if (M[i][j] < min)
                        min = M[i][j];
                }
            }
        } else {
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    if (j == index) continue;
                    if (M[i][j] < min)
                        min = M[i][j];
                }
            }
        }

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                positiveM[i][j] = M[i][j] - min;
            }
        }

        System.out.println("Make positive, " + row + ", " + index);
        printMatrix(M);
        System.out.println("----------------------");
        printMatrix(positiveM);
        return positiveM;
    }

    public int[][] removeRow(int[][] M, int row) {
        int[][] newM = new int[M.length - 1][M[0].length];
        int skipped = 0;
        for(int i = 0; i < newM.length; i++) {
            if(i == row) skipped = 1;
            newM[i] = M[i + skipped];
        }
        return newM;
    }

    public int[][] removeCol(int[][] M, int col) {
        int[][] newM = new int[M.length][M[0].length - 1];
        for (int i = 0; i < newM.length; i++) {
            int skipped = 0;
            for (int j = 0; j < newM[0].length; j++) {
                if (j == col)
                    skipped = 1;
                newM[i][j] = M[i][j + skipped];
            }
        }
        return newM;
    }

    public String[] removeLabel(String[] labels, int index) {
        List<String> temp = new ArrayList<>(Arrays.asList(labels));
        temp.remove(index);
        return temp.toArray(new String[0]);
    }

    public boolean isDominated(int[][] M, boolean isRow, int index) {
        int rows = M.length;
        int cols = M[0].length;

        if (isRow) {
            double[] c = new double[rows - 1];
            double[] lb = new double[rows - 1];

            for (int i = 0; i < c.length; i++)
                c[i] = 1.0;

            double[] bi = new double[cols];
            for (int i = 0; i < bi.length; i++)
                bi[i] = M[index][i];

            double[][] A = new double[cols][rows - 1];
            int skipped = 0;
            for (int i = 0; i < rows; i++) {
                if (i == index) {
                    skipped = 1;
                    rows--;
                }
                for (int j = 0; j < cols; j++) {
                    if (i < rows)
                        A[j][i] = M[i + skipped][j];
                }
            }

            LinearProgram lp = new LinearProgram(c);
            lp.setMinProblem(true);
            for (int i = 0; i < bi.length; i++) {
                lp.addConstraint(new LinearBiggerThanEqualsConstraint(A[i], bi[i], "c" + i));
            }
            lp.setLowerbound(lb);
            showLP(lp);
            double[] res = solveLP(lp);
            showSolution(lp, res);
            if (res == null) {
                return false;
            }
            return lp.evaluate(res) < 1;
        } else {
            double[] c = new double[cols - 1];
            double[] lb = new double[cols - 1];

            for (int i = 0; i < c.length; i++)
                c[i] = 1.0;

            double[] bi = new double[rows];
            for (int i = 0; i < bi.length; i++)
                bi[i] = M[i][index];

            double[][] A = new double[rows][cols - 1];
            for (int i = 0; i < rows; i++) {
                int skipped = 0;
                for (int j = 0; j < cols; j++) {
                    if (j == index) {
                        skipped = 1;
                        cols--;
                    }
                    if (j < cols)
                        A[i][j] = M[i][j + skipped];
                }
                cols++;
            }

            printMatrixDouble(A);

            LinearProgram lp = new LinearProgram(c);
            lp.setMinProblem(true);
            for (int i = 0; i < bi.length; i++) {
                lp.addConstraint(new LinearBiggerThanEqualsConstraint(A[i], bi[i], "c" + i));
            }
            lp.setLowerbound(lb);
            showLP(lp);
            double[] res = solveLP(lp);
            showSolution(lp, res);
            if (res == null) {
                return false;
            }
            return lp.evaluate(res) < 1;
        }

    }

    public void iteratedRemoval(int[][] M1, int[][] M2, String[] labelsP1, String[] labelsP2, PlayStrategy myStrategy) {
        boolean finished = false;
        int rows = M1.length;
        int cols = M1[0].length;
        while (!finished) {
            finished = true;
            System.out.println("Checking for dominated rows...");
            for (int i = 0; i < rows; i++) {
                if (rows == 1) break;
                int[][] positiveM1 = makeAllPositive(M1, true, i);
                if (isDominated(positiveM1, true, i)) {
                    System.out.printf("Removing row %d...\n", i);
                    M1 = removeRow(M1, i);
                    M2 = removeRow(M2, i);
                    labelsP1 = removeLabel(labelsP1, i);
                    rows--;
                    i = -1;
                    finished = false;
                    System.out.println("PRINTING M1 AND M2");
                    printMatrix(M1);
                    System.out.println("-------------------");
                    printMatrix(M2);
                }
            }
            System.out.println("Checking for dominated columns...");
            for (int i = 0; i < cols; i++) {
                if (cols == 1) break;
                int[][] positiveM2 = makeAllPositive(M2, false, i);
                if (isDominated(positiveM2, false, i)) {
                    System.out.printf("Removing column %d...\n", i);
                    M1 = removeCol(M1, i);
                    M2 = removeCol(M2, i);
                    labelsP2 = removeLabel(labelsP2, i);
                    cols--;
                    finished = false;
                    System.out.println("PRINTING M1 AND M2");
                    printMatrix(M1);
                    System.out.println("-------------------");
                    printMatrix(M2);
                    break;
                }
            }
        }
        for(String s : labelsP1) {
            System.out.print(s + " ");
        }
        System.out.println();
        printMatrix(M1);
        for (String s : labelsP2) {
            System.out.print(s + " ");
        }
        System.out.println();
        printMatrix(M2);

        //Set strategy so it doesn't crash
        for (String label : labelsP1) {
            myStrategy.put(label, 1.0 / labelsP1.length);
        }
        for (String label : labelsP2) {
            myStrategy.put(label, 1.0 / labelsP2.length);
        }
    }

    public void printMatrix(int[][] M) {
        for (int i = 0; i < M.length; i++) {
            for (int j = 0; j < M[0].length; j++) {
                System.out.print(M[i][j] + " | ");
            }
            System.out.println();
        }
    }

    public void printMatrixDouble(double[][] M) {
        for (int i = 0; i < M.length; i++) {
            for (int j = 0; j < M[0].length; j++) {
                System.out.print(M[i][j] + " | ");
            }
            System.out.println();
        }
    }

    public static double[] solveLP(LinearProgram lp) {
        LinearProgramSolver solver = SolverFactory.newDefault();
        double[] x = solver.solve(lp);
        return x;
    }

    public static void showSolution(LinearProgram lp, double[] x) {
        if (x == null)
            System.out.println("*********** NO SOLUTION FOUND ***********");
        else {
            System.out.println("*********** SOLUTION ***********");
            for (int i = 0; i < x.length; i++)
                System.out.println("x[" + i + "] = " + x[i]);
            System.out.println("f(x) = " + lp.evaluate(x));
        }
    }

    public static void showLP(LinearProgram lp) {
        System.out.println("*********** LINEAR PROGRAMMING PROBLEM ***********");
        String fs;
        if (lp.isMinProblem())
            System.out.print("  minimize: ");
        else
            System.out.print("  maximize: ");
        double[] cf = lp.getC();
        for (int i = 0; i < cf.length; i++)
            if (cf[i] != 0) {
                fs = String.format(Locale.US, "%+7.1f", cf[i]);
                System.out.print(fs + "*x[" + i + "]");
            }
        System.out.println("");
        System.out.print("subject to: ");
        ArrayList<Constraint> lcstr = lp.getConstraints();
        double aij;
        double[] ci = null;
        String str = null;
        for (int i = 0; i < lcstr.size(); i++) {
            if (lcstr.get(i) instanceof LinearSmallerThanEqualsConstraint) {
                str = " <= ";
                ci = ((LinearSmallerThanEqualsConstraint) lcstr.get(i)).getC();
            }
            if (lcstr.get(i) instanceof LinearBiggerThanEqualsConstraint) {
                str = " >= ";
                ci = ((LinearBiggerThanEqualsConstraint) lcstr.get(i)).getC();
            }
            if (lcstr.get(i) instanceof LinearEqualsConstraint) {
                str = " == ";
                ci = ((LinearEqualsConstraint) lcstr.get(i)).getC();
            }
            str = str + String.format(Locale.US, "%6.1f", lcstr.get(i).getRHS());
            if (i != 0)
                System.out.print("            ");
            for (int j = 0; j < lp.getDimension(); j++) {
                aij = ci[j];
                if (aij != 0) {
                    fs = String.format(Locale.US, "%+7.1f", aij);
                    System.out.print(fs + "*x[" + j + "]");
                } else
                    System.out.print("            ");
            }
            System.out.println(str);
        }
    }

    public double[] setStrategy(int P, String[] labels, PlayStrategy myStrategy) {
        int n = labels.length;
        double[] strategy = new double[n];
        for (int i = 0; i < n; i++)
            strategy[i] = 0;
        if (P == 1) { // if playing as player 1 then choose first action
            strategy[0] = 1;
        } else { // if playing as player 2 then choose first or second action randomly
            strategy[0] = 1;
        }
        for (int i = 0; i < n; i++)
            myStrategy.put(labels[i], strategy[i]);
        return strategy;
    }

    public void showStrategy(int P, double[] strategy, String[] labels) {
        System.out.println("Strategy Player " + P + ":");
        for (int i = 0; i < labels.length; i++)
            System.out.println("   " + strategy[i] + ":" + showLabel(labels[i]));
    }

}
