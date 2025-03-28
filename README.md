# OS-project
# Hybrid Virtual Memory Manager Simulation

A dynamic, web-based visual simulator demonstrating the principles of a hybrid virtual memory system, including Cache, RAM (Main Memory), and Swap Space, complete with various page replacement algorithms.

**(Optional: Add a GIF/Screenshot here showing the simulator in action)**
<!-- ![Simulator Demo](link_to_your_gif_or_screenshot.png) -->

## Features 🚀

*   🧠 **Memory Hierarchy Visualization:** Clearly displays processes residing in Cache (fastest), RAM, and Swap Space (disk simulation).
*   ⚙️ **Dynamic Configuration:** Easily adjust the sizes of RAM, Swap, and Cache via the UI (resets the simulation).
*   ➕ **Process Allocation:** Simulate adding new processes (pages) to memory. Handles RAM-full scenarios using selected eviction algorithms.
*   👆 **Process Access Simulation:** Simulate accessing processes. Tracks Cache Hits, RAM Hits, and Page Faults (requiring loading from Swap).
*   🔄 **Page Replacement Algorithms:** Implement and switch between multiple algorithms for RAM eviction:
    *   FIFO (First-In, First-Out)
    *   LRU (Least Recently Used)
    *   LFU (Least Frequently Used)
    *   LIFO (Last-In, First-Out)
    *   MRU (Most Recently Used)
    *   Random
*   ⚡ **Caching:** Includes a simple Cache layer (using LRU for its own eviction) to demonstrate faster access for recently used items from RAM.
*   💾 **Swapping:** Evicted pages from RAM are moved to Swap Space if available, otherwise discarded.
*   ❌ **Process Termination:** Right-click any process block to terminate it and remove it from all memory levels.
*   📊 **Usage Tracking:** Displays current usage counts for each memory level. Hover over blocks for details (size, frequency, access times).
*   📜 **Real-time System Log:** Provides detailed, timestamped feedback on all operations, hits, misses, faults, evictions, and errors.
*   ℹ️ **Informational Modal:** Includes an "About" section explaining the concepts and simulation features.
*   🎨 **Modern UI:** Clean, themed interface built with HTML, CSS (including CSS Variables), and vanilla JavaScript.

## Running the Simulation 🏃‍♀️

This is a purely front-end application built with standard web technologies. No complex installation is required.

1.  **Download or Clone:**
    ```sh
    git clone https://github.com/aalok2006/OS-project
    cd OS-project
    ```
    Alternatively, download the project files as a ZIP archive.

2.  **Open the HTML File:**
    Simply open the `index.html` (or the main HTML file's name) in your preferred web browser (Chrome, Firefox, Edge, Safari recommended).

## Usage & Interaction 🛠️

1.  **(Optional) Configure:** Adjust RAM, Swap, and Cache sizes using the input fields and click "Apply Config & Reset".
2.  **Select Algorithm:** Choose a page replacement algorithm from the dropdown menu.
3.  **Enter Process ID (Optional):** Type a specific Process ID (e.g., `P5`) into the input field for allocation or access. If left blank for allocation/access, a random available/existing process might be chosen.
4.  **Use Control Buttons:**
    *   `Allocate Process`: Adds the specified (or random) process to memory, handling eviction if RAM is full.
    *   `Access Process`: Simulates accessing the specified (or random) process, triggering cache/RAM hits or page faults.
    *   `Add to Cache`: Manually attempts to add the specified process (if in RAM) to the cache.
    *   `Clear Cache`: Empties the cache.
    *   `Reset Simulation`: Resets memory and logs to the initial state based on current configuration.
    *   `About`: Opens the informational modal.
5.  **Interact with Blocks:**
    *   **Left-Click:** Equivalent to clicking the "Access Process" button for that specific process.
    *   **Right-Click:** Terminates the process, removing it from Cache, RAM, and Swap.
    *   **Hover:** Displays a tooltip with details about the process (Size, Frequency, Timestamps).
6.  **Monitor the Log:** Observe the sequence of events, hits, misses, and algorithm decisions in the System Log panel.

## Example Workflow 🚶‍♂️

1.  Keep default sizes (RAM=4, Swap=4, Cache=3) and select `LRU` algorithm.
2.  Allocate `P1`, `P2`, `P3`, `P4`. Observe they fill the RAM.
3.  Allocate `P5`. Observe RAM is full. `P1` (the LRU) should be evicted to Swap. `P5` takes its place in RAM.
4.  Access `P2`. Observe RAM Hit. `P2` should now be in the Cache.
5.  Access `P1`. Observe Page Fault. `P1` is loaded from Swap into RAM. Another process (likely `P3` if `P2, P4, P5` were accessed more recently or added later) is evicted from RAM to Swap. `P1` is added to Cache.
6.  Access `P2` again. Observe Cache Hit.
7.  Right-click on `P4` in RAM/Cache/Swap. Observe it disappears from all memory levels and the log confirms termination.

## Technology Stack 💻

*   **HTML5:** Structure and content.
*   **CSS3:** Styling, layout (Flexbox), animations, and theming (CSS Variables).
*   **JavaScript (ES6+):** Core logic, DOM manipulation, event handling, algorithm implementation.

## Key Code Concepts ✨

*   **Memory Representation:** Arrays (`ram`, `swap`) and a `Map` (`cache`) store the process IDs present in each level.
*   **State Tracking:** Objects (`accessFrequency`, `ramAddTime`) and an array (`accessOrder`) store metadata needed for replacement algorithms (LRU, LFU, FIFO).
*   **Page Replacement Logic:** The `evictPage()` function implements the core logic using a `switch` statement based on the selected algorithm.
*   **Event Handling:** Functions like `allocateProcess()`, `accessProcess()`, `handlePageFault()` manage the core simulation steps.
*   **DOM Manipulation:** `updateDisplay()` function dynamically creates/updates the visual blocks in the UI based on the current state of `ram`, `swap`, and `cache`.
*   **Logging:** `logEvent()` function formats and appends messages to the system log panel with appropriate styling.

## Future Enhancements / Todo 📝

*   📈 Add more detailed statistics (Hit/Miss Ratios, Page Fault Rate).
*   🎨 Visualize process sizes within blocks.
*   ⏱️ Add simulation speed control (step-by-step or variable speed).
*   💾 Option to save/load simulation state.
*   🔄 Implement additional algorithms (e.g., Clock, Second Chance).
*   📑 Support for multiple independent processes/address spaces (more advanced).

## Contribution 🤝

Feel free to fork this repository, submit issues, or propose pull requests with improvements or new features!

## License 📜

MIT License
