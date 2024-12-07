package controllers;

import dungeon.play.GameCharacter;
import dungeon.play.PlayMap;
import dungeon.play.Hero;
import util.math2d.Point2D;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MCTSController extends Controller {
    private Random random;
    private int iterations;

    public MCTSController(PlayMap playMap, GameCharacter hero, int iterations) {
        super(playMap, hero, "MCTSController");
        this.random = new Random();
        this.iterations = iterations;
    }

    @Override
    public int getNextAction() {
        //System.out.println("MCTSController.getNextAction()");
        return mcts();
    }

    @Override
    public void reset() {
        // Reiniciar cualquier estado necesario
    }

    private int mcts() {
        //System.out.println("MCTS: Starting MCTS");
        Node root = new Node(null, -1, map.clone());
        for (int i = 0; i < iterations; i++) { // Número de iteraciones
            /* System.out.println("MCTS: Iteration " + i); */
            Node node = select(root);
            if (!node.playMap.isGameHalted()) {
                expand(node);
                Node child = node.children.get(random.nextInt(node.children.size()));
                double reward = simulate(child);
                backpropagate(child, reward);
            }
        }
        int bestAction = bestAction(root);
        /* System.out.println("MCTS: Best action selected: " + bestAction); */
        return bestAction;
    }

    private Node select(Node node) {
       /*  System.out.println("MCTS: Selecting node"); */
        while (!node.children.isEmpty()) {
            node = node.children.stream().max((n1, n2) -> Double.compare(ucb1(n1), ucb1(n2))).get();
        }
        return node;
    }

    private void expand(Node node) {
        /* System.out.println("MCTS: Expanding node"); */
        for (int action = 0; action < 4; action++) { // Asumiendo 4 acciones posibles
            PlayMap newMap = node.playMap.clone();
            newMap.updateGame(action);
            node.children.add(new Node(node, action, newMap));
        }
    }

    private double simulate(Node node) {
        /* System.out.println("MCTS: Simulating node"); */
        PlayMap simulationMap = node.playMap.clone();
        while (!simulationMap.isGameHalted()) {
            int action = random.nextInt(4); // Asumiendo 4 acciones posibles
            simulationMap.updateGame(action);
        }
        double reward = calculateReward(simulationMap);
        /* System.out.println("MCTS: Simulation reward: " + reward); */
        return reward;
    }

    private double calculateReward(PlayMap state) {
        double reward = 0.0;
        Hero hero = state.getHero();
        int score = hero.getScore();
        reward += score;

        // Penalizamos por perder vida
        int hp = hero.getHitpoints();
        int maxHp = hero.getStartingHitpoints();
        int healthFraction = maxHp - hp;
        reward -= (1.0 - healthFraction) * 5.0;

        // Premio mediano por encontrar la salida
        Point2D posHero = hero.getPosition();
        Point2D posExit = state.getExit(0);
        double distanceToExit = manhattan_distance(posHero, posExit);
        reward += 20.0 / (distanceToExit + 1);

        // Gran premio por encontrar tesoros
        int totalTreasures = state.rewardChars.size();
        int actualTreasures = totalTreasures - state.getDeadRewardArray().length;
        reward += actualTreasures * 10.0;

        int actualMonstersKilled = state.monsterChars.size() - state.getDeadMonsterArray().length;
        reward += 4 * actualMonstersKilled;

        int actualPotionsUsed = state.monsterChars.size() - state.getDeadPotionArray().length;
        reward += actualPotionsUsed * 2;

        return reward;
    }

    private double manhattan_distance(Point2D p1, Point2D p2){
        return Math.abs(p1.x - p2.x) + Math.abs(p1.y - p2.y);
    }

    private void backpropagate(Node node, double reward) {
        /* System.out.println("MCTS: Backpropagating reward"); */
        while (node != null) {
            node.visits++;
            node.value += reward;
            node = node.parent;
        }
    }

    private double ucb1(Node node) {
        return node.value / node.visits + Math.sqrt(2 * Math.log(node.parent.visits) / node.visits);
    }

    private int bestAction(Node root) {
        /* System.out.println("MCTS: Selecting best action"); */
        return root.children.stream().max((n1, n2) -> Double.compare(n1.visits, n2.visits)).get().action;
    }

    private class Node {
        Node parent;
        List<Node> children;
        int action;
        PlayMap playMap;
        int visits;
        double value;

        Node(Node parent, int action, PlayMap playMap) {
            this.parent = parent;
            this.action = action;
            this.playMap = playMap;
            this.children = new ArrayList<>();
            this.visits = 0;
            this.value = 0;
        }
    }
}