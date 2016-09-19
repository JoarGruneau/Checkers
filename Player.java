import java.util.*;

public class Player {
    int red=-1;
    int white=1;
    int valueWin=1000;
    int valueTie=0;
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
	pState.findPossibleMoves(lNextStates);

        if (lNextStates.size() == 0) {
            // Must play "pass" move if there are no other moves possible.
            return new GameState(pState, new Move());
        }
        if(pState.getNextPlayer()==Constants.CELL_RED){
            int bestValue=-valueWin;
            int value;
            GameState bestMove=lNextStates.firstElement();
            //System.err.println("new tern------------------------------------------------------");
            for(GameState childState: lNextStates){
                value=negaMax(childState, 6,1);
                if(value>bestValue){
                    bestValue=value;
                    bestMove=childState;
                }
            System.err.println(childState.toString(Constants.CELL_WHITE));
            //System.err.println(value);
            System.err.println(value(childState));
            }
            return bestMove;
            
        }
        else{
            Random random = new Random();
            return lNextStates.elementAt(random.nextInt(lNextStates.size()));
        }
    }
    public int negaMax(GameState gameState, int depth, int colour ){
        if(gameState.isEOG()){
            return valueEnd(gameState)*colour;
        }
        else if(depth==0){
            return value(gameState)*colour;
        }
        else{
            Vector<GameState> lNextStates = new Vector<>();
            gameState.findPossibleMoves(lNextStates);
            int bestMove=-10000;
            for(GameState childState:lNextStates){
                bestMove=Math.
                        max(bestMove, -negaMax(childState, depth-1,-colour));
            }
            return bestMove;
            
        }
    }
    
    public int valueEnd(GameState gameState){
        if(gameState.isRedWin()||gameState.isWhiteWin()){
            return valueWin;
        }
        else{
            return valueTie;
        }
        
    }
        public int value(GameState gameState){
            return number(gameState, Constants.CELL_WHITE)
                    -number(gameState, Constants.CELL_RED);
        }
    public int number(GameState gameState, int colourPlayer){
        int number=0;
        for( int row=0; row<8; row++){
            for(int col=0;col<8;col++){
                int cell=gameState.get(row, col);
                if(cell==colourPlayer){
                    number++;
                    //System.err.println(cell);
                }
                if(cell ==Constants.CELL_KING+colourPlayer){
                    number+=4;
                }
                
            }

        }
       return number;
    }
}
