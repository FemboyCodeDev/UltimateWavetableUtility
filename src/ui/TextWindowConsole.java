package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A custom console-like window that supports text output and captures arrow key inputs.
 * This class mimics System.out for text display within a GUI environment.
 */
public class TextWindowConsole extends JFrame {

    private final JTextArea outputArea;
    private final JScrollPane scrollPane;

    // A flag to simulate non-blocking input handling (like a game loop)
    private final AtomicBoolean running = new AtomicBoolean(true);


    public TimerTask space_task;
    private boolean space_toggle;

    public TextWindowConsole(String title) {
        super(title);

        // --- 1. Setup the GUI Components ---

        // Output Area: Where the text will appear
        outputArea = new JTextArea();
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        outputArea.setEditable(false); // Crucial: makes it an output log

        // Scroll Pane: Ensures the output log is scrollable
        scrollPane = new JScrollPane(outputArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        // Add components to the frame
        this.add(scrollPane, BorderLayout.CENTER);

        // --- 2. Configure the Frame ---

        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(800, 600);
        this.setLocationRelativeTo(null); // Center the window

        // --- 3. Add Key Input Handling ---

        // We add the KeyListener to the JTextArea since it is the component that
        // will usually have focus inside the window.
        outputArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                String keyName = "";

                // Identify the specific arrow key pressed
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_UP:
                        keyName = "Up Arrow";
                        break;
                    case KeyEvent.VK_DOWN:
                        keyName = "Down Arrow";
                        break;
                    case KeyEvent.VK_LEFT:
                        keyName = "Left Arrow";
                        break;
                    case KeyEvent.VK_RIGHT:
                        keyName = "Right Arrow";
                        break;
                    case KeyEvent.VK_SPACE:
                        keyName = "Space";

                        space_toggle = (space_toggle == false);
                        System.out.println(space_toggle);
                        if(space_toggle) {
                            space_task.cancel();
                        }else {
                            space_task.run();
                        }
                        break;
                    case KeyEvent.VK_ESCAPE:
                        // Example of a control key to stop the simulation
                        keyName = "Escape (Exiting Simulation)";
                        running.set(false);
                        break;
                    default:
                        // Ignore other keys for this example
                        return;
                }

                // Print the key press event to the console window
                println("[INPUT]: Key Pressed: " + keyName);
            }
        });

        // Ensure the JTextArea immediately gets focus to capture keys
        // Must be done *after* the frame is made visible
        this.setVisible(true);
        outputArea.requestFocusInWindow();
    }

    /**
     * Appends text to the console window without a trailing newline.
     * This method is thread-safe, ensuring GUI updates happen on the Event Dispatch Thread (EDT).
     * @param text The string to be written.
     */
    public void print(String text) {
        // Swing GUI updates must happen on the EDT. SwingUtilities.invokeLater ensures this.
        SwingUtilities.invokeLater(() -> {
            outputArea.append(text);

            // Auto-scroll to the bottom of the output area
            outputArea.setCaretPosition(outputArea.getDocument().getLength());
        });
    }

    /**
     * Appends a line of text to the console window, similar to System.out.println().
     * The text will be followed by a trailing newline character (\n).
     * This method is thread-safe, ensuring GUI updates happen on the Event Dispatch Thread (EDT).
     * @param text The string to be written.
     */
    public void println(String text) {
        // Swing GUI updates must happen on the EDT. SwingUtilities.invokeLater ensures this.
        SwingUtilities.invokeLater(() -> {
            outputArea.append(text + "\n");

            // Auto-scroll to the bottom of the output area
            outputArea.setCaretPosition(outputArea.getDocument().getLength());
        });
    }

    public void println() {
        println("");
    }

    public boolean isRunning() {
        return running.get();
    }

    /**
     * Main method for demonstrating the TextWindowConsole.
     */
    public static void main(String[] args) {
        // Use the Event Dispatch Thread to initialize the GUI
        SwingUtilities.invokeLater(() -> {
            TextWindowConsole console = new TextWindowConsole("Custom Java Console - Arrow Key Input");

            console.println("--- Welcome to the Custom Console ---");
            console.println("Type text in your main code, then run the program to see output here.");

            // Demonstration of print() vs println()
            console.print("This text uses the new ");
            console.print("print() method, so it ");
            console.println("stays on one line.");
            console.println("This line uses println() and starts on a new line.");

            console.println("Press the Up, Down, Left, or Right arrow keys to test input capture.");
            console.println("Press ESC to stop the background simulation.");
            console.println("-------------------------------------");

            // --- Demonstration of background process output ---

            // This simulates a background thread writing log data or game state updates
            new Thread(() -> {
                int count = 0;
                while (console.isRunning()) {
                    try {
                        Thread.sleep(2000); // Wait 2 seconds
                        console.println(String.format("[LOG] Simulation step %d complete.", ++count));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                console.println("Simulation stopped.");
            }).start();
        });
    }
}
