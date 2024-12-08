package controllers;

import dungeon.play.GameCharacter;
import dungeon.play.PlayMap;
import dungeon.play.Hero;
import util.math2d.Point2D;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Queue;
import java.util.LinkedList;

public class MCTSController extends Controller {
    private Random random;
    private int iterations;
    private int[][] distanceMatrixMap;

    /**
     * Constructor de MCTSController.
     * @param playMap Mapa inicial
     * @param hero Personaje a ocupar para entrenamiento
     * @param iterations Iteraciones para la cantidad de repeticiones de algoritmo MCTS
     */
    public MCTSController(PlayMap playMap, GameCharacter hero, int iterations) {
        super(playMap, hero, "MCTSController");
        this.random = new Random();
        this.iterations = iterations;

        generateDistanceMatrix(playMap);
        for (int i = 0; i < distanceMatrixMap.length; i++) {
            for (int j = 0; j < distanceMatrixMap[i].length; j++) {
                System.out.print(distanceMatrixMap[i][j] + " "); // Imprime los elementos de la fila en la misma línea
            }
            System.out.println(); // Salto de línea después de cada fila
        }
        System.out.println("-------------------");
        System.out.println(playMap.toASCII());


        //System.out.println(playMap.toASCII());
    }

    /**
     * Función para retornar acción siguiente según MCTS.
     * @return Decisión de la mejor acción tomada por el algoritmo MCTS
     */
    @Override
    public int getNextAction() {
        //System.out.println("MCTSController.getNextAction()");
        return mcts();
    }

    /**
     * Función para generar una matriz que implementa BFS hasta la salida, considerando las colisiones.
     * <ul>
     *   <li><b>Muros</b>: -1</li>
     *   <li><b>Salida</b>: 0</li>
     *   <li><b>int &gt; 0</b>: Distancia (en movimientos) a la salida</li>
     * </ul>
     * @param playMap Mapa del juego en su estado inicial
     */
    public void generateDistanceMatrix(PlayMap playMap) {
        int mapSizeX = playMap.getMapSizeX();
        int mapSizeY = playMap.getMapSizeY();
        distanceMatrixMap = new int[mapSizeX][mapSizeY];

        // Inicializar la matriz con -1 para muros y Integer.MAX_VALUE para celdas transitables
        // Se invierten x e y porque se ve el plano como cartesiano, no como matriz
        for (int x = 0; x < mapSizeX; x++) {
            for (int y = 0; y < mapSizeY; y++) {
                if (isWall(playMap, x, y)) {
                    distanceMatrixMap[y][x] = -1;
                } else {
                    distanceMatrixMap[y][x] = Integer.MAX_VALUE;
                }
            }
        }
        Point2D mainExit = playMap.getExit(1);
        System.out.println(mainExit);
        distanceMatrixMap[(int) mainExit.y][(int) mainExit.x] = 0;

        // BFS para calcular las distancias
        Queue<Point2D> queue = new LinkedList<>();
        queue.add(mainExit);

        int[] dx = {-1, 1, 0, 0};
        int[] dy = {0, 0, -1, 1};

        while (!queue.isEmpty()) {
            Point2D current = queue.poll();
            int currentDistance = distanceMatrixMap[(int) current.y][(int) current.x];

            for (int i = 0; i < 4; i++) {
                int newX = (int) current.x + dx[i];
                int newY = (int) current.y + dy[i];

                if (playMap.isWithinBounds(newX, newY) && !isWall(playMap, newX, newY) && distanceMatrixMap[newY][newX] == Integer.MAX_VALUE) {
                    distanceMatrixMap[newY][newX] = currentDistance + 1;
                    queue.add(new Point2D(newX, newY));
                }
            }
        }
    }

    /**
     * Función auxiliar booleana para la matriz de distancias, verifica si la casilla actual es colisión.
     * @param playMap Mapa actual
     * @param x Coordenada x
     * @param y Coordenada y
     * @return Verdadero si es muro, Falso en caso contrario
     */
    private boolean isWall(PlayMap playMap, int x, int y) {
        return !playMap.isPassable(x, y);
    }

    @Override
    public void reset() {
        // Reiniciar cualquier estado necesario
    }

    /**
     * Algoritmo principal de llamada a las etapas de MCTS que se repite según las iteraciones dadas.
     * @return La acción escogida {0,1,2,3} tras las etapas de selección, expansión, simulación y propagación
     */
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

    /**
     * Función que selecciona el nodo hijo con el mayor valor UCB1.
     * Itera entre los nodos hijos del nodo actual hasta alcanzar un nodo hoja.
     * @param node Nodo inicial para la selección
     * @return Nodo hoja seleccionado tras aplicar el criterio UCB1
     */
    private Node select(Node node) {
       /*  System.out.println("MCTS: Selecting node"); */
        while (!node.children.isEmpty()) {
            node = node.children.stream().max((n1, n2) -> Double.compare(ucb1(n1), ucb1(n2))).get();
        }
        return node;
    }

    /**
     * Expande un nodo generando sus hijos a partir de todas las acciones posibles y legales.
     * @param node Nodo a expandir
     */
    private void expand(Node node) {
        /* System.out.println("MCTS: Expanding node"); */
        for (int action = 0; action < 4; action++) { // Asumiendo 4 acciones posibles
            PlayMap newMap = node.playMap.clone();
            Point2D pos = newMap.getHero().getNextPosition(action);
            if (newMap.isValidMove(pos)) {
                newMap.updateGame(action);
                node.children.add(new Node(node, action, newMap));
            }
        }
    }
    /**
     * Simula una partida aleatoria desde el estado actual del nodo hasta que el juego termine.
     * Realiza movimientos aleatorios válidos y calcula el premio final.
     * @param node Nodo inicial de la simulación
     * @return Recompensa calculada al finalizar la simulación
     */
    private double simulate(Node node) {
        PlayMap simulationMap = node.playMap.clone();
        while (!simulationMap.isGameHalted()) {
            int action;
            Point2D position;
            do {
                action = random.nextInt(4); // Elige una acción al azar
                position = simulationMap.getHero().getNextPosition(action);
            } while (!simulationMap.isValidMove(position)); // Verifica validez antes de ejecutar

            simulationMap.updateGame(action); // Solo realiza la acción si es válida
        }
        return calculateReward(simulationMap);
    }

    /*
     * TODO: Editar función para que ocupe la matriz con BFS, premiar también por tesoros y pociones,
     *  penalizar por pisar monstruos, quitar Manhattan
     */

    /**
     * Función de cálculo de premios
     * @param state Mapa actual
     * @return Recompensa por el estado actual
     */
    private double calculateReward(PlayMap state) {
        double reward = 0.0;
        Point2D heroPosition = state.getHero().getPosition();
        Point2D exitPosition = state.getExit(1); // Suponiendo que la salida principal es la de índice 0
        //System.out.println(exitPosition);
        double distanceToExit = manhattan_distance(heroPosition, exitPosition);
        reward += 5.0/ (distanceToExit+1);
        if (distanceToExit == 0) {
            reward += 100;
        }

        // Recompensa inversamente proporcional a la distancia
        // Cuanto más cerca, mayor la recompensa (evitamos división por 0 sumando 1)
        return reward;
    }

    private double manhattan_distance(Point2D p1, Point2D p2){
        return Math.abs(p1.x - p2.x) + Math.abs(p1.y - p2.y);
    }

    /**
     * Propaga la recompensa desde un nodo hoja hasta la raíz del árbol.
     *
     * @param node Nodo desde el cual comienza la propagación
     * @param reward Recompensa obtenida en la simulación
     */
    private void backpropagate(Node node, double reward) {
        /* System.out.println("MCTS: Backpropagating reward"); */
        while (node != null) {
            node.visits++;
            node.value += reward;
            node = node.parent;
        }
    }

    /*
     * TODO: Intentar con otros valores para C_p
     */

    /**
     * Calcula el valor UCB1 de un nodo.
     * @param node Nodo para el cual se calcula el valor UCB1
     * @return Valor UCB1 del nodo
     */
    private double ucb1(Node node) {
        return node.value / node.visits + (Math.sqrt(2 * Math.log(node.parent.visits) / node.visits));
    }

    /**
     * Determina la mejor acción a partir del nodo raíz basada en el número de visitas.
     * @param root Nodo raíz del árbol
     * @return Acción asociada al nodo hijo más visitado
     */
    private int bestAction(Node root) {
        /* System.out.println("MCTS: Selecting best action"); */
        return root.children.stream().max((n1, n2) -> Double.compare(n1.visits, n2.visits)).get().action;
    }

    /**
     * Nodo para el árbol de búsqueda MCTS.
     */
    private class Node {
        Node parent;
        List<Node> children;
        int action;
        PlayMap playMap;
        int visits;
        double value;

        /**
         * Constructor de Nodo.
         * @param parent Nodo padre
         * @param action Acción que llevó a este nodo
         * @param playMap Estado del juego asociado al nodo
         */
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