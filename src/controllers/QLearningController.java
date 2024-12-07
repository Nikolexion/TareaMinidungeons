package controllers;

import dungeon.play.GameCharacter;
import dungeon.play.PlayMap;
import util.math2d.Point2D;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
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
    private double epsilon = 1;   //Exploration rate, Greddy La idea es cambiarlo de forma linear igual que en
    public static final int N_ACTIONS = 4; // UP(0), RIGTH(1), DOWN(2), LEFT(3), no consideraremos IDLE (no tiene sentido)
    public static final int HEALTH_LEVELS = 4; // 0, 1, 2,3  con 0 poca vida y 3 mucha vida
    private int LIMIT;
    private int counter;
    private boolean train;

    Random random;
    private String prevState;
    private int prevAction;

    /**
     * Constructor de QLearningController
     * @param map mapa de juego que se utilizara
     * @param controllingChar Caracter que usara QLearning
     * @param setTrain Si es verdadero, se entrena el modelo, No se actualiza la tabla.
     * @param fileName Nombre del archivo con el modelo
     * @param LIMIT Cantidad de Runs con epsilon = 1
     */
    public QLearningController(PlayMap map, GameCharacter controllingChar, boolean setTrain, String fileName, int LIMIT){
        super(map, controllingChar, "QlearningController");
        this.random = new Random();
        this.prevState = getCurrentState();
        this.prevAction = PlayMap.IDLE;
        this.LIMIT = LIMIT * 300; //
        this.counter = 0;
        this.train = setTrain;

        if (train){
            this.table = new HashMap<>();
        }
        else {
           this.table = getQTableFromCSV(fileName);
        }
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

        int x = (int) map.getHero().getPosition().x;
        int y = (int) map.getHero().getPosition().y;

        int index = map.getMapSizeX() * y + x;

        char reward_char = prevState.charAt(index);
        // Transformar de char a int
        int health_level = prevState.charAt(prevState.length()-1) - 48;
        double reward = 0;

        if(reward_char == '.'){ // Vacio
            reward = -1;
        } else if(reward_char == 'E'){ // Entrada
            reward = -1;
        } else if(reward_char == 'X'){ // Salida
            reward = 1000;
        } else if(reward_char == 'm'){ // Mounstro
            reward = - 20 * (HEALTH_LEVELS - health_level);
        } else if(reward_char == 'r'){ // Recompensa
            reward =  35;
        } else if(reward_char == 'p'){ // Poción
            reward =  5 * (HEALTH_LEVELS - health_level);
        } else if (reward_char == '@') { // Heroe
            reward = -10;
        } else { // Paredes
            reward = -10;
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

        if(counter < LIMIT) {
            epsilon = Math.max(0.1, epsilon * 0.99);
        } else{
            counter++;
        }

        //  epsilon Greedy, para que derrepente cambie de ruta
        if (random.nextDouble() < epsilon) {
            // Accion random
            action = random.nextInt(N_ACTIONS);
        } else {
            // Elegir mejor acción (Q-Value)
            action = maxQAction(qValues);
        }

        if (train){
            // Actualizar Q-Table
            if (prevState != null) {
                // Esto se calcula a partir del estado actua
                double reward = computeReward();
                String prevStateKey = prevState;
                updateQTable(prevStateKey, prevAction, reward, qValues);
            }
        }

        // Actualizar estados
        prevState = currentState;
        prevAction = action;

        // TODO: PRINT HASH
        //printHashMap();
        //System.out.println(table.size());

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

    public void saveTable(String fileName){

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

    public HashMap<String , double []> getQTableFromCSV(String fileName){

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

    }