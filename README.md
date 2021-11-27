# The Resistance - Agent
***
Project involves creating an Agent with a chosen method of playing the game **The Resistance** by Don Eskridge. Specifically this project contains 2 agents that are compared against for analysis purposes.

## Running Simulations for the experiment
***
**Important notes**
-----------------
The respective files were run on java version 16.0.2.

**Agents**
-----------------
The java classes that are involved in the report are within the cits3001_2021 folder; BasicRebel.java and Baerule.java. Data computed and analysed within the report, are held in the Data folder within the cits3001,2021 folder.

Agent_22511218.java is the agent that will be submitted to the tournament and is not analysed.

**Experiment.java**
-----------------
1. If you want to stimulate the experiment for each environment, comment out the one that is not simulated. Example, to simulate the amateur environment you must comment out the respective code under the random environment from lines 160-212. For random environment, comment out lines 99-150.
2. Additionally, in the Game.java file where the section the spies are allocated, the "or" conditional for excluding BasicRebel as a spy must be placed in when simulating the random environment. The section is at line 85 and the respective "or" conditional is: 

				|| players[spy].getName().contains("BasicRebel")

3. In Game.java again, when initialising the number of agents in a game, make sure to comment out the one that is not going to be simulated. Example, to simulate the amatuer environment, comment out lines 384-390. If random environment, then comment out lines 375-379.
4. When running either of the simulations, the statistical values from the data extracted based on the three aspects mentioned in the report will be logged into individual files named:
	
			“[number of spies identified]-spies-[agent]-[environment].txt”.

  * The characters will be “b” or “br” if it was BasicRebel or Baerule respectively. The last text refers to what environment the data is on. There will be an additional logfile that directly compares both agents’ data after each simulation round in a file named “research_log_game_[environment].txt”. 
  * In total, there will be seven logfiles after each environment simulations where each logfile will contain 200 lines of data in total.
