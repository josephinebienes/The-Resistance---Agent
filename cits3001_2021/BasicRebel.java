package cits3001_2021;

import java.util.*;

/**
 * A Basic Agent implementation to play in the game "The Resistance". This agent will be compared to AgentJB of its
 * performance in identifying which players are spies. Overall, it will only act as a resistance member for research
 * purposes, its actions when it's a spy won't be part of the analysis.
 *
 * This agent will be implemented with logic only, based on the game's rules. In a general sense, this agent will keep
 * a list of suspicion values on each of the players with the use of HashMap as a data structure. The suspicion value is
 * a whole integer that will be incremented based on the parameters of whether the players in the mission has failed it,
 * the leader is in the mission or not, current round, and number of fails in the mission. Further details will be in
 * the report.
 *
 * @author Josephine Bienes <22511218>
 * @since 4/10/2021
 * */
public class BasicRebel implements Agent {

    private final String name;
    private int numPlayers;
    private int id; //Id number for the agent in the game
    private boolean isSpy;
    private static int agentCount;
    private Map<Integer, Integer> suspicionValue; //Stores the suspicions values for each player
    private Set<Integer> comrades; //Stores other spies if itself is one
    private int voteCountForMission; /*Stores the amount of times a vote has failed; if on 5th vote session and spy is
                                    winning then always vote yes*/
    private int[] players;
    private int roundsLost;
    private int currentRound = 1;
    private int[] suspectedSpies = new int[2]; //Stores the top 2 players with the highest sus value

    /**
     * Creates the name of the agent.
     */
    public BasicRebel(String name) {
        this.isSpy = false;
        this.name = name;
    }


    /**
     * Returns an instance of this agent for testing.
     * The program should allocate the agent's name,
     * and can use a counter to ensure no two agents have the same name.
     * @return an instance of the agent.
     **/
    public static Agent init() {
        return switch (agentCount++) {
            case 0 -> new BasicRebel("BasicRebel-1st");
            case 1 -> new BasicRebel("BasicRebel-2nd");
            case 2 -> new BasicRebel("BasicRebel-3rd");
            default -> new BasicRebel("BasicRebel-" + agentCount + "th");
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
        if (spies.length != 0) isSpy = true; //Agent is a spy

        //Stores in the spies if the agent is a spy too
        if (isSpy) {
            for (Integer spy : spies) {
                comrades.add(spy);
            }
        } else {
            //Initialise the suspicion value for each players as 0; not including itself
            for (int i = 0; i < numPlayers; i++) {
                if (i != id) suspicionValue.put(i, 0);
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

            //Start choosing players
            while (count < teamsize) {
                //If losing, adds itself in the team
                if (currentRound > 2 && roundsLost < 2 && !inTheMission[id]) {
                    sentAway[count] = id;
                    inTheMission[id] = true;
                } else {
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
        else {
            int[] sortedPlayers = sortPlayers(players);
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
     * @return true if this agent votes that the mission should go ahead, false otherwise.
     **/
    public boolean vote(int[] mission, int leader) {
        // When agent is a spy
        if (isSpy) {
            //Will always vote true if the vote count in one round is already at 5
            //If the mission contains other spies or itself or agent is leader
            return leader == id || voteCountForMission > 4 || isPlayerInMission(mission, false)
                    || isPlayerInMission(mission, true) || currentRound == 1;
        }
        // When agent is a resistance member
        else {
            boolean isInMission = isPlayerInMission(mission, true);
            boolean isParanoid = currentRound >= 2 && roundsLost >= 2;

            if (voteCountForMission < 5) {
                if (isParanoid && (leader == id || isInMission)) return true;

                //Checks if any of the players have high suspicion value
                for (int player : mission) {

                    //Paranoid where the resistance is losing
                    if (isParanoid && player != id && suspicionValue.get(player) >1) {
                        return false;
                    }
                    //Less paranoid, can accept players with suspicion value 3
                    else if (!isParanoid && player != id && suspicionValue.get(player) > 3) {
                        return false;
                    }
                }
                return true; //If players have decent sus values then vote yes
            }
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
        boolean isComradeThere = isPlayerInMission(mission, false);

        //Will betray on rounds 2, 4, 5
        //Round 3 will betray if the other spy is not in it or if the spies are losing
        return (!isComradeThere && currentRound == 3) ||
                (currentRound > 2 && roundsLost < 3) ||
                currentRound == 2 ||
                currentRound > 3;
    }


    /**
     * Informs all agents of the outcome of the mission, including the number of agents who failed the mission.
     * @param mission        the array of agent indexes representing the mission team
     * @param leader         the agent who proposed the mission
     * @param numFails       the number of agent's who failed the mission
     * @param missionSuccess true if and only if the mission succeeded.
     **/
    public void missionOutcome(int[] mission, int leader, int numFails, boolean missionSuccess) {
        if (isSpy) {
            return;
        } //Do nothing if agent is spy

        //If resistance
        //Suspicion value depends on whether the mission failed and how many players failed it
        int susValue = !missionSuccess ? +1 : -1;
        susValue += numFails > 1 ? 2 : 0;

        for (int player : mission) {
            if (numFails < 2 && player != id) {
                //Don't make the sus value < 0
                if (susValue == -1 && suspicionValue.get(player) != 0) {
                    int temp = suspicionValue.get(player);
                    suspicionValue.replace(player, temp + susValue);
                } else if (susValue > 0) {
                    int temp = suspicionValue.get(player);
                    suspicionValue.replace(player, temp + susValue);
                }
            } else if (player != id) {
                int temp = suspicionValue.get(player);
                suspicionValue.replace(player, temp + susValue);
            }
        }

        //If leader was in the mission and not the agent, sus value is added +2
        if (isPlayerInMission(mission, leader) && leader != id) {
            if (susValue == -1 && suspicionValue.get(leader) != 0) {
                int temp = suspicionValue.get(leader);
                suspicionValue.replace(leader, temp + susValue);
            } else if (susValue >= 1) {
                int temp = suspicionValue.get(leader);
                suspicionValue.replace(leader, temp + 2);
            }
        }
        //If leader was not in the mission, sus value only increments to 1 if mission failed
        else {
            if (leader != id && susValue == -1 && suspicionValue.get(leader) != 0) {
                int temp = suspicionValue.get(leader);
                suspicionValue.replace(leader, temp + susValue);
            } else if (leader != id && susValue >= 1) {
                int temp = suspicionValue.get(leader);
                suspicionValue.replace(leader, temp + 1);
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
     * @param roundsLost the number of rounds the Resistance lost
     * @param spies      an array with the indexes of all the spies in the game.
     **/
    public void gameOutcome(int roundsLost, int[] spies) {

    }


    //---------------------------------------------------------------------------
    // Helper Functions
    //---------------------------------------------------------------------------

    /**
     * Helper function to check if the agent is in the mission proposed or another spy is in it if the agent is a spy.
     * @param mission Array of players proposed in the mission or that was sent in the mission.
     * @param ifAgent If true, it's looking for the agent in the mission otherwise see if a spy is in the mission.
     * @return True iff the agent is in the mission or iff a spy is in the mission (if agent is a spy).
     */
    private boolean isPlayerInMission(int[] mission, boolean ifAgent) {
        for (int player : mission) {
            if (ifAgent && player == id) {
                return true;
            } else if (!ifAgent && comrades.contains(player)) {
                return true;
            }
        }
        return false;
    }


    /**
     * Helper function to check if the player is in the mission proposed.
     * @param mission  Array of players proposed in the mission or that was sent in the mission.
     * @param playerID Player's ID in the game.
     * @return True iff the agent is in the mission or iff a spy is in the mission (if agent is a spy).
     */
    private boolean isPlayerInMission(int[] mission, int playerID) {
        for (int player : mission) {
            if (player == playerID) {
                return true;
            }
        }
        return false;
    }


    /**
     * Returns whether the vote session in a round was successful or not
     * @param votes Boolean array of votes.
     * @return True iff the total number of true is more than false.
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
        switch (currentRound){
            case 1: currentRound = 5;
                break;
            case 2: currentRound = 4;
                break;
            case 3: currentRound = 3;
                break;
            case 4: currentRound = 2;
                break;
            default: currentRound = 1;
                break;
        }
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
            int key = suspicionValue.get(players[i]);
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
}

