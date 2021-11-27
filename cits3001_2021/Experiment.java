package cits3001_2021;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * This program will simulate the data needed for analysis of the performance of both agents (BasicRebel & Baerule) in
 * identifying the spies. The data gathered will consider how many times the agent correctly identifies both spies,
 * how many times the agent correctly identifies at least one of the spy, and how many times either agents wasn't able
 * to identify either of the spies.
 *
 * When doing simulations on different environments, please comment out the environments that are analysed and uncomment
 * the one is needed. This is due to the logic in Game.java and simultaneously running all environment simulations will
 * cause errors. The README.txt file should provide a detailed explanation on this regard.
 *
 * @author Josephine Bienes <22511218>
 * @since 14/10/2021
 * */

public class Experiment {

    /**
     * Helper function to get the index the number should be added.
     * @param spiesIdentified How many spies the agent has identified in a game.
     * @return The index that the number should be incremented.
     * */
    private static int getIndex(int spiesIdentified){
        return switch (spiesIdentified){
            case 2 -> 2;
            case 1 -> 1;
            default -> 0;
        };
    }


    /**
     * Helper function to log the relevant data in the txt file. More general overview of the data for every incremental
     * simulation of the games.
     * @param totalTimesIdentified 3 size array that contains the total number of 0, 1, 2 spies identified.
     * @param name Filename that this data will be stored in.
     * @param isBaerule If true, Baerule's data will be logged. Othewise BasicRebel's.
     * */
    private static void log(int[] totalTimesIdentified, String name, boolean isBaerule) throws IOException {
        File logF = new File(name);
        FileWriter log = new FileWriter(logF, true);

        if(isBaerule){
            log.write("Baerule's total identification: [0: "+totalTimesIdentified[0]+"],");
        }
        else{
            log.write("BasicRebel's total identification: [0: "+totalTimesIdentified[0]+"],");
        }
        log.write("[1: "+totalTimesIdentified[1]+"], ");
        log.write("[2: "+totalTimesIdentified[2]+"]" +"\n");
        log.close();
    }


    /**
     * Helper function to log the relevant data in the txt file. Add the individual data for each simulation round
     * rather than placed together, ie only need the data for Baerule when it identified only 1 spy, then index provided
     * should be 1.
     * @param totalTimesIdentified 3 size array that contains the total number of 0, 1, 2 spies identified.
     * @param name Filename that this data will be stored in.
     * @param index Index of the array that is going to be written from totalTimesIdentified array provided.
     * */
    private static void log( int[] totalTimesIdentified, String name, int index) throws IOException {
        File logF = new File(name);
        FileWriter log = new FileWriter(logF, true);

        log.write(totalTimesIdentified[index] + "\n");
        log.close();
    }


    /**
     * Helper function to log the relevant data in the txt file. This is if there are certain text that needs to be
     * added in the logfile.
     * @param name Filename that this data will be stored in.
     * @param msg String that you want to add in the logfile.
     * */
    private static void log( String name, String msg) throws IOException {
        File logF = new File(name);
        FileWriter log = new FileWriter(logF, true);

        log.write(msg+"\n");
        log.close();
    }


    public static void main(String[] args) throws IOException {

        //-----------------------------------------------------------------------------------------------------------
        // Amateur environment
        //-----------------------------------------------------------------------------------------------------------

        //Stores the total amount of times a spy has been determined; 1st is 0, 2nd is 1, and 3rd is 2
        int[] determinedSpiesBR = new int[3]; //Baerule
        int[] determinedSpiesB = new int[3]; //BasicRebel

        int c = 1, n = 2000;

        while(c<201){

            for(int i = 0; i<n; i++){
                Agent[] agents = {Baerule.init(),
                        BasicRebel.init(),
                        BasicRebel.init(),
                        BasicRebel.init(),
                        BasicRebel.init()};
                Game game = new Game(agents);

                //Analysis

                //Baerule
                int temp = game.determinedSpies(true);
                int ind = getIndex(temp);
                determinedSpiesBR[ind] += 1;

                //BasicRebel
                temp = game.determinedSpies(false);
                ind = getIndex(temp);
                determinedSpiesB[ind] += 1;
            }

            //Logs the data in the txt file for each simulation
            log("research_log_game_amateur.txt", "Simulation round: " + c);
            log(determinedSpiesBR, "research_log_game_amateur.txt", true);
            log(determinedSpiesB, "research_log_game_amateur.txt", false);
            log("research_log_game_amateur.txt", " ");

            //Logs individual aspects into different files (6 files in total for the 2 agents)

            //BR
            log(determinedSpiesBR, "0-spies-br-amateur.txt", 0);
            log(determinedSpiesBR, "1-spies-br-amateur.txt", 1);
            log(determinedSpiesBR, "2-spies-br-amateur.txt", 2);

            //B
            log(determinedSpiesB, "0-spies-b-amateur.txt", 0);
            log(determinedSpiesB, "1-spies-b-amateur.txt", 1);
            log(determinedSpiesB, "2-spies-b-amateur.txt", 2);

            //Reset
            determinedSpiesBR = new int[3];
            determinedSpiesB = new int[3];

            c++;
        }




        //-----------------------------------------------------------------------------------------------------------
        // Random environment - Go to the Game.java file and add in the conditional logic for selecting spies
        //-----------------------------------------------------------------------------------------------------------

        //Reset
//        int c = 1, n = 2000;
//
//        //Stores the total amount of times a spy has been determined; 1st is 0, 2nd is 1, and 3rd is 2
//        int[] determinedSpiesBR = new int[3]; //Baerule
//        int[] determinedSpiesB = new int[3]; //BasicRebel
//
//        while(c<200){
//
//            for(int i = 0; i<n; i++){
//                Agent[] agents = {Baerule.init(),
//                        BasicRebel.init(),
//                        RandomAgent.init(),
//                        RandomAgent.init(),
//                        RandomAgent.init()};
//                Game game = new Game(agents);
//
//                //Analysis
//
//                //Baerule
//                int temp = game.determinedSpies(true);
//                int ind = getIndex(temp);
//                determinedSpiesBR[ind] += 1;
//
//                //BasicRebel
//                temp = game.determinedSpies(false);
//                ind = getIndex(temp);
//                determinedSpiesB[ind] += 1;
//            }
//
//            //Logs the data in the txt file for each simulation
//            log("research_log_game_random.txt", "Total game simulations: " + n);
//            log(determinedSpiesBR, "research_log_game_random.txt", true);
//            log(determinedSpiesB, "research_log_game_random.txt", false);
//            log("research_log_game_random.txt", " ");
//
//            //Logs individual aspects into different files (6 files in total for the 2 agents)
//
//            //BR
//            log(determinedSpiesBR, "0-spies-br-random.txt", 0);
//            log(determinedSpiesBR, "1-spies-br-random.txt", 1);
//            log(determinedSpiesBR, "2-spies-br-random.txt", 2);
//
//            //B
//            log(determinedSpiesB, "0-spies-b-random.txt", 0);
//            log(determinedSpiesB, "1-spies-b-random.txt", 1);
//            log(determinedSpiesB, "2-spies-b-random.txt", 2);
//
//            //Reset
//            determinedSpiesBR = new int[3];
//            determinedSpiesB = new int[3];
//
//            c++;
//        }
    }
}
