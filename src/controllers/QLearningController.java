package controllers;

import dungeon.play.GameCharacter;
import dungeon.play.PlayMap;
import util.math2d.Point2D;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;


public class QLearningController extends Controller {

    /* Las constantes para el Q-Learning fueron sacadas del paper:
     Generative Agents for Player Decision Modeling in Games.
     La Q-table consta de un String(Mapa + vida de Caracter) y los valores Q para cada acción
    */
    private final HashMap<String, double[]> table;
    private final double alpha = 0.5;   //Learning Rate
    private final double gamma = 0.9;   //Discount factor
    private final double epsilon = 0.5;   //Exploration rate, Greddy La idea es cambiarlo de forma linear igual que en
    public static final int N_ACTIONS = 4; // UP(0), RIGTH(1), DOWN(2), LEFT(3), no consideraremos IDLE (no tiene sentido)

    Random random;
    private String prevState;
    private int prevAction;

    /**
     * Constructor de QLearningController
     * @param map mapa de juego que se utilizara
     * @param controllingChar Caracter que usara QLearning
     */
    public QLearningController(PlayMap map, GameCharacter controllingChar){
        super(map, controllingChar, "QlearningController");
        this.table = new HashMap<>();
        this.random = new Random();
        this.prevState = getCurrentState();
        this.prevAction = PlayMap.IDLE;
    }

    /**
     * A partir de mapa de Juego, se genera un String con el mapa & vida del jugador(health) por niveles.
     * Los niveles son: (0) si 31 <= health, (1) 15 <= health < 31, (2) 6 <= health < 15, (3) 0 <= health < 6.
     *
     * @return key de Q_table
     */
    public String getCurrentState(){

        // Quizas guardar el estado completo no es lo mejor, pero igual así lo hicieron en el paper
        String result="";
        for(int y = 0; y< map.getMapSizeY(); y++){
            for(int x = 0; x< map.getMapSizeX(); x++){
                if(map.isEmpty(x,y)){
                    result+=".";
                } else if(map.isHero(x,y)){
                    result+="@";
                } else if(map.isEntrance(x,y)){
                    result+="E";
                } else if(map.isExit(x,y)){
                    result+="X";
                } else if(map.isMonster(x,y)){
                    result+="m";
                } else if(map.isReward(x,y)){
                    result+="r";
                } else if(map.isPotion(x,y)){
                    result+="p";
                } else {
                    result+="#";
                }
            }
        }
            int hero_health = map.getHero().getHitpoints();
            short num = -1;
            if ( 31 <= hero_health ) {
                num = 3;
            }else if (15 <= hero_health){
                num = 2;
            }else if (6 <= hero_health){
                num = 1;
            } else if (0 <= hero_health) {
                num = 0;
            }

        result+= num;
        return result;
    }

    /**
     * A partir de un estado, obtenemos los valores Q de la Q_table.
     * @param state clave del estado en Q_table
     */
    private double[] getQValues(String state) {
        // Si no hay estado lo crea uno jeje.
        return table.computeIfAbsent(state, k -> new double[N_ACTIONS]); // defecto: [0, 0, 0, 0]
    }

    /**
     * Obtenemos el valor más grande de una fila de la Q table.
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
     * @param prevState estado previo.
     * @param action acción hecha en el estado previo.
     * @param reward recompensa del estado actual.
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
     * Funcion que calcula la recompenza del estado actual.
     */
    private double computeReward() {
        // nota: Para obtener estado actual usar variable map.
        // TODO: Es una función muy mala, hay que mejorarla !!!!!!
        Point2D charPos = controllingChar.getPosition();
        Point2D exitPos = map.getExit(0);
        double distanceToExit = Math.hypot(charPos.x - exitPos.x, charPos.y - exitPos.y);

        double reward = 100/distanceToExit;

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

        // Actualizar Q-Table
        if (prevState != null) {
            // Esto se calcula a partir del estado actua
            double reward = computeReward();
            String prevStateKey = prevState;
            updateQTable(prevStateKey, prevAction, reward, qValues);
        }

        // Actualizar estados
        prevState = currentState;
        prevAction = action;

        // TODO: PRINT HASH
        printHashMap();

        return action;
    }

    /**
     * Función Debug, esta muestra el contenido de la Hash table, usado para verificar que el algoritmo funciona.
     */
    public void printHashMap() {
        System.out.println("HashMap:");
        for (Map.Entry<String, double[]> entry : table.entrySet()) {
            String key = entry.getKey();
            double[] values = entry.getValue();

            // imprme la clave
            System.out.print(key + " : ");

            // imprime los valores asociados a la clave
            System.out.print("[");
            for (int i = 0; i < values.length; i++) {
                System.out.print(values[i]);
                if (i < values.length - 1) {
                    System.out.print(", ");
                }
            }
            System.out.println("]");
        }
    }




}
