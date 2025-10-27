package ui;

public class text_ui {

    public static TextWindowConsole console = new TextWindowConsole("Custom Java Console - Arrow Key Input");

    private static String padIntToString(int number) {
        // The format specifier "%04d" means:
        // %d: Treat the argument as a decimal integer.
        // 4: Ensure the output width is at least 4 characters.
        // 0: Pad with leading zeros if the width is less than 4.
        return String.format("%04d", number);
    }

    public static int[][][] notes = new int[4][16][4];


    public static void render_notes(int set,int row){
        int[][] sequence = notes[set];
            for (int i=row; (i<16)&&(i-row < 4);i++){
                console.print(padIntToString(i));
                console.print(" | ");
                for (int note: sequence[i]) {
                    console.print(padIntToString(note));
                    console.print(" ");
                }
                console.println();

        }
    }
    public static void main(){
        notes[2][2][1] = 16;
        render_notes(2,2);
    }

    public void setup(TextWindowConsole console) {
        this.console = console;

    }
}
