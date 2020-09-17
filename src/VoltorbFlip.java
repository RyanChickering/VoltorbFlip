/*
 * Constraints problem. 5x5 grid, each row information about how many points are available and how many bombs are in each
 * row. Based on that information, solve the puzzle.
 */

import java.lang.reflect.Array;
import java.util.*;

public class VoltorbFlip {

    private static class OverConstrainedException extends Exception{
        @Override
        public String getMessage() {
            return "Something went wrong, and a domain was over constrained";
        }
    }
    private static class Tile{
        ArrayList<Integer> domain;
        ArrayList<Integer> neighbors;
        int num;

        Tile(int tileNum){
            domain = new ArrayList<Integer>(4);
            for(int i = 0; i < 4; i++) {
                domain.add(i);
            }
            num = tileNum;
            initializeNeighbors(tileNum);

        }

        //Given the number of a tile, come up with all of the neighbors of it.
        /* 00 01 02 03 04
         * 05 06 07 08 09
         * 10 11 12 13 14
         * 15 16 17 18 19
         * 20 21 22 23 24
         */

        private void initializeNeighbors(int tileNum){
            neighbors = new ArrayList<Integer>(8);
            //Rows
            for(int i = (tileNum/5)*5; i < ((tileNum/5)*5 + 5); i++){
                if(i != tileNum) {
                    neighbors.add(i);
                }
            }
            //Columns
            for(int i = tileNum%5; i<25; i+=5){
                if(i != tileNum) {
                    neighbors.add(i);
                }
            }
        }
    }
    final static private int[][][] levelConfigs = {
            {{3,1,6},{0,3,6},{5,0,6},{2,2,6},{4,1,6}},
            {{1,3,7},{6,0,7},{3,2,7},{0,4,7},{5,1,7}},
            {{2,3,8},{7,0,8},{4,2,8},{1,4,8},{6,1,8}},
            {{3,3,8},{0,5,8},{8,0,10},{5,2,10},{2,4,10}},
            {{7,1,10},{4,3,10},{1,5,10},{9,0,10},{6,2,10}},
            {{3,4,10},{0,6,10},{8,1,10},{5,3,10},{2,5,10}},
            {{7,2,10},{4,4,10},{1,6,13},{9,1,13},{6,3,10}},
            {{0,7,10},{8,2,10},{5,4,10},{2,6,10},{7,3,10}}};
    //generates a random board for the level.
    //Store configurations in a 3 dimensional array. First dimension is the levels. Second dimension is a set,
    // third dimension is the actual values
    static Board newBoard(int level){
        if(level < 0 || level > 9){
            return null;
        }
        level = level-1;
        Random rand = new Random();
        int config = rand.nextInt(5);
        int x2s = levelConfigs[level][config][0];
        int x3s = levelConfigs[level][config][1];
        int voltorbs = levelConfigs[level][config][2];
        int x1s = 25 - x2s - x3s - voltorbs;
        int board[][] = new int[5][5];
        int count = 0;
        //Super scuffed way to fill out the board
        int tile;
        while(x2s > 0 || x3s > 0 || voltorbs > 0 || x1s >0) {
            tile = rand.nextInt(4);
            switch (tile) {
                case 1:
                    if (x1s > 0) {
                        x1s--;
                        board[count / 5][count % 5] = 1;
                        count++;
                        break;
                    }
                case 2:
                    if (x2s > 0) {
                        x2s--;
                        board[count / 5][count % 5] = 2;
                        count++;
                        break;
                    }
                case 0:
                    if (voltorbs > 0) {
                        voltorbs--;
                        board[count / 5][count % 5] = 0;
                        count++;
                        break;
                    }
                case 3:
                    if(x3s > 0) {
                        x3s--;
                        board[count / 5][count % 5] = 3;
                        count++;
                        break;
                    }
            }
        }
        return new Board(board, level);

    }

    //Method that uses the data given on the sides as constraint satisfaction input.
    //create domains for each of the squares. Use constraint propogation to eliminate items from the domain
    //create a list of neighbors for each variable
    //Create the constraints based on the values that we have from the sides of the board.
    //have a map where each index is a variable and the associated items are the neighbors.
    //What are the constraints on the domain? The values in a row add up to the sum. There are b amount of bombs per row.
    //How to determine what is still needed: sum value / 5-(bomb value). If that is 1, eveything is 1 or 0. If it is 0,
    //all in the row/col are 0. If it is between 1 and 2... it depends. If the value is 3, the number is 3.
    //if a neighbor has a set domain, subtract that value from the sum and 1 from 5-bomb value.
    static Board ac3(Board board) throws OverConstrainedException{
        Tile[] tiles = new Tile[25];
        Queue<Integer> unchecked = new LinkedList<Integer>();
        //Creates the 25 tiles of the board
        for(int i = 0; i < tiles.length; i++){
            tiles[i] = new Tile(i);
            unchecked.add(i);
        }
        boolean revised;
        //Runs until there are no more tiles with new constraints added
        while(unchecked.size() > 0){
            int toCheck = unchecked.poll();
            //If the value of the tile is not yet known
           if(tiles[toCheck].domain.size() > 1) {
               //check if the domain will be constrained given the values that we have
               revised = reviser(toCheck, board, tiles, true);
               //If the domain was constrained

               //Now check the columns
               revised = revised | reviser(toCheck, board, tiles, false);

               if(revised){
                   //Add the neighbors to the queue to be checked as they have a new constraint
                   for(int i = 0; i < tiles[toCheck].neighbors.size(); i++){
                       if(tiles[tiles[toCheck].neighbors.get(i)].domain.size() > 1){
                           unchecked.add(tiles[tiles[toCheck].neighbors.get(i)].num);
                       }
                   }
               }
           }
       }

        return backtrackSearch(tiles,board);
    }

    //Conditions that allow us to restrict the domain of a tile
    private static boolean revise(Tile[] tiles, int bombNum, int sum, int unknowns, int i, Board board) throws OverConstrainedException{
        boolean revise = false;
        if(tiles[i].domain.size() != 1) {
            //If all that we don't know are bombs, the unknowns are bombs
            if (bombNum >= unknowns) {
                revise = tiles[i].domain.removeIf(p -> (p != 0));
            }
            //if there are no bombs
            if (bombNum == 0) {
                revise = revise | tiles[i].domain.removeIf(p -> (p == 0));
            }
            //If there is only one unknown left, it is whatever value the remaining sum is
            if (unknowns == 1) {
                revise = revise | tiles[i].domain.removeIf(p -> (p != sum));
            }
            //Calculate the average value of the remaining unknown tiles
            double val = (sum + 0.0) / (unknowns - bombNum);
            //If the average value is 1, all the tiles are either 1 or 0
            if (val == 1 && bombNum == 0) {
                revise = revise | tiles[i].domain.removeIf(p -> (p != 1));
            } else if (val == 1) {
                revise = revise | tiles[i].domain.removeIf(p -> (p > 1));
            }
            //if the value exceeds a threshold, cannot be 3
            if ((unknowns - bombNum - 1 + 3) < val) {
                revise = revise | tiles[i].domain.removeIf(p -> (p == 3));
            }
            if (tiles[i].domain.size() == 0) {
                throw new OverConstrainedException();
            }
            if (tiles[i].domain.size() == 1) {
                switch(tiles[i].domain.get(0)){
                    case 2:
                        board.config[0]++;
                        break;
                    case 3:
                        board.config[1]++;
                        break;
                    case 0:
                        board.config[2]++;
                        break;
                        default:
                            break;
                }
                //check the current config against all configs of that level to make sure that ours is legitimate
                configCheck(board);
            }
        }
        return revise;
    }

    private static void configCheck(Board board) throws OverConstrainedException{
        for(int j = 0; j < 3; j++){
            if(board.config[0] <= levelConfigs[board.level][j][0] && (
                    board.config[1] <= levelConfigs[board.level][j][1] &&
                            board.config[2] <= levelConfigs[board.level][j][2])){
                return;

            }
        }
        throw new OverConstrainedException();
    }

    private static boolean reviser(int toCheck, Board board, Tile[] tiles, boolean rows) throws OverConstrainedException{
        int sum;
        int bombNum;
        int unknowns = 1;
        if(rows) {
            sum = board.rows[(toCheck / 5) * 2];
            bombNum = board.rows[(toCheck / 5) * 2 + 1];
            //For each neighbor with a domain greater than 1, increase the number of unknowns in the row.
            //If there are neighbors known to be bombs, decrease the bomb count
            for (int j = 0; j < tiles[toCheck].neighbors.size() / 2; j++) {
                if (tiles[tiles[toCheck].neighbors.get(j)].domain.size() > 1) {
                    unknowns++;
                } else if (tiles[tiles[toCheck].neighbors.get(j)].domain.get(0) == 0) {
                    bombNum--;
                } else{
                    sum -= tiles[tiles[toCheck].neighbors.get(j)].domain.get(0);
                }
            }
            //check if the domain will be constrained given the values that we have
        } else {
            sum = board.cols[(toCheck%5) * 2];
            bombNum = board.cols[(toCheck%5)*2 + 1];
            for(int j = 4; j < tiles[toCheck].neighbors.size(); j++){
                if(tiles[tiles[toCheck].neighbors.get(j)].domain.size() > 1){
                    unknowns++;
                } else if(tiles[tiles[toCheck].neighbors.get(j)].domain.get(0) == 0){
                    bombNum--;
                } else{
                    sum -= tiles[tiles[toCheck].neighbors.get(j)].domain.get(0);
                }
            }

        }
        return revise(tiles, bombNum, sum, unknowns, toCheck, board);
    }

    //Have to set up the neighbors array and stuff.
    //Each tile in the board has a domain and a set of neighbors. Tiles 0-24

    //For the backtrack search, try limiting domains until it either works or doesn't
    //Need to save the unbacktracked domains of tiles we guess on as we might need to restore them, also need to store the original sums
    //Assign a domain. Revise all of it's neighbors with the new information. Then revise neighbors of neighbors. If still have
    //tiles with domains greater than 1, pick a new one to constrain. If a domain becomes size 0, revert to before assignment.
    //if there is a conflict, undo the previous assignment
    private static Board backtrackSearch(Tile[] tiles, Board board){
        Queue<Tile> unassigned = new LinkedList<>();
        ArrayList<int[]> removals = new ArrayList<>();
        for(Tile tile: tiles){
            if(tile.domain.size() > 1){
                unassigned.add(tile);
            }
        }
        int[] origConfig = Arrays.copyOf(board.config, 3);
        while(unassigned.size() > 0){
            Tile var = unassigned.poll();
            if(var.domain.size() > 1) {
                Random rand = new Random();
                int domain = var.domain.get(rand.nextInt(var.domain.size()));
                ArrayList<Integer> origDomain = new ArrayList<>(var.domain);
                var.domain = new ArrayList<>(1);
                var.domain.add(domain);
                switch(domain){
                    case(2):
                        board.config[0]++;
                        break;
                    case(3):
                        board.config[1]++;
                        break;
                    case(0):
                        board.config[2]++;
                        break;
                    default:
                        break;
                }
                for (Integer neighbor : var.neighbors) {
                    ArrayList<Integer> prevDomain = new ArrayList<Integer>(tiles[neighbor].domain);
                    try {
                        configCheck(board);
                        reviser(neighbor, board, tiles, true);
                        reviser(neighbor, board, tiles, false);
                        if(prevDomain.size() != tiles[neighbor].domain.size()) {
                            for (Integer domainMem: prevDomain) {
                                if(!tiles[neighbor].domain.contains(domainMem)){
                                    removals.add(new int[] {neighbor, domainMem});
                                }
                            }
                        }
                    } catch (Exception e) {
                        if(prevDomain.size() != tiles[neighbor].domain.size()) {
                            for (Integer domainMem: prevDomain) {
                                if(!tiles[neighbor].domain.contains(domainMem)){
                                    removals.add(new int[] {neighbor, domainMem});
                                }
                            }
                        }
                        /*if(origDomain.size() > 1) {
                            origDomain.remove(var.domain.get(0));
                        }*/
                        var.domain = origDomain;
                        board.config = Arrays.copyOf(origConfig, 3);
                        unassigned.add(var);
                        restore(tiles, removals);
                        break;
                    }
                }
                if (var.domain.size() > 1) {
                    unassigned.add(var);
                }
            }
        }

        int[][] finalTiles = new int[5][5];
        for(int i = 0; i<tiles.length; i++){
            finalTiles[i/5][i%5] = tiles[i].domain.get(0);
        }

        return new Board(finalTiles, board.level);
    }

    private static void restore(Tile[] tiles, ArrayList<int[]> removals){
        for(int[] removal: removals){
            tiles[removal[0]].domain.add(removal[1]);
        }
        removals.clear();
    }

    private static boolean boardCompare(Board A, Board B){
        for(int i = 0; i < 5; i++){
            for(int j = 0; j < 5; j++){
                if(A.board[i][j] == 0 && B.board[i][j] != 0){
                    return false;
                }
            }
        }
        return true;
    }

    private static void twoBoardPrint(Board A, Board B){
        for(int row = 0; row < 5; row++){
            System.out.print("[");
            for(int col = 0; col < 5; col++){
                System.out.print(A.board[row][col]);
                if(col < 4){
                    System.out.print(", ");
                }
            }
            System.out.print("]     [");
            for(int col = 0; col < 5; col++){
                System.out.print(B.board[row][col]);
                if(col < 4){
                    System.out.print(", ");
                }
            }
            System.out.println("]");
        }
    }

    private static void tilePrint(Tile[] tiles){
        System.out.print("\n[");
        for(int i = 0; i < 25; i++){
            if(i%5 == 0 && i != 0){
                System.out.print("]\n[");
            }
            if(tiles[i].domain.size() == 1){
                System.out.print(tiles[i].domain.get(0));
            } else {
                System.out.print("*");
            }
            if((i+1)%5 != 0){
                System.out.print(", ");
            }
        }
        System.out.println("]\n");
    }

    public static void main(String[]args) throws OverConstrainedException{
        //Board board = new Board(new int[][]{{3, 0, 1, 1, 2},{1, 0, 0, 2, 2},{0, 0, 0, 1, 1},{1, 1, 1, 1, 1},{1, 1, 1, 1, 1}},
        //        0);

        Board board = newBoard(1);
        /*
        for(int i = 0; i < 10; i++){
            Random rand = new Random();
            Board A = newBoard(rand.nextInt(8)+1);
            Board B = ac3(A);
            if(!boardCompare(A, B)){
                twoBoardPrint(A, B);
            }
        }*/
        twoBoardPrint(board, ac3(board));
    }
}

