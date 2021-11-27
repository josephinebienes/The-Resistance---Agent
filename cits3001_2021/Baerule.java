package cits3001_2021;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * An Agent implementation to play in the game "The Resistance". This agent will be compared to BasicRebel for research
 * purposes, and will only play as a resistance member; spy behaviour/actions are not implemented in this agent. Only
 * consider 5 player game setting.
 *
 * This agent will be implemented with Bayesian Reasoning to determine the suspicion values for each player, and based
 * on these values, the top two players with a higher probability suspicion values, will be identified as a spy. Just
 * like BasicRebel, the suspicion values will be stored in a HashMap data structure for easy access, and the parameters
 * considered are whether the player in the mission has failed it, the number of fails in the mission, and the current
 * round it's on. The leader's suspicion values also depends on whether it was in the mission or not. Further details
 * will be in the report.
 *
 * @author Josephine Bienes <22511218>
 * @since 12/10/2021
 */
public class Baerule implements Agent {

    private final String name;
    private int numPlayers;
    private int id; //Id number for the agent in the game
    private static int agentCount;
    private Map<Integer, Double> suspicionValue; //Stores the suspicion values for each player
    private int voteCountForMission; /*Stores the amount of times a vote has failed; if on 5th vote session always
                                        vote yes*/
    private int[] players;
    private int roundsLost;
    private int currentRound = 1;
    private int[] suspectedSpies = new int[2]; //Stores the top 2 players with the highest sus value

    //Stores the probabilities of being a spy given 1 total number of fails
    private double[] roundFail1 = {1 / 2d, 1 / 3d, 1 / 2d, 1 / 3d, 1 / 3d};
    //Stores the probabilities of being a spy given 2 total number of fails
    private double[] roundFail2 = {1.0, 2 / 3d, 1.0, 2 / 3d, 2 / 3d};

    //Stores the probabilities of spies failing the missions given the round; for rounds with 2 players only, if 2 fails
    //automatically both players are spies and the rest are resistance
    private double[] spyFailRound = {0.1, 0.85, 0.3, 0.80, 0.90};

    //The probability of a resistance member failing the mission will always be 0.1 for every round; will always try to
    //win every round
    private static final double RESFAIL = 0.1;


    /**
     * Creates the name of the agent.
     */
    public Baerule(String name) {
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
            case 0 -> new Baerule("Baerule-1st");
            case 1 -> new Baerule("Baerule-2nd");
            case 2 -> new Baerule("Baerule-3rd");
            default -> new Baerule("Baerule-" + agentCount + "th");
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
        players =  getPlayers(); //Store the player id's in the game, excluding the agent's id

        //Initialise the suspicion value for each player as 0; not including itself
        for (int i = 0; i < numPlayers; i++) {
            if (i != id) suspicionValue.put(i, 0.0);
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
        int[] sortedPlayers = sortPlayers(players);

        sentAway[0] = id; //Agent will always send itself in missions

        for(int i = 1; i<teamsize; i++){
            sentAway[i] = sortedPlayers[i];
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
        //Not needed for this agent
        return false;
    }


    /**
     * Informs all agents of the outcome of the mission, including the number of agents who failed the mission.
     * @param mission        the array of agent indexes representing the mission team
     * @param leader         the agent who proposed the mission
     * @param numFails       the number of agent's who failed the mission
     * @param missionSuccess true if and only if the mission succeeded.
     **/
    public void missionOutcome(int[] mission, int leader, int numFails, boolean missionSuccess) {

        double chancesOfSpy = numFails == 1 || numFails == 0 ? roundFail1[currentRound - 1] : roundFail2[currentRound - 1];

        //If number of fails were 2, and it was on a 2 size mission, then all players in that mission are spies.
        if (!missionSuccess && numFails == 2 && (currentRound == 1 || currentRound == 3)) {
            for (Integer player : mission) {
                suspicionValue.replace(player, 1.0);
            }
            return;
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
                //Certain that the leader is not a spy
                if(numFails == 2 && (currentRound == 1 || currentRound == 3)){
                    suspicionValue.replace(leader, -1.0);
                }
                //Other rounds with 2 fails
                else if(numFails == 2){
                    double oldSusValue = suspicionValue.get(leader);
                    double newSusValue = calculateSpyProbability(2/5d, spyFailRound[currentRound - 1],
                                                                    oldSusValue, false);
                    suspicionValue.replace(leader, newSusValue);
                }
                //Rounds with 1 fail
                else{
                    double oldSusValue = suspicionValue.get(leader);
                    double newSusValue = calculateSpyProbability(1/5d, spyFailRound[currentRound - 1],
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
                double newSusValue = calculateSpyProbability(1/5d, spyFailRound[currentRound - 1],
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
    public void gameOutcome(int roundsLost, int[] spies) {}


    //---------------------------------------------------------------------------
    // Helper Functions
    //---------------------------------------------------------------------------

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
     * in roundsFail1 and roundsFail2 arrays with 1 fails and 2 fails in a round, respectively.
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
            //100% know the other is spy when agent in a mission with 2 players, the other is always the spy
            if(currentRound == 1 || currentRound == 3){
                susValue = 1.0;
            }
            else{
                susValue = roundDecimal(1/2d);
            }
        }
        //Fails are 2; at this point the other 2 players are the spies
        else if(currentRound == 2 || currentRound == 4 || currentRound == 5){
            susValue = 1.0;
        }

        return susValue;
    }


    /**
     * Helper function that gets the suspected spied from the agent based on the suspicion values.
     * @return An array of spy id's.
     * */
    public int[] getSuspectedSpies(){
        players = sortPlayers(players);
        suspectedSpies[0] = players[players.length-1];
        suspectedSpies[1] = players[players.length-2];
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
            if (player == playerID) return  true;
        }
        return false;
    }
}

