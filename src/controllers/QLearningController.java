package controllers;

import dungeon.play.GameCharacter;
import dungeon.play.PlayMap;
import util.math2d.Point2D;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Esta clase es el Controller que utiliza Q_Learning. Cabe mencionar que las constantes del Modelo fueron sacadas del paper
 * "Generative Agents for Player Decision Modeling in Games".
 */
public class QLearningController extends Controller {

    /* Las constantes para el Q-Learning fueron sacadas del paper:
     Generative Agents for Player Decision Modeling in Games.
     La Q-table consta de un String(Mapa + vida de Caracter) y los valores Q para cada acción
    */
    private final HashMap<String, double[]> table;
    private final double alpha = 0.5;   //Learning Rate
    private final double gamma = 0.9;   //Discount factor
    private double epsilon;   //Exploration rate, Greddy
    public static final int N_ACTIONS = 4; // UP(0), RIGTH(1), DOWN(2), LEFT(3), no consideraremos IDLE (no tiene sentido)
    public static final int HEALTH_LEVELS = 4; // 0, 1, 2,3  con 0 poca vida y 3 mucha vida
    private boolean train;
    private int[][] distancesFromExit;

    Random random;
    private String prevState;
    private int prevAction;

    /**
     * Constructor de QLearningController
     *
     * @param map             mapa de juego que se utilizara
     * @param controllingChar Caracter que usara QLearning
     * @param setTrain        Si es verdadero, se entrena el modelo, No se actualiza la tabla.
     * @param fileName        Nombre del archivo con el modelo
     */
    public QLearningController(PlayMap map, GameCharacter controllingChar, boolean setTrain, String fileName) {
        super(map, controllingChar, "QlearningController");
        this.random = new Random();
        this.prevState = getCurrentState();
        this.prevAction = PlayMap.IDLE;

        this.train = setTrain;

        if (train) {
            this.table = new HashMap<>();
            epsilon = 1;
        } else {
            this.table = getQTableFromCSV(fileName);
            epsilon = 0;
        }

        distancesFromExit = getDistancesFromExit();
    }

    /**
     * A partir de mapa de Juego, se genera un String con el mapa & vida del jugador(health) por niveles.
     * Los niveles son: (0) si 31 <= health, (1) 15 <= health < 31, (2) 6 <= health < 15, (3) 0 <= health < 6.
     *
     * @return key de Q_table
     */
    public String getCurrentState() {
        Point2D heroPos = map.getHero().getPosition();
        int heroX = (int) heroPos.x;
        int heroY = (int) heroPos.y;

        StringBuilder result = new StringBuilder();

        for (int y = heroY - 2; y <= heroY + 2; y++) {
            for (int x = heroX - 2; x <= heroX + 2; x++) {
                if (x < 0 || y < 0 || x >= map.getMapSizeX() || y >= map.getMapSizeY()) {
                    result.append("#"); // Fuera del mapa se considera pared
                } else if (map.isHero(x, y)) {
                    result.append("@");
                } else if (map.isEmpty(x, y)) {
                    result.append(".");
                } else if (map.isEntrance(x, y)) {
                    result.append("E");
                } else if (map.isExit(x, y)) {
                    result.append("X");
                } else if (map.isMonster(x, y)) {
                    result.append("m");
                } else if (map.isReward(x, y)) {
                    result.append("r");
                } else if (map.isPotion(x, y)) {
                    result.append("p");
                } else {
                    result.append("#"); // Paredes u obstáculos
                }
            }
        }

        // Agregar nivel de la salud del héroe al final del estado
        int heroHealth = map.getHero().getHitpoints();
        int healthLevel = (heroHealth >= 31) ? 3 : (heroHealth >= 15) ? 2 : (heroHealth >= 6) ? 1 : 0;
        result.append(healthLevel);

        return result.toString();
    }

    /**
     * A partir de un estado, obtenemos los valores Q de la Q_table.
     *
     * @param state clave del estado en Q_table
     */
    private double[] getQValues(String state) {
        // Si no hay estado lo crea uno jeje.
        return table.computeIfAbsent(state, k -> new double[N_ACTIONS]); // defecto: [0, 0, 0, 0]
    }

    /**
     * Obtenemos el valor más grande de una fila de la Q table.
     *
     * @param qValues fila con valores Q para un estado.
     */
    private int maxQAction(double[] qValues) {
        int bestAction = 0;
        double maxQ = qValues[0];
        for (int i = 1; i < qValues.length; i++) {
            if (qValues[i] > maxQ) {
                maxQ = qValues[i];
                bestAction = i;
            }
        }
        return bestAction;
    }

    /**
     * Actualiza los valores de la Q table a partir de la formula de Q_Learning.
     *
     * @param prevState      estado previo.
     * @param action         acción hecha en el estado previo.
     * @param reward         recompensa del estado actual.
     * @param currentQValues Valores Q del estado actual.
     */
    private void updateQTable(String prevState, int action, double reward, double[] currentQValues) {

        double[] prevQValues = getQValues(prevState);   // Valores Q, nota: esto devuelve una referencia al array de la
        // hash table, por lo que si se modifica más adelante.
        double maxNextQ = currentQValues[maxQAction(currentQValues)]; // Escoger maximo

        if (action != -1) {
            // Si la acción no es IDLE cambiamos Q_table (IDLE = KK)
            prevQValues[action] = prevQValues[action] + alpha * (reward + gamma * maxNextQ - prevQValues[action]);
        }
    }

    /**
     * Funcion que calcula la recompensa del estado actual.
     */
    private double computeReward() {
        // nota: Para obtener estado actual usar variable map.
        // TODO: Es una función muy mala, hay que mejorarla !!!!!!

        int index = 0;

        if (prevAction == PlayMap.UP) { // UP
            index = 5 * 1 + 2;

        } else if (prevAction == PlayMap.RIGHT) { // RIGTH
            index = 5 * 2 + 3;

        } else if (prevAction == PlayMap.DOWN) { // DOWN
            index = 5 * 3 + 2;

        } else if (prevAction == PlayMap.LEFT) { // LEFT
            index = 5 * 2 + 1;
        }

        char reward_char = prevState.charAt(index);

        // Transformar de char a int
        int health_level = prevState.charAt(prevState.length() - 1) - 48;
        double reward = 0;

        if (reward_char == '.') { // Vacio
            reward = -1;
        } else if (reward_char == 'E') { // Entrada
            reward = -1;
        } else if (reward_char == 'X') { // Salida
            reward = 1000;
        } else if (reward_char == 'm') { // Mounstro
            reward = -20 * (HEALTH_LEVELS - health_level);
        } else if (reward_char == 'r') { // Recompensa
            reward = 35;
        } else if (reward_char == 'p') { // Poción
            reward = 5 * (HEALTH_LEVELS - health_level);
        } else if (reward_char == '@') { // Heroe
            reward = -10;
        } else { // Paredes
            reward = -10;
        }

        Point2D heroCoord = map.getHero().getPosition();
        int heroX = (int) heroCoord.x;
        int heroY = (int) heroCoord.y;
        int distance = distancesFromExit[heroY][heroX];

        if (distance == -1){
           reward += -200; // esto no deberia pasar pero porsiacasoo
            System.out.println("Esto no deberia estar pasando ....");
        } else {
            reward -= distance * 2;
        }

        // 0, 1,2,3 al tener menos vida quere tener pociones, al tener más vida, querer tener mounstruos,
        // System.out.println("Recompensa de ir a "  + reward_char + " con [" + health_level + "] De HP: " + reward);
        return reward;

    }

    /**
     * Se obtiene el proximo movimiento que efectuara el agente, este puede ser aleatorio o escogido mediante la
     * Q_table (valor máximo). Esta probabilidad depende la constante epsilon propia de la clase QLearningController,
     * en valores altos el agente toma más deciciones aleatorias.
     */
    public int getNextAction() {
        String currentState = getCurrentState();
        double[] qValues = getQValues(currentState);
        int action;

        //  epsilon Greedy, para que derrepente cambie de ruta
        if (random.nextDouble() < epsilon) {
            // Accion random
            action = random.nextInt(N_ACTIONS);
        } else {
            // Elegir mejor acción (Q-Value)
            action = maxQAction(qValues);
        }

        if (train) {
            // Actualizar Q-Table
            if (prevState != null) {
                // Esto se calcula a partir del estado actua
                double reward = computeReward();
                updateQTable(prevState, prevAction, reward, qValues);
            }
        }

        // Actualizar estados
        prevState = currentState;
        prevAction = action;

        return action;
    }

    /**
     * Guarda la Qtable en un archivo .csv
     *
     * @param fileName dirección del archivo.
     */
    public void saveTable(String fileName) {

        try (FileWriter writer = new FileWriter(fileName)) {
            // Escribir los encabezados
            writer.write("key");
            for (int i = 1; i <= table.values().iterator().next().length; i++) {
                writer.write(",a_" + i);
            }
            writer.write("\n");

            // Escribir los datos
            for (String key : table.keySet()) {
                writer.write(key);
                double[] values = table.get(key);
                for (double value : values) {
                    writer.write("," + value);
                }
                writer.write("\n");
            }

            System.out.println("Archivo CSV creado exitosamente: " + fileName);
        } catch (IOException e) {
            System.err.println("Error al escribir el archivo CSV: " + e.getMessage());
        }
    }

    /**
     * Carga la Qtable desde un archivo .csv
     *
     * @param fileName dirección del archivo.
     */
    public HashMap<String, double[]> getQTableFromCSV(String fileName) {

        HashMap<String, double[]> hashMap = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            // Leer la primera línea (encabezados)
            String line = reader.readLine(); // Ignorar encabezados

            // Leer las líneas restantes
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(","); // Dividir por comas

                // Primera parte es la clave
                String key = parts[0];

                // Las demás partes son los valores, convertidos a double
                double[] values = new double[parts.length - 1];
                for (int i = 1; i < parts.length; i++) {
                    values[i - 1] = Double.parseDouble(parts[i]);
                }

                // Guardar en la HashMap
                hashMap.put(key, values);
            }

            System.out.println("HashMap creada exitosamente:");

        } catch (IOException e) {
            System.err.println("Error al leer el archivo CSV: " + e.getMessage());
            return null;
        } catch (NumberFormatException e) {
            System.err.println("Error al convertir los valores a double: " + e.getMessage());
            return null;
        }

        return hashMap;
    }

    /**
     * Actualiza el epsilon con la formula epsilon*rate
     * @param rate ratio de actualización [0, 1]
     */
    public void updateEpsilon(double rate) {
        epsilon = Math.max(0.1, epsilon * rate);
    }

    /**
     * Función que devuelve una matriz de enteros de las mismas dimensiones del mapa, en esta están marcadas las distancias
     * desde cada bloque hasta la salida del nivel, las paredes se marcan con -1.
     * Si tenemos un bloque con coordenadas (x,y) esta guardado en la matriz como array[y][x]
     */
    public int[][] getDistancesFromExit(){
        int mapSizeX = map.getMapSizeX();
        int mapSizeY = map.getMapSizeY();

        // Crear la matriz de distancias inicializada con -1
        int[][] distances = new int[mapSizeY][mapSizeX];
        for (int y = 0; y < mapSizeY; y++) {
            Arrays.fill(distances[y], -1); // -1 representa paredes u obstáculos
        }
        Point2D exitPosition = new Point2D(0,0);

        // Obtener la posición de la salida, se hace así porque la entrada igual se considera una
        // salida ._.
        for (int i = 0; i < map.getMapSizeY(); i++){
            for (int j = 0; j< map.getMapSizeX(); j++){
                if (map.isExit(j,i) && !map.isEntrance(j,i)){
                    exitPosition = new Point2D(j,i);
                }
            }
        }

        int exitX = (int) exitPosition.x;
        int exitY = (int) exitPosition.y;

        // Cola para realizar la búsqueda BFS
        Queue<Point2D> queue = new LinkedList<>();
        queue.add(exitPosition);
        distances[exitY][exitX] = 0; // Distancia desde la salida hasta sí misma es 0

        // Movimientos posibles: UP, RIGHT, DOWN, LEFT
        int[] dX = {0, 1, 0, -1};
        int[] dY = {-1, 0, 1, 0};

        // BFS para calcular las distancias
        while (!queue.isEmpty()) {
            Point2D current = queue.poll();
            int currentX = (int) current.x;
            int currentY = (int) current.y;

            for (int i = 0; i < 4; i++) {
                int newX = currentX + dX[i];
                int newY = currentY + dY[i];

                // Validar los límites del mapa y evitar obstáculos o posiciones ya visitadas
                if (newX >= 0 && newX < mapSizeX && newY >= 0 && newY < mapSizeY && distances[newY][newX] == -1) {
                    if (map.isEmpty(newX, newY) || map.isHero(newX, newY) || map.isMonster(newX, newY) ||
                            map.isReward(newX, newY) || map.isPotion(newX, newY)) {
                        // Actualizar distancia y agregar la celda a la cola
                        distances[newY][newX] = distances[currentY][currentX] + 1;
                        queue.add(new Point2D(newX, newY));
                    }
                }
            }
        }

        return distances;
    }
}