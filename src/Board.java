class Board {
    int[] cols;
    int[] rows;
    int level;
    int[] config;
    int[][] board;

    //default constructor
    Board(){
        board = new int[5][5];
        cols = new int[10];
        rows = new int[10];
    }

    //specific constructor
    Board(int[][] board, int level){
        this.board = board;
        this.level = level;
        cols = new int[10];
        rows = new int[10];
        config = new int[] {0,0,0};
        calcVals();
    }

    /*
     * Need to caluclate the sums of the rows and the columns and how many voltorbs are in each
     */
    private void calcVals(){
        for(int i = 0; i < 5; i++){
            int cbombs = 0;
            int csum = 0;
            int rbombs = 0;
            int rsum = 0;
            for(int j = 0; j < 5; j++){
                if(board[i][j] == 0){
                    rbombs++;
                } else {
                    rsum += board[i][j];
                }
                if(board[j][i] == 0){
                    cbombs++;
                } else {
                    csum += board[j][i];
                }
            }
            cols[i*2] = csum;
            cols[(i*2)+1] = cbombs;
            rows[i*2] = rsum;
            rows[(i*2)+1] = rbombs;
        }
    }

    @Override
    public String toString(){
        StringBuilder stringBuilder = new StringBuilder();
        for(int i = 0; i < 5; i++){
            stringBuilder.append("[");
            for(int j = 0; j < 5; j++){
                stringBuilder.append(board[i][j]);
                if(j < 4) {
                    stringBuilder.append(", ");
                }
            }
            stringBuilder.append("] sum: ");
            stringBuilder.append(rows[i*2]);
            stringBuilder.append(", bombs: ");
            stringBuilder.append(rows[(i*2)+1]);
            stringBuilder.append("\n");
        }
        stringBuilder.append(" ");
        for(int i = 0; i < 5; i++){
            stringBuilder.append(cols[i*2]);
            if(i < 4) {
                stringBuilder.append(", ");
            }
        }
        stringBuilder.append("\n ");
        for(int i = 0; i < 5; i++){
            stringBuilder.append(cols[(i*2)+1]);
            if(i < 4) {
                stringBuilder.append(", ");
            }
        }
        stringBuilder.append("\n");
        return stringBuilder.toString();
    }
}
