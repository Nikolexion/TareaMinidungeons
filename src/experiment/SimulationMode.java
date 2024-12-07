package experiment;

import controllers.MCTSController;
import dungeon.Dungeon;
import dungeon.DungeonLoader;
import dungeon.play.PlayMap;
import util.math2d.Matrix2D;
import util.statics.StatisticUtils;

import java.util.Scanner;
import java.io.File;

public class SimulationMode {
    final int totalRuns = 10;
    final int maxActions = 300;
    String outputFolder = "./testResults/";

    double[] hpRemaining;
    double[] monstersKilled;
    double[] treasuresCollected;
    double[] potionsDrunk;
    double[] actionsTaken;
    double[] tilesExplored;

    public void runExperiment(String filename) {
        String[] temp = filename.split("/");
        String mapFile = temp[temp.length-1];

        initMetrics();

        String asciiMap = "";
        try {
            asciiMap = new Scanner(new File(filename)).useDelimiter("\\A").next();
        } catch(Exception e){
            System.out.println(e.toString());
        }
        Dungeon testDungeon = DungeonLoader.loadAsciiDungeon(asciiMap);
        PlayMap testPlay = new PlayMap(testDungeon);
        for(int i=0;i<totalRuns;i++){
            testPlay.startGame();

            MCTSController testAgent = new MCTSController(testPlay, testPlay.getHero(), 10);

            int actions = 0;

            while(!testPlay.isGameHalted() && actions < maxActions){
                testPlay.updateGame(testAgent.getNextAction());
                actions++;
            }
            updateMetrics(i, testPlay, actions);
        }
        System.out.println(printMetrics(maxActions));
        System.out.println("---------------------------------------");
        System.out.println(printFullMetrics());
    }

    protected void initMetrics() {
        hpRemaining = new double[totalRuns];
        monstersKilled = new double[totalRuns];
        treasuresCollected = new double[totalRuns];
        potionsDrunk = new double[totalRuns];
        actionsTaken = new double[totalRuns];
        tilesExplored = new double[totalRuns];
    }

    protected void updateMetrics(int index, PlayMap finishedMap, int actions) {
        hpRemaining[index] = finishedMap.getHero().getHitpoints();
        monstersKilled[index] = Matrix2D.count(finishedMap.getDeadMonsterArray());
        treasuresCollected[index] = Matrix2D.count(finishedMap.getDeadRewardArray());
        potionsDrunk[index] = Matrix2D.count(finishedMap.getDeadPotionArray());
        actionsTaken[index] = actions;
        tilesExplored[index] = Matrix2D.count(finishedMap.getAnyVisited());
    }

    protected String printMetrics(int maxActions) {
        String result = "";
        result += "hpRemaining: " + StatisticUtils.average(hpRemaining) + " (" + StatisticUtils.standardDeviation(hpRemaining) + ")\n";
        result += "monstersKilled: " + StatisticUtils.average(monstersKilled) + " (" + StatisticUtils.standardDeviation(monstersKilled) + ")\n";
        result += "treasuresCollected: " + StatisticUtils.average(treasuresCollected) + " (" + StatisticUtils.standardDeviation(treasuresCollected) + ")\n";
        result += "potionsDrunk: " + StatisticUtils.average(potionsDrunk) + " (" + StatisticUtils.standardDeviation(potionsDrunk) + ")\n";
        result += "actionsTaken: " + StatisticUtils.average(actionsTaken) + " (" + StatisticUtils.standardDeviation(actionsTaken) + ")\n";
        result += "tilesExplored: " + StatisticUtils.average(tilesExplored) + " (" + StatisticUtils.standardDeviation(tilesExplored) + ")\n";
        return result;
    }

    protected String printFullMetrics() {
        String result = "";
        result += "hpRemaining;";
        for(int i=0;i<hpRemaining.length;i++){ result+=hpRemaining[i]+";"; }
        result += "\n";
        result += "monstersKilled;";
        for(int i=0;i<monstersKilled.length;i++){ result+=monstersKilled[i]+";"; }
        result += "\n";
        result += "treasuresCollected;";
        for(int i=0;i<treasuresCollected.length;i++){ result+=treasuresCollected[i]+";"; }
        result += "\n";
        result += "potionsDrunk;";
        for(int i=0;i<potionsDrunk.length;i++){ result+=potionsDrunk[i]+";"; }
        result += "\n";
        result += "actionsTaken;";
        for(int i=0;i<actionsTaken.length;i++){ result+=actionsTaken[i]+";"; }
        result += "\n";
        result += "tilesExplored;";
        for(int i=0;i<tilesExplored.length;i++){ result+=tilesExplored[i]+";"; }
        result += "\n";
        return result;
    }

    public static void main(String[] args) {
        SimulationMode exp = new SimulationMode();
        for(int i=0;i<=10;i++){
            System.out.println("\n--------------\nMAP"+i+"\n--------------\n");
            exp.runExperiment("./dungeons/map"+i+".txt");
        }
    }
}