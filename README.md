# OSproject
# Hybrid Virtual Memory Manager Simulation

A web-based visual simulator demonstrating concepts of a hybrid virtual memory system, including Cache, RAM, Swap Space, priority-based page replacement algorithms, dirty bit handling, and thrashing detection, all presented with a modern, interactive UI.

## 🚀 Live Demo & Screenshot

[![Visit Website](https://img.shields.io/badge/Launch_Simulation-Deployed-red?style=for-the-badge&logo=firefox-browser)](https://hybridvmm.onrender.com)

![image](https://github.com/user-attachments/assets/ccc09618-09d6-4139-8c5a-6e246b17e5c7)

enhanced to 

![image](https://github.com/user-attachments/assets/82b9986a-3edb-4cd5-821f-b4261deeb529)


## Features 🚀

*   💾 **Memory Hierarchy Visualization:** Clearly displays processes residing in Cache, RAM (Main Memory), and Swap Space.
*   ⚙️ **Dynamic Configuration:** Set the size (number of frames/entries) for RAM, Swap, and Cache before starting or reset the simulation.
*   🔄 **Page Replacement Algorithms:** Select from multiple algorithms (FIFO, LRU, LFU, LIFO, MRU, Random) to handle RAM eviction.
*   👑 **Priority-Aware Eviction:** Processes are assigned a random priority (Low, Medium, High). Eviction algorithms primarily target the lowest priority pages first.
*   ✏️ **Dirty Bit Simulation:** Mark processes in RAM as 'dirty' (modified). Evicting a dirty page triggers a visual "Write-Back" animation with a simulated delay before moving to Swap.
*   ⚡ **Cache Simulation:** Processes accessed in RAM are moved to a simulated Cache (using LRU for cache eviction). Cache hits provide faster access.
*   🚨 **Thrashing Detection:** Monitors the recent page fault rate and displays a visual alert if it exceeds a defined threshold, indicating potential thrashing.
*   👆 **Interactive Process Blocks:**
    *   Click a block to simulate accessing that process.
    *   Right-click a block (with confirmation) to terminate the process entirely from the system.
    *   Hover over blocks for detailed tooltips (Process ID, Size, Priority, State, Access/Add Times).
*   📊 **Real-time Statistics:** Tracks and displays Cache Hits/Accesses, RAM Hits/Accesses, Page Faults/Swap Accesses, conceptual TLB Hits/Misses, Total Accesses, Hit Rate, and Fault Rate.
*   📜 **System Event Log:** Provides a detailed, timestamped log of all actions (allocation, access, eviction, write-back, errors, etc.).
*   🎬 **Visual Animations:** Smooth animations for block allocation, access highlights, state changes (dirty, write-back), termination, and movement between memory levels.
*   ⏱️ **Simulation Speed Control:** Adjust the speed of animations (Slow, Normal, Fast, Instant) for better observation or quicker results.
*   🎨 **Modern UI Theme:** Features a dark, "glassmorphism" inspired theme with clear visual distinction between memory areas.
*   📱 **Responsive Design:** Adapts layout for usability on different screen sizes (desktop, tablet, mobile).
*   ℹ️ **Informative 'About' Modal:** Explains the simulation's features and interactions.

## Running the Simulation 🏃‍♀️

This is a purely front-end application built with standard web technologies. No complex installation is required.

1.  **Download or Clone:**
    ```sh
    git clone https://github.com/aalok2006/OS-project
    cd OS-project
    ```
    Alternatively, download the project files as a ZIP archive.


## Usage & Interaction 🛠️

1.  **Configure (Optional):** Use the top input fields (RAM, Swap, Cache size) and click "Apply & Reset" to change the memory layout. This resets the simulation.
2.  **Select Algorithm:** Choose a page replacement algorithm from the dropdown menu. This algorithm is used when RAM is full and a process needs to be evicted (applied within the lowest priority group).
3.  **Simulation Speed:** Adjust the animation speed using the "Speed" dropdown.
4.  **Allocate Process:**
    *   Enter a specific Process ID (e.g., `P5`) in the input field, or leave it blank for a random available process.
    *   Click "Allocate". The process will be placed in RAM if space is available, or it will trigger an eviction from RAM (potentially moving the victim to Swap).
5.  **Access Process:**
    *   Enter a Process ID that *exists* in Cache, RAM, or Swap, or leave blank for a random existing process. Alternatively, click directly on a process block in the UI.
    *   Click "Access".
        *   Cache Hit: Highlights the block in Cache.
        *   RAM Hit: Highlights the block in RAM and moves it to Cache (evicting from Cache if full).
        *   Page Fault (Swap Hit): Triggers eviction from RAM (if needed), moves the process from Swap to RAM, and then to Cache. Log shows details.
6.  **Mark Dirty (Write):**
    *   Enter a Process ID currently in RAM.
    *   Click "Write/Dirty". The process block in RAM will be visually marked as dirty.
7.  **Manual Cache Add:**
    *   Enter a Process ID currently in RAM.
    *   Click "To Cache". The process is added to the cache (following cache eviction rules).
8.  **Clear Cache:** Click "Clear Cache" to remove all entries from the cache.
9.  **Reset Simulation:** Click "Reset Sim" to return to the initial empty state with current size configurations.
10. **Terminate Process:** Right-click on any process block (Cache, RAM, or Swap). A confirmation dialog will appear. If confirmed, the process is removed entirely.
11. **Observe:** Watch the blocks move, the stats update, and the log populate with events. Hover over blocks to see their details. Note the "Thrashing Alert" if page faults become too frequent.
12. **About:** Click the "About" button for a summary of features.

## Example Workflow 🚶‍♂️

1.  Open the `memory_manager.html` file in your browser.
2.  Keep default sizes (4 RAM, 4 Swap, 3 Cache) and select "LRU" algorithm.
3.  **Allocate P1:** Enter `P1`, click "Allocate". (P1 appears in RAM).
4.  **Allocate P2, P3, P4:** Allocate them similarly. (RAM is now full).
5.  **Access P1:** Enter `P1`, click "Access". (P1 highlights in RAM, then appears in Cache).
6.  **Access P2:** Enter `P2`, click "Access". (P2 highlights in RAM, then appears in Cache).
7.  **Allocate P5:** Enter `P5`, click "Allocate".
    *   Log shows RAM is full, initiating LRU eviction among lowest priority pages.
    *   Assume P3 is the victim (LRU, lowest priority). P3 moves from RAM to Swap (animation).
    *   P5 appears in RAM (animation).
8.  **Mark P2 Dirty:** Enter `P2`, click "Write/Dirty". (P2 block in RAM gets dirty indicator).
9.  **Allocate P6:** Enter `P6`, click "Allocate".
    *   Assume P4 is the LRU victim this time. P4 moves RAM -> Swap.
    *   P6 appears in RAM.
10. **Allocate P7:** Enter `P7`, click "Allocate".
    *   Assume P2 (the dirty one) is now the LRU victim.
    *   Log indicates write-back. P2 block pulses, simulates delay.
    *   P2 moves RAM -> Swap (animation).
    *   P7 appears in RAM.
11. **Access P3 (Page Fault):** Enter `P3`, click "Access".
    *   Log shows Page Fault.
    *   Assume P1 is now LRU victim in RAM (clean). P1 moves RAM -> Swap.
    *   P3 moves Swap -> RAM (animation).
    *   P3 is then added to Cache (animation).
12. **Terminate P5:** Right-click the P5 block (wherever it is), confirm. (P5 disappears with animation).
13. **Observe Stats:** Watch the Hit/Fault counts and rates change throughout the process.

## Technology Stack 💻

*   **HTML5:** Structure and content.
*   **CSS3:** Styling, layout (Flexbox), animations, and theming (CSS Variables).
*   **JavaScript (ES6+):** Core logic, DOM manipulation, event handling, algorithm implementation.

## Key Code Concepts ✨

*   **State Management:** Core simulation state (RAM/Swap contents, Cache map, dirty status, tracking data, stats) managed using global JavaScript variables and data structures (Arrays, Map, Set, Objects).
*   **Memory Representation:** `ram` and `swap` are arrays of process IDs. `cache` is a `Map` storing `{ data, lastAccess }`. `dirtyProcesses` is a `Set`.
*   **Algorithm Implementation:** Page replacement logic is encapsulated within the `evictPage` function, using a `switch` statement based on the selected algorithm and filtering candidates by priority.
*   **Priority Handling:** `getPriorityValue` function converts priority strings to numbers for comparison during eviction candidate selection.
*   **Dynamic Rendering:** `updateDisplay` function clears and redraws the memory sections based on the current state arrays/maps, creating block HTML using `createBlockHTML`.
*   **Event Handling:** Uses delegated event listeners on the `memoryGrid` container to handle clicks, right-clicks, and hovers on dynamically created blocks efficiently.
*   **Animation Orchestration:** Uses `async/await` with helper functions (`wait`, `getAnimationPromise`) and CSS classes to manage the timing and sequence of visual effects for highlights, fades, moves (`animateBlockMove`), and state changes.
*   **Thrashing Detection:** `checkThrashing` function calculates fault rate over a recent history (`accessHistory`) and toggles the UI indicator.
*   **Tooltip Logic:** `showTooltip` and `hideTooltip` dynamically generate and position tooltips based on block data and mouse position.
*   **Modularity:** Logic is broken down into functions for specific tasks (allocation, access, eviction, logging, updating display, handling animations, etc.).

## Future Enhancements / Todo 📝

*   📈 More advanced visualizations (e.g., timeline graph of memory access).
*   💾 Option to save/load simulation state using `localStorage`.
*   ⚙️ Add more Page Replacement Algorithms (e.g., Clock, Optimal).
*   🧩 Implement variable process sizes affecting allocation (currently informational).
*   📊 More detailed performance comparison metrics between algorithms.
*   📜 Allow scripting or batch processing of access patterns.
*   🎨 UI Theme customization options.

## Contribution 🤝

Feel free to fork this repository, submit issues, or propose pull requests with improvements or new features!

## License 📜

MIT License
