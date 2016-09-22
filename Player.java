import java.util.*;

public class Player {
    int red=-1;
    int white=1;
    int valueWin=1000;
    int valueTie=0;
    int boardSize=8;
    int hash_capacity = 1000000;
    HashMap<String, Integer> visited_states = new HashMap<String, Integer>(hash_capacity);
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
        int bestValue=-valueWin;
        int tmp_value;
        GameState bestMove=lNextStates.firstElement();
        //System.err.println("new tern------------------------------------------------------");
        for(GameState childState: lNextStates){
            if(pState.getNextPlayer()==Constants.CELL_RED){
                tmp_value=-negaMax(childState, 11, -valueWin, valueWin ,1);
                //bestMove=lNextStates.firstElement();
                //System.err.println(childState.toString(Constants.CELL_RED));
            }
            else{
                tmp_value=-negaMax(childState, 11, -valueWin, valueWin ,-1);
                //System.err.println(childState.toString(Constants.CELL_WHITE));
            }
            if(tmp_value>bestValue){
                bestValue=tmp_value;
                bestMove=childState;
            }
        //System.err.println(tmp_value);
        //System.err.println(value(childState, 1));
        }
        return bestMove;
            
    }
    public int negaMax(GameState gameState, int depth, int alpha, int beta,
            int colour){
        int tmpValue;
        String childStateString;
        if(gameState.isEOG()){
            return valueEnd(gameState)*colour;
        }
        else if(depth==0){
            return value(gameState, colour)*colour;
        }
        else{

            Vector<GameState> lNextStates = new Vector<>();
            gameState.findPossibleMoves(lNextStates);
            
            int bestMove=-1000;
            for(GameState childState:lNextStates){
                if(colour==1){
                   childStateString = getGamestateString(childState); 
                }
                else{
                    childStateString=getGamestateString(childState.reversed());
                }
                if(isVisited(childStateString)){
                    tmpValue = getVisitedValue(childStateString)*colour;  
                }
                
                else{
                        // if not in hashMap, compute value and store in hashMap
                    tmpValue=-negaMax(childState, depth-1,-beta,-alpha, -colour);
                    storeVisited(childStateString,tmpValue*colour);
                }
                //tmpValue=-negaMax(childState, depth-1,-beta,-alpha, -colour);
                bestMove=Math.max(bestMove, tmpValue);
                alpha=Math.max(alpha, tmpValue);
                if(alpha>=beta){
                    break;
                }
                
            }
            
            return bestMove;
            
        }
    }
    
    public int valueEnd(GameState gameState){
        if(gameState.isWhiteWin()){
            return valueWin;
        }
        else if(gameState.isRedWin()){
            return -valueWin;
        }
        else{
            return valueTie;
        }
        
    }
        public int value(GameState gameState, int colour){
            return number(gameState, Constants.CELL_WHITE, colour==1)
                    -number(gameState, Constants.CELL_RED, colour==-1);
        }
    public int number(GameState gameState, int colourPlayer, boolean play){
        int number=0;
        int longestJump=0;
        for( int row=0; row<boardSize; row++){
            for(int col=0;col<boardSize;col++){
                int cell=gameState.get(row, col);
                if(cell==colourPlayer && play){
                    number++;
                    longestJump=Math.max(longestJump, 
                            killerJump(gameState, row, col, colourPlayer));
                }
                else if(cell==colourPlayer){
                    number++;
                }
                else if(cell ==Constants.CELL_KING+colourPlayer){
                    number+=4;
                }
                
            }

        }
       return number +longestJump;
    }
    public int killerJump(GameState gameState, int pRow, 
            int pCol, int colourPlayer){
        int oponent= (colourPlayer==Constants.CELL_WHITE) 
                ? Constants.CELL_RED :Constants.CELL_WHITE;
        int cellLeft2;
        int cellRight2;
        int cellLeft;
        int cellRight;
        int jumpLeft=0;
        int jumpRight=0;
        int longestJump;
        GameState tmpState;
        cellLeft=gameState.get(pRow-1, pCol-1);
        cellLeft2=gameState.get(pRow-2, pCol-2);
        if(cellLeft==oponent && cellLeft2==Constants.CELL_EMPTY){
            tmpState=gameState;
            tmpState.set((pRow-1)*4+pCol-1,Constants.CELL_EMPTY);
            jumpLeft=1+killerJump(tmpState, pRow-2, pCol-2, colourPlayer);
        }
        cellRight=gameState.get(pRow-1, pCol+1);
        cellRight2=gameState.get(pRow-2, pCol+2);
        if(cellRight==oponent&& cellRight2==Constants.CELL_EMPTY){
            tmpState=gameState;
            tmpState.set((pRow-1)*4+pCol+1,Constants.CELL_EMPTY);
            jumpRight=1+killerJump(tmpState, pRow-2, pCol+2, colourPlayer);
        }
        longestJump=Math.max(jumpLeft,jumpRight);
                
        return longestJump;
    }
    public int takable(GameState gameState, int row, int col){
        return 1;
    }
    public String getGamestateString(GameState gamestate){
        String return_str = "";
        int nr_cells = boardSize*boardSize/2;
        for(int i = 0; i< nr_cells; i++){
            return_str += Integer.toString(gamestate.get(i));
        }
        return return_str;
    }

    public boolean isVisited(String gamestate_string){
        return visited_states.containsKey(gamestate_string);
    }

    public int getVisitedValue(String gamestate_string){
        // Only call this function if containsVisited has returned true first
        return visited_states.get(gamestate_string); 
    }

    public void storeVisited(String gamestate_string, int value){
        visited_states.put(gamestate_string, value);
        return;
    }
    
}