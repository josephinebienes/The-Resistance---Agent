package cits3001_2021;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * An Agent implementation to play in the game "The Resistance" for the tournament. This agent used the same method in
 * getting the suspicion values as agent Baerule.java. Unlike Baerule though, this agent is customized to play on a
 * different number of players of the game; dynamic now. This agent can also act as a spy based on BasicRebel.
 *
 * @author Josephine Bienes <22511218>
 * @since 22/10/2021
 */

public class Agent_22511218 implements Agent{
    private final String name;
    private int numPlayers;
    private int id; //Id number for the agent in the game
    private boolean isSpy;
    private Set<Integer> comrades; //Stores other spies if itself is one
    private static int agentCount;
    private Map<Integer, Double> suspicionValue; //Stores the suspicion values for each player
    private int voteCountForMission; /*Stores the amount of times a vote has failed; if on 5th vote session always
                                        vote yes*/
    private int[] players;
    private int roundsLost;
    private int currentRound = 1;
    int[] suspectedSpies; //Stores in the most susppicious players based on their suspicion values

    //Stores the probabilities of being a spy
    private double[] roundFail1; //1 total number of fails
    private double[] roundFail2; //2 total number of fails
    private double[] roundFail3; //3 total number of fails
    private double[] roundFail4; //4 total number of fails

    //Stores the probabilities of spies failing the missions given the round; for rounds with 2 players only, if 2 fails
    //automatically both players are spies and the rest are resistance
    private double[] spyFailRound;

    //The probability of a resistance member failing the mission will always be 0.1 for every round; will always try to
    //win every round
    private static final double RESFAIL = 0.1;


    /**
     * Creates the name of the agent.
     */
    public Agent_22511218(String name) {
        isSpy = false;
        this.name = name;
    }


    /**
     * Returns an instance of this agent for testing.
     * The program should allocate the agent's name,
     * and can use a counter to ensure no two agents have the same name.
     *
     * @return an instance of the agent.
     **/
    public static Agent init() {
        return switch (agentCount++) {
            case 0 -> new Agent_22511218("22511218");
            case 1 -> new Agent_22511218("22511218-2nd");
            case 2 -> new Agent_22511218("22511218-3rd");
            default -> new Agent_22511218("22511218-" + agentCount + "th");
        };
    }


    /**
     * Gets the name of the agent
     *
     * @return the agent's name.
     **/
    public String getName() {
        return name;
    }


    /**
     * Initialises a new game.
     * The agent should drop their current gameState and reinitialise all their game variables.
     * @param numPlayers  the number of players in the game.
     * @param playerIndex the players index in the game.
     * @param spies       the index of all the spies in the game, if this agent is a spy (i.e. playerIndex is an
     *                    element of spies)
     **/
    public void newGame(int numPlayers, int playerIndex, int[] spies) {
        this.numPlayers = numPlayers;
        id = playerIndex;
        suspicionValue = new HashMap<>();
        comrades = new HashSet<>();
        players =  getPlayers(); //Store the player id's in the game, excluding the agent's id
        initialiseProbs(); //Initialise the probabilities
        if (spies.length != 0) isSpy = true; //Agent is a spy

        //Stores in the spies if the agent is a spy too
        if (isSpy) {
            for (Integer spy : spies) {
                comrades.add(spy);
            }
        } else {
            //Initialise the suspicion value for each players as 0; not including itself
            for (int i = 0; i < numPlayers; i++) {
                if (i != id) suspicionValue.put(i, 0.0);
            }
        }
    }


    /**
     * This method is called when the agent is required to lead (propose) a mission
     * @param teamsize      the number of agents to go on the mission
     * @param failsRequired the number of agent fails required for the mission to fail
     * @return an array of player indexes, the proposed mission.
     **/
    public int[] proposeMission(int teamsize, int failsRequired) {
        int[] sentAway = new int[teamsize]; //Players proposed to a mission
        boolean[] inTheMission = new boolean[numPlayers];//Check if the player is already in the team proposed

        //If a spy
        int count = 0;
        if (isSpy) {
            boolean addedComrades = false;
            //Start choosing players
            while (count < teamsize) {

                if (failsRequired > 1 && !addedComrades) { //Need more than 1 spy in the team

                    Iterator<Integer> spies = comrades.iterator();
                    while (spies.hasNext() && count < (failsRequired)) {
                        int temp = spies.next();
                        sentAway[count] = temp;
                        inTheMission[temp] = true;
                        count++;
                    }
                    addedComrades = true;
                }

                //If losing, adds itself in the team
                if (currentRound > 2 && roundsLost < 2 && !inTheMission[id]) {
                    sentAway[count] = id;
                    inTheMission[id] = true;
                }
                else {
                    //Randomly choose the players
                    Random rand = new Random();
                    int randomPlayer = rand.nextInt(numPlayers);
                    while (inTheMission[randomPlayer]) {
                        randomPlayer = rand.nextInt(numPlayers);
                    }
                    sentAway[count] = randomPlayer;
                    inTheMission[randomPlayer] = true;
                }
                count++;
            }
        }

        //If resistance
        else{
            int[] sortedPlayers = sortPlayers(players);//Sort the players first
            sentAway[0] = id; //Agent will always send itself in missions

            for(int i = 1; i<teamsize; i++){
                sentAway[i] = sortedPlayers[i];
            }
        }
        return sentAway;
    }


    /**
     * This method is called when an agent is required to vote on whether a mission should proceed
     * @param mission the array of agent indexes who will be going on the mission.
     * @param leader  the index of the agent who proposed the mission.
     * @return true is this agent votes that the mission should go ahead, false otherwise.
     **/
    public boolean vote(int[] mission, int leader) {
        // When agent is a spy
        if (isSpy) {
            //Will always vote true if the vote count in one round is already at 5 or
            //If the mission contains other spies or itself or agent is leader
            return leader == id || voteCountForMission > 4 || isPlayerInMission(mission) || isPlayerInMission(mission, id)
                    || currentRound == 1;
        }

        //When agent is resistance
        boolean isParanoid = currentRound >= 2 && roundsLost >= 2;

        if (voteCountForMission < 5) {
            if (isParanoid && (leader == id)) return true;

            //Checks if any of the players have high suspicion value
            for (int player : mission) {

                //Paranoid where the resistance is losing
                if (isParanoid && player != id && suspicionValue.get(player) > 0.30) {
                    return false;
                }
                //Less paranoid, can accept players with suspicion values 0% - 60%
                else if (!isParanoid && player != id && suspicionValue.get(player) > 0.60) {
                    return false;
                }
            }
            return true; //If players have decent sus values then vote yes
        }
        return true; //No choice, have to vote yes for spy not to win round
    }


    /**
     * The method is called on an agent to inform them of the outcome of a vote,
     * and which agent voted for or against the mission.
     * @param mission the array of agent indexes represent the mission team
     * @param leader  the agent index of the leader, who proposed the mission
     * @param votes   an array of booleans such that votes[i] is true if and only if agent i voted for the mission to
     *                go ahead.
     **/
    public void voteOutcome(int[] mission, int leader, boolean[] votes) {
        if (!isVoteSuccess(votes)) {
            voteCountForMission++;
        } else {
            voteCountForMission = 0;
        }
    }


    /**
     * This method is called on an agent who has a choice to betray (fail) the mission
     * @param mission the array of agent indexes representing the mission team
     * @param leader  the agent who proposed the mission
     * @return true is the agent chooses to betray (fail) the mission
     **/
    public boolean betray(int[] mission, int leader) {
        //Check if the other spy is in the mission with you
        boolean isComradeThere = isPlayerInMission(mission);

        if(numPlayers == 5){
            //Will betray on rounds 2, 4, 5
            //Round 3 will betray if the other spy is not in it or if the spies are losing
            return (!isComradeThere && currentRound == 3) ||
                    (currentRound > 2 && roundsLost < 3) ||
                    currentRound == 2 ||
                    currentRound > 3;
        }
        else{
            //Will betray when spies are losing if more than 2 rounds otherwise no
            //Second round it will always betray
            return (currentRound > 2 && roundsLost < 3) || currentRound == 2;
        }
    }


    /**
     * Informs all agents of the outcome of the mission, including the number of agents who failed the mission.
     * @param mission        the array of agent indexes representing the mission team
     * @param leader         the agent who proposed the mission
     * @param numFails       the number of agent's who failed the mission
     * @param missionSuccess true if and only if the mission succeeded.
     **/
    public void missionOutcome(int[] mission, int leader, int numFails, boolean missionSuccess) {
        //Do nothing if agent is spy
        if (isSpy) {
            return;
        }

        //Getting the respective prior probability for each round and numFails
        double chancesOfSpy = numFails == 1 || numFails == 0 ? roundFail1[currentRound - 1] : roundFail2[currentRound - 1];
        if(numFails > 2){
            chancesOfSpy = numFails == 3 ? roundFail3[currentRound - 1] : roundFail4[currentRound - 1];
        }

        //Mission failed
        if (!missionSuccess) {
            for (Integer player : mission) {

                //When the agent is in the mission, prior probability changes
                if(player != id && isPlayerInMission(mission, id)){
                    double oldSusValue = suspicionValue.get(player);
                    double newChance = calculatePriorProbability(currentRound, numFails);
                    double newSusValue = calculateSpyProbability(newChance, spyFailRound[currentRound-1],
                            oldSusValue, false);
                    suspicionValue.replace(player, newSusValue);
                }
                else if (player != id) {
                    double oldSusValue = suspicionValue.get(player);
                    double newSusValue = calculateSpyProbability(chancesOfSpy, spyFailRound[currentRound - 1],
                            oldSusValue, false);
                    suspicionValue.replace(player, newSusValue);
                }
            }

            //Leader suspicion value calculated separately if not part of the mission
            if(!isPlayerInMission(mission, leader) && leader != id){
                //Conditions when the leader is certainly not the spy
                if(numPlayers == 5) {
                    if(numFails == 2 && (currentRound == 1 || currentRound == 3)){
                        suspicionValue.replace(leader, -1.0);
                    }
                }
                else if(numPlayers == 6){
                    if(numFails == 2 && (currentRound == 1 || currentRound == 3)){
                        suspicionValue.replace(leader, -1.0);
                    }
                }
                else if(numPlayers == 7){
                    if(numFails == 3 && (currentRound == 2 || currentRound == 3)){
                        suspicionValue.replace(leader, -1.0);
                    }
                }
                else if(numPlayers == 8 || numPlayers == 9){
                    if(numFails == 3 && currentRound == 1){
                        suspicionValue.replace(leader, -1.0);
                    }
                }
                else{
                    if(numFails == 4 && (currentRound == 2 || currentRound == 3)){
                        suspicionValue.replace(leader, -1.0);
                    }
                }

                //Other rounds with 2 fails
                if(numFails == 2){
                    double oldSusValue = suspicionValue.get(leader);
                    double newSusValue = calculateSpyProbability((double) 2/numPlayers, spyFailRound[currentRound - 1],
                            oldSusValue, false);
                    suspicionValue.replace(leader, newSusValue);
                }
                //Rounds with 3 fails
                else if(numFails == 3){
                    double oldSusValue = suspicionValue.get(leader);
                    double newSusValue = calculateSpyProbability((double) 3/numPlayers, spyFailRound[currentRound - 1],
                            oldSusValue, false);
                    suspicionValue.replace(leader, newSusValue);
                }
                //Rounds with 4 fails
                else if(numFails == 4){
                    double oldSusValue = suspicionValue.get(leader);
                    double newSusValue = calculateSpyProbability((double) 4/numPlayers, spyFailRound[currentRound - 1],
                            oldSusValue, false);
                    suspicionValue.replace(leader, newSusValue);
                }
                //Rounds with 1 fail
                else{
                    double oldSusValue = suspicionValue.get(leader);
                    double newSusValue = calculateSpyProbability((double) 1/numPlayers, spyFailRound[currentRound - 1],
                            oldSusValue, false);
                    suspicionValue.replace(leader, newSusValue);
                }
            }
        }

        //Mission success
        else {
            for (Integer player : mission) {
                if (player != id) {
                    double oldSusValue = suspicionValue.get(player);
                    double newSusValue = calculateSpyProbability(chancesOfSpy, spyFailRound[currentRound - 1],
                            oldSusValue, true);
                    suspicionValue.replace(player, newSusValue);
                }
            }

            //Leader not in mission
            if(isPlayerInMission(mission, leader) && leader != id){
                double oldSusValue = suspicionValue.get(leader);
                double newSusValue = calculateSpyProbability((double) 1/numPlayers, spyFailRound[currentRound - 1],
                        oldSusValue, true);
                suspicionValue.replace(leader, newSusValue);
            }
        }
    }


    /**
     * Informs all agents of the game state at the end of the round
     * @param roundsComplete the number of rounds played so far
     * @param roundsLost     the number of rounds lost so far
     **/
    public void roundOutcome(int roundsComplete, int roundsLost) {
        this.roundsLost = roundsLost;
        whichRound(roundsComplete);
    }


    /**
     * Informs all agents of the outcome of the game, including the identity of the spies.
     *
     * @param roundsLost the number of rounds the Resistance lost
     * @param spies      an array with the indexes of all the spies in the game.
     **/
    public void gameOutcome(int roundsLost, int[] spies) {
        if(!isSpy) getSuspectedSpies();
    }


    //---------------------------------------------------------------------------
    // Helper Functions
    //---------------------------------------------------------------------------

    /**
     * Initialise the probabilities, depending on the number of players and each round.
     * */
    private void initialiseProbs(){
        switch (numPlayers){
            case 5 : roundFail1 = new double[]{1 / 2d, 1 / 3d, 1 / 2d, 1 / 3d, 1 / 3d};
                roundFail2 = new double[]{1.0, 2 / 3d, 1.0, 2 / 3d, 2 / 3d};
                spyFailRound = new double[]{0.1, 0.85, 0.3, 0.80, 0.90};
                break;

            case 6: roundFail1 = new double[]{1 / 2d, 1 / 3d, 1 / 4d, 1 / 3d, 1 / 4d};
                roundFail2 = new double[]{1.0, 2 / 3d, 1/2d, 2 / 3d, 2 / 3d};
                spyFailRound = new double[]{0.1, 0.85, 0.90, 0.85, 0.90};
                break;

            case 7: roundFail1 = new double[]{1/2d, 1/3d, 1/3d, 1/4d, 1/4d};
                roundFail2 = new double[]{1.0, 2/3d, 1/2d, 2/3d, 2/3d};
                roundFail3 = new double[]{0.0, 1.0, 1.0, 3/4d, 3/4d};
                spyFailRound = new double[]{0.1, 0.85, 0.90, 0.90, 0.85};
                break;

            case 10: roundFail1 = new double[]{1/3d, 1/4d, 1/4d, 1/5d, 1/5d};
                roundFail2 = new double[]{2/3d, 1/2d, 1/2d, 2/5d, 2/5d};
                roundFail3 = new double[]{1.0, 3/4d, 3/4d, 3/5d, 3/5d};
                roundFail4 = new double[]{0.0, 1.0, 1.0, 4/5d, 4/4d};
                spyFailRound = new double[]{0.3, 0.85, 0.85, 0.90, 0.90};
                break;

            //For 8 and 9 players (same thing)
            default: roundFail1 = new double[]{1/3d, 1/4d, 1/4d, 1/5d, 1/5d};
                roundFail2 = new double[]{2/3d, 1/2d, 1/2d, 2/5d, 2/5d};
                roundFail3 = new double[]{1.0, 3/4d, 3/4d, 3/5d, 3/5d};
                spyFailRound = new double[]{0.3, 0.85, 0.85, 0.90, 0.90};
                break;

        }
    }


    /**
     * Helper function that rounds the decimal numbers.
     * @param number Number to be rounded.
     * @return Decimal rounded to the nearest 3 decimal places.
     */
    private double roundDecimal(double number) {
        BigDecimal decimal = new BigDecimal(number);
        decimal = decimal.setScale(3, RoundingMode.HALF_EVEN);
        return decimal.doubleValue();
    }


    /**
     * The Baye's Rule equation function, which calculates the probability of a player in the mission as a spy given
     * that the mission failed. The prior probability of the spy will depend on number of fails in that round; stored
     * in roundsFail1 and roundsFail2 arrays with 1 fails and 2 fails in a round, respectively. Same goes with 3 & 4
     * fails.
     * @param roundFail       The prior probability of the players being a spy in a specific round
     * @param spyFailRound    The likelihood of the spies making the mission fail at this round.
     * @param currentSusValue The suspicion value the players have at the moment, stored in the HashMap.
     * @param isSuccess       Whether the mission failed or not.
     * @return New posterior probability; probability given the set of parameters.
     * */
    private double calculateSpyProbability(double roundFail, double spyFailRound, double currentSusValue, boolean isSuccess) {

        //If currentValue is already 1.0 or -1.0 then return that back
        if(currentSusValue == 1.0 || currentSusValue == -1.0) return currentSusValue;

        //Set up the calculations for the numerator and denominator
        //Prior * Likelihood
        double likelihood = isSuccess ? 1 - spyFailRound : spyFailRound; //Changes depending on if mission won or not
        double resLikelihood = isSuccess ? 1-RESFAIL: RESFAIL; //Part of the marginal likelihood
        double numerator = currentSusValue == 0.0 ? roundFail * likelihood : roundFail * likelihood * currentSusValue;

        //Marginal likelihood
        double probFailAsResistance = (1.0 - roundFail) * resLikelihood;
        double denominator = numerator + probFailAsResistance;

        return currentSusValue == 0.0 ? roundDecimal(numerator) : roundDecimal(numerator / denominator);
    }


    /**
     * The function that calculates the suspicion values for players where the agent is in the mission. The prior
     * probability now changes to even more certainty depending on the number of fails and the round.
     * @param currentRound Current round the agent is on.
     * @param numFails     Number of fails at this round.
     * @return New posterior probability; probability given the set of parameters.
     * */
    private double calculatePriorProbability(int currentRound, int numFails) {

        double susValue = 0.0;

        //Fail is one
        if(numFails == 1){
            if(numPlayers == 5) {
                //100% know the other is spy when agent in a mission with 2 players, the other is always the spy
                if(currentRound == 1 || currentRound == 3){
                    susValue = 1.0;
                }
                else{
                    susValue = roundDecimal(2/3d);
                }
            }
            else if(numPlayers == 6){
                if(currentRound == 1){
                    susValue = 1.0;
                }
                else if(currentRound == 2 || currentRound == 4){
                    susValue = 2/3d;
                }
                else{
                    susValue = 1/2d;
                }
            }
            else if(numPlayers == 7){
                if(currentRound == 1){
                    susValue = 1.0;
                }
                else if(currentRound == 2 || currentRound == 3){
                    susValue = 2/3d;
                }
                else{
                    susValue = 1/2d;
                }
            }
            //8, 9 & 10
            else{
                if(currentRound == 1){
                    susValue = 2/3d;
                }
                else if(currentRound == 2 || currentRound == 3){
                    susValue = 1/2d;
                }
                else{
                    susValue = 2/5d;
                }
            }

        }

        //Fails are 2; at this point the other 2 players are the spies
        else if(numFails == 2){
            if(numPlayers == 5 && (currentRound == 2 || currentRound == 4 || currentRound == 5)){
                susValue = 1.0;
            }
            else if(numPlayers == 6){
                if(currentRound == 2 || currentRound == 4){
                    susValue = 1.0;
                }
                else{
                    susValue = 2/3d;
                }
            }
            else if(numPlayers == 7){
                if(currentRound == 2 || currentRound == 3){
                    susValue = 1.0;
                }
                else{
                    susValue = 1/2d;
                }
            }
            //8, 9 & 10
            else{
                if(currentRound == 1) {
                    susValue = 1.0;
                }
                else if(currentRound == 2 || currentRound == 3){
                    susValue = 1/2d;
                }
                else{
                    susValue = 3/5d;
                }
            }
        }

        //Fails are 3; only for 7-10 players
        else if(numFails == 3) {
            if (numPlayers == 7 && (currentRound == 4 || currentRound == 5)) {
                susValue = 1.0;
            }
            //8, 9 & 10
            else{
                if (currentRound == 2 || currentRound == 3) {
                    susValue = 1.0;
                } else {
                    susValue = 4/5d;
                }
            }
        }

        //Only for 10 players (numFails is 4)
        else{
            if(currentRound == 4 || currentRound == 5){
                susValue = 1.0;
            }
        }
        return susValue;
    }


    /**
     * Helper function that gets the suspected spies from the agent, based on the suspicion values. This was used for
     * debugging.
     * @return An array of spy id's.
     * */
    public int[] getSuspectedSpies(){
        players = sortPlayers(players);
        int ind = 1;

        //Size of spies depends on the number of players
        if(numPlayers == 5 || numPlayers == 6){
            suspectedSpies = new int[2];
        }
        else if(numPlayers > 6 && numPlayers != 10){
            suspectedSpies = new int[3];
        }
        else{
            suspectedSpies = new int[4];
        }

        //Find the most suspected players
        for(int i = 0; i < suspectedSpies.length; i++){
            suspectedSpies[i] = players[players.length-ind];
            ind++;
        }
        return suspectedSpies;
    }


    /**
     * Helper function that sorts the players in an ascending manner, given an array.
     * @param players Array of player ID's in the game, excluding the ID of the agent.
     * @return Sorted array of the players in ascending order of suspicion values.
     * */
    private int[] sortPlayers(int[] players){
        for(int i = 1; i<players.length; i++){
            double key = suspicionValue.get(players[i]);
            int id = players[i];
            int j = i -1;

            while(j>=0 && suspicionValue.get(players[j]) > key){
                players[j+1] = players[j];
                j -= 1;
            }

            players[j+1] = id;
        }
        return players;
    }


    /**
     * Helper function that generates all the players not including the agent's Id.
     * @return Array of player Id's, excluding the agent's id.
     * */
    private int[] getPlayers(){
        int[] players = new int[numPlayers-1];
        int index = 0;

        for(int i = 0; i<numPlayers; i++){
            if(i == id) continue;
            players[index] = i;
            index++;
        }
        return players;
    }


    /**
     * Returns whether the vote session in a round was successful or not
     * @param votes Boolean array of votes.
     * @return True if and only if the total number of true is more than false.
     */
    private boolean isVoteSuccess(boolean[] votes) {
        int totalTrue = 0;
        for (boolean vote : votes) {
            if (vote) totalTrue++;
        }
        return 2 * totalTrue > numPlayers;
    }


    /**
     * Helper function that tells the agent which round it's on.
     * @param roundsComplete Rounds completed at a certain point.
     */
    private void whichRound(int roundsComplete) {
        this.currentRound = 5 - roundsComplete;
        switch (currentRound) {
            case 1:
                currentRound = 5;
                break;
            case 2:
                currentRound = 4;
                break;
            case 3:
                currentRound = 3;
                break;
            case 4:
                currentRound = 2;
                break;
        }
    }


    /**
     * Helper function to check if the player is in the mission proposed.
     * @param mission  Array of players proposed in the mission or that was sent in the mission.
     * @param playerID Player's ID in the game.
     * @return True iff the agent is in the mission or iff a spy is in the mission (if agent is a spy).
     */
    private boolean isPlayerInMission(int[] mission, int playerID) {
        for (int player : mission) {
            if (player == playerID) return true;
        }
        return false;
    }


    /**
     * Helper function to check if another spy is in it if the agent is a spy.
     * @param mission Array of players proposed in the mission or that was sent in the mission.
     * @return True iff the agent is in the mission or iff a spy is in the mission (if agent is a spy).
     */
    private boolean isPlayerInMission(int[] mission) {
        for (int player : mission) {
            if(player != id && comrades.contains(player)) return true;
        }
        return false;
    }
}

