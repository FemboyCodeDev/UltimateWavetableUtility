package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * A custom console-like window that supports text output and captures arrow key inputs.
 * This class mimics System.out for text display within a GUI environment.
 */
public class TextWindowConsole extends JFrame {

    private final JTextArea outputArea;
    private final JScrollPane scrollPane;

    // A flag to simulate non-blocking input handling (like a game loop)
    private final AtomicBoolean running = new AtomicBoolean(true);

    // 🆕 New members for non-blocking task handling
    private final Timer timer;
    private TimerTask current_space_task; // The currently running task instance
    private Supplier<TimerTask> space_task_factory; // Function to create a NEW task instance
    private boolean space_toggle = false; // State: false=stopped, true=running

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

        // 🆕 Initialize the Timer for non-blocking execution
        this.timer = new Timer(true); // true makes it a daemon thread

        // --- 3. Add Key Input Handling ---

        outputArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                String keyName = "";

                // Identify the specific key pressed
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

                        // 🆕 Non-blocking space task logic
                        if (space_task_factory == null) {
                            println("[ERROR]: Space task factory not set. Cannot run task.");
                            return;
                        }

                        // Toggle the state
                        space_toggle = !space_toggle;

                        if (!space_toggle) { // Task was running, now stop
                            if (current_space_task != null) {
                                // Cancels the scheduled execution
                                current_space_task.cancel();
                            }
                            println("[INPUT]: Space pressed. Task STOPPED.");
                        } else { // Task was stopped, now start
                            // 1. Create a brand new task instance from the factory
                            current_space_task = space_task_factory.get();

                            // 2. Schedule the new task instance non-blockingly
                            // Example: Run every 500 milliseconds, starting immediately
                            // This runs the task on a separate thread (the Timer thread).
                            timer.schedule(current_space_task, 0, 500);

                            println("[INPUT]: Space pressed. Task STARTED (500ms interval).");
                        }
                        return; // Handled Space key

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
     * Sets the factory used to create a new TimerTask instance every time the
     * space key is pressed to start the task.
     * @param factory A function that returns a new TimerTask.
     */
    public void setSpaceTaskFactory(Supplier<TimerTask> factory) {
        this.space_task_factory = factory;
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

            // 🆕 Set the factory for the space task
            // This factory provides a *new* TimerTask instance every time it's called
            console.setSpaceTaskFactory(() -> new TimerTask() {
                private int counter = 0;
                @Override
                public void run() {
                    // This code runs on the Timer's thread, NOT the EDT.
                    // This is non-blocking to the GUI!
                    // Note: console.println() is still safe because it uses SwingUtilities.invokeLater() internally.
                    console.println("[SPACE TASK] Running non-blockingly! Count: " + ++counter);

                    if (counter >= 10) {
                        // Example: stop the task after 10 runs
                        console.println("[SPACE TASK] Auto-stopping after 10 runs.");
                        this.cancel();
                        // Note: The 'space_toggle' state in the JFrame won't update here
                        // unless you add logic to call the key handler to simulate a stop,
                        // or update the JFrame's state fields directly (more complex).
                    }
                }
            });

            console.println("Press SPACE to start/stop the non-blocking TimerTask (runs every 500ms).");
            console.println("-------------------------------------");

            // --- Demonstration of background process output (existing simulation log) ---
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