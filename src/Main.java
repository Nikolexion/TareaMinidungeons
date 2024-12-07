import experiment.CompetitionMode;
import experiment.DebugMode;
import experiment.SimulationMode;


import java.io.File;

public class Main {
    public static void main(String[] args) {
        File folder = new File("dungeons");
        File[] listOfFiles = folder.listFiles((dir, name) -> name.endsWith(".txt"));

        if (listOfFiles != null) {
            for (File file : listOfFiles) {
                if (file.isFile()) {
                    String filename = file.getPath();
                    System.out.println("Running simulations for: " + filename);

                    // Ejecutar SimulationMode
                    SimulationMode simulationMode = new SimulationMode();
                    simulationMode.runExperiment(filename);

                    /* // Ejecutar DebugMode
                    DebugMode debugMode = new DebugMode();
                    debugMode.runTest(filename);

                    // Ejecutar CompetitionMode
                    CompetitionMode competitionMode = new CompetitionMode();
                    competitionMode.runCompetition(filename); */
                }
            }
        } else {
            System.out.println("No dungeon files found in the dungeons folder.");
        }
    }
}