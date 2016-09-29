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
    int maxDepth = 8;
    int extraDepth = 6; // used for iterative NegaMax
    boolean useDeadline = true;
    long timeLimitThresh = 100000000; // 1*10^8 = 0.1 seconds 
    int hashCapacity = 10000000;
    HashMap<String, StateEntry> visitedStatesW = new HashMap<String, StateEntry>(hashCapacity);
    HashMap<String, StateEntry> visitedStatesR = new HashMap<String, StateEntry>(hashCapacity);

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
        }else if(lNextStates.size() == 1){
            // If only one move available, return that move
            return lNextStates.firstElement();
        }

        int initColor = (pState.getNextPlayer()==Constants.CELL_RED) ? 1:-1;
        GameState bestMove;
        bestMove = iterativeNegaMax(lNextStates, initColor, pDue);

        return bestMove;
            
    }

    public GameState iterativeNegaMax(Vector<GameState> nextStates, int initColor, Deadline deadline){
        Vector<GameState> newBestStates;
        Vector<GameState> lastBestStates = nextStates;
        double tmpValueMove;
        double bestValue;
        double bestMoveThresh = 1.5; /*Used to determine when list is cleared/updated*/
        boolean timeRanOut = false;
        GameState bestMove = lastBestStates.firstElement();

        for(int plusDepth = 0; plusDepth < extraDepth+1; plusDepth++){
            if(lastBestStates.size() == 1){ // Break if only one move available
                break;
            }
            if(useDeadline && deadline.timeUntil() < timeLimitThresh){
                timeRanOut = true;
                break;
            }
            // Reset the best states and transpo tables
            bestValue = -valueWin*2;
            newBestStates = new Vector<GameState>();
            visitedStatesW.clear();
            visitedStatesR.clear();

            for(GameState tmpState:lastBestStates){

                tmpValueMove= -negaMax(tmpState, maxDepth+plusDepth, -valueWin, valueWin, initColor, deadline);
                if(tmpValueMove > 500.0){ // Found a winning move
                    return tmpState;
                }

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
                    bestMove = tmpState;
                }else if(tmpValueMove*bestMoveThresh > bestValue){
                    // If move is kinda good, but not better than the best, keep it.
                    newBestStates.add(tmpState);
                }

            }
            lastBestStates = newBestStates;

        }

        return bestMove;

    }

    public double negaMax(GameState gameState, int depth, double alpha, double beta,
            int colour, Deadline deadline){

        double alphaOrig = alpha;

        String stateString = getGamestateString(gameState);
        if(isVisited(stateString, colour)){
            StateEntry entry = getVisitedValue(stateString, colour);
            if(entry.depth >= depth){
                if(entry.flag == 0){
                    return entry.value;
                }else if(entry.flag == -1){
                    alpha = Math.max(alpha, entry.value);
                }else if(entry.flag == 1){
                    beta = Math.min(beta, entry.value);
                }
                if(alpha >= beta){
                    return entry.value;
                }
            }
        }

        double tmpValue;

        if(gameState.isEOG()){
            return valueEnd(gameState)*colour;
        }
        else if(depth==0 || (useDeadline && deadline.timeUntil() < timeLimitThresh)){
            if(depth>=2){
                return 0;//-valueWin;//*colour; // In this case you're deep enough in the tree to give a good heuristic value
            }else{
                return value(gameState, colour)*colour;
            }
        }
        else{

            Vector<GameState> lNextStates = new Vector<>();
            gameState.findPossibleMoves(lNextStates);

            double bestMove=-valueWin*2;

            for(GameState childState:lNextStates){

                tmpValue=-negaMax(childState, depth-1,-beta,-alpha, -colour, deadline);
                bestMove=Math.max(bestMove, tmpValue);
                alpha=Math.max(alpha, tmpValue);
                if(alpha>=beta){
                    break;
                }
                
            }

            
            int flag;
            if(bestMove <= alphaOrig){
                // Store upper bound
                flag = 1;
            }else if(bestMove >= beta){
                // Store lower bound
                flag = -1;
            }else{
                // Store exact
                flag = 0;
            }

            //Storing state in transpo table
            storeVisited(stateString, flag, bestMove, depth, colour);

            //Storing reverse
            /*String stateStringReverse = getGamestateString(gameState.reversed());
            if(!isVisited(stateStringReverse, -colour)){
                storeVisited(stateStringReverse, flag, bestMove*colour, depth, -colour);
            }*/

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
                                colourPlayer, cell == Constants.CELL_KING+colourPlayer, new ArrayList<int[]>()));
                    }
                    // If piece on the sides, more score for being untakable
                    if (col == 0 || col == 7){
                        pieceScore += 0.2;
                    }
                    // Pieces closer to center get higher score
                    distanceToCenter = Math.abs(3.5-row) + Math.abs(3.5-col);
                    pieceScore += 0.3/distanceToCenter;
                }
                // Kings get higher score
                if(cell ==Constants.CELL_KING+colourPlayer){
                    pieceScore += 0.6;
                }
                // If players pawn far ahead on the board (almost king), more score
                if(cell == colourPlayer && cell != Constants.CELL_KING+colourPlayer){
                    if(cell == Constants.CELL_WHITE && row < 2 || cell == Constants.CELL_RED && row > 5){
                        pieceScore += 0.4;
                    }
                }
                
            }

        }

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
        HashMap<String, StateEntry> visitedStates = (colour == 1) ? visitedStatesW : visitedStatesR;
        return visitedStates.containsKey(gamestateString);
    }

    public StateEntry getVisitedValue(String gamestateString, int colour){
        // Only call this function if containsVisited has returned true first
        HashMap<String, StateEntry> visitedStates = (colour == 1) ? visitedStatesW : visitedStatesR;
        return visitedStates.get(gamestateString); 
    }

    public void storeVisited(String gamestateString, int flag, double value, int depth, int colour){
        StateEntry entry = new StateEntry(flag, value, depth);
        HashMap<String, StateEntry> visitedStates = (colour == 1) ? visitedStatesW : visitedStatesR;
        visitedStates.put(gamestateString, entry);
        return;
    }
    
}

class StateEntry{
    public int flag; // -1:lowerbound, 0:exact, 1:upperbound
    public double value;
    public int depth;

    public StateEntry(int flagValue, double inValue, int inDepth){
        flag = flagValue;
        value = inValue;
        depth = inDepth;
    }
}