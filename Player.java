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

        if (lNextStates.size() == 0) {
            // Must play "pass" move if there are no other moves possible.
            return new GameState(pState, new Move());
        }
        if(pState.getNextPlayer()==Constants.CELL_RED){
            int bestValue=-valueWin;
            int value;
            GameState bestMove=lNextStates.firstElement();
            for(GameState childState: lNextStates){
                value=negaMax(childState, 3,-1);
                if(value>bestValue){
                    bestValue=value;
                    bestMove=childState;
                }
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
            return 0;
        }
        else{
            Vector<GameState> lNextStates = new Vector<>();
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
}
