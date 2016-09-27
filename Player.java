import java.util.*;

/*POSSIBLE IMPROVEMENTS
    - Ordered alphabeta pruning
    - Fix the value function so it doesnt have to run twice
    - Better heuristics
    - Symmetry checking for the repeated states
    - Maybe taking the deadline into account helps
    - Try to implement iterative negamax
*/

public class Player {
    int red=-1;
    int white=1;
    double valueWin=1000.0;
    double valueTie=0.0;
    int boardSize=8;
    int maxDepth = 11;
    int extraDepth = 2; // used for iterative NegaMax
    boolean useDeadline = true;
    long timeLimitThresh = 100000; // 1*10^8 = 0.1 seconds 
    int hashCapacity = 10000000;
    HashMap<String, Double> visitedStatesW = new HashMap<String, Double>(hashCapacity);
    HashMap<String, Double> visitedStatesR = new HashMap<String, Double>(hashCapacity);

    /**
     * Performs a move
     *
     * @param pState
     *            the current state of the board
     * @param pDue
     *            time before which we must have returned
     * @return the next state the board is in after our move
     */
    public GameState play(final GameState pState, final Deadline pDue) {
        Vector<GameState> lNextStates = new Vector<GameState>();
        //Vector<GameState> lNextStates2 = new Vector<GameState>();
        pState.findPossibleMoves(lNextStates);

        if (lNextStates.size() == 0) {
            // Must play "pass" move if there are no other moves possible.
            return new GameState(pState, new Move());
        }

        int initColor = (pState.getNextPlayer()==Constants.CELL_RED) ? 1:-1;
        GameState bestMove;
        //bestMove = maxDepthNegaMax(lNextStates, initColor, pDue);
        bestMove = iterativeNegaMax(lNextStates, initColor, pDue);

        return bestMove;
            
    }

    public GameState maxDepthNegaMax(Vector<GameState> nextStates, int initColor, Deadline deadline){

        double bestValue=-valueWin;
        double tmp_value;
        GameState bestMove=nextStates.firstElement();
        for(GameState childState: nextStates){
            // Run negamax to max depth on each state
            tmp_value=-negaMax(childState, maxDepth, -valueWin, valueWin , initColor, deadline);

            if(tmp_value>bestValue){
                bestValue=tmp_value;
                bestMove=childState;
            }
            if(useDeadline && deadline.timeUntil() < timeLimitThresh){
                //System.err.println("TIME RAN OUT");
                break;
            }
        }

        return bestMove;
    }


    public GameState iterativeNegaMax(Vector<GameState> nextStates, int initColor, Deadline deadline){
        Vector<GameState> newBestStates = new Vector<GameState>();
        Vector<GameState> lastBestStates = nextStates;
        double tmpValueMove;
        double bestValue;
        double bestMoveThresh = 1.5; /*If a new tmpValueMove is above
            bestMoveThresh*bestValue, the newBestStates list is cleared*/
        boolean timeRanOut = false;
        GameState bestMove = lastBestStates.firstElement();

        for(int plusDepth = 0; plusDepth < extraDepth; plusDepth++){
            if(lastBestStates.size() == 1){ // Break if only one move available
                break;
            }
            if(useDeadline && deadline.timeUntil() < timeLimitThresh){
                timeRanOut = true;
                break;
            }
            // Reset the best states and transpo tables
            bestValue = -valueWin;
            newBestStates = new Vector<GameState>();
            visitedStatesW = new HashMap<String, Double>(hashCapacity);
            visitedStatesR = new HashMap<String, Double>(hashCapacity);

            for(GameState tmpState:lastBestStates){

                tmpValueMove=-negaMax(tmpState, maxDepth+plusDepth, -valueWin, valueWin, initColor, deadline);
                if( tmpValueMove>=bestValue){

                    if(tmpValueMove > bestValue*bestMoveThresh){
                        newBestStates.clear();
                        newBestStates.add(tmpState);
                    }else{ 
                        // Putting the new state with the best value first in the list.
                        // This effectively gives ordered alpha beta pruning
                        newBestStates.add(0,tmpState);
                    }
                    bestValue = tmpValueMove;
                }

            }
            /*while(newBestStates.size()>4){
                newBestStates.remove(newBestStates.size()-1);
            }*/
            lastBestStates = newBestStates;

        }

        //int counter = 0;
        if(newBestStates.size() != 1 && timeRanOut == false){
            // Reset transpo tables
            bestValue = -valueWin;
            visitedStatesW = new HashMap<String, Double>(hashCapacity);
            visitedStatesR = new HashMap<String, Double>(hashCapacity);
            for(GameState tmpState:newBestStates){
                if(useDeadline && deadline.timeUntil() < timeLimitThresh){
                    break;
                }
                tmpValueMove=-negaMax(tmpState, maxDepth+extraDepth, -valueWin, valueWin, initColor, deadline);
                if( tmpValueMove>=bestValue){
                    bestValue=tmpValueMove;
                    bestMove=tmpState;
                }
            }
        }else{
            bestMove = newBestStates.lastElement();
        }

        return bestMove;

    }

    public double negaMax(GameState gameState, int depth, double alpha, double beta,
            int colour, Deadline deadline){
        double tmpValue;
        String childStateString;
        if(gameState.isEOG()){
            return valueEnd(gameState)*colour;
        }
        else if(depth==0 || (useDeadline && deadline.timeUntil() < timeLimitThresh)){
            if(depth>=2){
                return -valueWin*colour; // In this case you're deep enough in the tree to give a good heuristic value
            }else{
                return value(gameState, colour)*colour;
            }
        }
        else{

            Vector<GameState> lNextStates = new Vector<>();
            gameState.findPossibleMoves(lNextStates);

            //Vector<GamestateValue> sortedStates = sortStates(lNextStates, colour);
            
            double bestMove=-valueWin;
            for(GameState childState:lNextStates){
            //for(GamestateValue stateValue:sortedStates){
                //GameState childState = stateValue.gamestate;

                childStateString = getGamestateString(childState);

                if(isVisited(childStateString, colour)){
                    tmpValue = getVisitedValue(childStateString, colour);  
                }
                else{
                    // if not in hashMap, compute value and store in hashMap
                    tmpValue=-negaMax(childState, depth-1,-beta,-alpha, -colour, deadline);
                    storeVisited(childStateString,tmpValue, colour);
                    storeVisited(getGamestateString(childState.reversed()), tmpValue*colour, -colour);
                }
                bestMove=Math.max(bestMove, tmpValue);
                alpha=Math.max(alpha, tmpValue);
                if(alpha>=beta){
                    break;
                }
                
            }
            
            return bestMove;
            
        }
    }
    
    public double valueEnd(GameState gameState){
        if(gameState.isWhiteWin()){
            return valueWin;
        }
        else if(gameState.isRedWin()){
            return -valueWin;
        }
        else{
            return 0;
        }
        
    }
    public double value(GameState gameState, int colour){
        return boardScore(gameState, Constants.CELL_WHITE, colour==1)
                -boardScore(gameState, Constants.CELL_RED, colour==-1);
        }
    public double boardScore(GameState gameState, int colourPlayer, boolean play){
        double pieceScore=0.0;
        double longestJump=0.0;
        double distanceToCenter;
        for( int row=0; row<boardSize; row++){
            for(int col=0;col<boardSize;col++){
                int cell=gameState.get(row, col);
                // +1 for player cell, either pawn or king
                if(cell==colourPlayer || cell==Constants.CELL_KING+colourPlayer){
                    pieceScore += 1.0;
                    // Bonus score if players turn and jump available
                    if (play){
                        longestJump=Math.max(longestJump, killerJump(gameState, row, col, 
                                colourPlayer, cell == Constants.CELL_KING, new ArrayList<int[]>()));
                    }
                    // If piece on the sides, more score for being untakable
                    if (col == 0 || col == 7){
                        pieceScore += 0.2;
                    }
                    // Pieces closer to center get higher score
                    distanceToCenter = Math.abs(3.5-row) + Math.abs(3.5-col);
                    pieceScore += 0.2/distanceToCenter;
                }
                // Kings get higher score
                if(cell ==Constants.CELL_KING+colourPlayer){
                    pieceScore += 0.4;
                }
                // If players pawn far ahead on the board (almost king), more score
                if(cell == colourPlayer && cell != Constants.CELL_KING){
                    if(cell == Constants.CELL_WHITE && row < 2 || cell == Constants.CELL_RED && row > 5){
                        pieceScore += 0.4;
                    }
                }
                
            }

        }/*
        String printPiece = "piece: "+pieceScore;
        String printJump = "jump: "+longestJump*10;
        //System.err.println(printPiece);
        System.err.println(printJump);
        System.err.println("");*/
        /*if(longestJump != 0.0){
            System.err.println(longestJump);
        }*/
       return pieceScore + longestJump*longestJump;
    }

    public double killerJump(GameState gameState, int pRow, 
            int pCol, int colourPlayer, boolean isKing, ArrayList<int[]> killedCells){

        int opponent= (colourPlayer==Constants.CELL_WHITE) 
                ? Constants.CELL_RED :Constants.CELL_WHITE;
        int cellAhead1;
        int cellAhead2;
        double longestJump = 0.0;
        double jumpValue = 3.0; // Value of a possible jump
        int nrJumps = (isKing) ? 4:2;
        double[] jumps = new double[nrJumps];
        int jumpidx = 0;

        ArrayList<int[]> newKilledCells;

        for(int rowFwd = -1; rowFwd < 3; rowFwd += 2){
            // If not king, set the rowFwd to different value and break after first loop
            if(!isKing){rowFwd = (colourPlayer == Constants.CELL_WHITE) ? -1 : 1;}
            for (int colFwd = -1; colFwd < 3; colFwd += 2){
                cellAhead1 = gameState.get(pRow + rowFwd, pCol + colFwd);
                cellAhead2 = gameState.get(pRow + rowFwd*2, pCol + colFwd*2);

                // Check if cell ahead is opponent 
                if(cellAhead1 == opponent && cellAhead2 == Constants.CELL_EMPTY && 
                        !checkKilledCells(killedCells, pRow + rowFwd, pCol + colFwd)){
                    newKilledCells = copyKilledCells(killedCells);
                    addKilledCell(newKilledCells, pRow + rowFwd, pCol + colFwd);
                    jumps[jumpidx] = jumpValue + killerJump(gameState, pRow+2*rowFwd, 
                                pCol+2*colFwd, colourPlayer, isKing, newKilledCells);
                }
                jumpidx += 1;
            }
            if(!isKing){ // If not king, dont go through the next rowFwd value
                break;
            }
        }

        for(int idx=0; idx<nrJumps; idx++){
            if(jumps[idx] > longestJump){
                longestJump = jumps[idx];
            }
        }
                
        return longestJump;
    }
    public ArrayList<int[]> copyKilledCells(ArrayList<int[]> killedCells){
        ArrayList<int[]> returnList = new ArrayList<int[]>();
        int[] newCoords;
        for(int[] coords:killedCells){
            newCoords = new int[2];
            newCoords[0] = coords[0];
            newCoords[1] = coords[1];
            returnList.add(newCoords);
        }
        return returnList;
    }
    public void addKilledCell(ArrayList<int[]> killedCells, int row, int col){
        int[] newCoords = {row,col};
        killedCells.add(newCoords);
    }
    public boolean checkKilledCells(ArrayList<int[]> killedCells, int row, int col){
        // Returns true if cell is in the killed cells
        boolean isKilled = false;
        for(int[] coords:killedCells){
            if(coords[0] == row && coords[1] == col){
                isKilled = true;
                break;
            }
        }
        return isKilled;
    }
    public String getGamestateString(GameState gamestate){
        String return_str = "";
        int nr_cells = boardSize*boardSize/2;
        for(int i = 0; i< nr_cells; i++){
            return_str += Integer.toString(gamestate.get(i));
        }
        return return_str;
    }

    public boolean isVisited(String gamestateString, int colour){
        HashMap<String, Double> visitedStates = (colour == 1) ? visitedStatesW : visitedStatesR;
        return visitedStates.containsKey(gamestateString);
    }

    public double getVisitedValue(String gamestateString, int colour){
        // Only call this function if containsVisited has returned true first
        HashMap<String, Double> visitedStates = (colour == 1) ? visitedStatesW : visitedStatesR;
        return visitedStates.get(gamestateString); 
    }

    public void storeVisited(String gamestateString, double value, int colour){
        HashMap<String, Double> visitedStates = (colour == 1) ? visitedStatesW : visitedStatesR;
        visitedStates.put(gamestateString, value);
        return;
    }

    public Vector<GamestateValue> sortStates(Vector<GameState> statesVector, int colour){
        Vector<GamestateValue> outVector = new Vector<>();
        double value;
        String stateString;
        for(GameState state:statesVector){
            // Getting the value for each state
            stateString = getGamestateString(state);
            if(isVisited(stateString, colour)){
                value = getVisitedValue(stateString, colour);  
            }else{
                if(state.isEOG()){
                    value = -valueEnd(state)*colour;
                }else{
                    value = -value(state, colour)*colour;
                }
            }
            outVector.add(new GamestateValue(state, value));
        }
        Collections.sort(outVector, new GamestateValueCompare());

        /*for(GamestateValue test:outVector){
            System.err.println(test.value);
        }
        System.err.println("");*/

        return outVector;
    }

    public boolean checkOneOnOne(GameState gamestate){
        int totalWhite = 0;
        int totalRed = 0;
        int nr_cells = boardSize*boardSize/2;
        for(int i = 0; i< nr_cells; i++){
            if(gamestate.get(i) == Constants.CELL_WHITE){
                totalWhite++;
            }else if(gamestate.get(i) == Constants.CELL_RED){
                totalRed++;
            }
        }
        if(totalRed == 1 && totalWhite == 1){
            return true;
        }else{
            return false;
        }
    }
    
}

class GamestateValue{
    public GameState gamestate;
    public double value;

    public GamestateValue(GameState inState, double inValue){
        gamestate = inState;
        value = inValue;
    }

}

class GamestateValueCompare implements Comparator<GamestateValue> {

    @Override
    public int compare(GamestateValue stateValue1, GamestateValue stateValue2) {
        if(Math.max(stateValue1.value, stateValue2.value) == stateValue1.value){
            return -1;
        }else if(Math.max(stateValue1.value, stateValue2.value) == stateValue2.value){
            return 1;
        }else{
            return 0;
        }
    }
}

class GameStateCopy{
    public GameState gamestate;

    public GameStateCopy(GameState inState){
        gamestate = inState;
    }
}