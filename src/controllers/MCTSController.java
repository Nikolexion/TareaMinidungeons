package controllers;

import dungeon.play.GameCharacter;
import dungeon.play.PlayMap;

public class MCTSController implements Controller {
    private PlayMap playMap;
    private GameCharacter hero;

    public MCTSController(PlayMap playMap, GameCharacter hero) {
        this.playMap = playMap;
        this.hero = hero;
    }

    @Override
    public int getNextAction() {
        // Implementar el algoritmo Monte Carlo Tree Search aquí
        return mcts();
    }

    @Override
    public void reset() {
        // Reiniciar cualquier estado necesario
    }

    private int mcts() {
        // Implementar la lógica de MCTS aquí
        // Retornar la acción seleccionada (0, 1, 2, 3)
        return 0; // Placeholder
    }
}